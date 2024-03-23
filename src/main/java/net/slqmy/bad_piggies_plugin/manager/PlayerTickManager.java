package net.slqmy.bad_piggies_plugin.manager;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class PlayerTickManager extends BukkitRunnable implements Listener {

    private final Map<Player, Integer> playerTicksExistedMap = new HashMap<>();

    public int getPlayerTicksExisted(Player player) {
        return playerTicksExistedMap.get(player);
    }

    private void setPlayerTicksExisted(Player player, int ticksExisted) {
        playerTicksExistedMap.put(player, ticksExisted);
    }

    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        playerTicksExistedMap.put(event.getPlayer(), 0);
    }

    @EventHandler
    public void onPlayerLeave(@NotNull PlayerQuitEvent event) {
        playerTicksExistedMap.remove(event.getPlayer());
    }

    @Override
    public void run() {
        for (Player player : playerTicksExistedMap.keySet()) {
            int ticksLived = getPlayerTicksExisted(player);

            setPlayerTicksExisted(player, ticksLived + 1);
        }
    }
}
