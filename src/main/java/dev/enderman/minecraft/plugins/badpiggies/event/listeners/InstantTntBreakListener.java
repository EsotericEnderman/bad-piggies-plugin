package dev.enderman.minecraft.plugins.badpiggies.event.listeners;

import dev.enderman.minecraft.plugins.badpiggies.BadPiggiesPlugin;
import dev.enderman.minecraft.plugins.badpiggies.managers.InstantTntManager;
import dev.enderman.minecraft.plugins.badpiggies.util.BlockUtil;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class InstantTntBreakListener implements Listener {

    private final BadPiggiesPlugin plugin;

    public InstantTntBreakListener(BadPiggiesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInstantTntBreak(@NotNull BlockDropItemEvent event) {
        Block block = event.getBlock();

        InstantTntManager instantTntManager = plugin.getInstantTntManager();

        if (!instantTntManager.isInstantTnt(block)) {
            return;
        }

        instantTntManager.removeInstantTnt(event.getBlock());

        List<Item> items = event.getItems();

        if (items.isEmpty()) {
            return;
        }

        items.clear();
        ItemStack instantTnt = instantTntManager.getInstantTntItem();

        World world = block.getWorld();
        world.dropItem(BlockUtil.getBlockCenterLocation(block), instantTnt);
    }
}
