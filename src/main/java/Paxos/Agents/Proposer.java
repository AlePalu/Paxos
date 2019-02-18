package Paxos.Agents;

import Paxos.Network.MessageType;
import Paxos.Network.NetworkManager;
import Paxos.Network.Message;

import java.awt.*;
import java.util.Random;


public class Proposer extends Agent {
    private int numOfProces;


    private Integer getId(){
        Random r = new Random();
        int id = r.nextInt();
        return id + (int)Thread.currentThread().getId();
    }


    Message makePropose(Integer val){
        return new Message(getId(),val, MessageType.BROADCAST);
    }



}
