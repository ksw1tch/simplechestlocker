package dev.vryxon.simplechestlocker.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleChestLockerClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("simplechestlocker");

    @Override
    public void onInitializeClient() {
        // Force the LockVisuals class to load (registers items on first reference)
        var locked = LockVisuals.LOCKED;
        var unlocked = LockVisuals.UNLOCKED;
        LockClientState.init();
        LockOverlayRenderer.init();
    }
}
