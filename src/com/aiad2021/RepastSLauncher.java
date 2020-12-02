package com.aiad2021;

import jade.core.Profile;
import jade.core.ProfileImpl;
import sajas.core.Runtime;
import sajas.wrapper.ContainerController;
import sajas.sim.repast3.Repast3Launcher;

public class RepastSLauncher extends Repast3Launcher {
    private ContainerController mainContainer;
    private Runtime rt;
    private Profile profile;
    @Override
    protected void launchJADE() {

        //This will start JADE Gui
        rt = Runtime.instance();
        profile = new ProfileImpl();
        profile.setParameter(Profile.GUI, "true");

        mainContainer = rt.createMainContainer(profile);
        
    }

    @Override
    public String[] getInitParam() {
        return new String[0];
    }

    @Override
    public String getName() {
        return "Auction Model";
    }

    @Override
    public void setup() {
        super.setup();  // crucial!

        // property descriptors
        // ...
    }

    @Override
    public void begin() {
        super.begin();  // crucial!
        World w = new World(rt,profile,mainContainer,"world.csv");
        // display surfaces, spaces, displays, plots, ...
        // ...
    }
}
