package net.slqmy.bad_piggies_plugin.manager;

import net.slqmy.bad_piggies_plugin.BadPiggiesPlugin;
import net.slqmy.bad_piggies_plugin.util.BlockUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InstantTntManager {

    private final BadPiggiesPlugin plugin;

    private final List<Vector> instantTntBlocks = new ArrayList<>();

    private final Map<Player, Vector> playerVelocityMap = new HashMap<>();

    public InstantTntManager(BadPiggiesPlugin plugin) {
        this.plugin = plugin;

        loadInstantTntData();
    }

    public Vector getPlayerVelocity(Player player) {
        return playerVelocityMap.get(player);
    }

    public void calculatePlayerVelocity(@NotNull PlayerMoveEvent event) {
        playerVelocityMap.put(
                event.getPlayer(),
                event.getTo().toVector().subtract(event.getFrom().toVector())
        );
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

    public boolean shouldInstantTntDetonate(Block instantTnt, @NotNull Entity cause) {
        double entityX = cause.getX();
        double entityY = cause.getY();
        double entityZ = cause.getZ();

        Location blockCenterLocation = BlockUtil.getBlockCenterLocation(instantTnt);

        double tntX = blockCenterLocation.getX();
        double tntY = blockCenterLocation.getY();
        double tntZ = blockCenterLocation.getZ();

        Vector entityVelocity = cause.getVelocity();

        double velocityX = entityVelocity.getX();
        double velocityY = entityVelocity.getY();
        double velocityZ = entityVelocity.getZ();

        double significantValue = 0.0D;

        if (entityY < tntY - 0.5D) {
            significantValue = velocityY;
        } else if (entityY > tntY + 0.5D) {
            significantValue = -velocityY;
        } else if (entityX < tntX - 0.5D) {
            significantValue = velocityX;
        } else if (entityX > tntX + 0.5D) {
            significantValue = -velocityX;
        } else if (entityZ < tntZ - 0.5D) {
            significantValue = velocityZ;
        } else if (entityZ > tntZ + 0.5D) {
            significantValue = -velocityZ;
        }

        return significantValue > plugin.getConfig().getDouble("features.instant-tnt.minimum-collision-detonation-speed") / 20.0D;
    }

    public void detonateInstantTnt(@NotNull Block instantTnt) {
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

    public void loadInstantTntData() {

    }

    public void saveInstantTntData() {

    }
}