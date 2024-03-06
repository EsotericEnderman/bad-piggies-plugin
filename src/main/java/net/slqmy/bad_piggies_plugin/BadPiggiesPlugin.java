package net.slqmy.bad_piggies_plugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public final class BadPiggiesPlugin extends JavaPlugin {

    private NamespacedKey instantTntKey;

    public NamespacedKey getInstantTntKey() {
        return instantTntKey;
    }

    @Override
    public void onEnable() {
        getDataFolder().mkdir();

        YamlConfiguration config = (YamlConfiguration) getConfig();

        config.options().copyDefaults();
        saveDefaultConfig();

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
        }
    }
}
