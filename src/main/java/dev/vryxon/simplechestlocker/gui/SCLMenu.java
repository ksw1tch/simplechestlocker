package dev.vryxon.simplechestlocker.gui;

import dev.vryxon.simplechestlocker.commands.CommandHelper;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class SCLMenu extends AbstractContainerMenu {

    public static final int GUI_SIZE = 27;

    private final SCLContainer container;

    public SCLMenu(int syncId, ServerPlayer player, SCLContainer container) {
        super(MenuType.GENERIC_9x3, syncId);
        this.container = container;

        int guiRows = 3;
        for (int row = 0; row < guiRows; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = col + row * 9;
                this.addSlot(new SCLSlot(container, slotIndex, 8 + col * 18, 18 + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = col + row * 9;
                int x = 8 + col * 18;
                int y = 86 + row * 18;
                this.addSlot(new net.minecraft.world.inventory.Slot(player.getInventory(), slotIndex, x, y));
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (slotIndex >= 0 && slotIndex < GUI_SIZE && player instanceof ServerPlayer sp) {
            handleGuiClick(slotIndex, sp);
            return;
        }
        super.clicked(slotIndex, button, input, player);
    }

    private void handleGuiClick(int slotIndex, ServerPlayer player) {
        SCLContainer.GuiType guiType = container.getGuiType();
        SCLMenuOpener opener = SCLMenuOpener.get(player);

        switch (guiType) {
            case MAIN_MENU -> handleMainMenu(slotIndex, player, opener);
            case CONTAINER_LIST -> handleContainerList(slotIndex, player, opener);
            case MANAGE_CONTAINER -> handleManageContainer(slotIndex, player, opener);
            case TRUST_LIST -> handleTrustList(slotIndex, player, opener);
            case LABEL_MANAGE -> handleLabelManage(slotIndex, player, opener);
        }
    }

    private void handleMainMenu(int slotIndex, ServerPlayer player, SCLMenuOpener opener) {
        if (slotIndex == 11) {
            SCLContainer.MainMenuContext ctx = (SCLContainer.MainMenuContext) container.getContext();
            if (!ctx.canAct()) return;
            if (ctx.isLocked()) {
                opener.openManageContainer();
            } else {
                runCommand(player, "scl lock");
                opener.openMainMenu();
            }
        } else if (slotIndex == 13) {
            opener.openContainerList();
        } else if (slotIndex == 15) {
            opener.closeGui();
        }
    }

    private void handleContainerList(int slotIndex, ServerPlayer player, SCLMenuOpener opener) {
        if (slotIndex == 22) {
            opener.openMainMenu();
            return;
        }
        SCLContainer.ContainerEntry[] entries = getContainerEntries();
        if (slotIndex < entries.length) {
            opener.openManageContainer(entries[slotIndex].pos);
        }
    }

    private void handleManageContainer(int slotIndex, ServerPlayer player, SCLMenuOpener opener) {
        Object ctx = container.getContext();
        GlobalPos pos = resolvePos(ctx);
        if (pos == null) return;

        String ref = CommandHelper.targetRef(player, pos);

        if (slotIndex == 10) {
            runCommand(player, ref != null ? "scl unlock " + ref : "scl unlock");
            opener.openMainMenu();
        } else if (slotIndex == 13) {
            opener.openTrustList(pos);
        } else if (slotIndex == 16) {
            opener.openLabelManage(pos);
        } else if (slotIndex == 22) {
            opener.openContainerList();
        }
    }

    private void handleTrustList(int slotIndex, ServerPlayer player, SCLMenuOpener opener) {
        Object ctx = container.getContext();
        if (!(ctx instanceof SCLContainer.TrustContext trustCtx)) return;

        GlobalPos pos = trustCtx.pos();
        List<String> trusted = trustCtx.trusted();
        String ref = CommandHelper.targetRef(player, pos);

        if (slotIndex < trusted.size()) {
            String playerName = trusted.get(slotIndex);
            runCommand(player, ref != null ? "scl untrust " + playerName + " " + ref : "scl untrust " + playerName);
            opener.openTrustList(pos);
        } else if (slotIndex == 20) {
            opener.requestPlayerInput("Enter player name", pos, () -> opener.openTrustList(pos));
        } else if (slotIndex == 22) {
            opener.openManageContainer(pos);
        }
    }

    private void handleLabelManage(int slotIndex, ServerPlayer player, SCLMenuOpener opener) {
        Object ctx = container.getContext();
        if (!(ctx instanceof SCLContainer.LabelContext labelCtx)) return;

        GlobalPos pos = labelCtx.pos();
        String currentLabel = labelCtx.label();
        String ref = CommandHelper.targetRef(player, pos);

        if (currentLabel == null) {
            if (slotIndex == 13) {
                opener.requestLabelInput("Enter label", pos, currentLabel, () -> opener.openLabelManage(pos));
            }
        } else {
            if (slotIndex == 11) {
                opener.requestLabelInput("Rename label", pos, currentLabel, () -> opener.openLabelManage(pos));
            } else if (slotIndex == 15) {
                runCommand(player, ref != null ? "scl unlabel " + ref : "scl unlabel");
                opener.openLabelManage(pos);
            }
        }
        if (slotIndex == 22) {
            opener.openManageContainer(pos);
        }
    }

    private void runCommand(ServerPlayer player, String command) {
        player.level().getServer().getCommands().performPrefixedCommand(
                player.createCommandSourceStack(), command);
    }

    private GlobalPos resolvePos(Object ctx) {
        if (ctx instanceof GlobalPos gp) return gp;
        if (ctx instanceof SCLContainer.TrustContext tc) return tc.pos();
        if (ctx instanceof SCLContainer.LabelContext lc) return lc.pos();
        return null;
    }

    private SCLContainer.ContainerEntry[] getContainerEntries() {
        Object ctx = container.getContext();
        if (ctx instanceof SCLContainer.ContainerEntry[]) {
            return (SCLContainer.ContainerEntry[]) ctx;
        }
        if (ctx instanceof List) {
            List<?> list = (List<?>) ctx;
            return list.toArray(new SCLContainer.ContainerEntry[0]);
        }
        return new SCLContainer.ContainerEntry[0];
    }

    public SCLContainer getSCLContainer() {
        return container;
    }
}