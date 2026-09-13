package net.azisaba.simplesit.listener;

import net.azisaba.simplesit.SeatManager;
import net.azisaba.simplesit.SimpleSit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

import java.util.UUID;

public class DismountListener implements Listener {

    private final SimpleSit plugin;
    private final SeatManager seatManager;

    public DismountListener(SimpleSit plugin, SeatManager seatManager) {
        this.plugin = plugin;
        this.seatManager = seatManager;
    }

    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Entity vehicle = event.getDismounted();
        if (!seatManager.isCustomSeat(vehicle)) return;

        UUID uuid = player.getUniqueId();
        seatManager.setDismounting(uuid, true);

        Location returnLocation = seatManager.popPreviousLocation(uuid);
        if (player.isDead()) {
            if (vehicle.isValid()) {
                vehicle.remove();
            }
            seatManager.setDismounting(uuid, false);
            return;
        }

        Location targetLocation = seatManager.getSafeDismountLocation(player, vehicle, returnLocation);

        if (vehicle.isValid()) {
            vehicle.remove();
        }

        player.setVelocity(new Vector(0, 0, 0));
        player.setFallDistance(0);
        player.teleport(targetLocation);

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                if (player.isOnline() && !player.isDead()) {
                    if (player.getLocation().getY() < targetLocation.getY() - 0.2) {
                        player.setVelocity(new Vector(0, 0, 0));
                        player.setFallDistance(0);
                        player.teleport(targetLocation);
                    }
                }
            } finally {
                seatManager.setDismounting(uuid, false);
                seatManager.setCooldown(uuid);
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        seatManager.removeData(event.getPlayer().getUniqueId());
    }
}
