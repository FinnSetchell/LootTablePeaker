package com.finndog.loottablepeeker;

//? if >=1.21 {

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;

import java.util.List;

/**
 * A read-only one-slot GUI showing what is stored inside a decorated pot, without taking the item.
 * Reuses the vanilla {@code GENERIC_9x2} screen so no client-side mod is needed.
 */
public final class PotPeekMenu extends ChestMenu {

    private static final int COLUMNS = 9;
    private static final int INFO_COLUMN = 0;

    private final SimpleContainer display;

    public static void open(ServerPlayer player, DecoratedPotBlockEntity pot) {
        ItemStack item = pot.getItem(0).copy();
        player.openMenu(new SimpleMenuProvider(
            (containerId, inventory, viewer) -> new PotPeekMenu(
                containerId, inventory, new SimpleContainer(COLUMNS * 2), item),
            Component.literal("Decorated Pot")
        ));
    }

    private PotPeekMenu(int containerId, Inventory inventory, SimpleContainer display, ItemStack item) {
        super(MenuType.GENERIC_9x2, containerId, inventory, display, 2);
        this.display = display;

        this.display.setItem(0, item.isEmpty() ? emptyMarker() : item);
        for (int col = 1; col < COLUMNS; col++) {
            this.display.setItem(col, ItemDecorator.filler());
        }

        for (int col = 0; col < COLUMNS; col++) {
            this.display.setItem(COLUMNS + col, ItemDecorator.filler());
        }
        this.display.setItem(COLUMNS + INFO_COLUMN, infoItem(item));
    }

    private static ItemStack emptyMarker() {
        return ItemDecorator.control(Items.STRUCTURE_VOID,
            Component.literal("Empty").withStyle(ChatFormatting.YELLOW),
            List.of());
    }

    private static ItemStack infoItem(ItemStack item) {
        List<Component> lore = List.of(
            Component.literal("Contents: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(item.isEmpty() ? "empty" : item.getHoverName().getString())
                    .withStyle(ChatFormatting.WHITE))
                .withStyle(style -> style.withItalic(false))
        );
        return ItemDecorator.control(Items.DECORATED_POT,
            Component.literal("Pot Contents").withStyle(ChatFormatting.YELLOW), lore);
    }

    //? if >=26.1 {
    /*@Override
    public void clicked(int slotId, int button, ContainerInput input, Player player) {}
    *///?} else {
    @Override
    public void clicked(int slotId, int button, ClickType input, Player player) {}
    //?}

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
}

//?}
