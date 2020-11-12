package com.aiad2021;


public class AuctionInfo {

    private String type;
    private Product product;
    private double basePrice;
    private double winningPrice;
    private String ip;

    public AuctionInfo(String type /*, Product product*/, double basePrice, double winningPrice, String ip){
        this.type = type;
        this.basePrice = basePrice;
        this.winningPrice = winningPrice;
        this.ip = ip;
    }

    public String getType() {
        return type;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public double getWinningPrice() {
        return winningPrice;
    }

    public String getIp() {
        return ip;
    }
}
