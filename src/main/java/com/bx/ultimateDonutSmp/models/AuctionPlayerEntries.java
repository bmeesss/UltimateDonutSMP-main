package com.bx.ultimateDonutSmp.models;

import java.util.ArrayList;
import java.util.List;

public final class AuctionPlayerEntries {

    private AuctionPlayerEntries() {
    }

    public static List<Object> combine(
            List<AuctionListing> listings,
            List<AuctionClaim> claims,
            boolean claimsEnabled
    ) {
        List<Object> entries = new ArrayList<>(listings == null ? java.util.Collections.emptyList() : listings);
        if (claimsEnabled && claims != null) {
            entries.addAll(claims);
        }
        return new java.util.ArrayList<>(entries);
    }
}
