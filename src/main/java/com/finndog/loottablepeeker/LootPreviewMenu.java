package com.finndog.loottablepeeker;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
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
 */
public class LootPreviewMenu extends ChestMenu {

    private static final int COLUMNS = 9;
    /** Leaves room for the control row inside a six-row screen, the largest vanilla chest GUI. */
    private static final int MAX_LOOT_ROWS = 5;
    private static final int INFO_COLUMN = 0;
    private static final int REROLL_COLUMN = 4;

    private final ServerLevel level;
    private final BlockPos pos;
    private final ResourceKey<LootTable> tableKey;
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

    public static void open(ServerPlayer player, ServerLevel level, BlockPos pos, RandomizableContainerBlockEntity container) {
        ResourceKey<LootTable> tableKey = container.getLootTable();
        if (tableKey == null) return;

        int lootSlots = Math.max(1, container.getContainerSize());
        long seed = container.getLootTableSeed();
        Layout layout = Layout.forSize(lootSlots);

        player.openMenu(new SimpleMenuProvider(
            (containerId, inventory, viewer) -> new LootPreviewMenu(
                containerId, inventory, viewer, layout,
                new SimpleContainer(layout.rows() * COLUMNS),
                level, pos, tableKey, lootSlots, seed),
            Component.literal(tableKey.location().toString())
        ));
    }

    private LootPreviewMenu(int containerId, Inventory inventory, Player viewer, Layout layout, SimpleContainer display,
                            ServerLevel level, BlockPos pos, ResourceKey<LootTable> tableKey, int lootSlots, long seed) {
        super(layout.type(), containerId, inventory, display, layout.rows());
        this.level = level;
        this.pos = pos;
        this.tableKey = tableKey;
        this.viewer = viewer;
        this.display = display;
        this.lootSlots = lootSlots;
        this.lootRows = layout.lootRows();

        // A seed of RANDOMIZE_SEED means the container would roll fresh on open, so pick one now to
        // have something concrete to display (and to let the tester reproduce it).
        this.usingContainerSeed = seed != LootTable.RANDOMIZE_SEED;
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

        SimpleContainer scratch = new SimpleContainer(this.lootSlots);
        try {
            LootTable table = this.level.getServer().reloadableRegistries().getLootTable(this.tableKey);
            LootParams params = new LootParams.Builder(this.level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(this.pos))
                .withParameter(LootContextParams.THIS_ENTITY, this.viewer)
                .withLuck(this.viewer.getLuck())
                .create(LootContextParamSets.CHEST);
            table.fill(scratch, params, this.seed);
        } catch (Exception e) {
            // A table wanting parameters a chest context cannot supply would otherwise take down the
            // interaction, which is a rough failure mode for a debugging tool.
            LootTablePeeker.LOGGER.error("Failed to roll loot table {}", this.tableKey.location(), e);
            scratch.clearContent();
            scratch.setItem(0, control(Items.BARRIER,
                Component.literal("Roll failed").withStyle(ChatFormatting.RED),
                List.of(loreLine("", String.valueOf(e)))));
        }

        int lootArea = this.lootRows * COLUMNS;
        for (int slot = 0; slot < lootArea; slot++) {
            if (slot >= this.lootSlots) {
                // Slots past the real container size are padding, not empty loot slots.
                this.display.setItem(slot, filler());
                continue;
            }
            ItemStack stack = scratch.getItem(slot);
            if (!stack.isEmpty()) {
                this.filledSlots++;
                this.totalItems += stack.getCount();
            }
            this.display.setItem(slot, stack);
        }
        buildControlRow();
    }

    private void buildControlRow() {
        int base = this.lootRows * COLUMNS;
        for (int column = 0; column < COLUMNS; column++) {
            this.display.setItem(base + column, filler());
        }
        this.display.setItem(base + INFO_COLUMN, infoItem());
        this.display.setItem(base + REROLL_COLUMN, rerollItem());
    }

    private ItemStack infoItem() {
        List<Component> lore = new ArrayList<>();
        lore.add(loreLine("Table: ", this.tableKey.location().toString()));
        lore.add(loreLine("Rolled: ", this.totalItems + " item" + (this.totalItems == 1 ? "" : "s")
            + " in " + this.filledSlots + "/" + this.lootSlots + " slots"));
        lore.add(loreLine("Seed: ", this.seed + (this.usingContainerSeed ? " (from container)" : " (random)")));
        lore.add(loreLine("Roll: ", "#" + this.rollCount));
        return control(Items.BOOK, Component.literal("Loot Preview").withStyle(ChatFormatting.YELLOW), lore);
    }

    private ItemStack rerollItem() {
        return control(Items.ENDER_EYE,
            Component.literal("Reroll").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
            List.of(loreLine("", "Click to roll this table again")));
    }

    private static ItemStack filler() {
        return control(Items.GRAY_STAINED_GLASS_PANE, Component.empty(), List.of());
    }

    private static ItemStack control(Item item, Component name, List<Component> lore) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, name.copy().withStyle(style -> style.withItalic(false)));
        if (!lore.isEmpty()) stack.set(DataComponents.LORE, new ItemLore(lore));
        return stack;
    }

    private static Component loreLine(String label, String value) {
        return Component.literal(label).withStyle(ChatFormatting.GRAY)
            .append(Component.literal(value).withStyle(ChatFormatting.WHITE))
            .withStyle(style -> style.withItalic(false));
    }

    /**
     * The preview is read-only, so every click is swallowed. The server writes the client's predicted
     * slot changes into its remote-state tracking before the next {@code broadcastChanges()}, which
     * then corrects the client back to the real (unchanged) contents.
     */
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        boolean isReroll = slotId == rerollSlot()
            && (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE);
        if (isReroll) {
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
    public boolean canTakeItemForPickAll(ItemStack stack, net.minecraft.world.inventory.Slot slot) {
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
