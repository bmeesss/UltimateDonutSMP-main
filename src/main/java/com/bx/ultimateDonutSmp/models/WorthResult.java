package com.bx.ultimateDonutSmp.models;

public final class WorthResult {
    private final boolean sellable;
    private final boolean container;
    private final double unitWorth;
    private final double totalWorth;
    private final double baseWorth;
    private final double containerContentsWorth;
    private final String resolutionType;
    private final String sourceKey;
    private final String categoryKey;

    public WorthResult(boolean sellable, boolean container, double unitWorth, double totalWorth, double baseWorth, double containerContentsWorth, String resolutionType, String sourceKey, String categoryKey) {
        this.sellable = sellable;
        this.container = container;
        this.unitWorth = unitWorth;
        this.totalWorth = totalWorth;
        this.baseWorth = baseWorth;
        this.containerContentsWorth = containerContentsWorth;
        this.resolutionType = resolutionType;
        this.sourceKey = sourceKey;
        this.categoryKey = categoryKey;
    }

    public boolean sellable() { return sellable; }
    public boolean container() { return container; }
    public double unitWorth() { return unitWorth; }
    public double totalWorth() { return totalWorth; }
    public double baseWorth() { return baseWorth; }
    public double containerContentsWorth() { return containerContentsWorth; }
    public String resolutionType() { return resolutionType; }
    public String sourceKey() { return sourceKey; }
    public String categoryKey() { return categoryKey; }



    public static WorthResult unsellable() {
        return new WorthResult(false, false, -1, -1, 0, 0, "unsellable", "", "");
    }

    public boolean hasContainerContentsWorth() {
        return containerContentsWorth > 0;
    }
    @Override public String toString() {
        return "WorthResult[sellable=+sellable, container=+container, unitWorth=+unitWorth, totalWorth=+totalWorth, baseWorth=+baseWorth, containerContentsWorth=+containerContentsWorth, resolutionType=+resolutionType, sourceKey=+sourceKey, categoryKey=+categoryKey]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorthResult that = (WorthResult) o;
        return java.util.Objects.equals(sellable, that.sellable) && java.util.Objects.equals(container, that.container) && java.util.Objects.equals(unitWorth, that.unitWorth) && java.util.Objects.equals(totalWorth, that.totalWorth) && java.util.Objects.equals(baseWorth, that.baseWorth) && java.util.Objects.equals(containerContentsWorth, that.containerContentsWorth) && java.util.Objects.equals(resolutionType, that.resolutionType) && java.util.Objects.equals(sourceKey, that.sourceKey) && java.util.Objects.equals(categoryKey, that.categoryKey);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(sellable, container, unitWorth, totalWorth, baseWorth, containerContentsWorth, resolutionType, sourceKey, categoryKey);
    }
}
