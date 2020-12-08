package com.aiad2021;

import com.aiad2021.Agents.Auction;
import com.aiad2021.Agents.User;
import jade.wrapper.StaleProxyException;
import sajas.wrapper.ContainerController;
import uchicago.src.sim.analysis.OpenSequenceGraph;

import java.util.ArrayList;

public class Simulation {

    private ContainerController mainContainer;
    private OpenSequenceGraph plot;

    private ArrayList<Auction> auctionsList;
    private ArrayList<User> users;

    private String bid_type;
    private int auto_bid_nr;
    private int smart_bid_nr;
    private int manual_bid_nr;
    private double[] smartness;

    public Simulation(ContainerController mainContainer, OpenSequenceGraph plot , int auto_bid_nr, int smart_bid_nr, int manual_bid_nr , String bid_type , double[] smartness){
        this.mainContainer = mainContainer;
        this.plot= plot;
        this.manual_bid_nr = manual_bid_nr;
        this.bid_type = bid_type;
        this.auto_bid_nr = auto_bid_nr;
        this.smartness = smartness;
        this.smart_bid_nr = smart_bid_nr;
        this.users = new ArrayList<User>();
    }

    //autobid 2 agents
    public void setup_agents(ArrayList<User> usersList, ArrayList<Auction> auctionsList){
        System.out.println("Bid type " + this.bid_type);
        System.out.println("auto bid nr " + this.auto_bid_nr);
        System.out.println("manual bid nr " + this.manual_bid_nr);
        System.out.println("smart bid nr " + this.smart_bid_nr);

        for(int i= 0; i<(this.manual_bid_nr + this.smart_bid_nr + this.auto_bid_nr); i++ ){
            User newUser = new User(i,"JohnDoe" + i,1000);
            addUser(newUser);
            usersList.add(newUser);
        }
        //only working with english bids so far
        Auction auction1 = new Auction(1,this.bid_type,250,10,5,plot);

        auctionsList.add(auction1);

        try {
            //creates all agents
            for (int i=0; i< usersList.size();i++) {
                this.mainContainer.acceptNewAgent("JohnDoe"+i,usersList.get(i)).start();
                System.out.println("here");
            }
            this.mainContainer.acceptNewAgent("Auction:1",auction1).start();
            System.out.println("here3");

        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }

    public void start(){
        ArrayList<User> users_list = this.getUsers();
        System.out.println(this.getUsers());
        for (int i= 0 ; i< users_list.size() ; i++) {
            users_list.get(i).handleMessage("bid 1 100");
        }
    }

    public void addUser(User user){
        this.users.add(user);
    }

    public ArrayList<User> getUsers() {
        return users;
    }
}
