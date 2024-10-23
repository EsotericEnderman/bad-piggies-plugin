package dev.enderman.minecraft.plugins.badpiggies;

import dev.enderman.minecraft.plugins.badpiggies.event.listeners.InstantTntBreakListener;
import dev.enderman.minecraft.plugins.badpiggies.event.listeners.InstantTntCollideListener;
import dev.enderman.minecraft.plugins.badpiggies.event.listeners.InstantTntDetonateListener;
import dev.enderman.minecraft.plugins.badpiggies.event.listeners.InstantTntPlaceListener;
import dev.enderman.minecraft.plugins.badpiggies.managers.InstantTntManager;
import dev.enderman.minecraft.plugins.badpiggies.managers.PlayerTickManager;
import dev.enderman.minecraft.plugins.badpiggies.managers.PlayerVelocityManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
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

        PluginManager pluginManager = Bukkit.getPluginManager();

        playerTickManager =  new PlayerTickManager();
        pluginManager.registerEvents(playerTickManager, this);
        playerTickManager.runTaskTimer(this, 0L, 1L);

        playerVelocityManager = new PlayerVelocityManager(this);
        playerVelocityManager.runTaskTimer(this, 0L, 1L);

        boolean isInstantTntEnabled = config.getBoolean("features.instant-tnt.enabled");

        if (isInstantTntEnabled) {
            instantTntManager = new InstantTntManager(this);
            instantTntKey = new NamespacedKey(this, "instant-tnt");

            ItemStack instantTnt = instantTntManager.getInstantTntItem();

            Material[] plankTypes = Arrays.stream(Material.values()).filter((material) -> material.name().endsWith("PLANKS")).toArray(Material[]::new);

            for (Material plankType : plankTypes) {
                NamespacedKey recipeKey = new NamespacedKey(this, "instant-tnt-recipe-" + plankType.name().toLowerCase());

                ShapedRecipe instantTntRecipe = new ShapedRecipe(recipeKey, instantTnt);

                instantTntRecipe.shape("GPG", "PGP", "GPG");

                instantTntRecipe.setIngredient('G', Material.GUNPOWDER);

                instantTntRecipe.setIngredient('P', plankType);

                Bukkit.addRecipe(instantTntRecipe);
            }

            pluginManager.registerEvents(new InstantTntPlaceListener(this), this);
            pluginManager.registerEvents(new InstantTntBreakListener(this), this);
            pluginManager.registerEvents(new InstantTntCollideListener(this), this);
            pluginManager.registerEvents(new InstantTntDetonateListener(this), this);
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
