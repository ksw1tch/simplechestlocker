package dev.vryxon.simplechestlocker;

import dev.vryxon.simplechestlocker.gui.SignInputHandler;
import dev.vryxon.simplechestlocker.networking.LockOverlayServer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleChestLocker implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("simplechestlocker");
    public static String ver = "2.2.0";

    @Override
    public void onInitialize() {
        LOGGER.info("[SimpleChestLocker] SimpleChestLocker v" + ver + " has been loaded!");
        CommandHandler.registerCommands();
        LockEvents.register();
        LockOverlayServer.register();
        ServerTickEvents.END_SERVER_TICK.register(server -> SignInputHandler.tickCleanup());
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                SignInputHandler.cleanup(handler.getPlayer()));
    }
}
