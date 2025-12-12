package net.azisaba.simplesit.listener;

import net.azisaba.simplesit.SeatManager;
import net.azisaba.simplesit.SimpleSit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.spigotmc.event.entity.EntityDismountEvent;

public class DismountListener implements Listener {

    private final SimpleSit plugin;
    private final SeatManager seatManager;

    public DismountListener(SimpleSit plugin, SeatManager seatManager) {
        this.plugin = plugin;
        this.seatManager = seatManager;
    }

    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        Entity vehicle = event.getDismounted();
        if (seatManager.isCustomSeat(vehicle)) {
            vehicle.remove();
            Location returnLocation = seatManager.popPreviousLocation(player.getUniqueId());
            if (returnLocation != null) {
                Location currentLocation = player.getLocation();
                returnLocation.setYaw(currentLocation.getYaw());
                returnLocation.setPitch(currentLocation.getPitch());
                runTeleport(player, returnLocation);
            } else {
                Location fallback = vehicle.getLocation().add(0, 0.5, 0);
                fallback.setYaw(player.getLocation().getYaw());
                fallback.setPitch(player.getLocation().getPitch());
                runTeleport(player, fallback);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        seatManager.removeData(event.getPlayer().getUniqueId());
    }

    private void runTeleport(Player player, Location location) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            player.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
            player.teleport(location);
        }, 1L);
    }
}