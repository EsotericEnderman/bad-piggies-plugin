package dev.enderman.minecraft.plugins.badpiggies.event.listeners;

import io.papermc.paper.event.entity.EntityMoveEvent;
import dev.enderman.minecraft.plugins.badpiggies.BadPiggiesPlugin;
import dev.enderman.minecraft.plugins.badpiggies.managers.InstantTntManager;
import dev.enderman.minecraft.plugins.badpiggies.util.EntityUtil;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
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
    }

    @EventHandler
    public void onInstantTntCollide(@NotNull EntityMoveEvent event) {
        Entity entity = event.getEntity();

        Vector velocity = plugin.getPlayerVelocityManager().getVelocity(entity);

        InstantTntManager instantTntManager = plugin.getInstantTntManager();

        double speedMetersPerTick = velocity.length();

        double speedMetersPerSecond = speedMetersPerTick * 20.0D;

        YamlConfiguration configuration = (YamlConfiguration) plugin.getConfig();

        double minimumCollisionDetonationSpeed = configuration.getDouble("features.instant-tnt.minimum-collision-detonation-speed");

        if (speedMetersPerSecond < minimumCollisionDetonationSpeed) {
            return;
        }

        List<Block> touchedBlocks = EntityUtil.getTouchedBlocks(entity, event.getTo());

        for (Block touchedBlock : touchedBlocks) {
            if (instantTntManager.isInstantTnt(touchedBlock) && instantTntManager.shouldInstantTntDetonate(touchedBlock, entity, event.getTo())) {
                instantTntManager.chainDetonateInstantTnt(touchedBlock, entity);
            }
        }
    }
}
