package com.aiad2021.Agents;

import com.aiad2021.view.AuctionGUI;
import jade.core.AID;
import sajas.core.Agent;
import sajas.core.behaviours.TickerBehaviour;
import sajas.core.behaviours.WakerBehaviour;
import sajas.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import sajas.proto.AchieveREResponder;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;

import java.awt.*;
import java.util.ArrayList;

public class Auction extends Agent implements Drawable {

    private final int id;
    private final int duration;
    protected String type;
    protected double basePrice;
    protected double minBid;
    private double winningPrice;
    private double secondBestBid;
    private int amountOfBids;
    private String currentWinnerId;

    private ArrayList<AID> participants;
    private ArrayList<AID> madeOffer;

    //Yellow Pages
    private String serviceName;

    //GUI
    private AuctionGUI auctionGUI;

    private OpenSequenceGraph plot;

    public Auction( int id, String type,int duration,double basePrice, double minBid, OpenSequenceGraph plot  ){
        this.id=id;
        this.type=type;
        this.duration=duration;
        this.basePrice=basePrice;
        this.minBid=minBid;
        this.plot= plot;
    }

    @Override
    protected void setup() {
        //used to get parameters passes on initialization
        //Object[] args = this.getArguments();

        //init class
       // this.id = (int) args[0];
       // this.type = (String) args[1];
       // this.duration = (int) args[2];
       // this.basePrice = (double) args[3];
       // this.minBid = (double) args[4];

        plot.step();

        this.winningPrice = this.basePrice - this.minBid;
        this.secondBestBid = this.basePrice - this.minBid;
        this.auctionGUI = new AuctionGUI("" + id);
        this.auctionGUI.setVisible(true);

        this.currentWinnerId = "none";
        this.amountOfBids = 0;
        this.participants = new ArrayList<>();
        this.madeOffer = new ArrayList<>();

        displayInformationGUI();

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
            sd.addProperties(new Property("basePrice", this.basePrice));
            sd.addProperties(new Property("minBid", this.minBid));
            sd.addProperties(new Property("winningPrice", this.winningPrice));
            sd.addProperties(new Property("duration", this.duration));
            dfd.addServices(sd);

            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        if (this.type.equals("dutch")) {
            addBehaviour(new DutchAuctionBehaviour(this, this, 5 * 1000));
        } else {
            addBehaviour(new notifyWinner(this, (this.duration) * 1000));
        }

        addBehaviour(new FIPARequestResp(this, MessageTemplate.MatchPerformative(ACLMessage.REQUEST)));
        addBehaviour(new FIPASubscribeResp(this, MessageTemplate.MatchPerformative(ACLMessage.SUBSCRIBE)));
        System.out.println("auction setup done");
    }

    public double getWinningPrice(){
        return this.winningPrice;
    }
    @Override
    public void draw(SimGraphics simGraphics) {
        simGraphics.drawHollowFastOval(Color.blue);
    }

    @Override
    public int getX() {
        return this.duration;
    }

    @Override
    public int getY() {
        return (int) this.winningPrice;
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
            if (type.equals("sprice"))
                winningPrice = secondBestBid;
                plot.step();

            System.out.println("Auction:" + this.a.getAID().getName() + " ended");
            if (currentWinnerId.equals("none"))
                System.out.println("There was no winner!");
            else {
                System.out.println("Winner was " + currentWinnerId + " and the price: " + winningPrice + "€");
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM_IF);
                msg.addReceiver(new sajas.core.AID(currentWinnerId, false));
                String price = String.valueOf(winningPrice);
                msg.setContent("Won " + this.getAgent().getName().split("@")[0] + " " + price);
                send(msg);
            }

            informAll(currentWinnerId, true);
            auctionGUI.setVisible(false);

            try {
                DFService.deregister(a);
            } catch (FIPAException e) {
                e.printStackTrace();
            }

            this.a.doDelete();
        }
    }

    //Dutch Auction

    class DutchAuctionBehaviour extends TickerBehaviour {

        private Auction auction;
        private Agent a;

        public DutchAuctionBehaviour(Agent a, Auction auction, long period) {
            super(a, period);
            this.a = a;
            this.auction = auction;
        }

        @Override
        protected void onTick() {

            if (this.auction.basePrice <= 0.00) {
                try {
                    DFService.deregister(a);
                } catch (FIPAException e) {
                    e.printStackTrace();
                }
                auctionGUI.setVisible(false);
                this.a.doDelete();
            }


            this.auction.basePrice = this.auction.basePrice - this.auction.minBid;

            informAllDutch();
        }
    }

    //FIPA - handle auction proposals
    class FIPARequestResp extends AchieveREResponder {

        public FIPARequestResp(Agent a, MessageTemplate mt) {
            super(a, mt);
        }

        protected ACLMessage handleRequest(ACLMessage request) {
            addParticipant(request.getSender());
            double bidValue = Double.parseDouble(request.getContent());
            ACLMessage reply = request.createReply();
            switch (type) {
                case "english":
                    if (bidValue >= winningPrice + minBid) {
                        reply.setPerformative(ACLMessage.AGREE);
                        amountOfBids++;
                        winningPrice = bidValue;
                        plot.step();
                        currentWinnerId = request.getSender().getName().split("@")[0];
                    } else {
                        reply.setContent("" + winningPrice);
                        reply.setPerformative(ACLMessage.REFUSE);
                    }
                    break;
                case "dutch":
                    if (currentWinnerId.equals("none")) {
                        reply.setPerformative(ACLMessage.INFORM_IF);
                        reply.setContent("Won " + this.getAgent().getName().split("@")[0] + "! " + basePrice);
                        currentWinnerId = "Dutch";
                    } else {
                        reply.setPerformative(ACLMessage.FAILURE);
                    }
                    break;
                case "sprice":
                    if (bidValue >= minBid) {
                        reply.setPerformative(ACLMessage.INFORM);
                        if (MadeOffer(request.getSender())) {
                            reply.setContent("You have already bid on this item. First price auctions only accept one bid per user");
                            break;
                        }
                        reply.setContent("Your offer was accepted");
                        if (bidValue > winningPrice) {
                            secondBestBid = winningPrice;
                            winningPrice = bidValue;
                            plot.step();
                            currentWinnerId = request.getSender().getName().split("@")[0];
                        } else if (bidValue > secondBestBid) {
                            secondBestBid = bidValue;
                        }
                    } else reply.setPerformative(ACLMessage.REFUSE); //in case the offer is less than whats the min
                    break;
                case "fprice":
                    if (bidValue >= minBid) {
                        reply.setPerformative(ACLMessage.INFORM);
                        if (MadeOffer(request.getSender())) {
                            reply.setContent("You have already bid on this item. Second price auctions only accept one bid per user");
                            break;
                        } else if (bidValue > winningPrice) {
                            winningPrice = bidValue;
                            plot.step();
                            currentWinnerId = request.getSender().getName().split("@")[0];
                        }
                        reply.setContent("Your offer was accepted");
                    } else reply.setPerformative(ACLMessage.REFUSE); //in case the offer is less than whats the min
                    break;

            }

            displayNewOffer(request.getSender().getName().split("@")[0], bidValue, new ACLMessage(reply.getPerformative()));

            return reply;
        }

        protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) {

            ACLMessage result = request.createReply();

            result.setPerformative(ACLMessage.INFORM);
            if (type.equals("english"))
                result.setContent(winningPrice + " " + currentWinnerId + " " + amountOfBids);
            else result.setContent("" + participants.size());

            informAll(request.getSender().getName(), false);

            return result;
        }

    }

    class FIPASubscribeResp extends AchieveREResponder {

        public FIPASubscribeResp(Agent a, MessageTemplate mt) {
            super(a, mt);
        }

        protected ACLMessage handleRequest(ACLMessage request) {

            if (!request.getContent().equals("subscribe"))
                System.out.println("Received " + request.getContent() + " instead of subscribe");

            ACLMessage reply = request.createReply();
            reply.setPerformative(ACLMessage.AGREE);
            addParticipant(request.getSender());
            if (type.equals("english"))
                reply.setContent(winningPrice + " " + currentWinnerId + " " + amountOfBids);
            else reply.setContent(basePrice + " " + participants.size());
            return reply;
        }

        protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) {

            ACLMessage result = request.createReply();
            result.setPerformative(ACLMessage.INFORM);
            if (type.equals("english"))
                result.setContent(winningPrice + " " + currentWinnerId + " " + amountOfBids);
            else result.setContent(basePrice + " " + participants.size());
            return result;
        }

    }

    public void informAll(String aidName, boolean ended) {//argument is person to be excluded of inform all
        for (AID participant : this.participants) {
            if ((!participant.getName().equals(aidName)) && !participant.getName().split("@")[0].equals((aidName))) {
                ACLMessage message = new ACLMessage(ACLMessage.INFORM_IF);
                message.addReceiver(participant);
                if (ended) {
                    if (currentWinnerId.equals("none")) {
                        message.setContent("Auction ended without a winner");
                    } else message.setContent("Lost " + currentWinnerId + " " + winningPrice);
                } else {
                    if (type.equals("english"))
                        message.setContent(winningPrice + " " + currentWinnerId + " " + amountOfBids);
                    else message.setContent("" + participants.size());
                }
                this.send(message);
            }
        }


    }

    public void informAllDutch() {//for dutch auctions

        if (!currentWinnerId.equals("none")) {
            for (AID participant : this.participants) {
                ACLMessage message = new ACLMessage(ACLMessage.INFORM_IF);
                message.addReceiver(participant);
                message.setContent("Ended");
                this.send(message);
            }
            try {
                DFService.deregister(this);

            } catch (FIPAException e) {
                e.printStackTrace();
            }
            this.auctionGUI.setVisible(false);
            this.doDelete();
        } else {
            for (AID participant : this.participants) {
                ACLMessage message = new ACLMessage(ACLMessage.INFORM_IF);
                message.addReceiver(participant);
                message.setContent(basePrice + " " + participants.size());
                this.send(message);
            }
        }


    }

    public void addParticipant(AID aid) {//add participant to subscribers list
        boolean found = false;
        for (AID participant : this.participants) {
            if (participant.getName().equals(aid.getName())) {
                found = true;
                break;
            }
        }
        if (!found) {
            this.participants.add(aid);
            this.auctionGUI.addText("     !New join alert!" + "     \n" + aid.getName() + "   just joined!\n");
        }
    }

    public boolean MadeOffer(AID aid) {//add participant to subscribers list
        boolean found = false;
        for (AID participant : this.madeOffer) {
            if (participant.getName().equals(aid.getName())) {
                found = true;
                break;
            }
        }

        if (!found) {
            this.madeOffer.add(aid);
        }
        return found;
    }

    public void displayInformationGUI() {
        auctionGUI.addText("                                                    Auction " + this.id + "\n\n\n\n" +
                "       Type            ->    " + this.type + "\n" +
                "       Base Price   ->    " + this.basePrice + "\n" +
                "       Min Bid         ->    " + this.minBid + "\n\n");

    }

    public void displayNewOffer(String bidder, double offer, ACLMessage response) {
        switch (this.type) {
            case "sprice":
                auctionGUI.addText("                                         New Offer from" + bidder + "\n\n" +
                        "       Offer By Bidder        ->    " + offer + "\n" +
                        "       Winner Price            ->    " + this.winningPrice + "\n" +
                        "       Second Best Offer   ->    " + this.secondBestBid + "\n" +
                        "       Response                ->    " + response.toString() + "\n\n");
                break;
            default:
                auctionGUI.addText("                                         New Offer from" + bidder + "\n\n" +
                        "       Offer By Bidder     ->    " + offer + "\n" +
                        "       Winner Price         ->    " + this.winningPrice + "\n" +
                        "       Response            ->    " + response.toString() + "\n\n");
                break;
        }
    }
}
