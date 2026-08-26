package com.bx.ultimateDonutSmp.models;

import com.bx.ultimateDonutSmp.managers.AuctionHouseManager;

public final class AuctionBrowseRequest {
    private final int page;
    private final AuctionHouseManager.AuctionSort sort;
    private final AuctionCategory category;
    private final String search;

    public AuctionBrowseRequest(int page, AuctionHouseManager.AuctionSort sort, AuctionCategory category, String search) {
        page = Math.max(1, page);
        sort = sort == null ? AuctionHouseManager.AuctionSort.NEWEST : sort;
        category = category == null ? AuctionCategory.ALL : category;
        search = search == null ? "" : search.trim();
        this.page = page;
        this.sort = sort;
        this.category = category;
        this.search = search;
    }

    public int page() { return page; }
    public AuctionHouseManager.AuctionSort sort() { return sort; }
    public AuctionCategory category() { return category; }
    public String search() { return search; }




    public AuctionBrowseRequest withPage(int nextPage) {
        return new AuctionBrowseRequest(nextPage, sort, category, search);
    }

    public AuctionBrowseRequest withSort(AuctionHouseManager.AuctionSort nextSort) {
        return new AuctionBrowseRequest(1, nextSort, category, search);
    }

    public AuctionBrowseRequest withCategory(AuctionCategory nextCategory) {
        return new AuctionBrowseRequest(1, sort, nextCategory, search);
    }

    public AuctionBrowseRequest withSearch(String nextSearch) {
        return new AuctionBrowseRequest(1, sort, category, nextSearch);
    }
    @Override public String toString() {
        return "AuctionBrowseRequest[page=+page, sort=+sort, category=+category, search=+search]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuctionBrowseRequest that = (AuctionBrowseRequest) o;
        return java.util.Objects.equals(page, that.page) && java.util.Objects.equals(sort, that.sort) && java.util.Objects.equals(category, that.category) && java.util.Objects.equals(search, that.search);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(page, sort, category, search);
    }
}
