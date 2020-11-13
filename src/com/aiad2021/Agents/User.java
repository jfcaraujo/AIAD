package com.aiad2021.Agents;

import com.aiad2021.AuctionInfo;
import com.aiad2021.view.CommunicationGUI;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;
import jade.proto.SubscriptionInitiator;
import jade.util.leap.Iterator;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

import java.util.Hashtable;
import java.util.Vector;

public class User extends Agent {

    // attributes
    private int id;
    private String username;
    CommunicationGUI gui;

    private Hashtable<String, AuctionInfo> auctionsList;

    // private ArrayList;

    @Override
    protected void setup() {
        // used to get parameters passes on initialization
        Object[] args = this.getArguments();

        // setup params
        this.id = (int) args[0];
        this.username = (String) args[1];

        this.auctionsList = new Hashtable<>();

        gui = new CommunicationGUI( this);
        gui.setVisible(true);

        gui.addText("My local name is " + getAID().getLocalName());
        gui.addText("My GUID is " + getAID().getName());
        gui.addText("My addresses are " + String.join(",", getAID().getAddressesArray()));
        gui.addText("Id: " + this.id + " Username: " + this.username + "\n");

        DFSearch();
        DFSubscribe();

        //todo delete - debug
        System.out.println(auctionsList.get("Auction:1").getType());

        //todo create a behaviour to read the winner message

    }

    private void createAuction(int id, String type, int duration, double baseprice, int prodID){

        try {
            Object[] params = {id, type, duration, baseprice, prodID, this};
            // Agent path on jade = com.aiad2021.Agents
            AgentController ac = getContainerController().createNewAgent(String.valueOf(params[0]),
                    "com.aiad2021.Agents.Auction", params);
            ac.start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
    //
    private void joinAuction(String auctionId){
        makeBid(auctionId);
        subscribeAuction(auctionId);
    }

    private void makeBid(String auctionId){
        //todo create bid value for the maesage content
        addBehaviour(new FIPARequestBid(this, new ACLMessage(ACLMessage.REQUEST),auctionId,this.auctionsList.get("Auction:1")));
    }

    private void subscribeAuction(String auctionId){
        ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
        message.addReceiver(new AID(auctionId, false));
        message.setContent("subscribe");
        this.send(message);
    }


    private void getStatus(String auctionId) {

    }

    // Bid
    class FIPARequestBid extends AchieveREInitiator {

        private String auctionId;
        private AuctionInfo auctionInfo;
        private String msgContent;

        public FIPARequestBid(Agent a, ACLMessage msg, String auctionId, AuctionInfo auctionInfo) {
            super(a, msg);
            this.auctionId = auctionId;
            this.auctionInfo = auctionInfo;

            this.msgContent = String.valueOf(this.auctionInfo.getWinningPrice() + 1.0);

        }

        protected Vector<ACLMessage> prepareRequests(ACLMessage msg) {//todo why a vector?
            Vector<ACLMessage> v = new Vector<ACLMessage>();
            // ...

            msg.addReceiver(new AID(this.auctionId, false));
            msg.setContent(this.msgContent);

            v.add(msg);
            return v;
        }

        protected void handleAgree(ACLMessage agree) {
            System.out.println(agree);
        }

        protected void handleRefuse(ACLMessage refuse) {
            makeBid(auctionId); //todo update bids
            System.out.println(refuse);
        }

        protected void handleInform(ACLMessage inform) {
            //todo idk
            System.out.println(inform);
        }

        protected void handleFailure(ACLMessage failure) {
            makeBid(auctionId); //todo try without updating teh bid
            System.out.println(failure);
        }

    }

    private void DFSubscribe() {
        // Build the description used as template for the subscription
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription templateSd = new ServiceDescription();
        templateSd.setType("auction-listing");
        // templateSd.addProperties(new Property("country", "Italy")); //todover quando
        // se colocar os tipos de leiloes
        template.addServices(templateSd);

        SearchConstraints sc = new SearchConstraints();
        // We want to receive 10 results at most
        sc.setMaxResults(10L);

        addBehaviour(new SubscriptionInitiator(this,
                DFService.createSubscriptionMessage(this, getDefaultDF(), template, sc)) {
            protected void handleInform(ACLMessage inform) {
                gui.addText("Agent " + getLocalName() + ": Notification received from DF");
                System.out.println("Agent " + getLocalName() + ": Notification received from DF");
                try {
                    DFAgentDescription[] results = DFService.decodeNotification(inform.getContent());
                    if (results.length > 0) {
                        for (DFAgentDescription dfd : results) {
                            AID provider = dfd.getName();
                            // The same agent may provide several services; we are only interested
                            // in the auction-listing one
                            Iterator it = dfd.getAllServices();
                            while (it.hasNext()) {
                                ServiceDescription sd = (ServiceDescription) it.next();
                                if (sd.getType().equals("auction-listing")) {
                                    gui.addText("- Service \"" + sd.getName() + "\" provided by agent " + provider.getName());
                                }
                                //properties
                                int i = 0;
                                String type = "";
                                double basePrice = 0.0;
                                double currentPrice = 0.0;
                                it = sd.getAllProperties();
                                while (it.hasNext()) {
                                    Property p = (Property) it.next();
                                    switch (i) {
                                        case 0:
                                            type = (String) p.getValue();
                                            break;
                                        case 1:
                                            basePrice = Double.parseDouble((String) p.getValue());
                                            break;
                                        case 2:
                                            currentPrice = Double.parseDouble((String) p.getValue());
                                            break;

                                    }
                                    i++;
                                }
                                AuctionInfo ai = new AuctionInfo(type, basePrice, currentPrice, provider.getName());
                                auctionsList.put(sd.getName(), ai);
                            }
                        }
                    }
                    gui.addText("\n");
                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }
            }
        });
    }

    private void DFSearch() { // todo nao testei
        // Search for services of type "weather-forecast"
        gui.addText("Agent " + getLocalName() + " searching for services of type \"auction-listing\"");
        try {
            // Build the description used as template for the search
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription templateSd = new ServiceDescription();
            templateSd.setType("auction-listing");
            template.addServices(templateSd);

            SearchConstraints sc = new SearchConstraints();
            // We want to receive 10 results at most
            sc.setMaxResults(10L);

            DFAgentDescription[] results = DFService.search(this, template, sc);
            if (results.length > 0) {
                gui.addText("Agent " + getLocalName() + " found the following auction-listing services:");
                for (DFAgentDescription dfd : results) {
                    AID provider = dfd.getName();
                    // The same agent may provide several services; we are only interested
                    // in the weather-forcast one
                    Iterator it = dfd.getAllServices();
                    while (it.hasNext()) {
                        ServiceDescription sd = (ServiceDescription) it.next();
                        if (sd.getType().equals("auction-listing")) {
                            gui.addText("- Service \"" + sd.getName() + "\" provided by agent " + provider.getName());
                        }
                        //properties
                        int i = 0;
                        String type = "";
                        double basePrice = 0.0;
                        double currentPrice = 0.0;
                        it = sd.getAllProperties();
                        while (it.hasNext()) {
                            Property p = (Property) it.next();
                            switch (i) {
                                case 0:
                                    type = (String) p.getValue();
                                    break;
                                case 1:
                                    basePrice = Double.parseDouble((String) p.getValue());
                                    break;
                                case 2:
                                    currentPrice = Double.parseDouble((String) p.getValue());
                                    break;

                            }
                            i++;
                        }
                        AuctionInfo ai = new AuctionInfo(type, basePrice, currentPrice, provider.getName());
                        auctionsList.put(sd.getName(), ai);
                    }
                }
            } else {
                gui.addText("Agent " + getLocalName() + " did not find any weather-forecast service");
            }
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    // handles input from user
    public void handleMessage(String message) {
        System.out.println(message);
        String[] parts = message.split(" ");
        //todo avoid bad messages
        switch(parts[0]){
            case "create":
                createAuction(Integer.parseInt(parts[1]),parts[2],Integer.parseInt(parts[3]),Double.parseDouble(parts[4]),Integer.parseInt(parts[5]));
                break;
            case "join":
                joinAuction(parts[1]); //todo parse auction id
                break;
            case "subscribe":
                subscribeAuction(parts[1]);
                break;
            default:
                System.out.println("Invalid command");
        }
//Todo isn't this unreachable?
        if(message.equals("join")){ //todo adapt to bid on the auction i type
            System.out.println("accepted");
            joinAuction("Auction:1"); //todo parse auction id
        }
    }

    private double getNewBid(Bid bid) {
        AuctionInfo auctionInfo = this.auctionsList.get(bid.auction.getName());
        switch (bid.auction.type) {
            case "english auction":
                if (auctionInfo.getWinningPrice() + bid.auction.minBid >= bid.maxBid)
                    return -1;//auction is too expensive
                // else if ((auction.endTime-auction.startTime)/auction.startTime<bid.delay && bid.maxBid-lastBid>= bid.auction.minBid*bid.aggressiveness) return 0;//wait more before bidding
                else if (bid.aggressiveness * bid.auction.minBid + auctionInfo.getWinningPrice() > bid.maxBid && bid.maxBid >= bid.auction.minBid + auctionInfo.getWinningPrice())
                    return bid.maxBid;
                else return bid.aggressiveness * bid.auction.minBid + auctionInfo.getWinningPrice();
            default:
                return -2;//auction type not found
        }
    }

}

class Bid {
    int maxBid;
    int aggressiveness;
    //int delay;
    Auction auction;
    String auctionName;

    public Bid(int maxBid, int aggressiveness, Auction auction, String auctionName) {
        this.maxBid = maxBid;
        this.aggressiveness = aggressiveness;
        //this.delay = delay;
        this.auction = auction;
        this.auctionName = auctionName;
    }
}