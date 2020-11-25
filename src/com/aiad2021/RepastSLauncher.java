package com.aiad2021;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.ContainerController;
import sajas.sim.repast3.Repast3Launcher;

public class RepastSLauncher extends Repast3Launcher {

    @Override
    protected void launchJADE() {

        //This will start JADE Gui
        Runtime rt = Runtime.instance();
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.GUI, "true");

        ContainerController cc = rt.createMainContainer(profile);
        //Load agents and products from world file

        World w = new World(rt,profile,cc,"world.csv");
    }

    @Override
    public String[] getInitParam() {
        return new String[0];
    }

    @Override
    public String getName() {
        return "Auction Model";
    }
}
