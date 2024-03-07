package net.slqmy.bad_piggies_plugin.util;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

public class BlockUtil {

    public static @NotNull Location getBlockCenterLocation(@NotNull Location location) {
        return location.add(0.5D, 0.5D, 0.5D);
    }

    public static @NotNull Location getBlockCenterLocation(@NotNull Block block) {
        return getBlockCenterLocation(block.getLocation());
    }
}
