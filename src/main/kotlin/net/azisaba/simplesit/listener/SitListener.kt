package net.azisaba.simplesit.listener

import net.azisaba.simplesit.SeatManager
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.type.Slab
import org.bukkit.entity.ArmorStand
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot

class SitListener(private val seatManager: SeatManager) : Listener {

    @EventHandler
    fun onRightClick(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        // メインハンド/オフハンドで 2 回呼ばれるのでメインハンドのみ処理
        if (event.hand != EquipmentSlot.HAND) return
        val player = event.player
        if (player.inventory.itemInMainHand.type != Material.AIR) return
        if (player.isSneaking) return
        if (!seatManager.canSitIn(player)) return
        val block = event.clickedBlock ?: return
        if (!isValidSeat(block)) return
        val occupied = block.world.getNearbyEntities(block.location, 0.0, 0.0, 0.0)
            .any { it is ArmorStand && it.passengers.isNotEmpty() }
        if (occupied) return

        event.isCancelled = true
        if (!seatManager.sitPlayer(player, block) && player.vehicle == null) {
            player.sendMessage(seatManager.msgPrefix + seatManager.msgSitDeny)
        }
    }

    private fun isValidSeat(block: Block): Boolean {
        val type = block.type.name
        if (type.contains("STAIRS")) return true
        if (type.contains("SLAB")) {
            val data = block.blockData
            return data !is Slab || data.type == Slab.Type.BOTTOM
        }
        return false
    }
}
