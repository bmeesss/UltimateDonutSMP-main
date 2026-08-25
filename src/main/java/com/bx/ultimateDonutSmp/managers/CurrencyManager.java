package com.bx.ultimateDonutSmp.managers;

import com.bx.ultimateDonutSmp.UltimateDonutSmp;
import org.bukkit.configuration.ConfigurationSection;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CurrencyManager {

    public enum CurrencyType {
        MONEY("MONEY", "dollar", "dollars", "$", "&a", "&a", 2,
                "{symbol_color}{symbol}{color}{amount}", "{symbol_color}{symbol}{color}{amount_short}"),
        SHARDS("SHARDS", "Shard", "Shards", "★", "&#A303F9", "&#A303F9", 0,
                "{symbol_color}{symbol} {color}{amount} {name}", "{symbol_color}{symbol} {color}{amount_short} {name}");

        private final String configKey;
        private final String defaultSingular;
        private final String defaultPlural;
        private final String defaultSymbol;
        private final String defaultColor;
        private final String defaultSymbolColor;
        private final int defaultDecimalPlaces;
        private final String defaultFormat;
        private final String defaultCompactFormat;

        CurrencyType(
                String configKey,
                String defaultSingular,
                String defaultPlural,
                String defaultSymbol,
                String defaultColor,
                String defaultSymbolColor,
                int defaultDecimalPlaces,
                String defaultFormat,
                String defaultCompactFormat
        ) {
            this.configKey = configKey;
            this.defaultSingular = defaultSingular;
            this.defaultPlural = defaultPlural;
            this.defaultSymbol = defaultSymbol;
            this.defaultColor = defaultColor;
            this.defaultSymbolColor = defaultSymbolColor;
            this.defaultDecimalPlaces = defaultDecimalPlaces;
            this.defaultFormat = defaultFormat;
            this.defaultCompactFormat = defaultCompactFormat;
        }
    }

    private static final List<String> DEFAULT_COMPACT_SUFFIXES = new java.util.ArrayList<>(java.util.Arrays.asList("K",  "M",  "B",  "T",  "Q"));

    private final UltimateDonutSmp plugin;

    public CurrencyManager(UltimateDonutSmp plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        // Values are read live from config so existing reload flow only needs a hook point.
    }

    public String formatMoney(double amount) {
        return format(CurrencyType.MONEY, amount);
    }

    public String formatMoneyCompact(double amount) {
        return format(CurrencyType.MONEY, amount, true);
    }

    public String formatShards(long amount) {
        return format(CurrencyType.SHARDS, amount);
    }

    public String formatShardsCompact(long amount) {
        return format(CurrencyType.SHARDS, amount, true);
    }

    public String format(CurrencyType type, double amount) {
        CurrencyDefinition definition = definition(type);
        return format(amount, definition, definition.compactEnabled());
    }

    public String format(CurrencyType type, double amount, boolean compact) {
        CurrencyDefinition definition = definition(type);
        return format(amount, definition, compact && definition.compactEnabled());
    }

    private String format(double amount, CurrencyDefinition definition, boolean compact) {
        String rawAmount = formatNumber(amount, definition.decimalPlaces(), definition);
        String shortAmount = formatShortNumber(amount, definition);
        String name = isSingular(amount) ? definition.singular() : definition.plural();

        return (compact ? definition.compactFormat() : definition.format())
                .replace("{amount}", rawAmount)
                .replace("{amount_short}", shortAmount)
                .replace("{symbol}", definition.symbol())
                .replace("{symbol_color}", definition.symbolColor())
                .replace("{symbol_colored}", definition.symbolColor() + definition.symbol())
                .replace("{name}", name)
                .replace("{name_singular}", definition.singular())
                .replace("{name_plural}", definition.plural())
                .replace("{color}", definition.color());
    }

    public String formatAmount(CurrencyType type, double amount) {
        CurrencyDefinition definition = definition(type);
        return formatNumber(amount, definition.decimalPlaces(), definition);
    }

    public String formatCompactAmount(CurrencyType type, double amount) {
        return formatShortNumber(amount, definition(type));
    }

    public String symbol(CurrencyType type) {
        return definition(type).symbol();
    }

    public String singular(CurrencyType type) {
        return definition(type).singular();
    }

    public String plural(CurrencyType type) {
        return definition(type).plural();
    }

    public String name(CurrencyType type, double amount) {
        return isSingular(amount) ? singular(type) : plural(type);
    }

    public String color(CurrencyType type) {
        return definition(type).color();
    }

    public String symbolColor(CurrencyType type) {
        return definition(type).symbolColor();
    }

    public String coloredSymbol(CurrencyType type) {
        CurrencyDefinition definition = definition(type);
        return definition.symbolColor() + definition.symbol();
    }

    public String applyPlaceholders(String input, CurrencyType type, double amount) {
        if (input == null) {
            return "";
        }
        return input
                .replace("{currency_amount}", formatAmount(type, amount))
                .replace("{currency_amount_short}", formatCompactAmount(type, amount))
                .replace("{currency_symbol}", symbol(type))
                .replace("{currency_symbol_color}", symbolColor(type))
                .replace("{currency_symbol_colored}", coloredSymbol(type))
                .replace("{currency_name}", name(type, amount))
                .replace("{currency_name_singular}", singular(type))
                .replace("{currency_name_plural}", plural(type))
                .replace("{currency_color}", color(type))
                .replace("{currency}", format(type, amount))
                .replace("{currency_short}", format(type, amount, true));
    }

    public String applyMoneyPlaceholders(String input, double amount) {
        return applyNamedPlaceholders(input, "money", CurrencyType.MONEY, amount);
    }

    public String applyShardPlaceholders(String input, double amount) {
        return applyNamedPlaceholders(input, "shards", CurrencyType.SHARDS, amount);
    }

    public String applyAllPlaceholders(String input, double moneyAmount, double shardAmount) {
        String result = applyNamedPlaceholders(input, "money", CurrencyType.MONEY, moneyAmount);
        return applyNamedPlaceholders(result, "shards", CurrencyType.SHARDS, shardAmount);
    }

    public String applyStaticPlaceholders(String input) {
        String result = applyNamedStaticPlaceholders(input, "money", CurrencyType.MONEY);
        return applyNamedStaticPlaceholders(result, "shards", CurrencyType.SHARDS);
    }

    public List<String> applyStaticPlaceholders(List<String> input) {
        if (input == null || input.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        List<String> replaced = new ArrayList<>(input.size());
        for (String line : input) {
            replaced.add(applyStaticPlaceholders(line));
        }
        return replaced;
    }

    private String applyNamedPlaceholders(String input, String prefix, CurrencyType type, double amount) {
        if (input == null) {
            return "";
        }
        return input
                .replace("{" + prefix + "_amount}", formatAmount(type, amount))
                .replace("{" + prefix + "_amount_short}", formatCompactAmount(type, amount))
                .replace("{" + prefix + "_symbol}", symbol(type))
                .replace("{" + prefix + "_symbol_color}", symbolColor(type))
                .replace("{" + prefix + "_symbol_colored}", coloredSymbol(type))
                .replace("{" + prefix + "_name}", name(type, amount))
                .replace("{" + prefix + "_name_singular}", singular(type))
                .replace("{" + prefix + "_name_plural}", plural(type))
                .replace("{" + prefix + "_color}", color(type))
                .replace("{" + prefix + "}", format(type, amount))
                .replace("{" + prefix + "_short}", format(type, amount, true));
    }

    private String applyNamedStaticPlaceholders(String input, String prefix, CurrencyType type) {
        if (input == null) {
            return "";
        }
        return input
                .replace("{" + prefix + "_symbol}", symbol(type))
                .replace("{" + prefix + "_symbol_color}", symbolColor(type))
                .replace("{" + prefix + "_symbol_colored}", coloredSymbol(type))
                .replace("{" + prefix + "_name}", plural(type))
                .replace("{" + prefix + "_name_singular}", singular(type))
                .replace("{" + prefix + "_name_plural}", plural(type))
                .replace("{" + prefix + "_color}", color(type))
                .replace("%economy_" + prefix + "_symbol%", symbol(type))
                .replace("%economy_" + prefix + "_symbol_color%", symbolColor(type))
                .replace("%economy_" + prefix + "_symbol_colored%", coloredSymbol(type))
                .replace("%economy_" + prefix + "_name%", singular(type))
                .replace("%economy_" + prefix + "_name_plural%", plural(type))
                .replace("%economy_" + prefix + "_color%", color(type));
    }

    private CurrencyDefinition definition(CurrencyType type) {
        ConfigurationSection section = plugin.getConfigManager().getConfig()
                .getConfigurationSection("CURRENCY." + type.configKey);

        String singular = getString(section, "SINGULAR", type.defaultSingular);
        String plural = getString(section, "PLURAL", type.defaultPlural);
        String symbol = getString(section, "SYMBOL", type.defaultSymbol);
        String color = getString(section, "COLOR", type.defaultColor);
        String symbolColor = getString(section, "SYMBOL-COLOR", color.isBlank() ? type.defaultSymbolColor : color);
        int decimalPlaces = clampDecimalPlaces(section != null
                ? section.getInt("DECIMAL-PLACES", type.defaultDecimalPlaces)
                : type.defaultDecimalPlaces);
        String format = getString(section, "FORMAT", type.defaultFormat);
        String compactFormat = getString(section, "COMPACT-FORMAT", type.defaultCompactFormat);
        boolean compactEnabled = section == null
                ? true
                : section.getBoolean("COMPACT-ENABLED", true);
        List<String> compactSuffixes = getCompactSuffixes(section, "COMPACT-SUFFIXES", DEFAULT_COMPACT_SUFFIXES);
        int compactDecimalPlaces = clampDecimalPlaces(section != null
                ? section.getInt("COMPACT-DECIMAL-PLACES", 1)
                : 1);
        char groupingSeparator = getSeparator(section, "GROUPING-SEPARATOR", '.');
        char decimalSeparator = getSeparator(section, "DECIMAL-SEPARATOR", ',');
        if (groupingSeparator == decimalSeparator) {
            decimalSeparator = groupingSeparator == ',' ? '.' : ',';
        }
        return new CurrencyDefinition(
                singular,
                plural,
                symbol,
                color,
                symbolColor,
                decimalPlaces,
                format,
                compactFormat,
                compactEnabled,
                compactSuffixes,
                compactDecimalPlaces,
                groupingSeparator,
                decimalSeparator
        );
    }

    private String getString(ConfigurationSection section, String key, String fallback) {
        if (section == null) {
            return fallback;
        }
        String value = section.getString(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private List<String> getStringList(ConfigurationSection section, String key, List<String> fallback) {
        if (section == null || !section.isList(key)) {
            return fallback;
        }

        List<String> values = new ArrayList<>();
        for (String value : section.getStringList(key)) {
            if (value != null && !value.isBlank()) {
                values.add(value.trim());
            }
        }
        return values.isEmpty() ? fallback : new java.util.ArrayList<>(values);
    }

    private List<String> getCompactSuffixes(ConfigurationSection section, String key, List<String> fallback) {
        List<String> suffixes = getStringList(section, key, fallback);
        List<String> normalized = new ArrayList<>(suffixes.size());
        for (String suffix : suffixes) {
            normalized.add(suffix.toUpperCase(Locale.US));
        }
        return new java.util.ArrayList<>(normalized);
    }

    private char getSeparator(ConfigurationSection section, String key, char fallback) {
        if (section == null) {
            return fallback;
        }

        String value = section.getString(key);
        return value == null || value.isEmpty() ? fallback : value.charAt(0);
    }

    private boolean isSingular(double amount) {
        return Math.abs(Math.abs(amount) - 1D) < 0.0000001D;
    }

    private int clampDecimalPlaces(int decimalPlaces) {
        return Math.max(0, Math.min(8, decimalPlaces));
    }

    private final java.util.Map<String, DecimalFormat> formatCache = new java.util.concurrent.ConcurrentHashMap<>();

    private String formatNumber(double amount, int decimalPlaces, CurrencyDefinition definition) {
        if (!Double.isFinite(amount)) {
            amount = 0D;
        }

        String key = decimalPlaces + "|" + definition.groupingSeparator() + "|" + definition.decimalSeparator();
        DecimalFormat format = formatCache.computeIfAbsent(key, k -> {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
            symbols.setGroupingSeparator(definition.groupingSeparator());
            symbols.setDecimalSeparator(definition.decimalSeparator());
            return new DecimalFormat(pattern(decimalPlaces), symbols);
        });
        synchronized (format) {
            return format.format(amount);
        }
    }

    private String formatShortNumber(double amount, CurrencyDefinition definition) {
        if (!Double.isFinite(amount)) {
            amount = 0D;
        }

        if (!definition.compactEnabled()) {
            return formatNumber(amount, definition.decimalPlaces(), definition);
        }

        double absolute = Math.abs(amount);
        int suffixIndex = -1;
        while (absolute >= 1_000D && suffixIndex < definition.compactSuffixes().size() - 1) {
            absolute /= 1_000D;
            suffixIndex++;
        }

        if (suffixIndex < 0) {
            return formatNumber(amount, definition.decimalPlaces(), definition);
        }

        double rolloverThreshold = 1_000D - (0.5D / Math.pow(10D, definition.compactDecimalPlaces()));
        if (absolute >= rolloverThreshold && suffixIndex < definition.compactSuffixes().size() - 1) {
            absolute /= 1_000D;
            suffixIndex++;
        }

        String sign = amount < 0D ? "-" : "";
        return sign + formatNumber(absolute, definition.compactDecimalPlaces(), definition)
                + definition.compactSuffixes().get(suffixIndex);
    }

    private String pattern(int decimalPlaces) {
        if (decimalPlaces <= 0) {
            return "#,##0";
        }

        return "#,##0." + "#".repeat(decimalPlaces);
    }

public final class CurrencyDefinition {
    private final String singular;
    private final String plural;
    private final String symbol;
    private final String color;
    private final String symbolColor;
    private final int decimalPlaces;
    private final String format;
    private final String compactFormat;
    private final boolean compactEnabled;
    private final List<String> compactSuffixes;
    private final int compactDecimalPlaces;
    private final char groupingSeparator;
    private final char decimalSeparator;

    public CurrencyDefinition(String singular, String plural, String symbol, String color, String symbolColor, int decimalPlaces, String format, String compactFormat, boolean compactEnabled, List<String> compactSuffixes, int compactDecimalPlaces, char groupingSeparator, char decimalSeparator) {
        this.singular = singular;
        this.plural = plural;
        this.symbol = symbol;
        this.color = color;
        this.symbolColor = symbolColor;
        this.decimalPlaces = decimalPlaces;
        this.format = format;
        this.compactFormat = compactFormat;
        this.compactEnabled = compactEnabled;
        this.compactSuffixes = compactSuffixes;
        this.compactDecimalPlaces = compactDecimalPlaces;
        this.groupingSeparator = groupingSeparator;
        this.decimalSeparator = decimalSeparator;
    }

    public String singular() { return singular; }
    public String plural() { return plural; }
    public String symbol() { return symbol; }
    public String color() { return color; }
    public String symbolColor() { return symbolColor; }
    public int decimalPlaces() { return decimalPlaces; }
    public String format() { return format; }
    public String compactFormat() { return compactFormat; }
    public boolean compactEnabled() { return compactEnabled; }
    public List<String> compactSuffixes() { return compactSuffixes; }
    public int compactDecimalPlaces() { return compactDecimalPlaces; }
    public char groupingSeparator() { return groupingSeparator; }
    public char decimalSeparator() { return decimalSeparator; }

    @Override public String toString() {
        return "CurrencyDefinition[singular=+singular, plural=+plural, symbol=+symbol, color=+color, symbolColor=+symbolColor, decimalPlaces=+decimalPlaces, format=+format, compactFormat=+compactFormat, compactEnabled=+compactEnabled, compactSuffixes=+compactSuffixes, compactDecimalPlaces=+compactDecimalPlaces, groupingSeparator=+groupingSeparator, decimalSeparator=+decimalSeparator]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CurrencyDefinition that = (CurrencyDefinition) o;
        return java.util.Objects.equals(singular, that.singular) && java.util.Objects.equals(plural, that.plural) && java.util.Objects.equals(symbol, that.symbol) && java.util.Objects.equals(color, that.color) && java.util.Objects.equals(symbolColor, that.symbolColor) && java.util.Objects.equals(decimalPlaces, that.decimalPlaces) && java.util.Objects.equals(format, that.format) && java.util.Objects.equals(compactFormat, that.compactFormat) && java.util.Objects.equals(compactEnabled, that.compactEnabled) && java.util.Objects.equals(compactSuffixes, that.compactSuffixes) && java.util.Objects.equals(compactDecimalPlaces, that.compactDecimalPlaces) && java.util.Objects.equals(groupingSeparator, that.groupingSeparator) && java.util.Objects.equals(decimalSeparator, that.decimalSeparator);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(singular, plural, symbol, color, symbolColor, decimalPlaces, format, compactFormat, compactEnabled, compactSuffixes, compactDecimalPlaces, groupingSeparator, decimalSeparator);
    }
}
}
