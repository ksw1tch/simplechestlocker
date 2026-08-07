package dev.vryxon.simplechestlocker.client;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public final class LockVisuals {
    public static final Identifier LOCKED_ID =
            Identifier.fromNamespaceAndPath("simplechestlocker", "lock_locked");
    public static final Identifier UNLOCKED_ID =
            Identifier.fromNamespaceAndPath("simplechestlocker", "lock_unlocked");

    public static final ResourceKey<Item> LOCKED_KEY =
            ResourceKey.create(Registries.ITEM, LOCKED_ID);
    public static final ResourceKey<Item> UNLOCKED_KEY =
            ResourceKey.create(Registries.ITEM, UNLOCKED_ID);

    public static final Item LOCKED;
    public static final Item UNLOCKED;

    static {
        LOCKED = registerLockItem(LOCKED_KEY);
        UNLOCKED = registerLockItem(UNLOCKED_KEY);
    }

    private LockVisuals() {}

    private static Item registerLockItem(ResourceKey<Item> key) {
        Item.Properties props = new Item.Properties().setId(key);
        Item item = new Item(props);
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }
}
