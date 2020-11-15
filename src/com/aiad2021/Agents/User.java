package com.aiad2021.Agents;

import com.aiad2021.AuctionInfo;
import com.aiad2021.view.CommunicationGUI;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.WakerBehaviour;
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

import static java.lang.Double.max;
import static java.lang.Double.min;

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
        gui.addText("\n User guide: Enter ? to get help\n");

        gui.addText("My local name is " + getAID().getLocalName());
        gui.addText("   Id: " + this.id);
        gui.addText("   Username: " + this.username);
        gui.addText("   Money available: " + this.money);

        DFSubscribe();
        addBehaviour(new ListeningBehaviour());

    }

    class ListeningBehaviour extends CyclicBehaviour {

        MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM_IF);

        public void action() {
            ACLMessage msg = receive(mt);
            if (msg != null) {
                String[] parts = msg.getContent().split(" ");
                String auctionID = msg.getSender().getName().split("@")[0];
                switch (parts.length) {
                    case 1:
                        if (parts[0].equals("Ended"))
                            gui.addText("Dutch " + auctionID + " ended ");
                        else
                            gui.addText("Dutch " + auctionID + " price decreased to " + parts[0] + " €");
                        break;
                    case 3://if regular inform
                        AuctionInfo auctionInfo = auctionsList.get(auctionID);
                         if (parts[0].equals("Won")){ //if in autobid
                            String winMsg = "You " + parts[0] + parts[1] + " for " + parts[2] + "€";
                            gui.addText(winMsg);
                        } else if (bidsList.containsKey(auctionID) && !parts[1].equals(username)) {//if end of auction
                            auctionInfo.setWinningPrice(Double.parseDouble(parts[0]));
                            auctionInfo.setMovement(Integer.parseInt(parts[2]));
                            gui.addText("I'm starting to lose");
                            makeBid(auctionID, getNewBid(bidsList.get(auctionID)));
                        } else {//update winner of auction
                            auctionInfo.setWinningPrice(Double.parseDouble(parts[0]));
                            double currentBid = auctionInfo.getCurrentBid();
                            if (currentBid != 0)
                                gui.addText("Recuperated " + currentBid + "€ from previous bid!");
                            money = money + currentBid;
                            auctionInfo.setCurrentBid(0);
                            gui.addText("New winner of " + auctionID + " is " + parts[1] + " with a current bid of " + parts[0]);
                            break;
                        }
                }
            } else {
                block();
            }
        }
    }

    private void createAuction(int id, String type, int duration, double basePrice, double minBid) {

        try {
            Object[] params = {id, type, duration, basePrice, minBid, this};
            // Agent path on jade = com.aiad2021.Agents
            AgentController ac = getContainerController().createNewAgent("Auction:" + params[0],
                    "com.aiad2021.Agents.Auction", params);
            ac.start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }

    private void makeBid(String auctionId, double bidValue) {
        Bid b = new Bid(money, 1, auctionId);
        if (bidsList.containsKey(auctionId))
            b = bidsList.get(auctionId);
        else if (bidValue == 0) { //create new bid value
            bidValue = getNewBid(b);
        }
        AuctionInfo auctionInfo = auctionsList.get(auctionId);
        if (b.delay > 0 && !b.receivedDelay) {
            if (b.smart) {
                gui.addText("SmartBid - Going to wait until the auction time is over " + b.delay * 100 + "% to bid");
            } else {
                gui.addText("AutoBid - Going to wait until the auction time is over " + b.delay * 100 + "% to bid");
            }
            double delay = max(0, auctionInfo.getDelay(b.delay));
            if (b.delayedBid != null) b.delayedBid.stop();//if a smart bid waiting for delay receives new inform
            b.delayedBid = new DelayedBid(this, (long) delay, auctionId);
            addBehaviour(b.delayedBid);
            gui.addText("Time remaining: " + delay / 1000.0 + " seconds");
            return;

        }
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
        money = money - bidValue;
        auctionInfo.setCurrentBid(bidValue);
        addBehaviour(new FIPARequestBid(this, new ACLMessage(ACLMessage.REQUEST), auctionId, auctionInfo, bidValue));

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
            Vector<ACLMessage> v = new Vector<>();

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
            if (bidsList.get(auctionId) != null) {//if autobid
                makeBid(auctionId, getNewBid(bidsList.get(auctionId)));
            } else
                makeBid(auctionId, 0);

        }

        protected void handleInform(ACLMessage inform) {
            if (auctionInfo.getType().equals("fprice") || auctionInfo.getType().equals("sprice")) {
                gui.addText("INFORM: " + auctionId + " - " + inform.getContent());
            } else
                //means that I am winning do nothing
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
            String[] parts = agree.getContent().split(" ");
            AuctionInfo auctionInfo = auctionsList.get(auctionId);
            auctionInfo.setWinningPrice(Double.parseDouble(parts[0]));
            auctionInfo.setMovement(Integer.parseInt(parts[3]));
            auctionInfo.setUpdated();
            gui.addText("Joined " + agree.getSender().getName().split("@")[0] + ", you will now receive notifications!");
        }

        /*protected void handleRefuse(ACLMessage refuse) {
            auctionsList.get(auctionId).setWinningPrice(Double.parseDouble(refuse.getContent()));
            makeBid(auctionId,0);
            System.out.println(refuse);
        }*/

        protected void handleInform(ACLMessage inform) {
            String[] parts = inform.getContent().split(" ");
            AuctionInfo auctionInfo = auctionsList.get(auctionId);
            auctionInfo.setWinningPrice(Double.parseDouble(parts[0]));
            auctionInfo.setMovement(Integer.parseInt(parts[3]));
            System.out.println("i am subscribe inform");
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
                //System.out.println("Agent " + getLocalName() + ": Notification received from DF");
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
                                }
                                //properties
                                int i = 0;
                                String type = "";
                                double basePrice = 0.0;
                                double minBid = 0.0;
                                double currentPrice = 0.0;
                                double duration = 0;
                                double start = System.currentTimeMillis();
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
                                        case 4:
                                            duration = Double.parseDouble((String) p.getValue());
                                            break;
                                    }
                                    i++;
                                }
                                AuctionInfo ai = new AuctionInfo(type, basePrice, minBid, currentPrice, duration, start, provider.getName());
                                if (!auctionsList.containsKey(sd.getName()))
                                    auctionsList.put(sd.getName(), ai);
                                else auctionsList.get(sd.getName()).setWinningPrice(currentPrice);

                                //Update auctions
                                gui.addText("\n Received Notification for new auction: \n");

                                gui.addText("       //        " + sd.getName() + "        //");
                                gui.addText("   Base Price  -> " + auctionsList.get(sd.getName()).getBasePrice() + "\n" +
                                        "   Auction IP   -> " + auctionsList.get(sd.getName()).getIp() + "\n" +
                                        "   Type           -> " + auctionsList.get(sd.getName()).getType() + "\n" +
                                        "   Winning Bid -> " + auctionsList.get(sd.getName()).getWinningPrice() + "\n");
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
        gui.addText("Agent " + getLocalName() + " searching for services of type \"auction-listing\"");
        try {
            // Build the description used as template for the search
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription templateSd = new ServiceDescription();
            templateSd.setType("auction-listing");
            template.addServices(templateSd);

            SearchConstraints sc = new SearchConstraints();
            // We want to receive 10 results at most
            sc.setMaxResults(50L);

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
                            //gui.addText("- Service \"" + sd.getName() + "\" provided by agent " + provider.getName());
                        }
                        //properties
                        int i = 0;
                        String type = "";
                        double basePrice = 0.0;
                        double minBid = 0.0;
                        double currentPrice = 0.0;
                        double duration = 0;
                        double start = System.currentTimeMillis();
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
                                case 4:
                                    duration = Double.parseDouble((String) p.getValue());
                                    break;
                            }
                            i++;
                        }
                        AuctionInfo ai = new AuctionInfo(type, basePrice, minBid, currentPrice, duration, start, provider.getName());
                        if (!auctionsList.containsKey(sd.getName()))
                            auctionsList.put(sd.getName(), ai);
                        else auctionsList.get(sd.getName()).setWinningPrice(currentPrice);

                    }
                }
            } else {
                gui.addText("Agent " + getLocalName() + " did not find any auction-listing service");
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
                if (parts.length == 6) {
                    if (parts[1].matches("\\d+") && parts[3].matches("\\d+") && parts[4].matches("\\d+(\\.\\d+)?") && parts[5].matches("\\d+(\\.\\d+)?")) {
                        createAuction(Integer.parseInt(parts[1]), parts[2], Integer.parseInt(parts[3]), Double.parseDouble(parts[4]), Double.parseDouble(parts[5]));
                    } else
                        gui.addText("Invalid create parameters. Expecting id, duration, basePrice and minBid to be a number");
                } else gui.addText("Invalid create command. Expecting: create id type duration basePrice minBid ");
                break;
            case "join":
                if (parts.length == 2) {
                    if (parts[1].matches("\\d+")) {
                        gui.addText("Received join request for auction " + parts[1] + "...");
                        subscribeAuction("Auction:" + parts[1]);
                    } else gui.addText("Invalid auction ID. ID needs to be a number");
                } else gui.addText("Invalid join command. Expecting: join auctionID");
                break;
            case "bid":
                if (parts.length == 3) {
                    if (parts[1].matches("\\d+") && parts[2].matches("\\d+(\\.\\d+)?")) {
                        System.out.println(parts[2].length());
                        makeBid("Auction:" + parts[1], Double.parseDouble(parts[2]));
                    } else gui.addText("Invalid auction ID or/and bidValue.These parameters need to be a number");
                } else gui.addText("Invalid bid command. Expected: bid auctionID bidValue");
                break;
            case "autobid": //arguments are auction,aggressiveness,maxBid, delay (newBid=oldBid+minBid*aggressiveness)
                if (parts.length == 5) {
                    if (parts[1].matches("\\d+") && parts[2].matches("[1-9]\\d*(\\.\\d+)?") && parts[3].matches("\\d+(\\.\\d+)?") && parts[4].matches("0(\\.\\d+)?")) {
                        createAutoBid("Auction:" + parts[1], Double.parseDouble(parts[2]), Double.parseDouble(parts[3]), Double.parseDouble(parts[4]));
                    } else
                        gui.addText("Invalid autobid command. auctionID, aggressiveness, maxBid and delay need to be of type number ");
                } else gui.addText("Invalid autobid command. Expecting: autobid auctionId aggressiveness maxBid delay");
                break;
            case "smartbid":
                if (parts.length == 3) {
                    if (parts[1].matches("\\d+") && parts[2].matches("[1-9]\\d*(\\.\\d+)?")) {
                        createAutoBid("Auction:" + parts[1], 0, Double.parseDouble(parts[2]), 0);
                    } else
                        gui.addText("Invalid smartbid command. auctionID and maxBid need to be of type number ");
                } else gui.addText("Invalid smartbid command. Expecting: smartbid auctionId maxBid");
                break;
            case "list":
                if (parts.length == 1)
                    displayAllAuctions();
                else gui.addText("list command should not have arguments");
            case "?":
                gui.addText(" Commands available are: create, join, bid, autobid, smartbid and list");
                break;
            default:
                gui.addText("Invalid command");
        }
    }

    private double getNewBid(Bid bid) {
        AuctionInfo auctionInfo = this.auctionsList.get(bid.auctionId);
        System.out.println(auctionInfo.getMinBid() + " | " + auctionInfo.getWinningPrice());
        switch (auctionInfo.getType()) {
            case "english":
                if (bid.smart) {
                    bid.delay = min(0.9, (auctionInfo.getMovement() + 1) / 5.0);//makes sure that the maximum is 0.9
                    bid.aggressiveness = max(1.0, auctionInfo.getMovement());//makes sure that the minimum is 1
                }
                if (bid.delay > 0 && bid.maxBid - auctionInfo.getWinningPrice() <= auctionInfo.getMinBid() * bid.aggressiveness) {
                    bid.delay = 0;//the prices are getting high, time to start bidding
                    gui.addText("Prices are getting too high, going to bid now");
                    if (bid.delayedBid != null) bid.delayedBid.stop(); //cancels the delayedBid
                    bid.receivedDelay = true;
                }
                if (auctionInfo.getWinningPrice() + auctionInfo.getMinBid() > bid.maxBid)
                    return -1;//auction is too expensive
                else if (bid.aggressiveness * auctionInfo.getMinBid() + auctionInfo.getWinningPrice() > bid.maxBid && bid.maxBid >= auctionInfo.getMinBid() + auctionInfo.getWinningPrice())
                    return bid.maxBid;
                else return bid.aggressiveness * auctionInfo.getMinBid() + auctionInfo.getWinningPrice();
            default:
                return -2;//auction type not found
        }
    }

    private void createAutoBid(String auctionId, double aggressiveness, double maxBid, double delay) {//delay is percentage, goes from 0 to 1
        if (aggressiveness == 0) {
            subscribeAuction(auctionId);
            while (!auctionsList.get(auctionId).isUpdated()) {
            }
        }
        Bid bid = new Bid(maxBid, aggressiveness, auctionId, delay);
        bidsList.put(auctionId, bid);
        makeBid(auctionId, getNewBid(bid));
    }

    private void displayAllAuctions() {
        //Update auctions
        DFSearch();
        gui.addText("\n The available auctions are the following: \n");
        auctionsList.forEach((k, v) -> {
            gui.addText("       //        " + k + "        //");
            gui.addText("   Base Price  -> " + v.getBasePrice() + "\n" +
                    "   Auction IP   -> " + v.getIp() + "\n" +
                    "   Type           -> " + v.getType() + "\n" +
                    "   Winning Bid -> " + v.getWinningPrice() + "\n");
        });
    }

    class DelayedBid extends WakerBehaviour {

        private final String auctionId;
        private Agent a;

        public DelayedBid(Agent a, long timeout, String auctionId) {
            super(a, timeout);
            this.a = a;
            this.auctionId = auctionId;
        }

        @Override
        protected void onWake() {
            super.onWake();
            bidsList.get(auctionId).receivedDelay = true;
            makeBid(auctionId, getNewBid(bidsList.get(auctionId)));
        }
    }
}

class Bid {
    double maxBid;
    double aggressiveness;
    double delay;
    String auctionId;
    boolean smart;
    boolean receivedDelay = false;
    User.DelayedBid delayedBid;

    public Bid(double maxBid, double aggressiveness, String auctionId) {
        this.maxBid = maxBid;
        if (aggressiveness == 0) {
            this.aggressiveness = 1;
            this.smart = true;
        } else this.aggressiveness = aggressiveness;
        this.delay = 0;
        this.auctionId = auctionId;
    }

    public Bid(double maxBid, double aggressiveness, String auctionId, double delay) {//to make smart, aggressiveness=0
        this.maxBid = maxBid;
        if (aggressiveness == 0) {
            this.aggressiveness = 1;
            this.smart = true;
        } else this.aggressiveness = aggressiveness;
        this.delay = delay;
        this.auctionId = auctionId;
    }
}


