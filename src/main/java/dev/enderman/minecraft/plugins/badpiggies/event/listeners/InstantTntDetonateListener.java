package dev.enderman.minecraft.plugins.badpiggies.event.listeners;

import dev.enderman.minecraft.plugins.badpiggies.BadPiggiesPlugin;
import dev.enderman.minecraft.plugins.badpiggies.managers.InstantTntManager;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.TNTPrimeEvent;
import org.jetbrains.annotations.NotNull;

public class InstantTntDetonateListener implements Listener {

    private final BadPiggiesPlugin plugin;

    public InstantTntDetonateListener(BadPiggiesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInstantTntDetonate(@NotNull TNTPrimeEvent event) {
        Block tnt = event.getBlock();

        InstantTntManager manager = plugin.getInstantTntManager();

        if (manager.isInstantTnt(tnt)) {
            event.setCancelled(true);

            Entity entity = event.getPrimingEntity();
            manager.chainDetonateInstantTnt(tnt, entity);
        }
    }
}
