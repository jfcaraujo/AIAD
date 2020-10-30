package com.aiad2021.Agents;

import com.aiad2021.World;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.proto.SubscriptionInitiator;
import jade.util.leap.Iterator;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

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
        System.out.println("My addresses are " + String.join(",", getAID().getAddressesArray()));
        System.out.println( "Id: " + this.id + " Username: "+this.Username+"\n");

        DFSubscribe();
        addBehaviour(new UserListeningBehaviour());

    }


    //todo see if its worth having a folder for all the behaviours
    //usar request para criar novos leiloes, pedir listagem de leiloes e dar join
    //usar inform para msg de accept e ganhar ou perder
    class UserListeningBehaviour extends CyclicBehaviour {

        public void action(){
            ACLMessage msg = receive();
            if(msg != null) {
                if(msg.getPerformative() == ACLMessage.REQUEST){
                    System.out.println(msg);

                    String[] parts = msg.getContent().split(" ");
                    //create new auction
                    try {
                        Object[] params = {Integer.parseInt(parts[0]),Double.parseDouble(parts[1])};
                        //Agent path on jade = com.aiad2021.Agents.
                        //todo create the auction in the main cotroller
                        AgentController ac = getContainerController().createNewAgent(String.valueOf(params[0]),"com.aiad2021.Agents.Auction",params);
                        ac.start();
                       // this.agentControllers.add(ac); //todo idk what is this used for
                    } catch (StaleProxyException e) {
                        e.printStackTrace();
                    }

                }

                //ACLMessage reply = msg.createReply();

                //send(reply);
            } else {
                block();
            }
        }
    }

    private void DFSubscribe(){
        // Build the description used as template for the subscription
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription templateSd = new ServiceDescription();
        templateSd.setType("auction-listing");
        //templateSd.addProperties(new Property("country", "Italy")); //todover quando se colocar os tipos de leiloes
        template.addServices(templateSd);

        SearchConstraints sc = new SearchConstraints();
        // We want to receive 10 results at most
        sc.setMaxResults(new Long(10));

        addBehaviour(new SubscriptionInitiator(this, DFService.createSubscriptionMessage(this, getDefaultDF(), template, sc)) {
            protected void handleInform(ACLMessage inform) {
                System.out.println("Agent "+getLocalName()+": Notification received from DF");
                try {
                    DFAgentDescription[] results = DFService.decodeNotification(inform.getContent());
                    if (results.length > 0) {
                        for (int i = 0; i < results.length; ++i) {
                            DFAgentDescription dfd = results[i];
                            AID provider = dfd.getName();
                            // The same agent may provide several services; we are only interested
                            // in the auction-listing one
                            Iterator it = dfd.getAllServices();
                            while (it.hasNext()) {
                                ServiceDescription sd = (ServiceDescription) it.next();
                                if (sd.getType().equals("auction-listing")) {
                                    System.out.println("auction-listing service found:");
                                    System.out.println("- Service \""+sd.getName()+"\" provided by agent "+provider.getName());
                                }
                            }
                        }
                    }
                    System.out.println();
                }
                catch (FIPAException fe) {
                    fe.printStackTrace();
                }
            }
        } );
    }

    private void DFSearch(){ //todo nao testei
        // Search for services of type "weather-forecast"
        System.out.println("Agent "+getLocalName()+" searching for services of type \"auction-listing\"");
        try {
            // Build the description used as template for the search
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription templateSd = new ServiceDescription();
            templateSd.setType("auction-listing");
            template.addServices(templateSd);

            SearchConstraints sc = new SearchConstraints();
            // We want to receive 10 results at most
            sc.setMaxResults(new Long(10));

            DFAgentDescription[] results = DFService.search(this, template, sc);
            if (results.length > 0) {
                System.out.println("Agent "+getLocalName()+" found the following auction-listing services:");
                for (int i = 0; i < results.length; ++i) {
                    DFAgentDescription dfd = results[i];
                    AID provider = dfd.getName();
                    // The same agent may provide several services; we are only interested
                    // in the weather-forcast one
                    Iterator it = dfd.getAllServices();
                    while (it.hasNext()) {
                        ServiceDescription sd = (ServiceDescription) it.next();
                        if (sd.getType().equals("auction-listing")) {
                            System.out.println("- Service \""+sd.getName()+"\" provided by agent "+provider.getName());
                        }
                    }
                }
            }
            else {
                System.out.println("Agent "+getLocalName()+" did not find any weather-forecast service");
            }
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

}
