package com.bx.ultimateDonutSmp.menus;

import com.bx.ultimateDonutSmp.UltimateDonutSmp;
import com.bx.ultimateDonutSmp.managers.AuctionHouseManager;
import com.bx.ultimateDonutSmp.managers.CurrencyManager;
import com.bx.ultimateDonutSmp.managers.LanguageManager;
import com.bx.ultimateDonutSmp.models.AuctionClaim;
import com.bx.ultimateDonutSmp.models.AuctionListing;
import com.bx.ultimateDonutSmp.utils.ColorUtils;
import com.bx.ultimateDonutSmp.utils.ItemUtils;
import com.bx.ultimateDonutSmp.utils.LegacyMaterialSupport;
import com.bx.ultimateDonutSmp.utils.NumberUtils;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

final class AuctionHouseMenuSupport {

    private AuctionHouseMenuSupport() {}

    static ItemStack createListingDisplay(
            UltimateDonutSmp plugin,
            AuctionHouseManager manager,
            AuctionListing listing,
            boolean ownedByViewer
    ) {
        LanguageManager language = plugin.getLanguageManager();
        List<String> extraLore = new ArrayList<>(language.menuList(
                "AUCTION_HOUSE.ENTRY.LISTING_LORE",
                new java.util.ArrayList<>(java.util.Arrays.asList(
                        "", 
                        "&7Seller: &f{seller}", 
                        "&7Price: {price}", 
                        "&7You receive: {payout}", 
                        "&7Time left: &f{time}", 
                        "&7Listing ID: &f#{id}", 
                        ""
                )),
                "{seller}", plugin.getHideManager().publicName(listing.sellerUuid(), listing.sellerName()),
                "{price}", plugin.getCurrencyManager().formatMoney(listing.price()),
                "{payout}", plugin.getCurrencyManager().formatMoney(listing.sellerPayout()),
                "{time}", manager.formatRemaining(listing.secondsRemaining(System.currentTimeMillis())),
                "{id}", String.valueOf(listing.id())
        ));
        extraLore.add(ownedByViewer
                ? language.menu("AUCTION_HOUSE.ENTRY.MANAGE", "&eClick to manage listing")
                : language.menu("AUCTION_HOUSE.ENTRY.BUY", "&eClick to buy"));
        return decorateItem(plugin, listing.item(), manager.describeItem(listing.item()), extraLore);
    }

    static ItemStack createClaimDisplay(
            UltimateDonutSmp plugin,
            AuctionHouseManager manager,
            AuctionClaim claim
    ) {
        LanguageManager language = plugin.getLanguageManager();
        if (claim.moneyClaim()) {
            return ItemUtils.createItem(
                    Material.GOLD_INGOT,
                    language.menu(
                            "AUCTION_HOUSE.ENTRY.MONEY_CLAIM_NAME",
                            "{money_color}{money_name} claim",
                            "{money_color}", plugin.getCurrencyManager().color(CurrencyManager.CurrencyType.MONEY),
                            "{money_name}", plugin.getCurrencyManager().singular(CurrencyManager.CurrencyType.MONEY)
                    ),
                    language.menuList(
                            "AUCTION_HOUSE.ENTRY.MONEY_CLAIM_LORE",
                            new java.util.ArrayList<>(java.util.Arrays.asList("&7Amount: {amount}",  "&7Source listing: &f#{id}",  "",  "&eClick to claim")),
                            "{amount}", plugin.getCurrencyManager().formatMoney(claim.moneyAmount()),
                            "{id}", String.valueOf(claim.sourceListingId())
                    )
            );
        }

        List<String> extraLore = language.menuList(
                "AUCTION_HOUSE.ENTRY.ITEM_CLAIM_LORE",
                new java.util.ArrayList<>(java.util.Arrays.asList(
                        "", 
                        "&7Claim type: &freturned item", 
                        "&7Source listing: &f#{id}", 
                        "&7Created: &f{created}", 
                        "", 
                        "&eClick to claim"
                )),
                "{id}", String.valueOf(claim.sourceListingId()),
                "{created}", NumberUtils.formatTimeLong(Math.max(0L,
                        (System.currentTimeMillis() - claim.createdAt()) / 1000L))
        );
        return decorateItem(plugin, claim.item(), manager.describeItem(claim.item()), extraLore);
    }

    static ItemStack decorateItem(
            UltimateDonutSmp plugin,
            ItemStack source,
            String fallbackDisplayName,
        List<String> extraLore
    ) {
        if (source == null || source.getType() == Material.AIR) {
            return ItemUtils.createItem(
                    Material.BARRIER,
                    plugin.getLanguageManager().menu("AUCTION_HOUSE.ENTRY.MISSING_NAME", "&cMissing item"),
                    plugin.getLanguageManager().menuList("AUCTION_HOUSE.ENTRY.MISSING_LORE",
                            java.util.Collections.singletonList("&7This entry has no item data."))
            );
        }

        ItemStack display = source.clone();
        ItemMeta meta = display.getItemMeta();
        if (meta == null) {
            return display;
        }

        List<String> combinedLore = new ArrayList<>();
        if (meta.hasLore() && meta.getLore() != null) {
            for (String line : meta.getLore()) {
                combinedLore.add(ColorUtils.toLegacyString(line));
            }
        }
        combinedLore.addAll(extraLore);

        if (!meta.hasDisplayName() && fallbackDisplayName != null && !fallbackDisplayName.trim().isEmpty()) {
            meta.setDisplayName(ColorUtils.toComponent("&b" + fallbackDisplayName));
        }
        meta.setLore(ColorUtils.toComponentList(combinedLore));
        display.setItemMeta(meta);
        return display;
    }

    static int slot(UltimateDonutSmp plugin, String path, int fallback) {
        return plugin.getConfigManager().getAuctionHouse().getInt(path + ".SLOT", fallback);
    }

    static ItemStack control(
            UltimateDonutSmp plugin,
            String path,
            Material fallbackMaterial,
            String fallbackName,
            List<String> fallbackLore,
            String... replacements
    ) {
        FileConfiguration config = plugin.getConfigManager().getAuctionHouse();
        Material material = fallbackMaterial;
        String configuredMaterial = config.getString(path + ".MATERIAL", fallbackMaterial.name());
        if (configuredMaterial != null) {
            try {
                material = Material.valueOf(configuredMaterial.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return ItemUtils.createItem(
                material,
                controlName(config, path, fallbackName, replacements),
                controlLore(config, path, fallbackLore, replacements)
        );
    }

    /**
     * 1.12.2 variant of {@link #control(UltimateDonutSmp, String, Material, String, List, String...)}
     * for icons whose colour lives in a legacy data value ({@code STAINED_GLASS_PANE} plus
     * durability) instead of a separate 1.13+ Material.
     *
     * <p>The configured value is resolved through {@link LegacyMaterialSupport}, so a
     * {@code *_STAINED_GLASS_PANE} entry in {@code auction-house.yml} keeps working and the default
     * written into {@code .MATERIAL} stays the flattened 1.13+ name. Missing, blank or unresolvable
     * values keep falling back to the caller's icon, exactly as the Material-based overload does.</p>
     */
    static ItemStack control(
            UltimateDonutSmp plugin,
            String path,
            LegacyMaterialSupport.Icon fallbackIcon,
            String fallbackName,
            List<String> fallbackLore,
            String... replacements
    ) {
        FileConfiguration config = plugin.getConfigManager().getAuctionHouse();
        LegacyMaterialSupport.Icon resolved = LegacyMaterialSupport.resolve(
                config.getString(path + ".MATERIAL", fallbackIcon.configuredName()),
                fallbackIcon
        );
        return ItemUtils.createItem(
                resolved.material(),
                resolved.data(),
                controlName(config, path, fallbackName, replacements),
                controlLore(config, path, fallbackLore, replacements)
        );
    }

    private static String controlName(
            FileConfiguration config,
            String path,
            String fallbackName,
            String... replacements
    ) {
        return replace(config.getString(path + ".NAME", fallbackName), replacements);
    }

    private static List<String> controlLore(
            FileConfiguration config,
            String path,
            List<String> fallbackLore,
            String... replacements
    ) {
        List<String> configuredLore = config.getStringList(path + ".LORE");
        return (configuredLore.isEmpty() ? fallbackLore : configuredLore).stream()
                .map(line -> replace(line, replacements))
                .collect(java.util.stream.Collectors.toList());
    }

    static String configText(
            UltimateDonutSmp plugin,
            String path,
            String fallback,
            String... replacements
    ) {
        return replace(plugin.getConfigManager().getAuctionHouse().getString(path, fallback), replacements);
    }

    static List<String> configList(
            UltimateDonutSmp plugin,
            String path,
            List<String> fallback,
            String... replacements
    ) {
        List<String> configured = plugin.getConfigManager().getAuctionHouse().getStringList(path);
        return (configured.isEmpty() ? fallback : configured).stream()
                .map(line -> replace(line, replacements))
                .collect(java.util.stream.Collectors.toList());
    }

    private static String replace(String value, String... replacements) {
        String result = value == null ? "" : value;
        for (int index = 0; index + 1 < replacements.length; index += 2) {
            result = result.replace(replacements[index], replacements[index + 1]);
        }
        return result;
    }
}
