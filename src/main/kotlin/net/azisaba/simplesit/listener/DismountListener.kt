package net.azisaba.simplesit.listener

import net.azisaba.simplesit.SeatManager
import net.azisaba.simplesit.SimpleSit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.util.Vector
import org.spigotmc.event.entity.EntityDismountEvent

class DismountListener(
    private val plugin: SimpleSit,
    private val seatManager: SeatManager,
) : Listener {

    @EventHandler
    fun onDismount(event: EntityDismountEvent) {
        val player = event.entity as? Player ?: return
        val vehicle = event.dismounted
        if (!seatManager.isCustomSeat(vehicle)) return

        vehicle.remove()
        val returnLocation = seatManager.popPreviousLocation(player.uniqueId)
        if (player.isDead) return

        val target = returnLocation ?: vehicle.location.add(0.0, 0.5, 0.0)
        val current = player.location
        target.yaw = current.yaw
        target.pitch = current.pitch
        runTeleport(player, target)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        seatManager.removeData(event.player.uniqueId)
    }

    private fun runTeleport(player: Player, location: Location) {
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            if (!player.isOnline || player.isDead) return@Runnable
            player.velocity = Vector(0, 0, 0)
            player.teleport(location)
        }, 1L)
    }
}
