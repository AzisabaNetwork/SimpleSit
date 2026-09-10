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

        seatManager.setDismounting(player.getUniqueId(), true);

        Location returnLocation = seatManager.popPreviousLocation(player.getUniqueId());
        if (player.isDead()) {
            vehicle.remove();
            seatManager.setDismounting(player.getUniqueId(), false);
            return;
        }

        Location targetLocation;
        if (returnLocation != null) {
            targetLocation = returnLocation.clone();
            Location currentLocation = player.getLocation();
            targetLocation.setYaw(currentLocation.getYaw());
            targetLocation.setPitch(currentLocation.getPitch());
        } else {
            targetLocation = vehicle.getLocation().clone().add(0, seatManager.getSitOffset(), 0);
            targetLocation.setYaw(player.getLocation().getYaw());
            targetLocation.setPitch(player.getLocation().getPitch());
        }
        targetLocation.add(0, 0.1, 0);

        vehicle.teleport(targetLocation);

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                if (player.isOnline() && !player.isDead()) {
                    player.setVelocity(new Vector(0, 0, 0));
                    player.setFallDistance(0);
                    player.teleport(targetLocation);
                }
            } finally {
                if (vehicle.isValid()) {
                    vehicle.remove();
                }
                seatManager.setDismounting(player.getUniqueId(), false);
                seatManager.setCooldown(player.getUniqueId());
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        seatManager.removeData(event.getPlayer().getUniqueId());
    }
}
