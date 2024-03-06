package net.slqmy.bad_piggies_plugin.listener;

import net.slqmy.bad_piggies_plugin.BadPiggiesPlugin;
import net.slqmy.bad_piggies_plugin.manager.InstantTntManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
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
        Block block = event.getBlock();

        BlockData blockData = block.getBlockData();

        Material material = blockData.getMaterial();

        if (material != Material.TNT) {
            return;
        }

        InstantTntManager instantTntManager = plugin.getInstantTntManager();

        instantTntManager.addInstantTnt(block);
    }
}
