package me.toobad.simplechestlocker;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleChestLocker implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("simplechestlocker");

    @Override
    public void onInitialize() {
        LOGGER.info("[SimpleChestLocker] SimpleChestLocker has been loaded!");
        LockCommands.registerCommands();
        LockEvents.register();
    }
}
