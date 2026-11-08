package net.azisaba.simplesit

import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Bisected
import org.bukkit.block.data.type.Slab
import org.bukkit.block.data.type.Stairs
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID

class SeatManager(private val plugin: SimpleSit) {

    companion object {
        const val SEAT_TAG = "SimpleSitSeat"
        const val ADMIN_PERMISSION = "simplesit.admin"
    }

    private val previousLocations = HashMap<UUID, Location>()

    private var sitOffset = 0.175
    private var isWhitelist = false
    private var restrictedWorlds: Set<String> = emptySet()

    /** 他プラグインによる座席の生成・騎乗キャンセルを上書きするか */
    var forceSeat = true
        private set

    var msgPrefix = ""
        private set
    var msgReload = ""
        private set
    var msgNoPerm = ""
        private set
    var msgDisabledWorld = ""
        private set
    var msgOnlyPlayer = ""
        private set
    var msgSitDeny = ""
        private set

    init {
        loadConfig()
    }

    fun loadConfig() {
        plugin.reloadConfig()
        val config = plugin.config
        sitOffset = config.getDouble("sit-offset", 0.175)
        isWhitelist = config.getString("world-restriction.type", "blacklist").equals("whitelist", ignoreCase = true)
        // 前後の空白・大文字小文字の違いで判定が外れないよう正規化しておく
        restrictedWorlds = config.getStringList("world-restriction.list")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()
        forceSeat = config.getBoolean("force-seat", true)

        fun msg(key: String, def: String): String =
            ChatColor.translateAlternateColorCodes('&', config.getString("messages.$key", def) ?: def)

        msgPrefix = msg("prefix", "&7[&6SimpleSit&7] &r")
        msgReload = msg("reload", "&a設定をリロードしました")
        msgNoPerm = msg("no-permission", "&c権限がありません")
        msgDisabledWorld = msg("disabled-world", "&cこのワールドでは座ることはできません")
        msgOnlyPlayer = msg("only-player", "&cこのコマンドはプレイヤーのみ実行できます")
        msgSitDeny = msg("sit-deny", "&cここで座ることはできません！")

        plugin.logger.info(
            "World restriction: type=${if (isWhitelist) "whitelist" else "blacklist"}, " +
                "list=$restrictedWorlds, force-seat=$forceSeat"
        )
    }

    fun isWorldEnabled(world: World?): Boolean {
        if (world == null) return false
        val listed = world.name.lowercase() in restrictedWorlds
        return if (isWhitelist) listed else !listed
    }

    fun canSitIn(player: Player): Boolean =
        isWorldEnabled(player.world) || player.hasPermission(ADMIN_PERMISSION)

    fun sitPlayer(player: Player, block: Block): Boolean {
        if (player.vehicle != null) return false
        val seatLoc = block.location.add(0.5, 0.0, 0.5)
        val data = block.blockData
        val heightOffset = when {
            data is Stairs -> {
                seatLoc.yaw = getStairYaw(data.facing)
                if (data.half == Bisected.Half.TOP) 1.0 else 0.5
            }
            data is Slab -> if (data.type == Slab.Type.BOTTOM) 0.5 else 1.0
            block.type.name.contains("CARPET") -> 0.0625
            else -> 1.0
        }
        seatLoc.add(0.0, heightOffset - sitOffset, 0.0)
        return spawnSeatAndRide(player, seatLoc)
    }

    fun sitOnGround(player: Player): Boolean {
        if (player.vehicle != null) return false
        val playerLoc = player.location
        val seatLoc = playerLoc.clone().add(0.0, -sitOffset, 0.0).apply {
            x = playerLoc.blockX + 0.5
            z = playerLoc.blockZ + 0.5
        }
        return spawnSeatAndRide(player, seatLoc)
    }

    private fun spawnSeatAndRide(player: Player, loc: Location): Boolean {
        val beforeSit = player.location
        val seat = player.world.spawn(loc, ArmorStand::class.java) { s ->
            s.isVisible = false
            s.setGravity(false)
            s.isMarker = true
            s.setBasePlate(false)
            s.isSmall = true
            s.setArms(false)
            s.canPickupItems = false
            s.isInvulnerable = true
            s.addScoreboardTag(SEAT_TAG)
        }

        // 他プラグイン (WorldGuard / Towny / ワールド保護系など) に生成を潰されると isValid が false になる
        if (!seat.isValid) {
            plugin.logger.warning(
                "Seat spawn was cancelled by another plugin (player=${player.name}, world=${player.world.name}, " +
                    "loc=${loc.blockX},${loc.blockY},${loc.blockZ})"
            )
            seat.remove()
            return false
        }

        previousLocations[player.uniqueId] = beforeSit
        if (!seat.addPassenger(player)) {
            plugin.logger.warning(
                "Mounting seat was cancelled by another plugin (player=${player.name}, world=${player.world.name})"
            )
            previousLocations.remove(player.uniqueId)
            seat.remove()
            return false
        }
        rotateSeatWithPlayer(player, seat)
        return true
    }

    private fun rotateSeatWithPlayer(player: Player, seat: ArmorStand) {
        object : BukkitRunnable() {
            override fun run() {
                if (!player.isOnline || !seat.isValid || player.vehicle != seat) {
                    cancel()
                    if (seat.isValid) seat.remove()
                    return
                }
                val yaw = player.location.yaw
                val loc = seat.location
                if (loc.yaw == yaw) return
                seat.setRotation(yaw, loc.pitch)
            }
        }.runTaskTimer(plugin, 0L, 2L)
    }

    private fun getStairYaw(face: BlockFace): Float = when (face) {
        BlockFace.NORTH -> 0f
        BlockFace.SOUTH -> 180f
        BlockFace.WEST -> -90f
        BlockFace.EAST -> 90f
        else -> 0f
    }

    fun popPreviousLocation(uuid: UUID): Location? = previousLocations.remove(uuid)

    fun removeData(uuid: UUID) {
        previousLocations.remove(uuid)
    }

    fun clearSeatState(player: Player?) {
        if (player == null) return
        val vehicle = player.vehicle
        if (vehicle != null && isCustomSeat(vehicle)) {
            vehicle.remove()
        }
        removeData(player.uniqueId)
    }

    fun isCustomSeat(entity: Entity?): Boolean =
        entity != null && SEAT_TAG in entity.scoreboardTags

    fun removeAllSeats() {
        for (uuid in previousLocations.keys) {
            val vehicle = plugin.server.getPlayer(uuid)?.vehicle ?: continue
            if (isCustomSeat(vehicle)) vehicle.remove()
        }
        previousLocations.clear()
    }
}
