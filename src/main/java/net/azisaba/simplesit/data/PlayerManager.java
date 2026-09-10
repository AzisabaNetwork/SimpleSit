package net.azisaba.simplesit.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.azisaba.simplesit.SimpleSit;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class PlayerManager {

    private final SimpleSit plugin;
    private final File playersDir;
    private final Gson gson;
    private final Map<UUID, PlayerSettings> cache = new ConcurrentHashMap<>();

    public PlayerManager(SimpleSit plugin) {
        this.plugin = plugin;
        this.playersDir = new File(plugin.getDataFolder(), "players");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        if (!playersDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            playersDir.mkdirs();
        }
    }

    public PlayerSettings getSettings(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::loadFromFile);
    }

    public void load(UUID uuid) {
        cache.put(uuid, loadFromFile(uuid));
    }

    private PlayerSettings loadFromFile(UUID uuid) {
        File file = new File(playersDir, uuid + ".json");
        if (!file.exists()) {
            return new PlayerSettings();
        }
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            PlayerSettings settings = gson.fromJson(reader, PlayerSettings.class);
            return settings != null ? settings : new PlayerSettings();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load player settings for " + uuid, e);
            return new PlayerSettings();
        }
    }

    public void save(UUID uuid) {
        PlayerSettings settings = cache.get(uuid);
        if (settings == null) return;
        saveToFile(uuid, settings);
    }

    public void saveAsync(UUID uuid) {
        PlayerSettings settings = cache.get(uuid);
        if (settings == null) return;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> saveToFile(uuid, settings));
    }

    private synchronized void saveToFile(UUID uuid, PlayerSettings settings) {
        if (!playersDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            playersDir.mkdirs();
        }
        File file = new File(playersDir, uuid + ".json");
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            gson.toJson(settings, writer);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save player settings for " + uuid, e);
        }
    }

    public void unload(UUID uuid) {
        save(uuid);
        cache.remove(uuid);
    }

    public void saveAll() {
        for (Map.Entry<UUID, PlayerSettings> entry : cache.entrySet()) {
            saveToFile(entry.getKey(), entry.getValue());
        }
    }
}
