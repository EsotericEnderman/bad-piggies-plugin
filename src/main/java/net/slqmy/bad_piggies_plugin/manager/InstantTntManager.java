package net.slqmy.bad_piggies_plugin.manager;

import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent;
import com.google.common.base.Predicate;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.ProtectionEnchantment;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.slqmy.bad_piggies_plugin.BadPiggiesPlugin;
import net.slqmy.bad_piggies_plugin.util.BlockUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_20_R3.util.CraftVector;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class InstantTntManager {

    private static final int CHUNK_CACHE_SHIFT = 2;
    private static final int CHUNK_CACHE_WIDTH = 1 << CHUNK_CACHE_SHIFT;
    private static final int BLOCK_EXPLOSION_CACHE_SHIFT = 3;
    private static final int BLOCK_EXPLOSION_CACHE_WIDTH = 1 << BLOCK_EXPLOSION_CACHE_SHIFT;
    private static final int BLOCK_EXPLOSION_CACHE_MASK = (1 << BLOCK_EXPLOSION_CACHE_SHIFT) - 1;
    private static final int CHUNK_CACHE_MASK = (1 << CHUNK_CACHE_SHIFT) - 1;

    Class<? extends Explosion> explosionClass = Explosion.class;

    private static Field BLOCK_CACHE_FIELD;
    private static Field CHUNK_POS_CACHE_FIELD;
    private static Field CHUNK_CACHE_FIELD;

    private static Field CACHED_RAYS_FIELD;

    {
        try {
            BLOCK_CACHE_FIELD = explosionClass.getDeclaredField("blockCache");
            CHUNK_POS_CACHE_FIELD = explosionClass.getDeclaredField("chunkPosCache");
            CHUNK_CACHE_FIELD = explosionClass.getDeclaredField("chunkCache");

            CACHED_RAYS_FIELD = explosionClass.getDeclaredField("CACHED_RAYS");

            BLOCK_CACHE_FIELD.setAccessible(true);
            CHUNK_POS_CACHE_FIELD.setAccessible(true);
            CHUNK_CACHE_FIELD.setAccessible(true);
            CACHED_RAYS_FIELD.setAccessible(true);
        } catch (NoSuchFieldException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static final float ZERO_RESISTANCE = -0.3F;

    private final BadPiggiesPlugin plugin;

    private final File instantTntDataFile;

    private final List<Vector> instantTntBlocks = new ArrayList<>();

    public InstantTntManager(@NotNull BadPiggiesPlugin plugin) {
        this.plugin = plugin;
        instantTntDataFile = new File(plugin.getDataFolder(), "data/instant-tnt-blocks.json");

        loadInstantTntData();
    }

    public void addInstantTnt(Vector blockCoordinates) {
        instantTntBlocks.add(blockCoordinates);
    }

    public void addInstantTnt(@NotNull Block block) {
        addInstantTnt(block.getLocation().toVector());
    }

    public void removeInstantTnt(Vector blockCoordinates) {
        instantTntBlocks.remove(blockCoordinates);
    }

    public void removeInstantTnt(@NotNull Block block) {
        removeInstantTnt(block.getLocation().toVector());
    }

    public boolean isInstantTnt(Vector blockCoordinates) {
        return instantTntBlocks.contains(blockCoordinates);
    }

    public boolean isInstantTnt(@NotNull Block block) {
        return isInstantTnt(block.getLocation().toVector());
    }

    public boolean isInstantTnt(@NotNull ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();

        PersistentDataContainer dataContainer = meta.getPersistentDataContainer();

        return Boolean.TRUE.equals(dataContainer.get(plugin.getInstantTntKey(), PersistentDataType.BOOLEAN));
    }

    public ItemStack getInstantTntItem() {
        ItemStack instantTnt = new ItemStack(Material.TNT);

        ItemMeta meta = instantTnt.getItemMeta();

        PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
        NamespacedKey instantTntKey = plugin.getInstantTntKey();
        dataContainer.set(instantTntKey, PersistentDataType.BOOLEAN, true);

        instantTnt.setItemMeta(meta);

        return instantTnt;
    }

    public boolean shouldInstantTntDetonate(Block instantTnt, @NotNull Entity cause, @NotNull Location locationOverride) {
        Location blockCenterLocation = BlockUtil.getBlockCenterLocation(instantTnt);

        double tntX = blockCenterLocation.getX();
        double tntY = blockCenterLocation.getY();
        double tntZ = blockCenterLocation.getZ();

        double minTntY = tntY - 0.5D;
        double maxTntY = tntY + 0.5D;

        double minTntX = tntX - 0.5D;
        double maxTntX = tntX + 0.5D;

        double minTntZ = tntZ - 0.5D;
        double maxTntZ = tntZ + 0.5D;

        Vector entityVelocity = plugin.getPlayerVelocityManager().getVelocity(cause);

        double velocityX = entityVelocity.getX();
        double velocityY = entityVelocity.getY();
        double velocityZ = entityVelocity.getZ();

        double significantValue = 0.0D;

        Vector locationDifference = locationOverride.clone().subtract(cause.getLocation()).toVector();

        BoundingBox boundingBox = cause.getBoundingBox();

        double minEntityY = boundingBox.getMinY() + locationDifference.getY();
        double maxEntityY = boundingBox.getMaxY() + locationDifference.getY();

        double maxEntityX = boundingBox.getMaxX() + locationDifference.getX();
        double minEntityX = boundingBox.getMinX() + locationDifference.getX();

        double maxEntityZ = boundingBox.getMaxZ() + locationDifference.getZ();
        double minEntityZ = boundingBox.getMinZ() + locationDifference.getZ();

        if (maxEntityY <= minTntY) {
            significantValue = velocityY;
        } else if (minEntityY >= maxTntY) {
            significantValue = -velocityY;
        } else if (maxEntityX <= minTntX) {
            significantValue = velocityX;
        } else if (minEntityX >= maxTntX) {
            significantValue = -velocityX;
        } else if (maxEntityZ <= minTntZ) {
            significantValue = velocityZ;
        } else if (minEntityZ >= maxTntZ) {
            significantValue = -velocityZ;
        }

        return significantValue > plugin.getConfig().getDouble("features.instant-tnt.minimum-collision-detonation-speed") / 20.0D;
    }

    public boolean shouldInstantTntDetonate(Block instantTnt, @NotNull Entity cause) {
        return shouldInstantTntDetonate(instantTnt, cause, cause.getLocation());
    }

    private void detonateInstantTnt(@NotNull Block instantTnt, @Nullable Entity cause) {
        instantTnt.setType(Material.AIR);

        World world = instantTnt.getWorld();

        YamlConfiguration configuration = (YamlConfiguration) plugin.getConfig();

        ConfigurationSection explosionSettings = configuration.getConfigurationSection("features.instant-tnt.explosion");
        assert explosionSettings != null;

        double instantTntExplosionPower = explosionSettings.getDouble("power");
        boolean instantTntBreaksBlocks = explosionSettings.getBoolean("breaks-blocks");
        boolean instantTntSetsFire = explosionSettings.getBoolean("sets-fire");

        Location centerLocation = BlockUtil.getBlockCenterLocation(instantTnt);

        double x = centerLocation.getX();
        double y = centerLocation.getY();
        double z = centerLocation.getZ();

        CraftWorld craftWorld = (CraftWorld) world;

        ServerLevel nmsWorld = craftWorld.getHandle();

        Level.ExplosionInteraction explosionSourceType = instantTntBreaksBlocks ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE;

        Explosion.BlockInteraction explosionEffect;

        if (explosionSourceType == Level.ExplosionInteraction.NONE) {
            explosionEffect = Explosion.BlockInteraction.KEEP;
        } else {
            explosionEffect =
                    nmsWorld.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                    ? getDestroyType(nmsWorld)
                    : Explosion.BlockInteraction.KEEP;
        }

        net.minecraft.world.entity.Entity nmsCause = cause != null ? ((CraftEntity) cause).getHandle() : null;

        Explosion explosion = new Explosion(
                nmsWorld,
                nmsCause,
                Explosion.getDefaultDamageSource(nmsWorld, nmsCause),
                null,
                x,
                y,
                z,
                (float) instantTntExplosionPower,
                instantTntSetsFire,
                explosionEffect,
                ParticleTypes.EXPLOSION,
                ParticleTypes.EXPLOSION_EMITTER,
                SoundEvents.GENERIC_EXPLODE
        );

        explode(explosion, nmsWorld);
        explosion.finalizeExplosion(false);

        if (!explosion.interactsWithBlocks()) {
            explosion.clearToBlow();
        }

        Iterator<ServerPlayer> iterator = nmsWorld.players().iterator();

        while (iterator.hasNext()) {
            ServerPlayer player = iterator.next();

            if (player.distanceToSqr(x, y, z) < 4096.0D) {
                player.connection.send(
                        new ClientboundExplodePacket(
                                x, y, z,
                                (float) instantTntExplosionPower,
                                explosion.getToBlow(),
                                explosion.getHitPlayers().get(player),
                                explosion.getBlockInteraction(),
                                explosion.getSmallExplosionParticles(),
                                explosion.getLargeExplosionParticles(),
                                explosion.getExplosionSound()
                        )
                );
            }
        }

        removeInstantTnt(instantTnt);
    }

    private Explosion.BlockInteraction getDestroyType(@NotNull ServerLevel nmsWorld) {
        return nmsWorld.getGameRules().getBoolean(GameRules.RULE_MOB_EXPLOSION_DROP_DECAY)
                ? Explosion.BlockInteraction.DESTROY_WITH_DECAY
                : Explosion.BlockInteraction.DESTROY;
    }

    private void explode(@NotNull Explosion explosion, ServerLevel nmsWorld) {
        Vec3 explosionCenter = explosion.center();

        double explosionCenterX = explosionCenter.x;
        double explosionCenterY = explosionCenter.y;
        double explosionCenterZ = explosionCenter.z;

        float explosionRadius = explosion.radius();

        net.minecraft.world.entity.Entity explosionSource = explosion.source;

        ExplosionDamageCalculator damageCalculator = new EntityBasedExplosionDamageCalculator(explosionSource);
        DamageSource damageSource = Explosion.getDefaultDamageSource(nmsWorld, explosionSource);

        if (explosionRadius < 0.1F) {
            return;
        }

        nmsWorld.gameEvent(
                explosionSource,
                GameEvent.EXPLODE,
                explosion.center()
        );

        try {
            // Paper start - optimise explosions
            BLOCK_CACHE_FIELD.set(explosion, new Long2ObjectOpenHashMap<>());

            int arraySize = CHUNK_CACHE_WIDTH * CHUNK_CACHE_WIDTH;
            long[] chunkPosCache = new long[arraySize];
            Arrays.fill(chunkPosCache, ChunkPos.INVALID_CHUNK_POS);
            CHUNK_POS_CACHE_FIELD.set(explosion, chunkPosCache);

            CHUNK_CACHE_FIELD.set(explosion, new LevelChunk[arraySize]);
        } catch (IllegalAccessException exception) {
            throw new RuntimeException(exception);
        }

        Explosion.ExplosionBlockCache[] blockCache = new Explosion.ExplosionBlockCache[
                BLOCK_EXPLOSION_CACHE_WIDTH * BLOCK_EXPLOSION_CACHE_WIDTH * BLOCK_EXPLOSION_CACHE_WIDTH
        ];

        // use initial cache value that is most likely to be used: the source position

        final Explosion.ExplosionBlockCache initialCache;

        final int blockX = Mth.floor(explosionCenterX);
        final int blockY = Mth.floor(explosionCenterY);
        final int blockZ = Mth.floor(explosionCenterZ);

        final long key = BlockPos.asLong(blockX, blockY, blockZ);

        initialCache = getOrCacheExplosionBlock(
                explosion,
                damageCalculator,
                nmsWorld,
                blockX, blockY, blockZ,
                key,
                true
        );

        /*
        Only ~1/3rd of the loop iterations in vanilla will result in a ray, as it is iterating the perimeter of
        a 16x16x16 cube.
        We can cache the rays and their normals as well, so that we eliminate the excess iterations / checks and
        calculations in one go.
        Additional aggressive caching of block retrieval is very significant, as at low power (minX.e tnt) most
        block retrievals are not unique
        */

        double[] CACHED_RAYS;

        try {
            CACHED_RAYS = (double[]) CACHED_RAYS_FIELD.get(null);
        } catch (IllegalAccessException exception) {
            throw new RuntimeException(exception);
        }

        for (int rayNumber = 0, length = CACHED_RAYS.length; rayNumber < length;) {
            Explosion.ExplosionBlockCache cachedBlock = initialCache;

            double rayA = CACHED_RAYS[rayNumber];
            double rayB = CACHED_RAYS[rayNumber + 1];
            double rayC = CACHED_RAYS[rayNumber + 2];
            rayNumber += 3;

            // Paper end - optimise explosions
            double centerX = explosionCenterX;
            double centerY = explosionCenterY;
            double centerZ = explosionCenterZ;

            float randomRadiusFactor = 0.7F;
            float randomRadiusIncreaseRange = 0.6F;
            float randomRadiusDecrement = 0.22500001F;

            for (
                    float randomRadius = explosionRadius * (randomRadiusFactor + nmsWorld.random.nextFloat() * randomRadiusIncreaseRange);
                    randomRadius > 0.0F;
                    randomRadius -= randomRadiusDecrement
            ) {
                // Paper start - optimise explosions
                final int floorBlockX = Mth.floor(centerX);
                final int floorBlockY = Mth.floor(centerY);
                final int floorBlockZ = Mth.floor(centerZ);

                final long newKey = BlockPos.asLong(floorBlockX, floorBlockY, floorBlockZ);

                if (cachedBlock.key != newKey) {
                    final int cacheKey = (floorBlockX & BLOCK_EXPLOSION_CACHE_MASK)
                            | (floorBlockY & BLOCK_EXPLOSION_CACHE_MASK) << (BLOCK_EXPLOSION_CACHE_SHIFT)
                            | (floorBlockZ & BLOCK_EXPLOSION_CACHE_MASK) << (BLOCK_EXPLOSION_CACHE_SHIFT + BLOCK_EXPLOSION_CACHE_SHIFT);

                    cachedBlock = blockCache[cacheKey];

                    if (cachedBlock == null || cachedBlock.key != newKey) {
                        blockCache[cacheKey] = cachedBlock = getOrCacheExplosionBlock(
                                explosion,
                                damageCalculator,
                                nmsWorld,
                                floorBlockX, floorBlockY, floorBlockZ, newKey,
                                true
                        );
                    }
                }

                if (cachedBlock.outOfWorld) {
                    break;
                }

                BlockState blockData = cachedBlock.blockState;
                // Paper end - optimise explosions

                if (!blockData.isDestroyable()) {
                    continue;
                }; // Paper
                // Paper - optimise explosions

                randomRadius -= cachedBlock.resistance; // Paper - optimise explosions

                centerX += rayA; // Paper - optimise explosions
                centerY += rayB; // Paper - optimise explosions
                centerZ += rayC; // Paper - optimise explosions
            }
        }

        explosion.getToBlow();
        float explosionDiameter = explosionRadius * 2.0F;

        int minX = Mth.floor(explosionCenterX - (double) explosionDiameter - 1.0D);
        int maxX = Mth.floor(explosionCenterX + (double) explosionDiameter + 1.0D);

        int minY = Mth.floor(explosionCenterY - (double) explosionDiameter - 1.0D);
        int maxY = Mth.floor(explosionCenterY + (double) explosionDiameter + 1.0D);

        int minZ = Mth.floor(explosionCenterZ - (double) explosionDiameter - 1.0D);
        int maxZ = Mth.floor(explosionCenterZ + (double) explosionDiameter + 1.0D);

        List<net.minecraft.world.entity.Entity> list = nmsWorld.getEntities(
                (net.minecraft.world.entity.Entity) null,
                new AABB(minX, minY, minZ, maxX, maxY, maxZ),
                (Predicate<net.minecraft.world.entity.Entity>) entity -> entity.isAlive() && !entity.isSpectator()
        ); // Paper - Fix lag from explosions processing dead entities

        Iterator<net.minecraft.world.entity.Entity> iterator = list.iterator();

        final BlockPos.MutableBlockPos blockPosition = new BlockPos.MutableBlockPos(); // Paper - optimise explosions

        while (iterator.hasNext()) {
            net.minecraft.world.entity.Entity entity = iterator.next();

            if (!entity.ignoreExplosion(explosion)) {
                double diameterDistance = Math.sqrt(entity.distanceToSqr(explosionCenter)) / (double) explosionDiameter;

                if (diameterDistance <= 1.0D) {
                    double dx = entity.getX() - explosionCenterX;
                    double dy = (entity instanceof PrimedTnt ? entity.getY() : entity.getEyeY()) - explosionCenterY;
                    double dz = entity.getZ() - explosionCenterZ;
                    double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

                    if (distance != 0.0D) {
                        dx /= distance;
                        dy /= distance;
                        dz /= distance;

                        if (damageCalculator.shouldDamageEntity(explosion, entity)) {
                            /*
                             CraftBukkit start
                             Special case ender dragon only give knockback if no damage is cancelled
                             Thinks to note:
                             - Setting a velocity to a ComplexEntityPart is ignored (and therefore not needed)
                             - Damaging ComplexEntityPart while forward the damage to EntityEnderDragon
                             - Damaging EntityEnderDragon does nothing
                             - EntityEnderDragon hit box always covers the other parts and is therefore always present
                            */

                            if (entity instanceof EnderDragonPart) {
                                continue;
                            }

                            entity.lastDamageCancelled = false;

                            if (entity instanceof EnderDragon) {
                                for (EnderDragonPart entityComplexPart : ((EnderDragon) entity).subEntities) {
                                    // Calculate damage separately for each EntityComplexPart
                                    if (list.contains(entityComplexPart)) {
                                        entityComplexPart.hurt(
                                                damageSource,
                                                damageCalculator.getEntityDamageAmount(
                                                        explosion,
                                                        entityComplexPart,
                                                        getSeenFraction(
                                                                explosion,
                                                                damageCalculator,
                                                                nmsWorld,
                                                                explosionCenter,
                                                                entityComplexPart,
                                                                blockCache,
                                                                blockPosition
                                                        )
                                                )
                                        ); // Paper - actually optimise explosions and use the right entity to calculate the damage
                                    }
                                }
                            } else {
                                entity.hurt(
                                        damageSource,
                                        damageCalculator.getEntityDamageAmount(
                                                explosion,
                                                entity,
                                                getSeenFraction(
                                                        explosion,
                                                        damageCalculator,
                                                        nmsWorld,
                                                        explosionCenter,
                                                        entity,
                                                        blockCache,
                                                        blockPosition
                                                )
                                        )
                                ); // Paper - actually optimise explosions
                            }

                            if (entity.lastDamageCancelled) { // SPIGOT-5339, SPIGOT-6252, SPIGOT-6777: Skip entity if damage event was cancelled
                                continue;
                            }
                            // CraftBukkit end
                        }

                        double blockDensityFactor = (1.0D - diameterDistance) * getBlockDensity(
                                explosion,
                                damageCalculator,
                                nmsWorld,
                                explosionCenter,
                                entity,
                                blockCache,
                                blockPosition
                        ); // Paper - Optimize explosion

                        double knockBackDecrease;

                        if (entity instanceof LivingEntity livingEntity) {
                            knockBackDecrease = entity instanceof Player && nmsWorld.paperConfig().environment.disableExplosionKnockback
                                    ? 0
                                    : ProtectionEnchantment.getExplosionKnockbackAfterDampener(livingEntity, blockDensityFactor); // Paper - Option to disable explosion knockback
                        } else {
                            knockBackDecrease = blockDensityFactor;
                        }

                        dx *= knockBackDecrease;
                        dy *= knockBackDecrease;
                        dz *= knockBackDecrease;

                        Vec3 acceleration = new Vec3(dx, dy, dz);

                        // CraftBukkit start - Call EntityKnockbackEvent
                        if (entity instanceof LivingEntity livingEntity) {
                            Vector finalAcceleration = new Vector(acceleration.x, acceleration.y, acceleration.z);

                            // Paper start - call EntityKnockbackByEntityEvent for explosions
                            acceleration = new Vec3(finalAcceleration.getX(), finalAcceleration.getY(), finalAcceleration.getZ()).subtract(entity.getDeltaMovement()); // changes on this line fix a bug where acceleration wasn't reassigned with the "change", but instead the final deltaMovement

                            final Entity hitBy = damageSource.getEntity() != null
                                    ? damageSource.getEntity().getBukkitEntity()
                                    : explosionSource == null ? null : explosion.source.getBukkitEntity();

                            entity.getDeltaMovement().add(acceleration);

                            acceleration = new Vec3(
                                    finalAcceleration.getX(),
                                    finalAcceleration.getY(),
                                    finalAcceleration.getZ()
                            ).subtract(entity.getDeltaMovement()); // changes on this line fix a bug where vec3d1 wasn't reassigned with the "change", but instead the final deltaMovement

                            EntityKnockbackByEntityEvent paperEvent = new EntityKnockbackByEntityEvent(
                                    livingEntity.getBukkitLivingEntity(),
                                    hitBy,
                                    (float) knockBackDecrease,
                                    CraftVector.toBukkit(acceleration)
                            );

                            if (!paperEvent.callEvent()) {
                                continue;
                            }

                            acceleration = CraftVector.toNMS(paperEvent.getAcceleration());
                            // Paper end - call EntityKnockbackByEntityEvent for explosions
                        }

                        // CraftBukkit end
                        entity.setDeltaMovement(entity.getDeltaMovement().add(acceleration));

                        if (entity instanceof Player player) {
                            if (!player.isSpectator() && (!player.isCreative() || !player.getAbilities().flying) && !nmsWorld.paperConfig().environment.disableExplosionKnockback) { // Paper - Option to disable explosion knockback
                                explosion.getHitPlayers().put(player, acceleration);
                            }
                        }
                    }
                }
            }
        }

        try {
            BLOCK_CACHE_FIELD.set(explosion, null);
            CHUNK_POS_CACHE_FIELD.set(explosion, null);
            CHUNK_CACHE_FIELD.set(explosion, null);
        } catch (IllegalAccessException exception) {
            throw new RuntimeException(exception);
        }
    }

    private float getBlockDensity(Explosion explosion, ExplosionDamageCalculator damageCalculator, @NotNull ServerLevel nmsWorld, Vec3 vec3d, net.minecraft.world.entity.Entity entity, Explosion.ExplosionBlockCache[] blockCache, BlockPos.MutableBlockPos blockPos) { // Paper - optimise explosions
        if (!nmsWorld.paperConfig().environment.optimizeExplosions) {
            return getSeenFraction(explosion, damageCalculator, nmsWorld, vec3d, entity, blockCache, blockPos); // Paper - optimise explosions
        }

        Class<?> explosionCacheKeyClass = Arrays.stream(explosionClass.getDeclaredClasses()).filter((containedClass) -> containedClass.getSimpleName().equals("Explosion.CacheKey")).toList().get(0);

        Object key;

        try {
            Constructor<?> explosionCacheKeyConstructor = explosionCacheKeyClass.getConstructor(Explosion.class, AABB.class);
            key = explosionCacheKeyConstructor.newInstance(explosion, entity.getBoundingBox());
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException exception) {
            throw new RuntimeException(exception);
        }

        Float blockDensity = nmsWorld.explosionDensityCache.get(key);

        if (blockDensity == null) {
            blockDensity = getSeenFraction(explosion, damageCalculator, nmsWorld, vec3d, entity, blockCache, blockPos); // Paper - optimise explosions;

            Class<?> cacheClass = nmsWorld.explosionDensityCache.getClass();

            try {
                Method put = cacheClass.getDeclaredMethod("put", Object.class, Object.class);
                put.invoke(nmsWorld.explosionDensityCache, key, blockDensity);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
                throw new RuntimeException(exception);
            }
        }

        return blockDensity;
    }

    private float getSeenFraction(
            Explosion explosion,
            ExplosionDamageCalculator damageCalculator,
            ServerLevel nmsWorld,
            final Vec3 source,
            final net.minecraft.world.entity.@NotNull Entity target,
            final Explosion.ExplosionBlockCache[] blockCache,
            final BlockPos.MutableBlockPos blockPos
    ) {
        final AABB boundingBox = target.getBoundingBox();
        final double diffX = boundingBox.maxX - boundingBox.minX;
        final double diffY = boundingBox.maxY - boundingBox.minY;
        final double diffZ = boundingBox.maxZ - boundingBox.minZ;

        final double incX = 1.0 / (diffX * 2.0 + 1.0);
        final double incY = 1.0 / (diffY * 2.0 + 1.0);
        final double incZ = 1.0 / (diffZ * 2.0 + 1.0);

        if (incX < 0.0 || incY < 0.0 || incZ < 0.0) {
            return 0.0f;
        }

        final double offX = (1.0 - Math.floor(1.0 / incX) * incX) * 0.5 + boundingBox.minX;
        final double offY = boundingBox.minY;
        final double offZ = (1.0 - Math.floor(1.0 / incZ) * incZ) * 0.5 + boundingBox.minZ;

        final io.papermc.paper.util.CollisionUtil.LazyEntityCollisionContext context = new io.papermc.paper.util.CollisionUtil.LazyEntityCollisionContext(target);

        int totalRays = 0;
        int missedRays = 0;

        for (double dx = 0.0; dx <= 1.0; dx += incX) {
            final double fromX = Math.fma(dx, diffX, offX);
            for (double dy = 0.0; dy <= 1.0; dy += incY) {
                final double fromY = Math.fma(dy, diffY, offY);
                for (double dz = 0.0; dz <= 1.0; dz += incZ) {
                    ++totalRays;

                    final Vec3 from = new Vec3(
                            fromX,
                            fromY,
                            Math.fma(dz, diffZ, offZ)
                    );

                    if (!clipsAnything(explosion, damageCalculator, nmsWorld, from, source, context, blockCache, blockPos)) {
                        ++missedRays;
                    }
                }
            }
        }

        return (float) missedRays / (float) totalRays;
    }

    private boolean clipsAnything(
            Explosion explosion,
            ExplosionDamageCalculator damageCalculator,
            ServerLevel nmsWorld,
            final @NotNull Vec3 from,
            final @NotNull Vec3 to,
            final io.papermc.paper.util.CollisionUtil.LazyEntityCollisionContext context,
            final Explosion.ExplosionBlockCache[] blockCache,
            final BlockPos.MutableBlockPos currPos)
    {
        // assume that context.delegated = false
        final double adjX = io.papermc.paper.util.CollisionUtil.COLLISION_EPSILON * (from.x - to.x);
        final double adjY = io.papermc.paper.util.CollisionUtil.COLLISION_EPSILON * (from.y - to.y);
        final double adjZ = io.papermc.paper.util.CollisionUtil.COLLISION_EPSILON * (from.z - to.z);

        if (adjX == 0.0 && adjY == 0.0 && adjZ == 0.0) {
            return false;
        }

        final double toXAdj = to.x - adjX;
        final double toYAdj = to.y - adjY;
        final double toZAdj = to.z - adjZ;
        final double fromXAdj = from.x + adjX;
        final double fromYAdj = from.y + adjY;
        final double fromZAdj = from.z + adjZ;

        int currX = Mth.floor(fromXAdj);
        int currY = Mth.floor(fromYAdj);
        int currZ = Mth.floor(fromZAdj);

        final double diffX = toXAdj - fromXAdj;
        final double diffY = toYAdj - fromYAdj;
        final double diffZ = toZAdj - fromZAdj;

        final double dxDouble = Math.signum(diffX);
        final double dyDouble = Math.signum(diffY);
        final double dzDouble = Math.signum(diffZ);

        final int dx = (int)dxDouble;
        final int dy = (int)dyDouble;
        final int dz = (int)dzDouble;

        final double normalizedDiffX = diffX == 0.0 ? Double.MAX_VALUE : dxDouble / diffX;
        final double normalizedDiffY = diffY == 0.0 ? Double.MAX_VALUE : dyDouble / diffY;
        final double normalizedDiffZ = diffZ == 0.0 ? Double.MAX_VALUE : dzDouble / diffZ;

        double normalizedCurrX = normalizedDiffX * (diffX > 0.0 ? (1.0 - Mth.frac(fromXAdj)) : Mth.frac(fromXAdj));
        double normalizedCurrY = normalizedDiffY * (diffY > 0.0 ? (1.0 - Mth.frac(fromYAdj)) : Mth.frac(fromYAdj));
        double normalizedCurrZ = normalizedDiffZ * (diffZ > 0.0 ? (1.0 - Mth.frac(fromZAdj)) : Mth.frac(fromZAdj));

        for (;;) {
            currPos.set(currX, currY, currZ);

            // ClipContext.Block.COLLIDER -> BlockBehaviour.BlockStateBase::getCollisionShape
            // ClipContext.Fluid.NONE -> ignore fluids

            // read block from cache
            final long key = BlockPos.asLong(currX, currY, currZ);

            final int cacheKey =
                    (currX & BLOCK_EXPLOSION_CACHE_MASK) |
                            (currY & BLOCK_EXPLOSION_CACHE_MASK) << (BLOCK_EXPLOSION_CACHE_SHIFT) |
                            (currZ & BLOCK_EXPLOSION_CACHE_MASK) << (BLOCK_EXPLOSION_CACHE_SHIFT + BLOCK_EXPLOSION_CACHE_SHIFT);
            Explosion.ExplosionBlockCache cachedBlock = blockCache[cacheKey];
            if (cachedBlock == null || cachedBlock.key != key) {
                blockCache[cacheKey] = cachedBlock = getOrCacheExplosionBlock(explosion, damageCalculator, nmsWorld, currX, currY, currZ, key, false);
            }

            final BlockState blockState = cachedBlock.blockState;

            if (blockState != null && !blockState.emptyCollisionShape()) {
                VoxelShape collision = cachedBlock.cachedCollisionShape;
                if (collision == null) {
                    collision = blockState.getConstantCollisionShape();

                    if (collision == null) {
                        collision = blockState.getCollisionShape(nmsWorld, currPos, context);
                        if (!context.isDelegated()) {
                            // if it was not delegated during this call, assume that for any future ones it will not be delegated
                            // again, and cache the result
                            cachedBlock.cachedCollisionShape = collision;
                        }
                    } else {
                        cachedBlock.cachedCollisionShape = collision;
                    }
                }

                if (!collision.isEmpty() && collision.clip(from, to, currPos) != null) {
                    return true;
                }
            }

            if (normalizedCurrX > 1.0 && normalizedCurrY > 1.0 && normalizedCurrZ > 1.0) {
                return false;
            }

            // inc the smallest normalized coordinate

            if (normalizedCurrX < normalizedCurrY) {
                if (normalizedCurrX < normalizedCurrZ) {
                    currX += dx;
                    normalizedCurrX += normalizedDiffX;
                } else {
                    // x < y && x >= z <--> z < y && z <= x
                    currZ += dz;
                    normalizedCurrZ += normalizedDiffZ;
                }
            } else if (normalizedCurrY < normalizedCurrZ) {
                // y <= x && y < z
                currY += dy;
                normalizedCurrY += normalizedDiffY;
            } else {
                // y <= x && z <= y <--> z <= y && z <= x
                currZ += dz;
                normalizedCurrZ += normalizedDiffZ;
            }
        }
    }

    private Explosion.@NotNull ExplosionBlockCache getOrCacheExplosionBlock(
            @NotNull Explosion explosion,
            ExplosionDamageCalculator damageCalculator,
            ServerLevel nmsWorld,
            final int x,
            final int y,
            final int z,
            final long key,
            final boolean calculateResistance
    ) {
        Long2ObjectOpenHashMap<Explosion.ExplosionBlockCache> blockCache;
        long[] chunkPosCache;
        LevelChunk[] chunkCache;

        try {
            blockCache = (Long2ObjectOpenHashMap<Explosion.ExplosionBlockCache>) BLOCK_CACHE_FIELD.get(explosion);
            chunkPosCache = (long[]) CHUNK_POS_CACHE_FIELD.get(explosion);
            chunkCache = (LevelChunk[]) CHUNK_CACHE_FIELD.get(explosion);
        } catch (IllegalAccessException exception) {
            throw new RuntimeException(exception);
        }

        Explosion.ExplosionBlockCache cachedBlock = blockCache.get(key);
        if (cachedBlock != null) {
            return cachedBlock;
        }

        BlockPos position = new BlockPos(x, y, z);

        if (!nmsWorld.isInWorldBounds(position)) {
            cachedBlock = new Explosion.ExplosionBlockCache(key, position, null, null, 0.0f, true);
        } else {
            net.minecraft.world.level.chunk.LevelChunk chunk;
            long chunkKey = io.papermc.paper.util.CoordinateUtils.getChunkKey(x >> 4, z >> 4);
            int chunkCacheKey = ((x >> 4) & CHUNK_CACHE_MASK) | (((z >> 4) << CHUNK_CACHE_SHIFT) & (CHUNK_CACHE_MASK << CHUNK_CACHE_SHIFT));
            if (chunkPosCache[chunkCacheKey] == chunkKey) {
                chunk = chunkCache[chunkCacheKey];
            } else {
                chunkPosCache[chunkCacheKey] = chunkKey;
                chunkCache[chunkCacheKey] = chunk = nmsWorld.getChunk(x >> 4, z >> 4);
            }

            BlockState blockState = chunk.getBlockStateFinal(x, y, z);
            FluidState fluidState = blockState.getFluidState();

            Optional<Float> resistance;

            try {
                resistance = calculateResistance
                        ? damageCalculator.getBlockExplosionResistance(explosion, nmsWorld, position, blockState, fluidState)
                        : Optional.empty();
            } catch (Exception exception) {
                resistance = Optional.empty();
            }

            cachedBlock = new Explosion.ExplosionBlockCache(
                    key,
                    position,
                    blockState,
                    fluidState,
                    (resistance.orElse(ZERO_RESISTANCE) + 0.3f) * 0.3f,
                    false
            );
        }

        blockCache.put(key, cachedBlock);

        return cachedBlock;
    }

    private List<Vector> chainDetonateInstantTnt(@NotNull Block startingTnt, List<Vector> blocksToDetonate) {
        List<Vector> blocksToIterateThrough;

        double explosionRadiusBlocks = plugin.getConfig().getDouble("features.instant-tnt.explosion.spread-radius-blocks");
        double cubeVolume = Math.pow(explosionRadiusBlocks * 2, 3);

        double totalInstantTnts = instantTntBlocks.size();

        Vector center = BlockUtil.getBlockCenterLocation(startingTnt).toVector();

        if (cubeVolume < totalInstantTnts) {
            blocksToIterateThrough = new ArrayList<>();

            for (double x = center.getX() - explosionRadiusBlocks; x <= center.getX() + explosionRadiusBlocks; x++) {
                for (double y = center.getY() - explosionRadiusBlocks; y <= center.getY() + explosionRadiusBlocks; y++) {
                    for (double z = center.getZ() - explosionRadiusBlocks; z <= center.getZ() + explosionRadiusBlocks; z++) {
                        Vector location = new Vector(x, y, z);

                        double distance = center.distance(location);

                        if (distance > explosionRadiusBlocks) {
                            continue;
                        }

                        blocksToIterateThrough.add(new Vector(x, y, z));
                    }
                }
            }
        } else {
            blocksToIterateThrough = instantTntBlocks.stream().filter((Vector location) -> BlockUtil.getBlockCenterLocation(location).distance(center) <= explosionRadiusBlocks).toList();
        }

        World world = startingTnt.getWorld();

        for (Vector tntBlockLocation : blocksToIterateThrough) {
            if (blocksToDetonate.contains(tntBlockLocation)) {
                continue;
            }

            blocksToDetonate.add(tntBlockLocation);

            chainDetonateInstantTnt(
                    world.getBlockAt(
                            new Location(
                                    world,
                                    tntBlockLocation.getX(),
                                    tntBlockLocation.getY(),
                                    tntBlockLocation.getZ()
                            )
                    ),
                    blocksToDetonate
            );
        }

        return blocksToDetonate;
    }

    public void chainDetonateInstantTnt(Block startingTnt, Entity cause) {
        List<Vector> blocksToDetonate = chainDetonateInstantTnt(startingTnt, new ArrayList<>());

        Vector explosionOrigin = BlockUtil.getBlockCenterLocation(startingTnt).toVector();
        World world = startingTnt.getWorld();

        double blocksPerTickDelay = plugin.getConfig().getInt("features.instant-tnt.explosion.blocks-per-tick-delay");

        for (Vector explosionLocation : blocksToDetonate) {
            double distance = explosionOrigin.distance(explosionLocation);

            int tickDelay = blocksPerTickDelay == 0 ? 0 : (int) Math.floor(distance / blocksPerTickDelay);

            Bukkit.getScheduler().runTaskLater(plugin, () -> detonateInstantTnt(
                    world.getBlockAt(
                            new Location(world, explosionLocation.getX(), explosionLocation.getY(), explosionLocation.getZ())
                    ),
                    cause
            ), tickDelay);
        }
    }

    public void loadInstantTntData() {
        try {
            Reader reader = new FileReader(instantTntDataFile);

            Gson gson = new Gson();

            List<LinkedTreeMap<String, Double>> linkedTreeMaps = (List<LinkedTreeMap<String, Double>>) gson.fromJson(reader, List.class);

            reader.close();

            for (LinkedTreeMap<String, Double> linkedTreeMap : linkedTreeMaps) {
                instantTntBlocks.add(
                        new Vector(
                                linkedTreeMap.get("x"),
                                linkedTreeMap.get("y"),
                                linkedTreeMap.get("z")
                        )
                );
            }
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void saveInstantTntData() {
        try {
            Writer writer = new FileWriter(instantTntDataFile);

            Gson gson = new Gson();

            gson.toJson(instantTntBlocks, writer);

            writer.flush();
            writer.close();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}