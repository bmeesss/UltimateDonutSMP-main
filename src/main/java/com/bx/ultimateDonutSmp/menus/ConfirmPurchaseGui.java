package com.bx.ultimateDonutSmp.menus;

import com.bx.ultimateDonutSmp.UltimateDonutSmp;
import com.bx.ultimateDonutSmp.models.AuctionBrowseRequest;
import com.bx.ultimateDonutSmp.models.AuctionListing;
import com.bx.ultimateDonutSmp.utils.ColorUtils;
import com.bx.ultimateDonutSmp.utils.NumberUtils;
import com.bx.ultimateDonutSmp.utils.SoundUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.List;
import java.util.function.Consumer;

public final class ConfirmPurchaseGui extends BaseMenu {

    private final AuctionListing listing;
    private final AuctionBrowseRequest returnRequest;
    private final Consumer<Player> returnAction;

    public ConfirmPurchaseGui(UltimateDonutSmp plugin, AuctionListing listing) {
        this(plugin, listing, null, null);
    }

    public ConfirmPurchaseGui(
            UltimateDonutSmp plugin,
            AuctionListing listing,
            AuctionBrowseRequest returnRequest
    ) {
        this(plugin, listing, returnRequest, null);
    }

    public ConfirmPurchaseGui(
            UltimateDonutSmp plugin,
            AuctionListing listing,
            Consumer<Player> returnAction
    ) {
        this(plugin, listing, null, returnAction);
    }

    private ConfirmPurchaseGui(
            UltimateDonutSmp plugin,
            AuctionListing listing,
            AuctionBrowseRequest returnRequest,
            Consumer<Player> returnAction
    ) {
        super(plugin, plugin.getConfigManager().getAuctionHouse()
                .getString("GUI.CONFIRM_PURCHASE.TITLE", "&8Confirm purchase"), 27);
        this.listing = listing;
        this.returnRequest = returnRequest;
        this.returnAction = returnAction;
    }

    @Override
    public void build(Player player) {
        clear();
        fill(Material.STAINED_GLASS_PANE, (short) 7);
        set(AuctionHouseMenuSupport.slot(plugin, "GUI.CONFIRM_PURCHASE.ITEM", 11),
                AuctionHouseMenuSupport.createListingDisplay(
                        plugin,
                        plugin.getAuctionHouseManager(),
                        listing,
                        false
                ));
        set(AuctionHouseMenuSupport.slot(plugin, "GUI.CONFIRM_PURCHASE.CONFIRM", 15),
                AuctionHouseMenuSupport.control(
                        plugin,
                        "GUI.CONFIRM_PURCHASE.CONFIRM",
                        Material.LIME_STAINED_GLASS_PANE,
                        "&aConfirm purchase",
                        new java.util.ArrayList<>(java.util.Arrays.asList("&7Price: {price}",  "&eClick to purchase")),
                        "{price}", plugin.getCurrencyManager().formatMoney(listing.price())
                ));
        set(AuctionHouseMenuSupport.slot(plugin, "GUI.CONFIRM_PURCHASE.CANCEL", 22),
                AuctionHouseMenuSupport.control(
                        plugin,
                        "GUI.CONFIRM_PURCHASE.CANCEL",
                        Material.RED_STAINED_GLASS_PANE,
                        "&cCancel",
                        java.util.Collections.singletonList("&7Return to auction house")
                ));
    }

    @Override
    public void handleClick(int slot, Player player, ClickType clickType) {
        if (slot == AuctionHouseMenuSupport.slot(plugin, "GUI.CONFIRM_PURCHASE.CANCEL", 22)) {
            reopen(player);
            return;
        }
        if (slot != AuctionHouseMenuSupport.slot(plugin, "GUI.CONFIRM_PURCHASE.CONFIRM", 15)) {
            return;
        }
        com.bx.ultimateDonutSmp.managers.AuctionHouseManager manager = plugin.getAuctionHouseManager();
        if (manager.isOnClickCooldown(player.getUniqueId())) {
            return;
        }
        manager.updateClickCooldown(player.getUniqueId());

        SoundUtils.play(player, plugin.getConfigManager().getSound("MENUS.BUTTON-CLICK"));
        manager.purchaseListing(player, listing.id())
                .thenAccept(result -> plugin.getSpigotScheduler().runEntity(player, () -> {
                    if (result.success()) {
                        player.sendMessage(ColorUtils.toComponent(plugin.getConfigManager().getMessage(
                                "AUCTION_HOUSE.PURCHASE_SUCCESS",
                                "{item}", plugin.getAuctionHouseManager().describeItem(listing.item()),
                                "{price}", NumberUtils.format(listing.price()),
                                "{price_formatted}", plugin.getCurrencyManager().formatMoney(listing.price()),
                                "{seller}", listing.sellerName()
                        )));
                        SoundUtils.play(player, plugin.getConfigManager().getSound("AUCTION_HOUSE.SUCCESS"));
                    } else {
                        String key = "AUCTION_HOUSE.PURCHASE_DATABASE_ERROR";
                        switch (result.reason()) {
                            case DISABLED:
                                key = "AUCTION_HOUSE.DISABLED";
                                break;
                            case NO_PERMISSION:
                                key = "AUCTION_HOUSE.NO_PERMISSION";
                                break;
                            case LISTING_NOT_FOUND:
                                key = "AUCTION_HOUSE.LISTING_NOT_FOUND";
                                break;
                            case NOT_ACTIVE:
                                key = "AUCTION_HOUSE.LISTING_NOT_ACTIVE";
                                break;
                            case OWN_LISTING:
                                key = "AUCTION_HOUSE.CANNOT_BUY_OWN";
                                break;
                            case NO_MONEY:
                                key = "AUCTION_HOUSE.NOT_ENOUGH_MONEY";
                                break;
                            case INVENTORY_FULL:
                                key = "AUCTION_HOUSE.FULL_INVENTORY";
                                break;
                            case NO_PLAYER_DATA:
                            case DATABASE_ERROR:
                                key = "AUCTION_HOUSE.PURCHASE_DATABASE_ERROR";
                                break;
                            default:
                                key = "AUCTION_HOUSE.PURCHASE_DATABASE_ERROR";
                                break;
                        }
                        player.sendMessage(ColorUtils.toComponent(plugin.getConfigManager().getMessage(key)));
                        SoundUtils.play(player, plugin.getConfigManager().getSound("AUCTION_HOUSE.FAIL"));
                    }
                    reopen(player);
                }));
    }

    private void reopen(Player player) {
        SoundUtils.play(player, plugin.getConfigManager().getSound("MENUS.BUTTON-CLICK"));
        if (returnAction != null) {
            returnAction.accept(player);
            return;
        }
        AuctionBrowseRequest request = returnRequest == null
                ? plugin.getAuctionHouseManager().session(player.getUniqueId()).request()
                : returnRequest;
        plugin.getAuctionHouseManager().startNavigating(player.getUniqueId());
        new AuctionHouseBrowseMenu(
                plugin,
                request.page(),
                request.sort(),
                request.category().name()
        ).open(player);
    }

    @Override
    public void onClose(Player player) {
        plugin.getAuctionHouseManager().stopNavigating(player.getUniqueId());
    }
}
