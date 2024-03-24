package net.slqmy.bad_piggies_plugin.util;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class BlockUtil {

    public static @NotNull Location getBlockCenterLocation(@NotNull Location location) {
        return location.clone().add(0.5D, 0.5D, 0.5D);
    }

    public static @NotNull Location getBlockCenterLocation(@NotNull Block block) {
        return getBlockCenterLocation(block.getLocation());
    }

    public static @NotNull Vector getBlockCenterLocation(@NotNull Vector vector) {
        return vector.clone().add(new Vector(0.5D, 0.5D, 0.5D));
    }
}
