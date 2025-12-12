package net.azisaba.simplesit;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class SeatManager {

    private final SimpleSit plugin;
    private final Map<UUID, Location> previousLocations = new HashMap<>();
    private final String SEAT_TAG = "SimpleSitSeat";

    private double sitOffset = 0.175;
    private boolean isWhitelist = false;
    private List<String> restrictedWorlds;

    public String msgPrefix;
    public String msgReload;
    public String msgNoPerm;
    public String msgDisabledWorld;
    public String msgOnlyPlayer;
    public String msgSitDeny;


    public SeatManager(SimpleSit plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.reloadConfig();
        this.sitOffset = plugin.getConfig().getDouble("sit-offset", 0.175);
        String type = plugin.getConfig().getString("world-restriction.type", "blacklist");
        this.isWhitelist = "whitelist".equalsIgnoreCase(type);
        this.restrictedWorlds = plugin.getConfig().getStringList("world-restriction.list");
        this.msgPrefix = ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getConfig().getString("messages.prefix", "&7[&6SimpleSit&7] &r")));
        this.msgReload = ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getConfig().getString("messages.reload", "&a設定をリロードしました")));
        this.msgNoPerm = ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getConfig().getString("messages.no-permission", "&c権限がありません")));
        this.msgDisabledWorld = ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getConfig().getString("messages.disabled-world", "&cこのワールドでは座ることはできません")));
        this.msgOnlyPlayer = ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getConfig().getString("messages.only-player", "&cこのコマンドはプレイヤーのみ実行できます")));
        this.msgSitDeny = ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(plugin.getConfig().getString("messages.sit-deny", "&cここで座ることはできません！")));
    }

    public boolean isWorldEnabled(World world) {
        if (world == null) return false;
        String name = world.getName();
        if (isWhitelist) return restrictedWorlds.contains(name);
        else return !restrictedWorlds.contains(name);
    }

    public void sitPlayer(Player player, Block block) {
        if (player.getVehicle() != null) return;
        Location seatLoc = block.getLocation().add(0.5, 0, 0.5);
        double heightOffset = 0.0;
        if (block.getBlockData() instanceof Stairs) {
            Stairs stairs = (Stairs) block.getBlockData();
            if (stairs.getHalf() == Bisected.Half.TOP) {
                heightOffset = 1.0;
            } else {
                heightOffset = 0.5;
            }
            float yaw = getStairYaw(stairs.getFacing());
            seatLoc.setYaw(yaw);
        } else if (block.getBlockData() instanceof Slab) {
            Slab slab = (Slab) block.getBlockData();
            if (slab.getType() == Slab.Type.BOTTOM) {
                heightOffset = 0.5;
            } else {
                heightOffset = 1.0;
            }
        } else if (block.getType().name().contains("CARPET")) {
            heightOffset = 0.0625;
        } else {
            heightOffset = 1.0;
        }
        seatLoc.add(0, heightOffset - sitOffset, 0);
        saveLocation(player);
        spawnSeatAndRide(player, seatLoc);
    }

    public void sitOnGround(Player player) {
        if (player.getVehicle() != null) return;
        Location playerLoc = player.getLocation();
        Location seatLoc = playerLoc.clone().add(0, -sitOffset, 0);
        seatLoc.setX(playerLoc.getBlockX() + 0.5);
        seatLoc.setZ(playerLoc.getBlockZ() + 0.5);
        saveLocation(player);
        spawnSeatAndRide(player, seatLoc);
    }

    private void spawnSeatAndRide(Player player, Location loc) {
        ArmorStand seat = player.getWorld().spawn(loc, ArmorStand.class, (ArmorStand s) -> {
            s.setVisible(false);
            s.setGravity(false);
            s.setMarker(true);
            s.setBasePlate(false);
            s.setSmall(true);
            s.setArms(false);
            s.setCanPickupItems(false);
            s.setInvulnerable(true);
            s.addScoreboardTag(SEAT_TAG);
        });
        seat.addPassenger(player);
        rotateSeatWithPlayer(player, seat);
    }

    private void rotateSeatWithPlayer(Player player, ArmorStand seat) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !seat.isValid() || player.getVehicle() != seat) {
                    this.cancel();
                    if (seat.isValid()) seat.remove();
                    return;
                }
                float yaw = player.getLocation().getYaw();
                Location loc = seat.getLocation();
                if (loc.getYaw() == yaw) return;
                loc.setYaw(yaw);
                seat.teleport(loc);
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    private float getStairYaw(org.bukkit.block.BlockFace face) {
        switch (face) {
            case NORTH: return 0;
            case SOUTH: return 180;
            case WEST:  return -90;
            case EAST:  return 90;
            default:    return 0;
        }
    }

    public void saveLocation(Player player) {
        previousLocations.put(player.getUniqueId(), player.getLocation());
    }

    public Location popPreviousLocation(UUID uuid) {
        return previousLocations.remove(uuid);
    }

    public void removeData(UUID uuid) {
        previousLocations.remove(uuid);
    }

    public boolean isCustomSeat(Entity entity) {
        return entity.getScoreboardTags().contains(SEAT_TAG);
    }

    public void removeAllSeats() {
        for (UUID uuid : previousLocations.keySet()) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null && p.getVehicle() != null && isCustomSeat(p.getVehicle())) {
                p.getVehicle().remove();
            }
        }
        previousLocations.clear();
    }
}
