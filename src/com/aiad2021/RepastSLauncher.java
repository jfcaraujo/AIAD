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

    private String bid_type;
    private int manual_agent_number;
    private int smart_agent_number;
    private int auto_agent_number;
    private Simulation sim;

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
        return new String[]{
                "bid_type",
                "manual_agent_number",
                "smart_agent_number",
                "auto_agent_number",

        };
    }

    @Override
    public String getName() {
        return "Auction Model";
    }

    @Override
    public void setup() {
        super.setup();  // crucial!

        setBid_type("english");
        setManual_agent_number(1);
        setAuto_agent_number(0);
        setSmart_agent_number(1);

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


        // build
        // model
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
        sim = new Simulation(mainContainer,
                plot,
                this.auto_agent_number,
                this.smart_agent_number,
                this.manual_agent_number,
                this.bid_type
                );
        sim.setup_agents(this.usersList,this.auctionsList);
        buildSchedule();
        for(User u: this.usersList){
            System.out.println("ola "+u.getName());
        }

    }

    private void buildSchedule(){
        getSchedule().scheduleActionAt(30*(this.auto_agent_number + this.smart_agent_number + this.manual_agent_number), this, "startSimulation");
    }

    public void startSimulation(){
        sim.start();
    }

    //for parameters to work, there needs to be a setter and a getter for each parameter
    public void setBid_type(String bid_type) {
        this.bid_type = bid_type;
    }

    public void setAuto_agent_number(int auto_agent_number) {
        this.auto_agent_number = auto_agent_number;
    }

    public void setManual_agent_number(int manual_agent_number) {
        this.manual_agent_number = manual_agent_number;
    }

    public void setSmart_agent_number(int smart_agent_number) {
        this.smart_agent_number = smart_agent_number;
    }

    public int getAuto_agent_number() {
        return auto_agent_number;
    }

    public int getManual_agent_number() {
        return manual_agent_number;
    }

    public int getSmart_agent_number() {
        return smart_agent_number;
    }

    public String getBid_type() {
        return bid_type;
    }
}
