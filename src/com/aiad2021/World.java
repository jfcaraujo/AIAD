package com.aiad2021;

import jade.core.Agent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

public class World {

    //jade components
    private Runtime runtime;
    private Profile profile;
    private ContainerController mainContainer;
    private ArrayList<AgentController> userAgentControllers;
    private ArrayList<AgentController> auctionAgentControllers;
    //file strings
    private String worldFilename;
    private String productsFilename;

    public World(){}

    public World(Runtime rt,Profile profile,ContainerController cc, String worldFilename,String productsFilename ) {

        //Jade components
        this.runtime = rt;
        this.profile = profile;
        this.userAgentControllers = new ArrayList<>();
        this.auctionAgentControllers = new ArrayList<>();
        //This will create the main controller
        this.mainContainer = cc;
        //Files
        this.worldFilename = worldFilename;
        this.productsFilename = productsFilename;

        //Loading
        this.Load(); //Throws exception if fails

         }

    private void Load(){

        processWorldFile();
        //try to create one agent
    }

    //create agents
    private void createUserAgent(int id, String Username, double money) throws StaleProxyException {

        //params to be passed on the agent creation
        Object[] params = {id,Username,money};
        //Agent path on jade = com.aiad2021.Agents.User
        AgentController  ac = this.mainContainer.createNewAgent(id + Username,"com.aiad2021.Agents.User",params);
        ac.start();
        this.userAgentControllers.add(ac);

    }

    //create agents
    private void createAuctionAgent(int id, String type, int duration, double basePrice, double minBid) throws StaleProxyException { //todo add args
        //params to be passed on the agent creation
        Object[] params = {id,type,duration,basePrice,minBid};
        //Agent path on jade = com.aiad2021.Agents.
        AgentController ac = this.mainContainer.createNewAgent("Auction:"+id,"com.aiad2021.Agents.Auction",params);
        ac.start();
        this.auctionAgentControllers.add(ac);

    }

    //read file
    private void processWorldFile() {

        try {
        File f = new File(this.worldFilename);
        Scanner reader = new Scanner(f);

        while (reader.hasNextLine()){
            String data = reader.nextLine();
            //process line

            String[] parts = data.split(",");

            switch(parts[0]){
                case "USER":
                    createUserAgent(Integer.parseInt(parts[1]),parts[2], Double.parseDouble(parts[3]));
                    break;
                case "AUCTION":
                    createAuctionAgent(Integer.parseInt(parts[1]), parts[2],Integer.parseInt(parts[3]),Double.parseDouble(parts[4]), Double.parseDouble(parts[5]));
                    break;

                default: System.exit(1);
            }
        }

        reader.close();
        } catch (FileNotFoundException | StaleProxyException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

}
