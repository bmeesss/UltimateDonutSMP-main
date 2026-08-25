package com.bx.ultimateDonutSmp.managers;

import net.skinsrestorer.api.SkinsRestorer;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.property.InputDataResult;
import net.skinsrestorer.api.property.SkinProperty;
import org.bukkit.entity.Player;

final class SkinsRestorerHideBridge {

    private SkinsRestorerHideBridge() {
    }

    static ResolvedSkin resolve(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        try {
            SkinsRestorer skinsRestorer = SkinsRestorerProvider.get();
            InputDataResult result = skinsRestorer.getSkinStorage()
                    .findOrCreateSkinData(input)
                    .orElse(null);
            if (result == null || result.getProperty() == null) {
                return null;
            }
            SkinProperty property = result.getProperty();
            if (property.getValue() == null || property.getValue().isBlank()) {
                return null;
            }
            return new ResolvedSkin(
                    result.getIdentifier() == null ? input : result.getIdentifier().toString(),
                    property.getValue(),
                    property.getSignature()
            );
        } catch (Throwable ignored) {
            return null;
        }
    }

    static boolean apply(Player player, String value, String signature) {
        if (player == null || value == null || value.isBlank()) {
            return false;
        }
        try {
            SkinsRestorer skinsRestorer = SkinsRestorerProvider.get();
            SkinProperty property = SkinProperty.of(value, blankToNull(signature));
            skinsRestorer.getSkinApplier(Player.class).applySkin(player, property);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

public final class ResolvedSkin {
    private final String identifier;
    private final String value;
    private final String signature;

    public ResolvedSkin(String identifier, String value, String signature) {
        this.identifier = identifier;
        this.value = value;
        this.signature = signature;
    }

    public String identifier() { return identifier; }
    public String value() { return value; }
    public String signature() { return signature; }

    @Override public String toString() {
        return "ResolvedSkin[identifier=+identifier, value=+value, signature=+signature]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResolvedSkin that = (ResolvedSkin) o;
        return java.util.Objects.equals(identifier, that.identifier) && java.util.Objects.equals(value, that.value) && java.util.Objects.equals(signature, that.signature);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(identifier, value, signature);
    }
}
}
