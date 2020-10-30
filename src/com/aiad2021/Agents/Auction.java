package com.aiad2021.Agents;

import com.aiad2021.Product;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;

import java.util.Date;

public class Auction extends Agent {

    private int id;
    private Date startTime;
    private Date endDate;
    private Product product;
    private double basePrice;
    private double lastPrice;
    private User currentWinner;

    //Yellow Pages
    private String serviceName;
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

        //yellow pages setup
        this.serviceName = "Auction:"+this.id;

        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setName(serviceName);
            sd.setType("auction-listing");
            // Agents that want to use this service need to "know" the weather-forecast-ontology
            sd.addOntologies("auction-listing-ontology");
            // Agents that want to use this service need to "speak" the FIPA-SL language
            sd.addLanguages(FIPANames.ContentLanguage.FIPA_SL);
            sd.addProperties(new Property("type", "english")); //todo adaptar consoante o tipo de leilao
            dfd.addServices(sd);

            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }
}
