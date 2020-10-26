package com.aiad2021.Agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class User extends Agent {

    //attributes
    private int id;
    private String Username;

    @Override
    protected void setup() {

       //used to get parameters passes on intilialization
        Object[] args = this.getArguments();

        //setup params
        this.id = (int) args[0];
        this.Username = (String) args[1];

        System.out.println("My local name is " + getAID().getLocalName());
        System.out.println("My GUID is " + getAID().getName());

        System.out.println( "Id: " + this.id + " Username: "+this.Username);

        System.out.println("My addresses are " + String.join(",", getAID().getAddressesArray()));

    }

}
