package com.finndog.loottablepeeker;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
// 26.1 renamed ClickType to ContainerInput, changing AbstractContainerMenu#clicked's signature.
//? if >=26.1 {
/*import net.minecraft.world.inventory.ContainerInput;
*///?} else {
import net.minecraft.world.inventory.ClickType;
//?}
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * A read-only chest GUI showing what a container's loot table <em>would</em> roll, with a reroll
 * button in the bottom row. Nothing here touches the real block entity: the loot is rolled into a
 * scratch container every time, so the container keeps its unresolved loot table no matter how many
 * times it is previewed.
 *
 * <p>The menu reuses the vanilla {@code GENERIC_9xN} chest screens so no client-side mod is needed.
 * Loot table lookup and item naming are delegated to {@link LootTableAccess} and
 * {@link ItemDecorator}, which own the differences between Minecraft versions.</p>
 */
public class LootPreviewMenu extends ChestMenu {

    private static final int COLUMNS = 9;
    /** Leaves room for the control row inside a six-row screen, the largest vanilla chest GUI. */
    private static final int MAX_LOOT_ROWS = 5;
    private static final int INFO_COLUMN = 0;
    private static final int REROLL_COLUMN = 4;

    private final ServerLevel level;
    private final BlockPos pos;
    private final RandomizableContainerBlockEntity container;
    private final String tableId;
    private final Player viewer;
    private final SimpleContainer display;
    /** Size of the real container, so the preview lays items out over the same slot count. */
    private final int lootSlots;
    private final int lootRows;

    private long seed;
    private boolean usingContainerSeed;
    private int rollCount;
    private int filledSlots;
    private int totalItems;
    private Status status = Status.OK;
    private String failure;

    /** An empty loot area on its own is ambiguous, so each way of ending up with one is named. */
    private enum Status {
        OK,
        EMPTY_ROLL,
        MISSING_TABLE,
        FAILED
    }

    public static void open(ServerPlayer player, ServerLevel level, BlockPos pos,
                            RandomizableContainerBlockEntity container, String tableId) {
        int lootSlots = Math.max(1, container.getContainerSize());
        long seed = LootTableAccess.seedOf(container);
        Layout layout = Layout.forSize(lootSlots);

        player.openMenu(new SimpleMenuProvider(
            (containerId, inventory, viewer) -> new LootPreviewMenu(
                containerId, inventory, viewer, layout,
                new SimpleContainer(layout.rows() * COLUMNS),
                level, pos, container, tableId, lootSlots, seed),
            Component.literal(tableId)
        ));
    }

    private LootPreviewMenu(int containerId, Inventory inventory, Player viewer, Layout layout,
                            SimpleContainer display, ServerLevel level, BlockPos pos,
                            RandomizableContainerBlockEntity container, String tableId,
                            int lootSlots, long seed) {
        super(layout.type(), containerId, inventory, display, layout.rows());
        this.level = level;
        this.pos = pos;
        this.container = container;
        this.tableId = tableId;
        this.viewer = viewer;
        this.display = display;
        this.lootSlots = lootSlots;
        this.lootRows = layout.lootRows();

        // A seed of RANDOMIZE_SEED means the container would roll fresh on open, so pick one now to
        // have something concrete to display (and to let the tester reproduce it).
        this.usingContainerSeed = seed != LootTableAccess.RANDOMIZE_SEED;
        this.seed = this.usingContainerSeed ? seed : level.getRandom().nextLong();
        roll();
    }

    private int rerollSlot() {
        return this.lootRows * COLUMNS + REROLL_COLUMN;
    }

    /** Rolls the loot table into a scratch container and copies the result into the display slots. */
    private void roll() {
        this.rollCount++;
        this.filledSlots = 0;
        this.totalItems = 0;
        this.status = Status.OK;
        this.failure = null;

        SimpleContainer scratch = new SimpleContainer(this.lootSlots);
        try {
            LootTable table = LootTableAccess.tableOf(this.level.getServer(), this.container);
            if (table == null) {
                // A typo'd or datapack-less table id would otherwise look exactly like a table that
                // happens to roll nothing.
                LootTablePeeker.LOGGER.warn("No loot table registered as {}", this.tableId);
                this.status = Status.MISSING_TABLE;
            } else {
                LootParams params = new LootParams.Builder(this.level)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(this.pos))
                    .withParameter(LootContextParams.THIS_ENTITY, this.viewer)
                    .withLuck(this.viewer.getLuck())
                    .create(LootContextParamSets.CHEST);
                table.fill(scratch, params, this.seed);
            }
        } catch (Exception e) {
            // A table wanting parameters a chest context cannot supply would otherwise take down the
            // interaction, which is a rough failure mode for a debugging tool.
            LootTablePeeker.LOGGER.error("Failed to roll loot table {}", this.tableId, e);
            scratch.clearContent();
            this.status = Status.FAILED;
            this.failure = String.valueOf(e);
        }

        int lootArea = this.lootRows * COLUMNS;
        for (int slot = 0; slot < lootArea; slot++) {
            if (slot >= this.lootSlots) {
                // Slots past the real container size are padding, not empty loot slots.
                this.display.setItem(slot, ItemDecorator.filler());
                continue;
            }
            ItemStack stack = scratch.getItem(slot);
            if (!stack.isEmpty()) {
                this.filledSlots++;
                this.totalItems += stack.getCount();
            }
            this.display.setItem(slot, stack);
        }

        if (this.status == Status.OK && this.filledSlots == 0) {
            this.status = Status.EMPTY_ROLL;
        }
        if (this.status != Status.OK) {
            // Counting is already done, so the marker never inflates the item totals.
            this.display.setItem(Math.min(lootArea / 2, this.lootSlots - 1), statusMarker());
        }
        buildControlRow();
    }

    /** Placed in the middle of an otherwise empty loot area to say why it is empty. */
    private ItemStack statusMarker() {
        return switch (this.status) {
            case EMPTY_ROLL -> ItemDecorator.control(Items.STRUCTURE_VOID,
                Component.literal("Rolled nothing").withStyle(ChatFormatting.YELLOW),
                List.of(
                    loreLine("", "This table produced no items."),
                    loreLine("", "That can be normal — try rerolling.")));
            case MISSING_TABLE -> ItemDecorator.control(Items.BARRIER,
                Component.literal("Loot table not found").withStyle(ChatFormatting.RED),
                List.of(
                    loreLine("", "Nothing is registered under this id."),
                    loreLine("", "Check for a typo or a missing datapack.")));
            case FAILED -> ItemDecorator.control(Items.BARRIER,
                Component.literal("Roll failed").withStyle(ChatFormatting.RED),
                List.of(
                    loreLine("", String.valueOf(this.failure)),
                    loreLine("", "See the server log for the stack trace.")));
            case OK -> ItemStack.EMPTY;
        };
    }

    private void buildControlRow() {
        int base = this.lootRows * COLUMNS;
        for (int column = 0; column < COLUMNS; column++) {
            this.display.setItem(base + column, ItemDecorator.filler());
        }
        this.display.setItem(base + INFO_COLUMN, infoItem());
        this.display.setItem(base + REROLL_COLUMN, rerollItem());
    }

    private ItemStack infoItem() {
        List<Component> lore = new ArrayList<>();
        lore.add(loreLine("Table: ", this.tableId));
        if (this.status == Status.OK) {
            lore.add(loreLine("Rolled: ", this.totalItems + " item" + (this.totalItems == 1 ? "" : "s")
                + " in " + this.filledSlots + "/" + this.lootSlots + " slots"));
        } else {
            lore.add(loreLine("Rolled: ", statusSummary(), statusColour()));
        }
        lore.add(loreLine("Seed: ", this.seed + (this.usingContainerSeed ? " (from container)" : " (random)")));
        lore.add(loreLine("Roll: ", "#" + this.rollCount));
        return ItemDecorator.control(Items.BOOK,
            Component.literal("Loot Preview").withStyle(ChatFormatting.YELLOW), lore);
    }

    private String statusSummary() {
        return switch (this.status) {
            case EMPTY_ROLL -> "nothing";
            case MISSING_TABLE -> "loot table not found";
            case FAILED -> "roll failed";
            case OK -> "";
        };
    }

    private ChatFormatting statusColour() {
        return this.status == Status.EMPTY_ROLL ? ChatFormatting.YELLOW : ChatFormatting.RED;
    }

    private ItemStack rerollItem() {
        return ItemDecorator.control(Items.ENDER_EYE,
            Component.literal("Reroll").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
            List.of(loreLine("", "Click to roll this table again")));
    }

    private static Component loreLine(String label, String value) {
        return loreLine(label, value, ChatFormatting.WHITE);
    }

    private static Component loreLine(String label, String value, ChatFormatting valueColour) {
        return Component.literal(label).withStyle(ChatFormatting.GRAY)
            .append(Component.literal(value).withStyle(valueColour))
            .withStyle(style -> style.withItalic(false));
    }

    /**
     * The preview is read-only, so every click is swallowed. The server writes the client's predicted
     * slot changes into its remote-state tracking before the next {@code broadcastChanges()}, which
     * then corrects the client back to the real (unchanged) contents.
     */
    //? if >=26.1 {
    /*@Override
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        onClick(slotId, input == ContainerInput.PICKUP || input == ContainerInput.QUICK_MOVE);
    }
    *///?} else {
    @Override
    public void clicked(int slotId, int button, ClickType input, Player player) {
        onClick(slotId, input == ClickType.PICKUP || input == ClickType.QUICK_MOVE);
    }
    //?}

    /**
     * Shared click body. Only a plain left/shift click on the reroll slot does anything; drags and
     * other input kinds are ignored so a held-button sweep cannot spam rerolls.
     */
    private void onClick(int slotId, boolean plainClick) {
        if (slotId == rerollSlot() && plainClick) {
            this.seed = this.level.getRandom().nextLong();
            this.usingContainerSeed = false;
            roll();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    /** Chest screen dimensions: one row per nine container slots, plus a row for the controls. */
    private record Layout(MenuType<ChestMenu> type, int rows, int lootRows) {

        static Layout forSize(int containerSize) {
            int lootRows = Math.min(MAX_LOOT_ROWS, Math.max(1, (containerSize + COLUMNS - 1) / COLUMNS));
            int rows = lootRows + 1;
            MenuType<ChestMenu> type = switch (rows) {
                case 2 -> MenuType.GENERIC_9x2;
                case 3 -> MenuType.GENERIC_9x3;
                case 4 -> MenuType.GENERIC_9x4;
                case 5 -> MenuType.GENERIC_9x5;
                default -> MenuType.GENERIC_9x6;
            };
            return new Layout(type, rows, lootRows);
        }
    }
}
