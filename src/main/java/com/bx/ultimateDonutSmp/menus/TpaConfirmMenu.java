package com.bx.ultimateDonutSmp.menus;

import com.bx.ultimateDonutSmp.UltimateDonutSmp;
import com.bx.ultimateDonutSmp.utils.ItemUtils;
import com.bx.ultimateDonutSmp.utils.SoundUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

public class TpaConfirmMenu extends BaseMenu {

    private final String requesterName;
    private final boolean tpaHere;

    public TpaConfirmMenu(UltimateDonutSmp plugin, String requesterName, boolean tpaHere) {
        super(
                plugin,
                plugin.getConfigManager().getMenus()
                        .getString("TPA-CONFIRM-MENU.TITLE", "&8Confirm TPA {here}")
                        .replace("{here}", tpaHere ? "Here" : ""),
                plugin.getConfigManager().getMenus().getInt("TPA-CONFIRM-MENU.SIZE", 27)
        );
        this.requesterName = requesterName;
        this.tpaHere = tpaHere;
    }

    @Override
    public void build(Player player) {
        clear();
        fill(Material.STAINED_GLASS_PANE, (short) 7);

        set(11, ItemUtils.createItem(Material.STAINED_GLASS_PANE, (short) 14, "&cCancel",
                java.util.Collections.singletonList("&7Deny this teleport request.")));

        set(15, ItemUtils.createItem(Material.STAINED_GLASS_PANE, (short) 5, "&aConfirm",
                java.util.Collections.singletonList("&7Accept this teleport request.")));

        String requestText = tpaHere
                ? "&7" + requesterName + " wants you to teleport to them."
                : "&7" + requesterName + " wants to teleport to you.";
        set(13, createRequesterItem(java.util.Collections.singletonList(requestText)));
    }

    @Override
    public void handleClick(int slot, Player player) {
        if (slot != 11 && slot != 15) return;

        SoundUtils.play(player, plugin.getConfigManager().getSound("MENUS.BUTTON-CLICK"));
        player.closeInventory();

        if (slot == 11) {
            player.performCommand("tpadeny " + requesterName);
            return;
        }

        player.performCommand("tpaccept " + requesterName);
    }

    private ItemStack createRequesterItem(List<String> lore) {
        ItemStack item = ItemUtils.createItem(Material.SKULL_ITEM, "&a" + requesterName, lore);
        if (!(item.getItemMeta() instanceof SkullMeta)) {
            return item;
        }
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        OfflinePlayer requester = Bukkit.getOfflinePlayer(requesterName);
        com.bx.ultimateDonutSmp.utils.ItemUtils.applyOwnerToSkullMeta(meta, requester);
        item.setItemMeta(meta);
        return item;
    }
}
