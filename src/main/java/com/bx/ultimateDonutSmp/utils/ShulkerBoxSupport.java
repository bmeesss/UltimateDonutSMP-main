package com.bx.ultimateDonutSmp.utils;

import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.block.ShulkerBox;

import java.util.ArrayList;
import java.util.List;

public final class ShulkerBoxSupport {

    private ShulkerBoxSupport() {}

    public static boolean isShulkerBox(Material material) {
        if (material == null) return false;
        return material.name().contains("SHULKER_BOX");
    }

    public static boolean isShulkerBox(ItemStack item) {
        if (item == null) return false;
        return isShulkerBox(item.getType());
    }

    public static List<ItemStack> getContents(ItemStack item) {
        if (!isShulkerBox(item)) {
            return java.util.Collections.emptyList();
        }

        ItemMeta itemMeta = item.getItemMeta();
        if (!(itemMeta instanceof BlockStateMeta)) {
            return java.util.Collections.emptyList();
        }
        BlockStateMeta bsm = (BlockStateMeta) itemMeta;

        BlockState blockState = bsm.getBlockState();
        if (!(blockState instanceof ShulkerBox)) {
            return java.util.Collections.emptyList();
        }
        ShulkerBox box = (ShulkerBox) blockState;

        List<ItemStack> list = new ArrayList<>();
        ItemStack[] contents = box.getInventory().getContents();
        for (ItemStack current : contents) {
            if (current != null && current.getType() != Material.AIR) {
                list.add(current.clone());
            }
        }
        return list;
    }

    public static int getItemCount(ItemStack item) {
        if (!isShulkerBox(item)) {
            return 0;
        }

        ItemMeta itemMeta = item.getItemMeta();
        if (!(itemMeta instanceof BlockStateMeta)) {
            return 0;
        }
        BlockStateMeta bsm = (BlockStateMeta) itemMeta;

        BlockState blockState = bsm.getBlockState();
        if (!(blockState instanceof ShulkerBox)) {
            return 0;
        }
        ShulkerBox box = (ShulkerBox) blockState;

        int count = 0;
        ItemStack[] contents = box.getInventory().getContents();
        for (ItemStack current : contents) {
            if (current != null && current.getType() != Material.AIR) {
                count++;
            }
        }
        return count;
    }
}
