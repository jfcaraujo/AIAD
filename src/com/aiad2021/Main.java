package com.aiad2021;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.ContainerController;
import jade.core.Runtime;

import java.util.Scanner;

public class Main {

    public static void main(String[] args){

        //This will start JADE Gui
        Runtime rt = Runtime.instance();
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.GUI, "true");

        ContainerController cc = rt.createMainContainer(profile);
        //Load agents and products from world file

        World w = new World(rt,profile,cc,"world.csv","products.csv");

    }
}
