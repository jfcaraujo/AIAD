package com.aiad2021.Agents;

import jade.core.AID;
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
    private double basePrice;
    private double minBid;
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

    @Override
    protected void setup() {
        //used to get parameters passes on initialization
        Object[] args = this.getArguments();

        //init class
        this.id = (int) args[0];
        this.type = (String) args[1];
        this.duration = (int) args[2];
        this.basePrice = (double) args[3]; //todo change
        this.minBid = (double) args[4];
        this.winningPrice = this.basePrice - this.minBid;
        this.secondBestBid = this.basePrice - this.minBid;
        this.auctionGUI = new AuctionGUI("" + id);
        this.auctionGUI.setVisible(true);
        //this.owner = (User) args[5];

        this.currentWinnerId = " ";
        this.amountOfBids = 0;
        this.participants = new ArrayList<>();
        this.madeOffer = new ArrayList<>();

        displayInformationGUI();

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
            sd.addProperties(new Property("basePrice", this.basePrice));
            sd.addProperties(new Property("minBid", this.minBid));
            sd.addProperties(new Property("winningPrice", this.winningPrice));
            dfd.addServices(sd);

            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        addBehaviour(new notifyWinner(this, (this.duration + 5) * 1000));
        addBehaviour(new FIPARequestResp(this, MessageTemplate.MatchPerformative(ACLMessage.REQUEST)));
        addBehaviour(new FIPASubscribeResp(this, MessageTemplate.MatchPerformative(ACLMessage.SUBSCRIBE)));
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
            if(type.equals("sprice"))
                    winningPrice = secondBestBid;

            System.out.println("Auction:" + this.a.getAID() + " ended");
            System.out.println("Winner was " + currentWinnerId + " and the price: " + winningPrice);
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM_IF);
            msg.addReceiver(new AID(currentWinnerId, false));
            msg.setContent("You won " + this.getAgent().getName().split("@")[0] + "!");
            send(msg);

            informAll(currentWinnerId);

            auctionGUI.setVisible(false);

            this.a.doDelete();
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

            switch(type){
                case "english":

                    if (bidValue >= winningPrice + minBid) {
                        reply.setPerformative(ACLMessage.AGREE);
                        amountOfBids++;
                        winningPrice = bidValue;
                        currentWinnerId = request.getSender().getName().split("@")[0];
                    } else {
                        reply.setContent("" + winningPrice);
                        reply.setPerformative(ACLMessage.REFUSE);
                    }
                    break;
                case "fprice":

                    reply.setPerformative(ACLMessage.INFORM);
                    if(MadeOffer(request.getSender())){
                        reply.setContent("You have already bid on this item. First price bids only accept one bid per user");
                        break;
                    }
                    if (bidValue >= minBid) {

                        if(bidValue > winningPrice) {
                            winningPrice = bidValue;
                            currentWinnerId = request.getSender().getName().split("@")[0];
                        }
                        reply.setContent("Your offer was accepted");
                    }
                    else reply.setPerformative(ACLMessage.REFUSE); //in case the offer is less than whats the min
                    break;

                case "sprice":

                    reply.setPerformative(ACLMessage.INFORM);
                    if(MadeOffer(request.getSender())){
                        reply.setContent("You have already bid on this item. First price bids only accept one bid per user");
                        break;
                    }
                    if (bidValue >= minBid) {

                        if(bidValue > winningPrice) {
                            secondBestBid = winningPrice;
                            winningPrice = bidValue;
                            currentWinnerId = request.getSender().getName().split("@")[0];
                        }else if(bidValue > secondBestBid){
                            secondBestBid = bidValue;
                        }
                        reply.setContent("Your offer was accepted");
                    }
                    else reply.setPerformative(ACLMessage.REFUSE); //in case the offer is less than whats the min
                    break;

            }

            displayNewOffer(request.getSender().getName().split("@")[0],bidValue, new ACLMessage(reply.getPerformative()));

            return reply;
        }

        protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) {

            ACLMessage result = request.createReply();

            result.setPerformative(ACLMessage.INFORM);
            result.setContent(winningPrice + " " + currentWinnerId);

            informAll(request.getSender().getName());

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
            return reply;
        }

        protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) {

            ACLMessage result = request.createReply();

            result.setPerformative(ACLMessage.INFORM);
            result.setContent(winningPrice + " " + currentWinnerId);

            return result;
        }

    }

    public void informAll(String aidName) {//argument is person to be excluded of inform all

        for (AID participant : this.participants) {
            System.out.println("-------participant--------" + participant.getName());
            if ((!participant.getName().equals(aidName)) && !participant.getName().split("@")[0].equals((aidName))) {
                ACLMessage message = new ACLMessage(ACLMessage.INFORM_IF);
                message.addReceiver(participant);
                message.setContent(winningPrice + " " + currentWinnerId);
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
        if (!found)
            this.participants.add(aid);
    }

    public boolean MadeOffer(AID aid) {//add participant to subscribers list
        boolean found = false;
        for (AID participant : this.madeOffer) {
            if (participant.getName().equals(aid.getName())) {
                found = true;
                break;
            }
        }

        if (!found){
            this.madeOffer.add(aid);
        }
        return found;
    }

    public void displayInformationGUI(){
        auctionGUI.addText("                                                    Auction " + this.id + "\n\n\n\n" +
                "       Type            ->    " + this.type + "\n" +
                "       Base Price   ->    " + this.basePrice +"\n" +
                "       Min Bid         ->    " + this.minBid + "\n\n" );

    }

    public void displayNewOffer(String bidder , double offer , ACLMessage response){
        switch (this.type) {
            case "sprice":
                auctionGUI.addText("                                         New Offer from" + bidder + "\n\n" +
                        "       Offer By Bidder        ->    " + offer + "\n" +
                        "       Winner Price            ->    " + this.winningPrice + "\n" +
                        "       Second Best Offer   ->    " + this.secondBestBid + "\n"+
                        "       Response                ->    " + response.toString() + "\n\n" );
                break;
            default:
                auctionGUI.addText("                                         New Offer from" + bidder + "\n\n" +
                        "       Offer By Bidder     ->    " + offer + "\n" +
                        "       Winner Price         ->    " + this.winningPrice + "\n"+
                        "       Response            ->    " + response.toString() + "\n\n" );
                break;
        }
    }
}
