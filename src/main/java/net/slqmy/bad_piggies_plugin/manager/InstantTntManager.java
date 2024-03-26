package net.slqmy.bad_piggies_plugin.manager;

import net.slqmy.bad_piggies_plugin.BadPiggiesPlugin;
import net.slqmy.bad_piggies_plugin.util.BlockUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class InstantTntManager {

    private final BadPiggiesPlugin plugin;

    private final List<Vector> instantTntBlocks = new ArrayList<>();

    public InstantTntManager(BadPiggiesPlugin plugin) {
        this.plugin = plugin;

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

    private void detonateInstantTnt(@NotNull Block instantTnt) {
        instantTnt.setType(Material.AIR);

        World world = instantTnt.getWorld();

        YamlConfiguration configuration = (YamlConfiguration) plugin.getConfig();

        ConfigurationSection explosionSettings = configuration.getConfigurationSection("features.instant-tnt.explosion");
        assert explosionSettings != null;

        double instantTntExplosionPower = explosionSettings.getDouble("power");
        boolean instantTntBreaksBlocks = explosionSettings.getBoolean("breaks-blocks");
        boolean instantTntSetsFire = explosionSettings.getBoolean("sets-fire");

        world.createExplosion(BlockUtil.getBlockCenterLocation(instantTnt),
                (float) instantTntExplosionPower,
                instantTntSetsFire,
                instantTntBreaksBlocks
        );

        removeInstantTnt(instantTnt);
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

    public void chainDetonateInstantTnt(Block startingTnt) {
        List<Vector> blocksToDetonate = chainDetonateInstantTnt(startingTnt, new ArrayList<>());

        Vector explosionOrigin = BlockUtil.getBlockCenterLocation(startingTnt).toVector();
        World world = startingTnt.getWorld();

        double blocksPerTickDelay = plugin.getConfig().getInt("features.instant-tnt.explosion.blocks-per-tick-delay");

        for (Vector explosionLocation : blocksToDetonate) {
            double distance = explosionOrigin.distance(explosionLocation);

            int tickDelay = (int) Math.floor(distance / blocksPerTickDelay);

            Bukkit.getScheduler().runTaskLater(plugin, () -> detonateInstantTnt(
                    world.getBlockAt(
                            new Location(world, explosionLocation.getX(), explosionLocation.getY(), explosionLocation.getZ())
                    )
            ), tickDelay);
        }
    }

    public void loadInstantTntData() {

    }

    public void saveInstantTntData() {

    }
}