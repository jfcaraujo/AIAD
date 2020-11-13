package com.aiad2021.Agents;

import com.aiad2021.Product;
import jade.core.Agent;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;

import java.util.ArrayList;

import com.aiad2021.view.AuctionGUI;

public class Auction extends Agent {

    private int id;

    private int duration;
    protected String type;
    private Product product;
    private double basePrice;
    protected double minBid;
    private double winningPrice;
    private int amountOfBids;
    private User owner;
    private String currentWinnerId;

    private ArrayList<User> participants;

    //Yellow Pages
    private String serviceName;

    //GUI
    private AuctionGUI auctionGUI;

    @Override
    protected void setup() {
        //used to get parameters passes on initialization
        Object[] args = this.getArguments();

        //init class
        this.id = (int) args[0];
        this.type = (String) args[1];
        this.duration = (int) args[2];
        this.basePrice = (double) args[3]; //todo change
        this.winningPrice = this.basePrice;//todo add min bid
        this.product = new Product(); //TODO pass the id
        this.auctionGUI = new AuctionGUI("" + id);
        this.auctionGUI.setVisible(true);
        //this.owner = (User) args[5];

        this.currentWinnerId = " "; //todo
        this.amountOfBids = 0;
        this.participants = new ArrayList<>();

        System.out.println("My local name is " + getAID().getLocalName());
        System.out.println("My GUID is " + getAID().getName());
        System.out.println("My addresses are " + String.join(",", getAID().getAddressesArray()));

        System.out.println("Id: " + this.id + "\n");

        //yellow pages setup
        this.serviceName = "Auction:" + this.id;

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
            sd.addProperties(new Property("type", this.type));
            /*sd.addProperties(new Property("product",this.product));*/ //todo check about products
            sd.addProperties(new Property("basePrice", this.basePrice));
            sd.addProperties(new Property("winningPrice", this.winningPrice));
            dfd.addServices(sd);

            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        addBehaviour(new notifyWinner(this, (this.duration + 5) * 1000));
        addBehaviour(new FIPARequestResp(this, MessageTemplate.MatchPerformative(ACLMessage.REQUEST)));
    }

    //notify winner
    class notifyWinner extends WakerBehaviour {

        private Agent a;

        public notifyWinner(Agent a, long timeout) {
            super(a, timeout);
            this.a = a;
        }

        @Override
        protected void onWake() {
            super.onWake();
            System.out.println("Auction:" + this.a.getAID() + " ended");
            //todo message the winner
            this.a.doDelete();
        }
    }

    //FIPA - handle auction proposals
    class FIPARequestResp extends AchieveREResponder {

        public FIPARequestResp(Agent a, MessageTemplate mt) {
            super(a, mt);
        }

        protected ACLMessage handleRequest(ACLMessage request) {

            double bidValue = Double.parseDouble(request.getContent());

            ACLMessage reply = request.createReply();

            if (bidValue > winningPrice) {
                reply.setPerformative(ACLMessage.AGREE);
                winningPrice = bidValue;
                currentWinnerId = request.getSender().getName();
            } else {
                reply.setPerformative(ACLMessage.REFUSE);
            }
            return reply;
        }

        protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) {

            ACLMessage result = request.createReply();

           /* //update winning price
            winningPrice = Double.parseDouble(request.getContent());
            //update current winner
            currentWinnerId = request.getSender().getName();

            result.setPerformative(ACLMessage.INFORM);
            result.setContent("You are winning");*/

            //posso enviar tbm objetos se quiser
            result.setPerformative(ACLMessage.INFORM);
            result.setContent(winningPrice+" "+currentWinnerId);

            return result;
        }

    }

    class FIPASubscribeResp extends AchieveREResponder {

        public FIPASubscribeResp(Agent a, MessageTemplate mt) {
            super(a, mt);
        }

        protected ACLMessage handleRequest(ACLMessage request) {

            if(!request.getContent().equals("subscribe"))
                System.out.println("Received "+request.getContent() +" instead of subscribe");

            ACLMessage reply = request.createReply();
            reply.setPerformative(ACLMessage.AGREE);
            return reply;
        }

        protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) {

            ACLMessage result = request.createReply();

           /* //update winning price
            winningPrice = Double.parseDouble(request.getContent());
            //update current winner
            currentWinnerId = request.getSender().getName();

            result.setPerformative(ACLMessage.INFORM);
            result.setContent("You are winning");*/

            //posso enviar tbm objetos se quiser
            result.setPerformative(ACLMessage.INFORM);
            result.setContent(winningPrice+" "+currentWinnerId);

            return result;
        }

    }

}
