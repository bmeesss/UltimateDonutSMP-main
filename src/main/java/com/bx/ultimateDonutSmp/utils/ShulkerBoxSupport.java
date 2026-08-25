package com.bx.ultimateDonutSmp.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
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

        if (!(item.getItemMeta() instanceof BlockStateMeta bsm)) {
            return java.util.Collections.emptyList();
        }

        if (!(bsm.getBlockState() instanceof ShulkerBox box)) {
            return java.util.Collections.emptyList();
        }

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

        if (!(item.getItemMeta() instanceof BlockStateMeta bsm)) {
            return 0;
        }

        if (!(bsm.getBlockState() instanceof ShulkerBox box)) {
            return 0;
        }

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
