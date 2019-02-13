package Paxos.Agents;

import Paxos.Network.NetworkManager;
import Paxos.Network.Message;

import java.util.Random;


public class Proposer extends Agent {
    private int numOfProces;
    private NetworkManager n = NetworkManager.getInstance();


    private Integer getId(){
        Random r = new Random();
        int id = r.nextInt();
        return id + (int)Thread.currentThread().getId();
    }


    public void makePropose(Integer val){
        Message m = new Message(getId(),val);
        n.enqueueMaessage(m);
    }

    public static void main(String[] args){
        Proposer p = new Proposer();
        p.getId();
    }

}
