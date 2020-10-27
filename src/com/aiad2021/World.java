package com.aiad2021;

import jade.core.Agent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
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
    private ArrayList<AgentController> agentControllers;

    //file strings
    private String worldFilename;
    private String productsFilename;

    public World(){

    }

    public World(Profile profile,ContainerController cc, String worldFilename,String productsFilename ) {

        //Jade components
        this.runtime = Runtime.instance();
        //this.profile = new ProfileImpl(); //old
        this.profile = profile;
        //this.mainContainer = this.runtime.createMainContainer(this.profile); //old
        this.mainContainer = cc;
        this.agentControllers = new ArrayList<>();

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
    public void createUserAgent(int id, String Username) throws StaleProxyException {

        //params to be passed on the agent creation
        Object[] params = {id,Username};
        //Agent path on jade = com.aiad2021.Agents.User
        AgentController  ac = this.mainContainer.createNewAgent(id + Username,"com.aiad2021.Agents.User",params);
        ac.start();
        this.agentControllers.add(ac);

    }

    //create agents
    public void createAuctionAgent(int id, double basePrice) throws StaleProxyException { //todo add args

        //params to be passed on the agent creation
        Object[] params = {id,basePrice};
        //Agent path on jade = com.aiad2021.Agents.
        AgentController ac = this.mainContainer.createNewAgent(String.valueOf(id+basePrice),"com.aiad2021.Agents.Auction",params);
        ac.start();
        this.agentControllers.add(ac);

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
                    createUserAgent(Integer.parseInt(parts[1]),parts[2]);
                    break;

                case "AUCTION":
                    createAuctionAgent(Integer.parseInt(parts[1]), Double.parseDouble(parts[2]));
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

    private void processProductsFile(){
        //todo
    }

}
