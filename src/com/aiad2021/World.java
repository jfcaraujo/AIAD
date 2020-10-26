package com.aiad2021;

import jade.core.Agent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import java.util.ArrayList;

public class World {

    //jade components
    private Runtime runtime;
    private Profile profile;
    private ContainerController mainContainer;
    private ArrayList<AgentController> agentControllers;

    //file strings
    private String worldFilename;
    private String productsFilename;

    public World(String worldFilename,String productsFilename ) throws StaleProxyException {

        //Jade components
        this.runtime = Runtime.instance();
        this.profile = new ProfileImpl();
        this.mainContainer = this.runtime.createMainContainer(this.profile);
        this.agentControllers = new ArrayList<>();

        //Files
        this.worldFilename = worldFilename;
        this.productsFilename = productsFilename;

        //Loading
        this.Load(); //Throws exception if fails
    }

    private void Load() throws StaleProxyException {

        //try to create one agent
        createUserAgent(0,"John Doe");
    }

    //create agents
    private void createUserAgent(int id, String Username) throws StaleProxyException {

        //params to be passed on the agent creation
        Object[] params = {id,Username};
        //Agent path on jade = com.aiad2021.Agents.User
        AgentController  ac = this.mainContainer.createNewAgent(Username,"com.aiad2021.Agents.User",params);
        ac.start();
        this.agentControllers.add(ac);

    }

}
