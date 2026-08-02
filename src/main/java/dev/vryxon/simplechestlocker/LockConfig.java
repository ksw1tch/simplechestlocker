package dev.vryxon.simplechestlocker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LockConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("simplechestlocker.json");

    private static LockConfig INSTANCE;

    public boolean protectFromHoppers = true;
    public boolean protectFromPistons = true;
    public boolean protectFromExplosions = true;

    public static LockConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (var reader = Files.newBufferedReader(CONFIG_PATH)) {
                INSTANCE = GSON.fromJson(reader, LockConfig.class);
            } catch (IOException e) {
                SimpleChestLocker.LOGGER.warn("Failed to read config, using defaults", e);
                INSTANCE = new LockConfig();
            }
        } else {
            INSTANCE = new LockConfig();
            save();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (var writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(INSTANCE, LockConfig.class, writer);
            }
        } catch (IOException e) {
            SimpleChestLocker.LOGGER.warn("Failed to save config", e);
        }
    }

    public static void reload() {
        load();
    }
}