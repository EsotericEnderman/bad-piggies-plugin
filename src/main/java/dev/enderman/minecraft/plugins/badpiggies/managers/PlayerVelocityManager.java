package dev.enderman.minecraft.plugins.badpiggies.managers;

import dev.enderman.minecraft.plugins.badpiggies.BadPiggiesPlugin;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerVelocityManager extends BukkitRunnable {

    private final BadPiggiesPlugin plugin;

    private final Map<Player, Vector> playerVelocityMap = new HashMap<>();

    private final Map<Player, Vector> playerPositionMap = new HashMap<>();

    public PlayerVelocityManager(BadPiggiesPlugin plugin) {
        this.plugin = plugin;
    }

    public Vector getPlayerVelocity(Player player) {
        return playerVelocityMap.get(player);
    }

    public Vector getVelocity(Entity entity) {
        if (entity instanceof Player player) {
            Vector velocity = getPlayerVelocity(player);

            if (velocity == null) {
                return new Vector();
            }

            return velocity;
        }

        return entity.getVelocity();
    }

    public void updatePlayerVelocityData(@NotNull Player player) {
        Vector oldPosition = playerPositionMap.get(player);
        Vector newPosition = player.getLocation().toVector();

        if (oldPosition == null) {
            oldPosition = newPosition;
        }

        Vector velocity = newPosition.clone().subtract(oldPosition);

        playerVelocityMap.put(
                player,
                velocity
        );

        playerPositionMap.put(
                player,
                newPosition
        );
    }

    public void updateVelocityData() {
        List<? extends Player> players = plugin.getServer().getOnlinePlayers().stream().toList();

        for (Player player : players) {
            int ticksLived = plugin.getPlayerTickManager().getPlayerTicksExisted(player);

            if (ticksLived > 1) {
                updatePlayerVelocityData(player);
            }
        }
    }

    @Override
    public void run() {
        updateVelocityData();
    }
}
