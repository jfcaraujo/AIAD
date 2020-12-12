package com.aiad2021;

import com.aiad2021.Agents.Auction;
import com.aiad2021.Agents.User;
import jade.core.Profile;
import jade.core.ProfileImpl;
import sajas.core.Runtime;
import sajas.wrapper.ContainerController;
import sajas.sim.repast3.Repast3Launcher;
import uchicago.src.sim.analysis.DataRecorder;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.space.Object2DGrid;
import uchicago.src.sim.space.Object2DTorus;

import java.awt.*;
import java.util.ArrayList;

public class RepastSLauncher extends Repast3Launcher {

    private ContainerController mainContainer;
    private Runtime rt;
    private Profile profile;

    private ArrayList<Auction> auctionsList;
    private ArrayList<User> usersList;

    private DisplaySurface dsurf;
    private Object2DTorus space;

    private OpenSequenceGraph plot;

    DataRecorder dr;

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

    public int getWinningBid1(){
        return (int) auctionsList.get(0).getWinningPrice();
    }
    public int getWinningBid2(){
        return (int) auctionsList.get(1).getWinningPrice();
    }

    @Override
    public void begin() {
        super.begin();  // crucial!

        dr = new DataRecorder("data.csv",this);

        buildModel();
        buildDisplay();
        buildSchedule();

        //build simulation ambient
        Simulation sim = new Simulation(mainContainer,plot,space,dr);
        sim.sim2(this.usersList,this.auctionsList);
        int i=1;

        for(User u: this.usersList){
            dr.createNumericDataSource("User "+i,u,"getBid" );
            dr.createNumericDataSource("User "+i,u,"getAuction" );
            i++;
        }
        space.putObjectAt(0,0,this.auctionsList.get(0));
        space.putObjectAt(0,0,this.auctionsList.get(1));



    }

    private void buildModel(){
        // build model
        usersList= new ArrayList<>();
        auctionsList = new ArrayList<>();

        if (plot != null) plot.dispose();
        plot = new OpenSequenceGraph("Auction", this);
        plot.setAxisTitles("Time", "Bid Value");

        plot.addSequence("English", this::getWinningBid1, Color.GREEN, 10);
        plot.addSequence("Duth", this::getWinningBid2, Color.BLUE, 10);
        plot.display();

        space = new Object2DTorus(500,1000);

        //space.putObjectAt(0,0,this.auctionsList.get(0));
        //space.putObjectAt(0,0,this.auctionsList.get(1));

    }

    private void buildSchedule(){
        getSchedule().scheduleActionAtInterval(1, dsurf, "updateDisplay", Schedule.LAST);
        getSchedule().scheduleActionAt(0.1, plot, "step", Schedule.LAST);
        getSchedule().scheduleActionAtEnd(plot,"writeToFile");
        getSchedule().scheduleActionAt(1,dr,"record");
        getSchedule().scheduleActionAtEnd(dr,"writeToFile");
        //getSchedule().execute();

    }

    private void buildDisplay(){

        if (dsurf != null) dsurf.dispose();
        this.dsurf = new DisplaySurface(this,"Auction Simulation");
        registerDisplaySurface("Auction Simulation",dsurf);

        // space and display surface
        Object2DDisplay display = new Object2DDisplay(space);
        display.setObjectList(auctionsList);
        dsurf.addDisplayableProbeable(display, "Agents Space");
        addSimEventListener(dsurf);
        dsurf.display();


    }
}
