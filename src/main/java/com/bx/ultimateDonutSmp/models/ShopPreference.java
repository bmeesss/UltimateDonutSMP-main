package com.bx.ultimateDonutSmp.models;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class ShopPreference {
    private final UUID playerId;
    private final Set<String> favorites;

    public ShopPreference(UUID playerId, Set<String> favorites) {
        this.playerId = playerId;
        this.favorites = favorites == null ? Collections.emptySet() : Collections.unmodifiableSet(new LinkedHashSet<>(favorites));
    }

    public UUID playerId() { return playerId; }
    public Set<String> favorites() { return favorites; }

    public ShopPreference withFavorite(String favoriteId, boolean favorite) {
        LinkedHashSet<String> updated = new LinkedHashSet<>(favorites);
        if (favorite) {
            updated.add(favoriteId);
        } else {
            updated.remove(favoriteId);
        }
        return new ShopPreference(playerId, updated);
    }
    @Override public String toString() {
        return "ShopPreference[playerId=" + playerId + ", favorites=" + favorites + "]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShopPreference that = (ShopPreference) o;
        return java.util.Objects.equals(playerId, that.playerId) && java.util.Objects.equals(favorites, that.favorites);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(playerId, favorites);
    }
}
