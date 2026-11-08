package net.azisaba.simplesit

import net.azisaba.simplesit.command.SitCommand
import net.azisaba.simplesit.listener.DismountListener
import net.azisaba.simplesit.listener.PlayerDeathListener
import net.azisaba.simplesit.listener.SeatGuardListener
import net.azisaba.simplesit.listener.SitListener
import org.bukkit.plugin.java.JavaPlugin

class SimpleSit : JavaPlugin() {

    private var seatManager: SeatManager? = null

    override fun onEnable() {
        saveDefaultConfig()
        val manager = SeatManager(this)
        seatManager = manager

        server.pluginManager.apply {
            registerEvents(SitListener(manager), this@SimpleSit)
            registerEvents(DismountListener(this@SimpleSit, manager), this@SimpleSit)
            registerEvents(PlayerDeathListener(manager), this@SimpleSit)
            registerEvents(SeatGuardListener(manager), this@SimpleSit)
        }
        getCommand("sit")?.setExecutor(SitCommand(manager))
        logger.info("SimpleSit has been enabled")
    }

    override fun onDisable() {
        seatManager?.removeAllSeats()
        logger.info("SimpleSit has been disabled")
    }
}
