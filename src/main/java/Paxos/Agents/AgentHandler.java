package Paxos.Agents;

import Paxos.Network.Message;
import Paxos.Network.NetworkManager;

public class AgentHandler extends Agent implements Runnable {
    private Aceptor a = new Aceptor();
    private Learner l = new Learner("/home/prosdothewolf/Desktop/test.txt");
    private Proposer p = new Proposer();
    private NetworkManager n = NetworkManager.getInstance();


    public AgentHandler(){
        n.subscribe(this);
    }

    public void makeProposer(int val){
        Message m = p.makePropose(val);
        n.enqueueMessage(m);
    }

    @Override
    public void run(){
        Message m;
        while(true) {
           try{
               Thread.sleep(1000);
           }
           catch(InterruptedException e){}

           if (n.isThereAnyMessage(this)) {
               m = n.dequeueMessage(this);
               dispac(m);
            }
        }
    }

    private void dispac(Message m){
        //TODO
        //switch ()
    }


/*
    public static void main(String[] args){
        AgentHandler a = new AgentHandler();
    }
*/


}
