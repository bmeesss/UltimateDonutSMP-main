package com.bx.ultimateDonutSmp.models;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class AuctionPage {
    private final List<AuctionListing> listings;
    private final int page;
    private final int totalPages;
    private final int totalListings;

    public AuctionPage(List<AuctionListing> listings, int page, int totalPages, int totalListings) {
        this.listings = listings == null ? Collections.emptyList() : new java.util.ArrayList<>(listings);
        this.page = Math.max(1, page);
        this.totalPages = Math.max(1, totalPages);
        this.totalListings = Math.max(0, totalListings);
    }

    public List<AuctionListing> listings() { return listings; }
    public int page() { return page; }
    public int totalPages() { return totalPages; }
    public int totalListings() { return totalListings; }


    public boolean hasPrevious() {
        return page > 1;
    }

    public boolean hasNext() {
        return page < totalPages;
    }
    @Override public String toString() {
        return "AuctionPage[listings=" + listings + ", page=" + page + ", totalPages=" + totalPages + ", totalListings=" + totalListings + "]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuctionPage that = (AuctionPage) o;
        return java.util.Objects.equals(listings, that.listings) && java.util.Objects.equals(page, that.page) && java.util.Objects.equals(totalPages, that.totalPages) && java.util.Objects.equals(totalListings, that.totalListings);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(listings, page, totalPages, totalListings);
    }
}
