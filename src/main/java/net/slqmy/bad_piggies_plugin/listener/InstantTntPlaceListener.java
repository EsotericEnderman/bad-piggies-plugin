package net.slqmy.bad_piggies_plugin.listener;

import net.slqmy.bad_piggies_plugin.BadPiggiesPlugin;
import net.slqmy.bad_piggies_plugin.manager.InstantTntManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.jetbrains.annotations.NotNull;

public class InstantTntPlaceListener implements Listener {

    private final BadPiggiesPlugin plugin;

    public InstantTntPlaceListener(BadPiggiesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onTntPlace(@NotNull BlockPlaceEvent event) {
        if (!plugin.getInstantTntManager().isInstantTnt(event.getItemInHand())) {
            return;
        }

        InstantTntManager instantTntManager = plugin.getInstantTntManager();

        instantTntManager.addInstantTnt(event.getBlock());
    }
}
