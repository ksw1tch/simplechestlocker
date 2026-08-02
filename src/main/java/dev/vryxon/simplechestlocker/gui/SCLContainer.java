package dev.vryxon.simplechestlocker.gui;

import dev.vryxon.simplechestlocker.LockableContainers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public class SCLContainer extends SimpleContainer {

    private final ServerPlayer player;
    private final GuiType guiType;
    private final Object context;

    public SCLContainer(int size, ServerPlayer player, GuiType guiType, Object context) {
        super(size);
        this.player = player;
        this.guiType = guiType;
        this.context = context;
        populate();
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public GuiType getGuiType() {
        return guiType;
    }

    public Object getContext() {
        return context;
    }

    private void populate() {
        switch (guiType) {
            case MAIN_MENU -> populateMainMenu();
            case CONTAINER_LIST -> populateContainerList();
            case MANAGE_CONTAINER -> populateManageContainer();
            case TRUST_LIST -> populateTrustList();
            case LABEL_MANAGE -> populateLabelManage();
        }
    }

    private void populateMainMenu() {
        MainMenuContext ctx = (MainMenuContext) context;
        boolean isLocked = ctx.isLocked();
        boolean canAct = ctx.canAct();

        ItemStack contextItem;
        if (!canAct) {
            contextItem = new ItemStack(Blocks.RED_CONCRETE);
            contextItem.set(DataComponents.CUSTOM_NAME, Component.literal("§c§lNo container in sight"));
            contextItem.set(DataComponents.LORE, new ItemLore(List.of(
                    Component.literal("§7Look at a container to enable this")
            )));
        } else if (isLocked) {
            contextItem = new ItemStack(Blocks.CHEST);
            contextItem.set(DataComponents.CUSTOM_NAME, Component.literal("§a§lManage Container"));
            contextItem.set(DataComponents.LORE, new ItemLore(List.of(
                    Component.literal("§7Click to manage the container")
            )));
        } else {
            contextItem = new ItemStack(Blocks.IRON_DOOR);
            contextItem.set(DataComponents.CUSTOM_NAME, Component.literal("§a§lLock Container"));
            contextItem.set(DataComponents.LORE, new ItemLore(List.of(
                    Component.literal("§7Click to lock the container")
            )));
        }
        setItem(11, contextItem);

        ItemStack manageItem = new ItemStack(Blocks.HOPPER);
        manageItem.set(DataComponents.CUSTOM_NAME, Component.literal("§e§lManage Containers"));
        manageItem.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7View all your locked containers")
        )));
        setItem(13, manageItem);

        ItemStack quitItem = new ItemStack(Blocks.BARRIER);
        quitItem.set(DataComponents.CUSTOM_NAME, Component.literal("§c§lQuit"));
        setItem(15, quitItem);
    }

    private void populateContainerList() {
        ContainerEntry[] entries;
        if (context instanceof ContainerEntry[]) {
            entries = (ContainerEntry[]) context;
        } else if (context instanceof List) {
            List<?> list = (List<?>) context;
            entries = list.toArray(new ContainerEntry[0]);
        } else {
            entries = new ContainerEntry[0];
        }

        int slotIndex = 0;
        for (ContainerEntry entry : entries) {
            if (slotIndex >= 18) break;

            String display = "§7(Unlabeled)";
            if (entry.label != null) {
                display = entry.label;
            }

            ItemStack stack = new ItemStack(entry.iconBlock);
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(
                    "§fContainer §e" + entry.index + " §7- §e" + display));
            stack.set(DataComponents.LORE, new ItemLore(List.of(
                    Component.literal("§7Container type: §f" + entry.type),
                    Component.literal("§7Position: §e" + entry.x + ", " + entry.y + ", " + entry.z),
                    Component.literal("§7Dimension: §8" + entry.dimension),
                    Component.literal(""),
                    Component.literal("§aClick to manage")
            )));
            // stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            setItem(slotIndex, stack);
            slotIndex++;
        }

        ItemStack backItem = new ItemStack(Items.ARROW);
        backItem.set(DataComponents.CUSTOM_NAME, Component.literal("§7§lGo Back"));
        setItem(22, backItem);
    }

    private void populateManageContainer() {
        ItemStack unlockItem = new ItemStack(Blocks.REDSTONE_TORCH);
        unlockItem.set(DataComponents.CUSTOM_NAME, Component.literal("§c§lUnlock Container"));
        unlockItem.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7Click to unlock this container")
        )));
        setItem(10, unlockItem);

        ItemStack trustItem = new ItemStack(Items.NAME_TAG);
        trustItem.set(DataComponents.CUSTOM_NAME, Component.literal("§a§lManage Trusts"));
        trustItem.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7Click to manage trusted players")
        )));
        setItem(13, trustItem);

        ItemStack labelItem = new ItemStack(Items.PAPER);
        labelItem.set(DataComponents.CUSTOM_NAME, Component.literal("§b§lManage Label"));
        labelItem.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7Click to manage the label")
        )));
        setItem(16, labelItem);

        ItemStack backItem = new ItemStack(Items.ARROW);
        backItem.set(DataComponents.CUSTOM_NAME, Component.literal("§7§lGo Back"));
        setItem(22, backItem);
    }

    private void populateTrustList() {
        if (!(context instanceof TrustContext trustCtx)) return;

        List<String> trusted = trustCtx.trusted();

        int slotIndex = 0;
        for (String playerName : trusted) {
            if (slotIndex >= 18) break;

            ItemStack stack = new ItemStack(Items.SLIME_BALL);
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("§a" + playerName));
            stack.set(DataComponents.LORE, new ItemLore(List.of(
                    Component.literal("§7Click to untrust this player")
            )));
            setItem(slotIndex, stack);
            slotIndex++;
        }

        ItemStack addItem = new ItemStack(Items.LIME_DYE);
        addItem.set(DataComponents.CUSTOM_NAME, Component.literal("§a§l+ Add Player"));
        addItem.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7Click to add a player")
        )));
        setItem(20, addItem);

        ItemStack backItem = new ItemStack(Items.ARROW);
        backItem.set(DataComponents.CUSTOM_NAME, Component.literal("§7§lGo Back"));
        setItem(22, backItem);
    }

    private void populateLabelManage() {
        if (!(context instanceof LabelContext labelCtx)) return;

        String currentLabel = labelCtx.label();

        if (currentLabel == null) {
            ItemStack addItem = new ItemStack(Items.LIME_DYE);
            addItem.set(DataComponents.CUSTOM_NAME, Component.literal("§a§lAdd Label"));
            addItem.set(DataComponents.LORE, new ItemLore(List.of(
                    Component.literal("§7Click to set a label")
            )));
            setItem(13, addItem);
        } else {
            ItemStack renameItem = new ItemStack(Items.NAME_TAG);
            renameItem.set(DataComponents.CUSTOM_NAME, Component.literal("§e§lRename"));
            renameItem.set(DataComponents.LORE, new ItemLore(List.of(
                    Component.literal("§7Click to rename the label")
            )));
            setItem(11, renameItem);

            ItemStack removeItem = new ItemStack(Items.BARRIER);
            removeItem.set(DataComponents.CUSTOM_NAME, Component.literal("§c§lRemove"));
            removeItem.set(DataComponents.LORE, new ItemLore(List.of(
                    Component.literal("§7Click to remove the label")
            )));
            setItem(15, removeItem);
        }

        ItemStack backItem = new ItemStack(Items.ARROW);
        backItem.set(DataComponents.CUSTOM_NAME, Component.literal("§7§lGo Back"));
        setItem(22, backItem);
    }

    public enum GuiType {
        MAIN_MENU,
        CONTAINER_LIST,
        MANAGE_CONTAINER,
        TRUST_LIST,
        LABEL_MANAGE
    }

    public record MainMenuContext(boolean isLocked, boolean canAct) {}

    public record TrustContext(GlobalPos pos, List<String> trusted) {}

    public record LabelContext(GlobalPos pos, String label) {}

    public static class ContainerEntry {
        public final Block iconBlock;
        public final String type;
        public final String label;
        public final int index;
        public final int x;
        public final int y;
        public final int z;
        public final String dimension;
        public final GlobalPos pos;

        public ContainerEntry(Block iconBlock, String type, String label, int index, int x, int y, int z,
                              String dimension, GlobalPos pos) {
            this.iconBlock = iconBlock;
            this.type = type;
            this.label = label;
            this.index = index;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimension = dimension;
            this.pos = pos;
        }
    }
}