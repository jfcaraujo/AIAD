package com.aiad2021;


public class AuctionInfo {

    private final String type;
    private Product product;
    private final double basePrice;
    private double winningPrice;
    private final String ip;
    private double minBid;

    public AuctionInfo(String type /*, Product product*/, double basePrice, double minBid, double winningPrice, String ip){
        this.type = type;
        this.basePrice = basePrice;
        this.minBid = minBid;
        this.winningPrice = winningPrice;
        this.ip = ip;
    }

    public String getType() {
        return type;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public void setWinningPrice(double winningPrice){
        this.winningPrice = winningPrice;
    }

    public double getWinningPrice() {
        return winningPrice;
    }

    public String getIp() {
        return ip;
    }

    public double getMinBid() {
        return minBid;
    }

    public void setMinBid(double minBid) {
        this.minBid = minBid;
    }
}
