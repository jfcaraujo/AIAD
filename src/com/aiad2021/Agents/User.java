package com.aiad2021.Agents;

import com.aiad2021.AuctionInfo;
import com.aiad2021.view.CommunicationGUI;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
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

        gui = new CommunicationGUI(this.username);
        gui.setVisible(true);

        gui.addText("My local name is " + getAID().getLocalName());
        gui.addText("My GUID is " + getAID().getName());
        gui.addText("My addresses are " + String.join(",", getAID().getAddressesArray()));
        gui.addText("Id: " + this.id + " Username: " + this.username + "\n");

        DFSearch();
        DFSubscribe();

        System.out.println(auctionsList.get("Auction:1").getType());

        addBehaviour(new UserListeningBehaviour());

    }

    // todo see if its worth having a folder for all the behaviours
    // usar request para criar novos leiloes, pedir listagem de leiloes e dar join
    // usar inform para msg de accept e ganhar ou perder

    // todo adapt to be used on the GUI
    class UserListeningBehaviour extends CyclicBehaviour {

        public void action() {
            ACLMessage msg = receive();
            if (msg != null) {
                switch (msg.getPerformative()) {
                    case ACLMessage.REQUEST:
                        System.out.println(msg);

                        String[] parts = msg.getContent().split(" ");
                        // create new auction
                        try {
                            Object[] params = {Integer.parseInt(parts[0]), parts[1], Integer.parseInt(parts[2]),
                                    Double.parseDouble(parts[3]), Integer.parseInt(parts[4]), this};
                            // Agent path on jade = com.aiad2021.Agents
                            AgentController ac = getContainerController().createNewAgent(String.valueOf(params[0]),
                                    "com.aiad2021.Agents.Auction", params);
                            ac.start();
                            // this.agentControllers.add(ac); //todo idk what is this used for
                        } catch (StaleProxyException e) {
                            e.printStackTrace();
                        }
                        break;
                    case ACLMessage.INFORM:
                        break;

                }

                // ACLMessage reply = msg.createReply();

                // send(reply);
            } else {
                block();
            }
        }
    }

    // Bid
    class FIPARequestInit extends AchieveREInitiator {

        public FIPARequestInit(Agent a, ACLMessage msg) {
            super(a, msg);
        }

        protected Vector<ACLMessage> prepareRequests(ACLMessage msg) {
            Vector<ACLMessage> v = new Vector<ACLMessage>();
            // ...

            msg.addReceiver(new AID("qqq", false));
            msg.setContent("can you do this for me?");

            v.add(msg);
            return v;
        }

        protected void handleAgree(ACLMessage agree) {
            // ...
            System.out.println(agree);
        }

        protected void handleRefuse(ACLMessage refuse) {
            // ...
            System.out.println(refuse);
        }

        protected void handleInform(ACLMessage inform) {
            // ...
            System.out.println(inform);
        }

        protected void handleFailure(ACLMessage failure) {
            // ...
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
    static public void handleMessage(String message) {
        System.out.println(message);
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