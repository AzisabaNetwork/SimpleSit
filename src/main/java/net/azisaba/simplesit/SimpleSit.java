package net.azisaba.simplesit;

import net.azisaba.simplesit.command.SitCommand;
import net.azisaba.simplesit.data.PlayerManager;
import net.azisaba.simplesit.listener.DismountListener;
import net.azisaba.simplesit.listener.PlayerConnectionListener;
import net.azisaba.simplesit.listener.PlayerDeathListener;
import net.azisaba.simplesit.listener.SitListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class SimpleSit extends JavaPlugin {

    private PlayerManager playerManager;
    private SeatManager seatManager;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.playerManager = new PlayerManager(this);
        this.seatManager = new SeatManager(this, playerManager);

        getServer().getPluginManager().registerEvents(new SitListener(seatManager), this);
        getServer().getPluginManager().registerEvents(new DismountListener(this, seatManager), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(seatManager), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(playerManager), this);

        SitCommand sitCommand = new SitCommand(seatManager);
        PluginCommand sitPluginCommand = getCommand("sit");
        if (sitPluginCommand != null) {
            sitPluginCommand.setExecutor(sitCommand);
            sitPluginCommand.setTabCompleter(sitCommand);
        }
        PluginCommand sitTogglePluginCommand = getCommand("sittoggle");
        if (sitTogglePluginCommand != null) {
            sitTogglePluginCommand.setExecutor(sitCommand);
            sitTogglePluginCommand.setTabCompleter(sitCommand);
        }

        for (Player player : getServer().getOnlinePlayers()) {
            playerManager.load(player.getUniqueId());
        }

        getLogger().info("SimpleSit has been enabled");
    }

    @Override
    public void onDisable() {
        if (seatManager != null) {
            seatManager.removeAllSeats();
        }
        if (playerManager != null) {
            playerManager.saveAll();
        }
        getLogger().info("SimpleSit has been disabled");
    }
}
