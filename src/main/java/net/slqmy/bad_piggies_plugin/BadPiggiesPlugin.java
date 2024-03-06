package net.slqmy.bad_piggies_plugin;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class BadPiggiesPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getDataFolder().mkdir();

        YamlConfiguration config = (YamlConfiguration) getConfig();

        config.options().copyDefaults();
        saveDefaultConfig();
    }
}
