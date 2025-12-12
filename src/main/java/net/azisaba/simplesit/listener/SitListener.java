package net.azisaba.simplesit.listener;

import net.azisaba.simplesit.SeatManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Slab;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class SitListener implements Listener {

    private final SeatManager seatManager;

    public SitListener(SeatManager seatManager) {
        this.seatManager = seatManager;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (player.isSneaking()) return;
        if (event.getItem() != null && event.getItem().getType() != Material.AIR) return;
        if (!seatManager.isWorldEnabled(player.getWorld()) && !player.hasPermission("simplesit.admin")) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (!isValidSeat(block)) return;
        for (Entity entity : block.getWorld().getNearbyEntities(block.getLocation(), 0, 0, 0)) {
            if (entity instanceof ArmorStand && !entity.getPassengers().isEmpty()) return;
        }
        event.setCancelled(true);
        seatManager.sitPlayer(player, block);
    }

    private boolean isValidSeat(Block block) {
        String type = block.getType().name();
        if (type.contains("STAIRS")) return true;
        if (type.contains("SLAB")) {
            if (block.getBlockData() instanceof Slab) {
                return ((Slab) block.getBlockData()).getType() == Slab.Type.BOTTOM;
            }
            return true;
        }
        return false;
    }
}