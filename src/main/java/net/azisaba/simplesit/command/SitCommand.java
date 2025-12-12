package net.azisaba.simplesit.command;

import net.azisaba.simplesit.SeatManager;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SitCommand implements CommandExecutor {

    private final SeatManager seatManager;

    public SitCommand(SeatManager seatManager) {
        this.seatManager = seatManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("simplesit.admin")) {
                sender.sendMessage(seatManager.msgPrefix + seatManager.msgNoPerm);
                return true;
            }
            seatManager.loadConfig();
            sender.sendMessage(seatManager.msgPrefix + seatManager.msgReload);
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(seatManager.msgOnlyPlayer);
            return true;
        }
        Player player = (Player) sender;
        if (!seatManager.isWorldEnabled(player.getWorld()) && !player.hasPermission("simplesit.admin")) {
            player.sendMessage(seatManager.msgPrefix + seatManager.msgDisabledWorld);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            return true;
        }
        if (!player.isOnGround() && !player.hasPermission("simplesit.admin")) {
            player.sendMessage(seatManager.msgPrefix + seatManager.msgSitDeny);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            return true;
        }
        seatManager.sitOnGround(player);
        return true;
    }
}
