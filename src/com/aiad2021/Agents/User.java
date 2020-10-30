package com.aiad2021.Agents;

import com.aiad2021.World;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
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

        addBehaviour(new UserListeningBehaviour());
    }


    //todo see if its worth having a folder for all the behaviours
    //usar request para criar novos leiloes, pedir listagem de leiloes e dar join
    //usar inform para msg de accept e ganhar ou perder
    class UserListeningBehaviour extends CyclicBehaviour {

        private  World world = new World();

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

}
