package com.aiad2021;

import com.aiad2021.Agents.Auction;
import com.aiad2021.Agents.User;
import jade.core.Profile;
import jade.core.ProfileImpl;
import sajas.core.Runtime;
import sajas.wrapper.ContainerController;
import sajas.sim.repast3.Repast3Launcher;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.engine.Schedule;

import java.awt.*;
import java.util.ArrayList;

public class RepastSLauncher extends Repast3Launcher {

    private ContainerController mainContainer;
    private Runtime rt;
    private Profile profile;

    private ArrayList<Auction> auctionsList;
    private ArrayList<User> usersList;

    private OpenSequenceGraph plot;

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

    public int getWinningBid(){
        return (int) auctionsList.get(0).getWinningPrice();
    }

    @Override
    public void begin() {
        super.begin();  // crucial!
        //World w = new World(rt,profile,mainContainer,"world.csv");
        // display surfaces, spaces, displays, plots, ...
        // ...


        // build model
        usersList= new ArrayList<>();
        auctionsList = new ArrayList<>();

        if (plot != null) plot.dispose();
        plot = new OpenSequenceGraph("Population", this);
        plot.setAxisTitles("World cycles", "# of people");

        plot.addSequence("Green - CC", this::getWinningBid, Color.GREEN, 10);
        plot.display();

        //build schedule
        getSchedule().scheduleActionAt(100, plot, "step", Schedule.LAST);
        getSchedule().execute();

        //build simulation ambient
        Simulation sim = new Simulation(mainContainer,plot);
        sim.sim1(this.usersList,this.auctionsList);

        for(User u: this.usersList){
            System.out.println("ola "+u.getName());
        }

    }
}
