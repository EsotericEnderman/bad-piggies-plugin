package net.slqmy.bad_piggies_plugin.listener;

import net.slqmy.bad_piggies_plugin.BadPiggiesPlugin;
import net.slqmy.bad_piggies_plugin.manager.InstantTntManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class InstantTntPlaceListener implements Listener {

    private final BadPiggiesPlugin plugin;

    public InstantTntPlaceListener(BadPiggiesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onTntPlace(@NotNull BlockPlaceEvent event) {
        ItemStack tntItem = event.getItemInHand();

        ItemMeta meta = tntItem.getItemMeta();

        PersistentDataContainer dataContainer = meta.getPersistentDataContainer();

        Boolean isInstantTnt = dataContainer.get(plugin.getInstantTntKey(), PersistentDataType.BOOLEAN);

        if (Boolean.FALSE.equals(isInstantTnt)) {
            return;
        }

        InstantTntManager instantTntManager = plugin.getInstantTntManager();

        instantTntManager.addInstantTnt(event.getBlock());
    }
}
