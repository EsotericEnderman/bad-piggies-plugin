package net.slqmy.bad_piggies_plugin.util;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class EntityUtil {

    public static @NotNull List<Block> getTouchedBlocks(@NotNull Entity entity) {
        BoundingBox box = entity.getBoundingBox();

        box.expand(0.01F);

        double minX = box.getMinX();
        double minY = box.getMinY();
        double minZ = box.getMinZ();

        double maxX = box.getMaxX();
        double maxY = box.getMaxY();
        double maxZ = box.getMaxZ();

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
}
