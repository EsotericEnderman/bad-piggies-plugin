package net.slqmy.bad_piggies_plugin.manager;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class PlayerVelocityManager {
    private final Map<Player, Vector> playerVelocityMap = new HashMap<>();

    public Vector getPlayerVelocity(Player player) {
        return playerVelocityMap.get(player);
    }

    public void calculatePlayerVelocity(@NotNull PlayerMoveEvent event) {
        playerVelocityMap.put(
                event.getPlayer(),
                event.getTo().toVector().subtract(event.getFrom().toVector())
        );
    }

    public Vector getVelocity(Entity entity) {
        if (entity instanceof Player player) {
            Vector velocity = getPlayerVelocity(player);

            if (velocity == null) {
                return new Vector();
            }
        }

        return entity.getVelocity();
    }
}
