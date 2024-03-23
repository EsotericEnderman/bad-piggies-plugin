package net.slqmy.bad_piggies_plugin.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class EntityUtil {

    public static @NotNull List<Block> getTouchedBlocks(@NotNull Entity entity, @NotNull Location locationOverride) {
        BoundingBox box = entity.getBoundingBox();

        box.expand(0.01D);

        Vector locationDifference = locationOverride.clone().subtract(entity.getLocation()).toVector();

        double minX = box.getMinX() + locationDifference.getX();
        double maxX = box.getMaxX() + locationDifference.getX();

        double minY = box.getMinY() + locationDifference.getY();
        double maxY = box.getMaxY() + locationDifference.getY();

        double minZ = box.getMinZ() + locationDifference.getZ();
        double maxZ = box.getMaxZ() + locationDifference.getZ();

        World world = entity.getWorld();

        List<Block> touchedBlocks = new ArrayList<>();

        for (int x = (int) Math.floor(minX); x <= Math.floor(maxX); x++) {
            for (int y = (int) Math.floor(minY); y <= Math.floor(maxY); y++) {
                for (int z = (int) Math.floor(minZ); z <= Math.floor(maxZ); z++) {
                    touchedBlocks.add(
                            world.getBlockAt(
                                    x,
                                    y,
                                    z
                            )
                    );
                }
            }
        }

        return touchedBlocks;
    }

    public static @NotNull List<Block> getTouchedBlocks(@NotNull Entity entity) {
        return getTouchedBlocks(entity, entity.getLocation());
    }
}
