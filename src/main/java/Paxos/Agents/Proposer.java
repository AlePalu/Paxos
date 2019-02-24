package Paxos.Agents;

import Paxos.Network.AgentType;
import Paxos.Network.Message;

import java.util.Random;


public class Proposer extends Agent {
    private int numOfProces;


    private Integer getId(){
        Random r = new Random();
        int id = r.nextInt();
        return id + (int)Thread.currentThread().getId();
    }

    void updateProcessCount(int n){
        this.numOfProces = n;
    }

    Message propose(int val){
        Message m;
        m = new Message(null,val, AgentType.ACCEPTOR);
        m.setAsBroadcast();
        return m;
    }




}
