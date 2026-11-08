package net.azisaba.simplesit.command

import net.azisaba.simplesit.SeatManager
import org.bukkit.Sound
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class SitCommand(private val seatManager: SeatManager) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isNotEmpty() && args[0].equals("reload", ignoreCase = true)) {
            if (!sender.hasPermission(SeatManager.ADMIN_PERMISSION)) {
                sender.sendMessage(seatManager.msgPrefix + seatManager.msgNoPerm)
                return true
            }
            seatManager.loadConfig()
            sender.sendMessage(seatManager.msgPrefix + seatManager.msgReload)
            return true
        }

        val player = sender as? Player ?: run {
            sender.sendMessage(seatManager.msgPrefix + seatManager.msgOnlyPlayer)
            return true
        }
        val isAdmin = player.hasPermission(SeatManager.ADMIN_PERMISSION)

        if (!seatManager.isWorldEnabled(player.world) && !isAdmin) {
            deny(player, seatManager.msgDisabledWorld)
            return true
        }
        if (!player.isOnGround && !isAdmin) {
            deny(player, seatManager.msgSitDeny)
            return true
        }
        if (player.vehicle != null) return true
        if (!seatManager.sitOnGround(player)) {
            deny(player, seatManager.msgSitDeny)
        }
        return true
    }

    private fun deny(player: Player, message: String) {
        player.sendMessage(seatManager.msgPrefix + message)
        player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
    }
}
