package com.finndog.loottablepeeker;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

//? if >=1.21 {
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemLore;
//?}

/**
 * Builds the preview GUI's control items (info book, reroll button, filler panes).
 *
 * <p>Isolated from {@link LootPreviewMenu} because naming and lore are the one part of that menu
 * that changed shape across the matrix: 1.20.5 replaced item NBT with data components, so on 1.20.1
 * the name and lore have to be written into the legacy {@code display} tag instead.</p>
 */
public final class ItemDecorator {

    private ItemDecorator() {}

    /** A control item with a non-italic custom name and optional lore lines. */
    public static ItemStack control(Item item, Component name, List<Component> lore) {
        ItemStack stack = new ItemStack(item);
        Component styled = name.copy().withStyle(style -> style.withItalic(false));

        //? if >=1.21 {
        stack.set(DataComponents.CUSTOM_NAME, styled);
        if (!lore.isEmpty()) stack.set(DataComponents.LORE, new ItemLore(lore));
        //?} else {
        /*stack.setHoverName(styled);
        if (!lore.isEmpty()) {
            net.minecraft.nbt.ListTag lines = new net.minecraft.nbt.ListTag();
            for (Component line : lore) {
                lines.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(line)));
            }
            stack.getOrCreateTagElement("display").put("Lore", lines);
        }
        *///?}

        return stack;
    }

    /** A grey pane used to pad the loot area and back the control row. */
    public static ItemStack filler() {
        // 26.2 folded the sixteen dyed pane constants into one ColorCollection record.
        //? if >=26.2 {
        /*Item pane = net.minecraft.world.item.Items.STAINED_GLASS_PANE.gray();
        *///?} else {
        Item pane = net.minecraft.world.item.Items.GRAY_STAINED_GLASS_PANE;
        //?}
        return control(pane, Component.empty(), List.of());
    }
}
