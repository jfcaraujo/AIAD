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
import jade.lang.acl.MessageTemplate;
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
    private double money;
    CommunicationGUI gui;

    private Hashtable<String, Bid> bidsList;
    private Hashtable<String, AuctionInfo> auctionsList;

    // private ArrayList;

    @Override
    protected void setup() {
        // used to get parameters passes on initialization
        Object[] args = this.getArguments();

        // setup params
        this.id = (int) args[0];
        this.username = (String) args[1];
        this.money = (double) args[2];

        this.auctionsList = new Hashtable<>();
        this.bidsList = new Hashtable<>();

        gui = new CommunicationGUI(this);
        gui.setVisible(true);

        gui.addText("My local name is " + getAID().getLocalName());
        gui.addText("My GUID is " + getAID().getName());
        gui.addText("My addresses are " + String.join(",", getAID().getAddressesArray()));
        gui.addText("Id: " + this.id + " Username: " + this.username + "\n");

        DFSearch();
        DFSubscribe();
        addBehaviour(new ListeningBehaviour());

    }

    class ListeningBehaviour extends CyclicBehaviour {

        MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
        MessageTemplate mt2 = MessageTemplate.MatchPerformative(ACLMessage.INFORM_IF);

        public void action() {
            ACLMessage msg = receive(mt);
            ACLMessage msg2 = receive(mt2);
            if (msg != null) {
                String[] parts = msg.getContent().split(" ");
                if (parts.length == 3) {//if regular inform
                    gui.addText(msg.getSender().getName());
                    System.out.println("INFORM WAS " + msg);
                    String auctionID = msg.getSender().getName().split("@")[0];
                    auctionsList.get(auctionID).setWinningPrice(Double.parseDouble(parts[0]));
                    if (bidsList.containsKey(auctionID) && !parts[1].equals(username)) { //if in autobid
                        gui.addText("I'm starting to lose");
                        makeBid(auctionID, getNewBid(bidsList.get(auctionID)));
                    } else
                        gui.addText("New winner of " + auctionID.split("@")[0] + " is " + parts[1] + " with a current bid of " + parts[0]);
                }
            } else if (msg2 != null) {
                gui.addText(msg2.getContent());
            } else {
                block();
            }
        }
    }

    private void createAuction(int id, String type, int duration, double basePrice, int prodID) {

        try {
            Object[] params = {id, type, duration, basePrice, prodID, this};
            // Agent path on jade = com.aiad2021.Agents
            AgentController ac = getContainerController().createNewAgent(String.valueOf(params[0]),
                    "com.aiad2021.Agents.Auction", params);
            ac.start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }

    private void makeBid(String auctionId, double bidValue) {

        if (bidValue == 0) { //create new bid value
            Bid b = new Bid(money, 1, auctionId);
            bidValue = getNewBid(b);
        }//else do nth repeat
        switch ((int) bidValue) {
            case -1:
                gui.addText("MakeBid - Auction is too expensive!");
                return;
            case -2:
                gui.addText("MakeBid - Auction type not found!");
                return;
            default:
                break;
        }
        addBehaviour(new FIPARequestBid(this, new ACLMessage(ACLMessage.REQUEST), auctionId, this.auctionsList.get(auctionId), bidValue));
    }

    private void subscribeAuction(String auctionId) {
        addBehaviour(new FIPASubscribe(this, new ACLMessage(ACLMessage.SUBSCRIBE), auctionId));
    }

    // Bid
    class FIPARequestBid extends AchieveREInitiator {

        private String auctionId;
        private AuctionInfo auctionInfo;
        private String msgContent;

        public FIPARequestBid(Agent a, ACLMessage msg, String auctionId, AuctionInfo auctionInfo, Double bidValue) {
            super(a, msg);
            this.auctionId = auctionId;
            this.auctionInfo = auctionInfo;

            this.msgContent = String.valueOf(bidValue);

        }

        protected Vector<ACLMessage> prepareRequests(ACLMessage msg) {
            Vector<ACLMessage> v = new Vector<ACLMessage>();

            msg.addReceiver(new AID(this.auctionId, false));
            msg.setContent(this.msgContent);

            v.add(msg);
            return v;
        }

        protected void handleAgree(ACLMessage agree) {
            gui.addText("AGREE: " + auctionId);
        }

        protected void handleRefuse(ACLMessage refuse) {
            gui.addText("REFUSE: " + auctionId + " - I will try again with a higher value!");
            //make new bid with a higher value
            auctionsList.get(auctionId).setWinningPrice(Double.parseDouble(refuse.getContent()));
            makeBid(auctionId, 0);

        }

        protected void handleInform(ACLMessage inform) {//todo delete
            //means that i am winning do nothing
            gui.addText("INFORM: " + auctionId + " - I am winning !");
        }

        protected void handleFailure(ACLMessage failure) {//todo
            //repeat 3 times and stop with same value
            //makeBid(auctionId,bidValue);
            gui.addText("FAILURE: " + auctionId);
        }

    }

    class FIPASubscribe extends AchieveREInitiator {

        private String auctionId;
        private String msgContent;

        public FIPASubscribe(Agent a, ACLMessage msg, String auctionId) {
            super(a, msg);
            this.auctionId = auctionId;
            this.msgContent = "subscribe";

        }

        protected Vector<ACLMessage> prepareRequests(ACLMessage msg) {
            Vector<ACLMessage> v = new Vector<>();
            // ...

            msg.addReceiver(new AID(this.auctionId, false));
            msg.setContent(this.msgContent);

            v.add(msg);
            return v;
        }

        protected void handleAgree(ACLMessage agree) {
            System.out.println(agree);
            gui.addText("Joined " + agree.getSender().getName().split("@")[0] + ", you will now receive notifications!");
        }

        /*protected void handleRefuse(ACLMessage refuse) {
            auctionsList.get(auctionId).setWinningPrice(Double.parseDouble(refuse.getContent()));
            makeBid(auctionId,0);
            System.out.println(refuse);
        }*/

        protected void handleInform(ACLMessage inform) {//todo delete
            //todo idk
            System.out.println(inform);
        }

        protected void handleFailure(ACLMessage failure) {
            //makeBid(auctionId); //todo try without updating teh bid
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
                                double minBid = 0.0;
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
                                            minBid = Double.parseDouble((String) p.getValue());
                                            break;
                                        case 3:
                                            currentPrice = Double.parseDouble((String) p.getValue());
                                            break;
                                    }
                                    i++;
                                }
                                AuctionInfo ai = new AuctionInfo(type, basePrice, minBid, currentPrice, provider.getName());
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

    private void DFSearch() {
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
                        double minBid = 0.0;
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
                                    minBid = Double.parseDouble((String) p.getValue());
                                    break;
                                case 3:
                                    currentPrice = Double.parseDouble((String) p.getValue());
                                    break;

                            }
                            i++;
                        }
                        AuctionInfo ai = new AuctionInfo(type, basePrice, minBid, currentPrice, provider.getName());
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
        switch (parts[0]) {
            case "create":
                createAuction(Integer.parseInt(parts[1]), parts[2], Integer.parseInt(parts[3]), Double.parseDouble(parts[4]), Integer.parseInt(parts[5]));
                break;
            case "join":
                subscribeAuction(parts[1]);
                break;
            case "bid":
                System.out.println(parts[2].length());
                makeBid(parts[1], Double.parseDouble(parts[2]));
                break;
            case "autobid"://arguments are auction,aggressiveness,maxBid (newBid=oldBid+minBid*aggressiveness)
                createAutoBid(parts[1], Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
                break;
            default:
                System.out.println("Invalid command ????");
        }
    }

    private double getNewBid(Bid bid) {
        AuctionInfo auctionInfo = this.auctionsList.get(bid.auctionId);
        System.out.println(auctionInfo.getMinBid() + " | " + auctionInfo.getWinningPrice());
        switch (this.auctionsList.get(bid.auctionId).getType()) {
            case "english":
                if (auctionInfo.getWinningPrice() + auctionInfo.getMinBid() >= bid.maxBid)
                    return -1;//auction is too expensive
                    // else if ((auction.endTime-auction.startTime)/auction.startTime<bid.delay && bid.maxBid-lastBid>= bid.auction.minBid*bid.aggressiveness) return 0;//wait more before bidding
                else if (bid.aggressiveness * auctionInfo.getMinBid() + auctionInfo.getWinningPrice() > bid.maxBid && bid.maxBid >= auctionInfo.getMinBid() + auctionInfo.getWinningPrice())
                    return bid.maxBid;
                else return bid.aggressiveness * auctionInfo.getMinBid() + auctionInfo.getWinningPrice();
            default:
                return -2;//auction type not found
        }
    }

    private void createAutoBid(String auctionId, double aggressiveness, double maxBid) {
        Bid bid = new Bid(maxBid, aggressiveness, auctionId);
        bidsList.put(auctionId, bid);
        makeBid(auctionId, getNewBid(bid));
    }
}

class Bid {
    double maxBid;
    double aggressiveness;
    //int delay;
    String auctionId;

    public Bid(double maxBid, double aggressiveness, String auctionId) {
        this.maxBid = maxBid;
        this.aggressiveness = aggressiveness;
        //this.delay = delay;
        this.auctionId = auctionId;
    }
}