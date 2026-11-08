package net.azisaba.simplesit.listener

import net.azisaba.simplesit.SeatManager
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntitySpawnEvent
import org.spigotmc.event.entity.EntityMountEvent

/**
 * WorldGuard / Towny / ワールド保護プラグイン等が ArmorStand の生成や騎乗をキャンセルすると
 * 座席が作れず、何のメッセージも出ずに「座れない」状態になる。
 * ワールドごとの可否は SimpleSit 側の world-restriction で判定済みなので、
 * SimpleSit の座席に限り他プラグインのキャンセルを上書きする (config: force-seat)。
 */
class SeatGuardListener(private val seatManager: SeatManager) : Listener {

    // CreatureSpawnEvent は EntitySpawnEvent のサブクラスなので両方ここで拾える
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onSeatSpawn(event: EntitySpawnEvent) {
        if (!event.isCancelled || !seatManager.forceSeat) return
        if (!seatManager.isCustomSeat(event.entity)) return
        event.isCancelled = false
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onSeatMount(event: EntityMountEvent) {
        if (!event.isCancelled || !seatManager.forceSeat) return
        if (event.entity !is Player || !seatManager.isCustomSeat(event.mount)) return
        event.isCancelled = false
    }
}
