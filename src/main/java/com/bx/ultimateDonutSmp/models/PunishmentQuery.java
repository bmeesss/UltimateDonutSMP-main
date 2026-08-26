package com.bx.ultimateDonutSmp.models;

public final class PunishmentQuery {
    private final PunishmentType typeFilter;
    private final PunishmentFilterState stateFilter;
    private final PunishmentSortOrder sortOrder;

    public PunishmentQuery(PunishmentType typeFilter, PunishmentFilterState stateFilter, PunishmentSortOrder sortOrder) {
        stateFilter = stateFilter == null ? PunishmentFilterState.ALL : stateFilter;
        sortOrder = sortOrder == null ? PunishmentSortOrder.NEWEST : sortOrder;
        this.typeFilter = typeFilter;
        this.stateFilter = stateFilter;
        this.sortOrder = sortOrder;
    }

    public PunishmentType typeFilter() { return typeFilter; }
    public PunishmentFilterState stateFilter() { return stateFilter; }
    public PunishmentSortOrder sortOrder() { return sortOrder; }




    public static PunishmentQuery defaultQuery() {
        return new PunishmentQuery(null, PunishmentFilterState.ALL, PunishmentSortOrder.NEWEST);
    }

    public PunishmentQuery nextTypeFilter() {
        return new PunishmentQuery(PunishmentType.nextFilter(typeFilter), stateFilter, sortOrder);
    }

    public PunishmentQuery nextStateFilter() {
        return new PunishmentQuery(typeFilter, stateFilter.next(), sortOrder);
    }

    public PunishmentQuery nextSortOrder() {
        return new PunishmentQuery(typeFilter, stateFilter, sortOrder.next());
    }
    @Override public String toString() {
        return "PunishmentQuery[typeFilter=+typeFilter, stateFilter=+stateFilter, sortOrder=+sortOrder]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PunishmentQuery that = (PunishmentQuery) o;
        return java.util.Objects.equals(typeFilter, that.typeFilter) && java.util.Objects.equals(stateFilter, that.stateFilter) && java.util.Objects.equals(sortOrder, that.sortOrder);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(typeFilter, stateFilter, sortOrder);
    }
}
