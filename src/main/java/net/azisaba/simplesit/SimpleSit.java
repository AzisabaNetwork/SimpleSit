package net.azisaba.simplesit;

import net.azisaba.simplesit.command.SitCommand;
import net.azisaba.simplesit.listener.DismountListener;
import net.azisaba.simplesit.listener.SitListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class SimpleSit extends JavaPlugin {

    private SeatManager seatManager;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.seatManager = new SeatManager(this);
        getServer().getPluginManager().registerEvents(new SitListener(seatManager), this);
        getServer().getPluginManager().registerEvents(new DismountListener(this, seatManager), this);
        getCommand("sit").setExecutor(new SitCommand(seatManager));
        getLogger().info("SimpleSit has been enabled");
    }

    @Override
    public void onDisable() {
        if (seatManager != null) {
            seatManager.removeAllSeats();
        }
        getLogger().info("SimpleSit has been disabled");
    }
}
