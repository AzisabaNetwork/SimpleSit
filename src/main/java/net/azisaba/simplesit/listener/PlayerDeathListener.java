package net.azisaba.simplesit.listener;

import net.azisaba.simplesit.SeatManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerDeathListener implements Listener {

    private final SeatManager seatManager;

    public PlayerDeathListener(SeatManager seatManager) {
        this.seatManager = seatManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        seatManager.clearSeatState(player);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        seatManager.removeData(event.getPlayer().getUniqueId());
    }
}
