package net.azisaba.simplesit.listener

import net.azisaba.simplesit.SeatManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerRespawnEvent

class PlayerDeathListener(private val seatManager: SeatManager) : Listener {

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        seatManager.clearSeatState(event.entity)
    }

    @EventHandler
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        seatManager.removeData(event.player.uniqueId)
    }
}
