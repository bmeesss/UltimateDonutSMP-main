package com.bx.ultimateDonutSmp.menus;

import com.bx.ultimateDonutSmp.UltimateDonutSmp;
import com.bx.ultimateDonutSmp.utils.ItemUtils;
import com.bx.ultimateDonutSmp.utils.PaymentUtils;
import com.bx.ultimateDonutSmp.utils.SoundUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

public class PayConfirmMenu extends BaseMenu {

    public enum PaymentType {
        MONEY,
        SHARDS
    }

    private final String targetName;
    private final PaymentType paymentType;
    private final double moneyAmount;
    private final long shardAmount;

    public PayConfirmMenu(UltimateDonutSmp plugin, String targetName, double amount) {
        super(
                plugin,
                plugin.getConfigManager().getMenus().getString("PAY-CONFIRM-MENU.TITLE", "&8Confirm payment"),
                plugin.getConfigManager().getMenus().getInt("PAY-CONFIRM-MENU.SIZE", 27)
        );
        this.targetName = targetName;
        this.paymentType = PaymentType.MONEY;
        this.moneyAmount = amount;
        this.shardAmount = 0L;
    }

    public PayConfirmMenu(UltimateDonutSmp plugin, String targetName, long amount) {
        super(
                plugin,
                plugin.getConfigManager().getMenus().getString("PAY-CONFIRM-MENU.TITLE", "&8Confirm payment"),
                plugin.getConfigManager().getMenus().getInt("PAY-CONFIRM-MENU.SIZE", 27)
        );
        this.targetName = targetName;
        this.paymentType = PaymentType.SHARDS;
        this.moneyAmount = 0.0;
        this.shardAmount = amount;
    }

    @Override
    public void build(Player player) {
        clear();
        fill(Material.STAINED_GLASS_PANE, (short) 7);

        set(11, ItemUtils.createItem(Material.STAINED_GLASS_PANE, (short) 14, "&cCancel",
                java.util.Collections.singletonList("&7Click to cancel this payment.")));

        set(15, ItemUtils.createItem(Material.STAINED_GLASS_PANE, (short) 5, "&aConfirm",
                java.util.Collections.singletonList("&7Click to confirm this payment.")));

        String amountText = paymentType == PaymentType.MONEY
                ? plugin.getCurrencyManager().formatMoney(moneyAmount)
                : plugin.getCurrencyManager().formatShards(shardAmount);

        set(13, createTargetItem(new java.util.ArrayList<>(java.util.Arrays.asList(
                "&7Target: &f" + targetName, 
                "&7Amount: " + amountText
        ))));
    }

    @Override
    public void handleClick(int slot, Player player) {
        if (slot != 11 && slot != 15) return;

        SoundUtils.play(player, plugin.getConfigManager().getSound("MENUS.BUTTON-CLICK"));
        player.closeInventory();

        if (slot == 11) {
            return;
        }

        if (paymentType == PaymentType.MONEY) {
            PaymentUtils.transferMoney(plugin, player, targetName, moneyAmount);
        } else {
            PaymentUtils.transferShards(plugin, player, targetName, shardAmount);
        }
    }

    private ItemStack createTargetItem(List<String> lore) {
        ItemStack item = ItemUtils.createItem(Material.PLAYER_HEAD, "&a" + targetName, lore);
        ItemMeta itemMeta = item.getItemMeta();
        if (!(itemMeta instanceof SkullMeta)) {
            return item;
        }
        SkullMeta meta = (SkullMeta) itemMeta;

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        meta.setOwningPlayer(target);
        item.setItemMeta(meta);
        return item;
    }
}
