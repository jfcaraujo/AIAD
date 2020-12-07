package com.aiad2021;

import com.aiad2021.Agents.Auction;
import jade.core.Profile;
import sajas.core.Runtime;
import sajas.wrapper.AgentController;
import sajas.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import java.io.File;
import java.io.FileNotFoundException;
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

    public World(){}

    public World(Runtime rt, Profile profile, ContainerController cc, String worldFilename) {

        //Jade components
        this.runtime = rt;
        this.profile = profile;
        this.userAgentControllers = new ArrayList<>();
        this.auctionAgentControllers = new ArrayList<>();
        //This will create the main controller
        this.mainContainer = cc;
        //Files
        this.worldFilename = worldFilename;

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
    private void createAuctionAgent(int id, String type, int duration, double basePrice, double minBid) throws StaleProxyException {
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
