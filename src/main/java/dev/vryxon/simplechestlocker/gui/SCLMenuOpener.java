package dev.vryxon.simplechestlocker.gui;

import dev.vryxon.simplechestlocker.LockManager;
import dev.vryxon.simplechestlocker.LockableContainers;
import dev.vryxon.simplechestlocker.commands.CommandHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.Set;

public class SCLMenuOpener {

    private static final Map<ServerPlayer, SCLMenuOpener> OPENERS = new WeakHashMap<>();

    private final ServerPlayer player;

    public SCLMenuOpener(ServerPlayer player) {
        this.player = player;
    }

    public static SCLMenuOpener get(ServerPlayer player) {
        return OPENERS.computeIfAbsent(player, SCLMenuOpener::new);
    }

    public void requestPlayerInput(String prompt, GlobalPos containerPos, Runnable reopenGui) {
        player.closeContainer();
        SignInputHandler.request(player, prompt, (input) -> {
            if (!input.isEmpty()) {
                String command = "scl trust " + input;
                String ref = CommandHelper.targetRef(player, containerPos);
                if (ref != null) {
                    command += " " + ref;
                }
                player.level().getServer().getCommands().performPrefixedCommand(
                        player.createCommandSourceStack(), command);
            }
            reopenGui.run();
        });
    }

    public void requestLabelInput(String prompt, GlobalPos containerPos, String currentLabel, Runnable reopenGui) {
        player.closeContainer();
        SignInputHandler.request(player, prompt, (input) -> {
            if (!input.isEmpty()) {
                String command;
                String ref = CommandHelper.targetRef(player, containerPos);
                if (ref != null) {
                    String action = currentLabel == null ? "label" : "rename";
                    command = "scl " + action + " " + input + " " + ref;
                } else if (currentLabel == null) {
                    command = "scl label " + input;
                } else {
                    command = "scl rename " + input;
                }
                player.level().getServer().getCommands().performPrefixedCommand(
                        player.createCommandSourceStack(), command);
            }
            reopenGui.run();
        });
    }

    public void openMainMenu() {
        BlockPos targetedPos = GuiUtils.getTargetedContainerPos(player);
        boolean isLocked = false;

        if (targetedPos != null) {
            ServerLevel world = player.level().getLevel();
            isLocked = LockManager.isAnyLocked(world, targetedPos);
        }

        boolean canLockOrManage = targetedPos != null;
        SCLContainer.MainMenuContext ctx = new SCLContainer.MainMenuContext(isLocked, canLockOrManage);
        SCLContainer container = new SCLContainer(SCLMenu.GUI_SIZE, player, SCLContainer.GuiType.MAIN_MENU, ctx);
        openGui(container, Component.literal("SimpleChestLocker"));
    }

    public void openContainerList() {
        ServerLevel world = player.level().getLevel();
        UUID playerId = player.getUUID();
        List<GlobalPos> owned = LockManager.getSortedLockedContainersForOwner(world, playerId);

        List<SCLContainer.ContainerEntry> entries = new ArrayList<>();
        for (GlobalPos gp : owned) {
            ServerLevel level = world.getServer().getLevel(gp.dimension());
            if (level == null) continue;

            String label = LockManager.getLabel(level, playerId, gp.pos());
            String type = LockableContainers.getContainerType(level, gp.pos());
            BlockState state = level.getBlockState(gp.pos());
            Block block = state.getBlock();
            int index = LockManager.getContainerIndex(world, playerId, gp);

            String dim = gp.dimension().identifier().toString();
            entries.add(new SCLContainer.ContainerEntry(block, type, label, index,
                    gp.pos().getX(), gp.pos().getY(), gp.pos().getZ(), dim, gp));
        }

        SCLContainer.ContainerEntry[] entryArray = entries.toArray(new SCLContainer.ContainerEntry[0]);
        SCLContainer container = new SCLContainer(SCLMenu.GUI_SIZE, player, SCLContainer.GuiType.CONTAINER_LIST, entryArray);
        openGui(container, Component.literal("SimpleChestLocker - Containers"));
    }

    public void openManageContainer() {
        BlockPos targetedPos = GuiUtils.getTargetedContainerPos(player);
        if (targetedPos == null) return;
        openManageContainer(GlobalPos.of(player.level().getLevel().dimension(), targetedPos));
    }

    public void openManageContainer(GlobalPos pos) {
        SCLContainer container = new SCLContainer(SCLMenu.GUI_SIZE, player,
                SCLContainer.GuiType.MANAGE_CONTAINER, pos);
        openGui(container, Component.literal("SimpleChestLocker - Manage"));
    }

    public void openTrustList(GlobalPos pos) {
        ServerLevel world = player.level().getLevel();
        UUID playerId = player.getUUID();
        Set<String> trusted = LockManager.getTrusted(world, playerId, pos.pos());

        List<String> trustedList = new ArrayList<>(trusted);
        SCLContainer.TrustContext ctx = new SCLContainer.TrustContext(pos, trustedList);
        SCLContainer container = new SCLContainer(SCLMenu.GUI_SIZE, player,
                SCLContainer.GuiType.TRUST_LIST, ctx);
        openGui(container, Component.literal("SimpleChestLocker - Trusts"));
    }

    public void openLabelManage(GlobalPos pos) {
        ServerLevel world = player.level().getLevel();
        UUID playerId = player.getUUID();
        String label = LockManager.getLabel(world, playerId, pos.pos());

        SCLContainer.LabelContext ctx = new SCLContainer.LabelContext(pos, label);
        SCLContainer container = new SCLContainer(SCLMenu.GUI_SIZE, player,
                SCLContainer.GuiType.LABEL_MANAGE, ctx);
        openGui(container, Component.literal("SimpleChestLocker - Label"));
    }

    public void closeGui() {
        player.closeContainer();
    }

    private void openGui(SCLContainer container, Component title) {
        SimpleMenuProvider provider = new SimpleMenuProvider(
                (syncId, inv, p) -> new SCLMenu(syncId, (ServerPlayer) p, container),
                title);
        player.openMenu(provider);
    }
}