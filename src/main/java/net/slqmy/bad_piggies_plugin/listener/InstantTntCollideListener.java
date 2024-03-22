package net.slqmy.bad_piggies_plugin.listener;

import io.papermc.paper.event.entity.EntityMoveEvent;
import net.slqmy.bad_piggies_plugin.BadPiggiesPlugin;
import net.slqmy.bad_piggies_plugin.manager.InstantTntManager;
import net.slqmy.bad_piggies_plugin.util.EntityUtil;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class InstantTntCollideListener implements Listener {

    private final BadPiggiesPlugin plugin;

    public InstantTntCollideListener(BadPiggiesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInstantTntCollide(@NotNull PlayerMoveEvent event) {
        onInstantTntCollide(
                new EntityMoveEvent(
                        event.getPlayer(),
                        event.getFrom(),
                        event.getTo()
                )
        );

        plugin.getInstantTntManager().calculatePlayerVelocity(event);
    }

    @EventHandler
    public void onInstantTntCollide(@NotNull EntityMoveEvent event) {
        Entity entity = event.getEntity();

        Vector velocity = entity.getVelocity();

        InstantTntManager instantTntManager = plugin.getInstantTntManager();

        if (entity instanceof Player player) {
            Vector playerVelocity = instantTntManager.getPlayerVelocity(player);

            if (playerVelocity != null) {
                velocity = playerVelocity;
            }

            Bukkit.getLogger().info("velocity = " + velocity);
        }

        double speedMetersPerTick = velocity.length();

        float tickRate = Bukkit.getServerTickManager().getTickRate();

        double speedMetersPerSecond = speedMetersPerTick * tickRate;

        YamlConfiguration configuration = (YamlConfiguration) plugin.getConfig();

        double minimumCollisionDetonationSpeed = configuration.getDouble("features.instant-tnt.minimum-collision-detonation-speed");

        if (speedMetersPerSecond < minimumCollisionDetonationSpeed) {
            return;
        }

        List<Block> touchedBlocks = EntityUtil.getTouchedBlocks(entity, event.getTo());

        if (entity instanceof Player) {
            List<Block> touchedTnt = touchedBlocks.stream().filter((Block touchedBlock) -> touchedBlock.getBlockData().getMaterial() == Material.TNT).toList();

            if (!touchedTnt.isEmpty()) {
                Bukkit.getLogger().info("touchedBlocks = " + touchedTnt);
            }
        }

        for (Block touchedBlock : touchedBlocks) {
            if (instantTntManager.isInstantTnt(touchedBlock) && instantTntManager.shouldInstantTntDetonate(touchedBlock, entity)) {
                instantTntManager.detonateInstantTnt(touchedBlock);
            }
        }
    }
}
