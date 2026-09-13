package net.azisaba.simplesit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import net.azisaba.simplesit.data.PlayerManager;
import net.azisaba.simplesit.data.PlayerSettings;

import java.util.*;

public class SeatManager {

    private final SimpleSit plugin;
    private final PlayerManager playerManager;
    private final Map<UUID, Location> previousLocations = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Set<UUID> dismountingPlayers = new HashSet<>();
    private static final long COOLDOWN_MILLIS = 500L;
    private final String SEAT_TAG = "SimpleSitSeat";

    private double sitOffset = 0.175;
    private boolean rightClickSit = true;
    private boolean isWhitelist = false;
    private List<String> restrictedWorlds;

    public Component msgPrefix;
    public Component msgReload;
    public Component msgNoPerm;
    public Component msgDisabledWorld;
    public Component msgOnlyPlayer;
    public Component msgSitDeny;
    public Component msgToggleEnabled;
    public Component msgToggleDisabled;
    public Component msgRightClickDisabled;


    public SeatManager(SimpleSit plugin, PlayerManager playerManager) {
        this.plugin = plugin;
        this.playerManager = playerManager;
        loadConfig();
    }

    public void loadConfig() {
        plugin.reloadConfig();
        this.sitOffset = plugin.getConfig().getDouble("sit-offset", 0.175);
        this.rightClickSit = plugin.getConfig().getBoolean("right-click-sit", true);
        String type = plugin.getConfig().getString("world-restriction.type", "blacklist");
        this.isWhitelist = "whitelist".equalsIgnoreCase(type);
        this.restrictedWorlds = plugin.getConfig().getStringList("world-restriction.list");
        this.msgPrefix = parseMessage("messages.prefix", "&7[&6SimpleSit&7] &r");
        this.msgReload = parseMessage("messages.reload", "&a設定をリロードしました");
        this.msgNoPerm = parseMessage("messages.no-permission", "&c権限がありません");
        this.msgDisabledWorld = parseMessage("messages.disabled-world", "&cこのワールドでは座れません");
        this.msgOnlyPlayer = parseMessage("messages.only-player", "&cプレイヤーのみ実行可能です");
        this.msgSitDeny = parseMessage("messages.sit-deny", "&cここで座ることはできません");
        this.msgToggleEnabled = parseMessage("messages.toggle-enabled", "&a階段などの右クリック着席を有効にしました");
        this.msgToggleDisabled = parseMessage("messages.toggle-disabled", "&c階段などの右クリック着席を無効にしました");
        this.msgRightClickDisabled = parseMessage("messages.right-click-disabled", "&c階段などの右クリック着席機能は無効化されています");
    }

    private Component parseMessage(String key, String def) {
        String raw = plugin.getConfig().getString(key, def);
        return LegacyComponentSerializer.legacyAmpersand().deserialize(raw.replace('§', '&'));
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public boolean isRightClickSitGloballyEnabled() {
        return rightClickSit;
    }

    public boolean isRightClickSitEnabled(Player player) {
        if (!rightClickSit) return false;
        return playerManager.getSettings(player.getUniqueId()).isRightClickSit();
    }

    public boolean toggleRightClickSit(Player player) {
        PlayerSettings settings = playerManager.getSettings(player.getUniqueId());
        boolean newState = !settings.isRightClickSit();
        settings.setRightClickSit(newState);
        playerManager.saveAsync(player.getUniqueId());
        return newState;
    }

    public boolean isWorldEnabled(World world) {
        if (world == null) return true;
        String name = world.getName();
        if (isWhitelist) return restrictedWorlds.contains(name);
        else return !restrictedWorlds.contains(name);
    }

    public double getSitOffset() {
        return sitOffset;
    }

    public boolean isOnCooldown(UUID uuid) {
        Long time = cooldowns.get(uuid);
        return time != null && (System.currentTimeMillis() - time) < COOLDOWN_MILLIS;
    }

    public void setCooldown(UUID uuid) {
        cooldowns.put(uuid, System.currentTimeMillis());
    }

    public boolean isDismounting(UUID uuid) {
        return dismountingPlayers.contains(uuid);
    }

    public void setDismounting(UUID uuid, boolean dismounting) {
        if (dismounting) {
            dismountingPlayers.add(uuid);
        } else {
            dismountingPlayers.remove(uuid);
        }
    }

    public void sitPlayer(Player player, Block block) {
        if (player.getVehicle() != null || isDismounting(player.getUniqueId()) || isOnCooldown(player.getUniqueId())) return;
        Location seatLoc = block.getLocation().add(0.5, 0, 0.5);
        double heightOffset = 0.0;
        if (block.getBlockData() instanceof Stairs stairs) {
            if (stairs.getHalf() == Bisected.Half.TOP) {
                heightOffset = 1.0;
            } else {
                heightOffset = 0.5;
            }
            float yaw = getStairYaw(stairs.getFacing());
            seatLoc.setYaw(yaw);
        } else if (block.getBlockData() instanceof Slab slab) {
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
        setCooldown(player.getUniqueId());
    }

    public void sitOnGround(Player player) {
        if (player.getVehicle() != null || isDismounting(player.getUniqueId()) || isOnCooldown(player.getUniqueId())) return;
        Location playerLoc = player.getLocation();
        Location seatLoc = playerLoc.clone().add(0, -sitOffset, 0);
        seatLoc.setX(playerLoc.getBlockX() + 0.5);
        seatLoc.setZ(playerLoc.getBlockZ() + 0.5);
        saveLocation(player);
        spawnSeatAndRide(player, seatLoc);
        setCooldown(player.getUniqueId());
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
            s.setPersistent(false);
            s.addScoreboardTag(SEAT_TAG);
        });
        seat.addPassenger(player);
    }

    public Location getSafeDismountLocation(Player player, Entity vehicle, Location returnLocation) {
        Location seatLocation = vehicle != null ? vehicle.getLocation() : player.getLocation();
        World world = seatLocation.getWorld();

        if (returnLocation != null && returnLocation.getWorld() != null && returnLocation.getWorld().equals(world)) {
            double distSq = returnLocation.distanceSquared(seatLocation);
            if (distSq <= 25.0) {
                Block feetBlock = returnLocation.getBlock();
                Block headBlock = returnLocation.clone().add(0, 1, 0).getBlock();
                if (!feetBlock.getType().isSolid() && !headBlock.getType().isSolid()) {
                    Location safeReturn = returnLocation.clone();
                    safeReturn.setYaw(player.getLocation().getYaw());
                    safeReturn.setPitch(player.getLocation().getPitch());
                    safeReturn.add(0, 0.05, 0);
                    return safeReturn;
                }
            }
        }

        Block seatBlock = seatLocation.getBlock();
        if (seatBlock.isPassable() || seatBlock.isEmpty()) {
            Block below = seatBlock.getRelative(BlockFace.DOWN);
            if (below.getType().isSolid()) {
                seatBlock = below;
            }
        }

        double surfaceY = getBlockSurfaceY(seatBlock);
        return new Location(
                world,
                seatBlock.getX() + 0.5,
                surfaceY + 0.05,
                seatBlock.getZ() + 0.5,
                player.getLocation().getYaw(),
                player.getLocation().getPitch()
        );
    }

    private double getBlockSurfaceY(Block block) {
        if (block.getBlockData() instanceof Slab slab) {
            return slab.getType() == Slab.Type.BOTTOM ? block.getY() + 0.5 : block.getY() + 1.0;
        } else if (block.getBlockData() instanceof Stairs stairs) {
            return stairs.getHalf() == Bisected.Half.BOTTOM ? block.getY() + 0.5 : block.getY() + 1.0;
        } else if (block.getType().name().contains("CARPET")) {
            return block.getY() + 0.0625;
        } else if (block.isPassable() || block.isEmpty()) {
            return block.getY();
        } else {
            return block.getY() + 1.0;
        }
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
        if (!previousLocations.containsKey(player.getUniqueId())) {
            previousLocations.put(player.getUniqueId(), player.getLocation());
        }
    }

    public Location popPreviousLocation(UUID uuid) {
        return previousLocations.remove(uuid);
    }

    public void removeData(UUID uuid) {
        previousLocations.remove(uuid);
        cooldowns.remove(uuid);
        dismountingPlayers.remove(uuid);
    }

    public void clearSeatState(Player player) {
        if (player == null) return;
        Entity vehicle = player.getVehicle();
        if (vehicle != null && isCustomSeat(vehicle)) {
            vehicle.remove();
        }
        removeData(player.getUniqueId());
    }

    public boolean isCustomSeat(Entity entity) {
        if (entity == null) return false;
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
        cooldowns.clear();
        dismountingPlayers.clear();
    }
}
