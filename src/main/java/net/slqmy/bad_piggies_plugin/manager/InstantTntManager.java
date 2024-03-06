package net.slqmy.bad_piggies_plugin.manager;

import net.slqmy.bad_piggies_plugin.BadPiggiesPlugin;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class InstantTntManager {

    private final BadPiggiesPlugin plugin;

    private final List<Vector> instantTntBlocks = new ArrayList<>();

    public InstantTntManager(BadPiggiesPlugin plugin) {
        this.plugin = plugin;


    }

    public void addInstantTnt(@NotNull Block instantTnt) {
        instantTntBlocks.add(new Vector(instantTnt.getX(), instantTnt.getY(), instantTnt.getZ()));
    }
}