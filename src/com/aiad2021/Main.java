package com.aiad2021;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.ContainerController;
import uchicago.src.sim.engine.SimInit;

import java.util.Locale;

public class Main {

    public static void main(String[] args){
        Locale.setDefault(new Locale("en", "US"));
        SimInit init = new SimInit();
        init.loadModel(new RepastSLauncher(), null, false);

    }
}
