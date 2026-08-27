package com.bx.ultimateDonutSmp.listeners;

import com.bx.ultimateDonutSmp.UltimateDonutSmp;
import com.bx.ultimateDonutSmp.menus.BaseMenu;
import com.bx.ultimateDonutSmp.menus.CrateEditorMenu;
import com.bx.ultimateDonutSmp.menus.ShopEditorMenu;
import com.bx.ultimateDonutSmp.menus.OrdersInventoryItemMenu;
import com.bx.ultimateDonutSmp.menus.OrdersDepositMenu;
import com.bx.ultimateDonutSmp.menus.OrdersNewMenu;
import com.bx.ultimateDonutSmp.menus.RTPMenu;
import com.bx.ultimateDonutSmp.menus.SellMenu;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InventoryClickListener implements Listener {

    private final UltimateDonutSmp plugin;

    public InventoryClickListener(UltimateDonutSmp plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Inventory inv = event.getInventory();
        if (!(inv.getHolder() instanceof BaseMenu)) return;
        BaseMenu menu = (BaseMenu) inv.getHolder();
        Inventory topInventory = event.getView().getTopInventory();

        if (menu instanceof SellMenu) {
            SellMenu sellMenu = (SellMenu) menu;
            sellMenu.handleInventoryClick(event);
            return;
        }

        if (menu instanceof RTPMenu) {
            handleRtpMenuClick(event, player, menu);
            return;
        }

        if (menu instanceof CrateEditorMenu) {
            CrateEditorMenu crateEditorMenu = (CrateEditorMenu) menu;
            crateEditorMenu.handleInventoryClick(event);
            return;
        }

        if (menu instanceof ShopEditorMenu) {
            ShopEditorMenu shopEditorMenu = (ShopEditorMenu) menu;
            shopEditorMenu.handleInventoryClick(event);
            return;
        }

        if (menu instanceof OrdersInventoryItemMenu) {
            OrdersInventoryItemMenu ordersInventoryItemMenu = (OrdersInventoryItemMenu) menu;
            ordersInventoryItemMenu.handleInventoryClick(event);
            return;
        }

        if (menu instanceof OrdersDepositMenu) {
            OrdersDepositMenu ordersDepositMenu = (OrdersDepositMenu) menu;
            ordersDepositMenu.handleInventoryClick(event);
            return;
        }

        if (menu instanceof com.bx.ultimateDonutSmp.menus.SpawnerStorageMenu) {
            com.bx.ultimateDonutSmp.menus.SpawnerStorageMenu spawnerStorageMenu = (com.bx.ultimateDonutSmp.menus.SpawnerStorageMenu) menu;
            spawnerStorageMenu.handleInventoryClick(event);
            return;
        }

        if (menu instanceof OrdersNewMenu && event.getRawSlot() == 23) {
            handleProtectedMenuClick(event, player, menu);
            return;
        }

        event.setCancelled(true);
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(topInventory)) return;
        if (event.getCurrentItem() == null) return;
        menu.handleClick(event.getSlot(), player, event.getClick());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Inventory inv = event.getInventory();
        if (!(inv.getHolder() instanceof BaseMenu)) return;
        BaseMenu menu = (BaseMenu) inv.getHolder();

        if (menu instanceof com.bx.ultimateDonutSmp.menus.SpawnerStorageMenu) {
            com.bx.ultimateDonutSmp.menus.SpawnerStorageMenu spawnerStorageMenu = (com.bx.ultimateDonutSmp.menus.SpawnerStorageMenu) menu;
            spawnerStorageMenu.handleInventoryDrag(event);
            return;
        }

        if (menu instanceof SellMenu) {
            SellMenu sellMenu = (SellMenu) menu;
            sellMenu.handleInventoryDrag(event);
            return;
        }

        if (menu instanceof RTPMenu && event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
            syncInventory(player);
            return;
        }

        if (menu instanceof CrateEditorMenu) {
            CrateEditorMenu crateEditorMenu = (CrateEditorMenu) menu;
            crateEditorMenu.handleInventoryDrag(event);
            return;
        }

        if (menu instanceof ShopEditorMenu) {
            ShopEditorMenu shopEditorMenu = (ShopEditorMenu) menu;
            shopEditorMenu.handleInventoryDrag(event);
            return;
        }

        if (menu instanceof OrdersInventoryItemMenu) {
            OrdersInventoryItemMenu ordersInventoryItemMenu = (OrdersInventoryItemMenu) menu;
            ordersInventoryItemMenu.handleInventoryDrag(event);
            return;
        }

        if (menu instanceof OrdersDepositMenu) {
            OrdersDepositMenu ordersDepositMenu = (OrdersDepositMenu) menu;
            ordersDepositMenu.handleInventoryDrag(event);
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        org.bukkit.inventory.InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof BaseMenu) {
            ((BaseMenu) holder).onClose(player);
        }
    }

    private void handleRtpMenuClick(InventoryClickEvent event, Player player, BaseMenu menu) {
        Inventory topInventory = event.getView().getTopInventory();
        ItemStack originalCursor = event.getCursor() == null ? null : event.getCursor().clone();
        int rawSlot = event.getRawSlot();
        ClickType clickType = event.getClick();
        boolean validTopClick = event.getClickedInventory() != null
                && event.getClickedInventory().equals(topInventory)
                && event.getCurrentItem() != null
                && event.getCurrentItem().getType() != Material.AIR;

        event.setCancelled(true);
        event.setResult(Event.Result.DENY);

        plugin.getSpigotScheduler().runEntity(player, () -> {
            if (!player.isOnline()) {
                return;
            }

            player.setItemOnCursor(originalCursor);
            player.closeInventory();
            player.updateInventory();

            if (validTopClick) {
                menu.handleClick(rawSlot, player, clickType);
            }
        });
    }

    private void handleProtectedMenuClick(InventoryClickEvent event, Player player, BaseMenu menu) {
        Inventory topInventory = event.getView().getTopInventory();
        ItemStack originalCursor = event.getCursor() == null ? null : event.getCursor().clone();
        int rawSlot = event.getRawSlot();
        ClickType clickType = event.getClick();
        boolean validTopClick = event.getClickedInventory() != null
                && event.getClickedInventory().equals(topInventory)
                && event.getCurrentItem() != null
                && event.getCurrentItem().getType() != Material.AIR;

        event.setCancelled(true);
        event.setResult(Event.Result.DENY);

        plugin.getSpigotScheduler().runEntity(player, () -> {
            if (!player.isOnline()) {
                return;
            }

            player.setItemOnCursor(originalCursor);
            player.updateInventory();

            if (validTopClick) {
                menu.handleClick(rawSlot, player, clickType);
            }
        });
    }

    private void syncInventory(Player player) {
        plugin.getSpigotScheduler().runEntity(player, player::updateInventory);
    }
}
