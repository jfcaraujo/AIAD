package com.aiad2021.Agents;

import com.aiad2021.Product;
import jade.core.Agent;

import java.util.Date;

public class Auction extends Agent {

    private int id;
    private Date startTime;
    private Date endDate;
    private Product product;
    private double basePrice;
    private double lastPrice;
    private User currentWinner;

    @Override
    protected void setup(){

        //used to get parameters passes on intilialization
        Object[] args = this.getArguments();

        //init class
        this.id = (int) args[0];
        this.startTime = new Date(); //TODO
        this.endDate = new Date(); //TODO
        this.product = new Product(); //TODO
        this.basePrice = (double) args[1]; //todo change
        this.lastPrice = 0; //todo
        this.currentWinner = null; //todo

        System.out.println("My local name is " + getAID().getLocalName());
        System.out.println("My GUID is " + getAID().getName());
        System.out.println("My addresses are " + String.join(",", getAID().getAddressesArray()));

        System.out.println( "Id: " + this.id+"\n");
    }
}
