package com.aiad2021;

import com.aiad2021.Agents.Auction;
import com.aiad2021.Agents.User;
import jade.wrapper.StaleProxyException;
import sajas.wrapper.ContainerController;
import uchicago.src.sim.analysis.OpenSequenceGraph;

import java.util.ArrayList;
import java.util.Arrays;

public class Simulation {

    private ContainerController mainContainer;
    private OpenSequenceGraph plot;

    private ArrayList<Auction> auctionsList;
    private ArrayList<User> users;

    private String bid_type;
    private int auto_bid_nr;
    private int smart_bid_nr;
    private int manual_bid_nr;
    private String aggressiveness;
    private String delay;
    private int[] aggressivenessList;
    private double[] delayList;

    public Simulation(ContainerController mainContainer, OpenSequenceGraph plot , int auto_bid_nr, int smart_bid_nr, int manual_bid_nr , String bid_type , String aggressiveness, String delay){
        this.mainContainer = mainContainer;
        this.plot= plot;
        this.manual_bid_nr = manual_bid_nr;
        this.bid_type = bid_type;
        this.auto_bid_nr = auto_bid_nr;
        this.smart_bid_nr = smart_bid_nr;
        this.users = new ArrayList<User>();
        this.auctionsList = new ArrayList<Auction>();
        this.aggressiveness = aggressiveness;
        this.delay = delay;

        int[] aggressivenessArray = new int[this.getAuto_bid_nr()];
        Arrays.fill(aggressivenessArray, 2);//fill the array with the default values
        setAggressivenessList(aggressivenessArray);
        double[] delayArray = new double[this.getAuto_bid_nr()];
        Arrays.fill(delayArray, 0.2);//fill the array with the default values
        setDelayList(delayArray);

    }


    public boolean setup_agents(ArrayList<User> usersList, ArrayList<Auction> auctionsList){

        for(int i= 0; i<(this.manual_bid_nr + this.smart_bid_nr + this.auto_bid_nr); i++ ){
            User newUser = new User(i,"JohnDoe" + i,1000);
            addUser(newUser);
            usersList.add(newUser);
        }

        if(!auctionSetup(auctionsList))
            return false;

        System.out.println("Auction list size " + auctionsList.size());
        System.out.println("Real auction list size " + getAuctionsList().size());

        try {
            //creates all agents
            for (int i=0; i< usersList.size();i++) {
                this.mainContainer.acceptNewAgent("JohnDoe"+i,usersList.get(i)).start();
            }
            //creates bids
            for (int i=1; i<= auctionsList.size();i++) {
                System.out.println("Im hereee");
                this.mainContainer.acceptNewAgent("Auction:"+i,auctionsList.get(i-1)).start();            }

        } catch (StaleProxyException e) {
            e.printStackTrace();
        }

        return true;
    }

    public boolean auctionSetup(ArrayList<Auction> auctionsList){

        String[] auctions = this.getBid_type().split(" ");

        if(auctions.length==0){
            System.out.println("Error: Bid type needs to have at least one type of auction or else no auctions can be created");
            return false;
        }else {
            if (auctions.length>1 && !this.getBid_type().matches("(english|dutch)( (english|dutch))+")) {
                System.out.println("Error: When declaring more than one auction, the only types available are english and dutch");
                return false;
            }else if(auctions.length==1 && !this.getBid_type().matches("(english|dutch|fprice|sprice)")){
                System.out.println("Error: Invalid type of auction. The existing types are english, dutch, fprice and sprice");
                return false;
            }
        }

        //only working with english bids so far
        for (int i = 1; i <= auctions.length; i++) {
            Auction auction = new Auction(i,auctions[i-1],250,10,5,plot);

            addAuction(auction);
            auctionsList.add(auction);//for some reason this is the only way that it updates this var
        }

        return true;

    }

    public void start(){

        //WARNING - DOES NOT TOLERATE SPACES AFTER THE FINAL NUMBER
        if(getAggressiveness().matches("(\\d+( \\d+)*)?") && getDelay().matches("(\\d+(\\.\\d+)?( \\d+(\\.\\d+)?)*)?")){
            System.out.println(":) I was accepted");
        }else System.out.println(":( I was not accepted");

        dataSetUp();

        bidsGenerator();
    }

    public void dataSetUp(){

        if(!getAggressiveness().equals("")){
            int[] aggressiveness = Arrays.stream(getAggressiveness().split(" ")).mapToInt(Integer::parseInt).toArray();
            if(aggressiveness.length>this.auto_bid_nr ){
                System.out.println("Error: aggressiveness arguments need to be less than the number of agents");
                return;
            }
            System.arraycopy(aggressiveness, 0, getAggressivenessList(), 0, aggressiveness.length);
        }

        if(!getDelay().equals("")) {
            double[] delay = Arrays.stream(getDelay().split(" ")).mapToDouble(Double::parseDouble).toArray();
            if(delay.length>this.auto_bid_nr){
                System.out.println("Error: delay arguments need to be less than the number of agents");
                return;
            }
            System.arraycopy(delay, 0, getDelayList(), 0, delay.length);
        }
    }

    public void bidsGenerator(){

        ArrayList<User> users_list = this.getUsers();
        int i= 0;
        //manual - can bid on every single type of auction
        for (;i< this.getManual_bid_nr() ; i++) {
            for (int j = 1; j <= getAuctionsList().size(); j++) {
                users_list.get(i).handleMessage("bid "+ j + " 10");
            }
        }

        //auto and smart - can only bid in dutch and english auctions
        if(!getBid_type().equals("fprice")&&!getBid_type().equals("sprice")) {

            //auto
            int[] aggressiveness = getAggressivenessList();
            double[] delay = getDelayList();

            for (; i < (this.getManual_bid_nr() + this.getAuto_bid_nr()); i++) {
                System.out.println("Auto nr " + (i - this.getManual_bid_nr()));
                users_list.get(i).handleMessage("autobid 1 " + aggressiveness[i - this.getManual_bid_nr()] + " 200 " + delay[i - this.getManual_bid_nr()]);
            }

            //smart
            for (; i < (this.getManual_bid_nr() + this.getAuto_bid_nr() + this.getSmart_bid_nr()); i++) {
                users_list.get(i).handleMessage("smartbid 1 300");
            }
        } else System.out.println("Warning: fprice or sprice was declared, cant bid with auto and smart agents with those bid types");
    }


    //setters and getters
    public void addUser(User user){
        this.users.add(user);
    }

    public void addAuction(Auction auction){
        this.auctionsList.add(auction);
    }

    public ArrayList<User> getUsers() {
        return users;
    }

    public int getAuto_bid_nr() {
        return auto_bid_nr;
    }

    public int getManual_bid_nr() {
        return manual_bid_nr;
    }

    public int getSmart_bid_nr() {
        return smart_bid_nr;
    }

    public ArrayList<Auction> getAuctionsList() {
        return auctionsList;
    }

    public int[] getAggressivenessList() {
        return aggressivenessList;
    }

    public double[] getDelayList() {
        return delayList;
    }

    public String getAggressiveness() {
        return aggressiveness;
    }

    public String getDelay() {
        return delay;
    }

    public String getBid_type() {
        return bid_type;
    }

    public void setAggressivenessList(int[] aggressivenessList) {
        this.aggressivenessList = aggressivenessList;
    }

    public void setDelayList(double[] delayList) {
        this.delayList = delayList;
    }

    public void setDelay(String delay) {
        this.delay = delay;
    }

    public void setAggressiveness(String aggressiveness) {
        this.aggressiveness = aggressiveness;
    }
}
