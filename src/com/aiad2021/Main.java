package com.aiad2021;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.ContainerController;
import jade.core.Runtime;
import jade.wrapper.StaleProxyException;

public class Main {

    public static void main(String[] args){

        //This will start JADE Gui
        Runtime rt = Runtime.instance();
        Profile p = new ProfileImpl();
        p.setParameter(Profile.GUI, "true");

        //This will create the main controller
        ContainerController cc = rt.createMainContainer(p);

        //Load agents and products from world file

        World w = new World("world.csv","products.csv");


        //When program ends save its state
        //todo
    }
}
