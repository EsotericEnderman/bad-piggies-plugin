package net.slqmy.bad_piggies_plugin;

import net.slqmy.bad_piggies_plugin.listener.InstantTntCollideListener;
import net.slqmy.bad_piggies_plugin.listener.InstantTntPlaceListener;
import net.slqmy.bad_piggies_plugin.listener.InstantTntRemoveListener;
import net.slqmy.bad_piggies_plugin.manager.InstantTntManager;
import net.slqmy.bad_piggies_plugin.manager.PlayerTickManager;
import net.slqmy.bad_piggies_plugin.manager.PlayerVelocityManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public final class BadPiggiesPlugin extends JavaPlugin {

    private PlayerTickManager playerTickManager;

    private PlayerVelocityManager playerVelocityManager;

    private InstantTntManager instantTntManager;

    private NamespacedKey instantTntKey;

    public PlayerTickManager getPlayerTickManager() {
        return playerTickManager;
    }

    public PlayerVelocityManager getPlayerVelocityManager() {
        return playerVelocityManager;
    }

    public InstantTntManager getInstantTntManager() {
        return instantTntManager;
    }

    public NamespacedKey getInstantTntKey() {
        return instantTntKey;
    }

    @Override
    public void onEnable() {
        getDataFolder().mkdir();

        YamlConfiguration config = (YamlConfiguration) getConfig();

        config.options().copyDefaults();
        saveDefaultConfig();

        saveResource("data/instant-tnt-blocks.json", false);

        boolean isInstantTntEnabled = config.getBoolean("features.instant-tnt.enabled");

        if (isInstantTntEnabled) {
            ItemStack instantTnt = new ItemStack(Material.TNT);

            ItemMeta meta = instantTnt.getItemMeta();

            PersistentDataContainer dataContainer = meta.getPersistentDataContainer();

            instantTntKey = new NamespacedKey(this, "instant-tnt");

            dataContainer.set(instantTntKey, PersistentDataType.BOOLEAN, true);

            instantTnt.setItemMeta(meta);

            Material[] plankTypes = Arrays.stream(Material.values()).filter((material) -> material.name().endsWith("PLANKS")).toArray(Material[]::new);

            for (Material plankType : plankTypes) {
                NamespacedKey recipeKey = new NamespacedKey(this, "instant-tnt-recipe-" + plankType.name().toLowerCase());

                ShapedRecipe instantTntRecipe = new ShapedRecipe(recipeKey, instantTnt);

                instantTntRecipe.shape("GPG", "PGP", "GPG");

                instantTntRecipe.setIngredient('G', Material.GUNPOWDER);

                instantTntRecipe.setIngredient('P', plankType);

                Bukkit.addRecipe(instantTntRecipe);
            }

            PluginManager pluginManager = Bukkit.getPluginManager();

            pluginManager.registerEvents(new InstantTntPlaceListener(this), this);
            pluginManager.registerEvents(new InstantTntRemoveListener(this), this);
            pluginManager.registerEvents(new InstantTntCollideListener(this), this);

            playerTickManager =  new PlayerTickManager();
            pluginManager.registerEvents(playerTickManager, this);
            playerTickManager.runTaskTimer(this, 0L, 1L);

            playerVelocityManager = new PlayerVelocityManager(this);
            playerVelocityManager.runTaskTimer(this, 0L, 1L);

            instantTntManager = new InstantTntManager(this);
        }
    }

    @Override
    public void onDisable() {
        if (instantTntManager == null) {
            return;
        }

        instantTntManager.saveInstantTntData();
    }
}
