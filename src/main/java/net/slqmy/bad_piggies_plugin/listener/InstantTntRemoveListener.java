package net.slqmy.bad_piggies_plugin.listener;

import net.slqmy.bad_piggies_plugin.BadPiggiesPlugin;
import net.slqmy.bad_piggies_plugin.manager.InstantTntManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;

public class InstantTntRemoveListener implements Listener {

    private final BadPiggiesPlugin plugin;

    public InstantTntRemoveListener(BadPiggiesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInstantTntRemove(@NotNull BlockBreakEvent event) {
        InstantTntManager instantTntManager = plugin.getInstantTntManager();

        instantTntManager.removeInstantTnt(event.getBlock());
    }
}
