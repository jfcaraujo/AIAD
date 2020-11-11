package com.aiad2021.Agents;

import com.aiad2021.Product;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;

import java.util.ArrayList;

public class Auction extends Agent {

    private int id;

    private int duration;
    private String type;
    private Product product;
    private double basePrice;
    private ArrayList<Double> bids;
    private User owner;
    private User currentWinner;

    private ArrayList<User> participants;

    //Yellow Pages
    private String serviceName;

    @Override
    protected void setup(){
        //used to get parameters passes on intilialization
        Object[] args = this.getArguments();

        //init class
       this.id = (int) args[0];
       this.type = (String) args[1];
       this.duration = (int) args[2];
       this.basePrice = (double) args[3]; //todo change
       this.product = new Product(); //TODO pass the id
       //this.owner = (User) args[5];

        this.currentWinner = null; //todo
        this.bids = new ArrayList<>();
        this.participants = new ArrayList<>();

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
            sd.addProperties(new Property("type", type));
            dfd.addServices(sd);

            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }

    }

    //read messages

    //subscribe

}
