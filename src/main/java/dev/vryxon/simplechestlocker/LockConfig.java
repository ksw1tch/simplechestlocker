package dev.vryxon.simplechestlocker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LockConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("simplechestlocker.json");

    private static LockConfig INSTANCE;

    public boolean protectFromHoppers = true;
    public boolean protectFromPistons = true;
    public boolean protectFromExplosions = true;
    public List<String> blockBlacklist = new ArrayList<>(List.of("example:block", "example:block2"));

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
                if (INSTANCE.blockBlacklist == null) {
                    INSTANCE.blockBlacklist = new ArrayList<>();
                }
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

    public static boolean isBlacklisted(Level world, BlockPos pos) {
        List<String> blacklist = get().blockBlacklist;
        if (blacklist == null || blacklist.isEmpty()) {
            return false;
        }
        BlockState state = world.getBlockState(pos);
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (blockId == null) {
            return false;
        }
        String fullId = blockId.toString();
        for (String entry : blacklist) {
            if (entry == null) continue;
            if (entry.equals(fullId)) {
                return true;
            }
            String trimmed = entry.trim();
            if (trimmed.equals(fullId)) {
                return true;
            }
        }
        return false;
    }
}