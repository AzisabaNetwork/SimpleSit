package net.azisaba.simplesit.command;

import net.azisaba.simplesit.SeatManager;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SitCommand implements TabExecutor {

    private final SeatManager seatManager;

    public SitCommand(SeatManager seatManager) {
        this.seatManager = seatManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("sittoggle") || (args.length > 0 && args[0].equalsIgnoreCase("toggle"))) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(seatManager.msgOnlyPlayer);
                return true;
            }
            if (!player.hasPermission("simplesit.toggle")) {
                player.sendMessage(seatManager.msgPrefix.append(seatManager.msgNoPerm));
                return true;
            }
            if (!seatManager.isRightClickSitGloballyEnabled()) {
                player.sendMessage(seatManager.msgPrefix.append(seatManager.msgRightClickDisabled));
                return true;
            }
            boolean newState = seatManager.toggleRightClickSit(player);
            if (newState) {
                player.sendMessage(seatManager.msgPrefix.append(seatManager.msgToggleEnabled));
            } else {
                player.sendMessage(seatManager.msgPrefix.append(seatManager.msgToggleDisabled));
            }
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("simplesit.admin")) {
                sender.sendMessage(seatManager.msgPrefix.append(seatManager.msgNoPerm));
                return true;
            }
            seatManager.loadConfig();
            sender.sendMessage(seatManager.msgPrefix.append(seatManager.msgReload));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(seatManager.msgOnlyPlayer);
            return true;
        }
        if (seatManager.isWorldEnabled(player.getWorld()) && !player.hasPermission("simplesit.admin")) {
            player.sendMessage(seatManager.msgPrefix.append(seatManager.msgDisabledWorld));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            return true;
        }
        if (player.isSneaking()) {
            player.sendMessage(seatManager.msgPrefix.append(seatManager.msgSitDeny));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            return true;
        }
        if (seatManager.isOnCooldown(player.getUniqueId()) || seatManager.isDismounting(player.getUniqueId())) {
            player.sendMessage(seatManager.msgPrefix.append(seatManager.msgSitDeny));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            return true;
        }
        if (!((org.bukkit.entity.Entity) player).isOnGround() && !player.hasPermission("simplesit.admin")) {
            player.sendMessage(seatManager.msgPrefix.append(seatManager.msgSitDeny));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            return true;
        }
        seatManager.sitOnGround(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("sittoggle")) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if (sender.hasPermission("simplesit.admin")) {
                completions.add("reload");
            }
            if (sender.hasPermission("simplesit.toggle")) {
                completions.add("toggle");
            }
            String input = args[0].toLowerCase();
            return completions.stream().filter(s -> s.startsWith(input)).toList();
        }
        return Collections.emptyList();
    }
}
