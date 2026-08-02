package dev.vryxon.simplechestlocker;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleChestLocker implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("simplechestlocker");
    public static String ver = "1.2.0";

    @Override
    public void onInitialize() {
        LOGGER.info("[SimpleChestLocker] SimpleChestLocker v" + ver + " has been loaded!");
        CommandHandler.registerCommands();
        LockEvents.register();
    }
}
