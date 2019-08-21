package Paxos.PaxosWithFault;

import Paxos.Agents.AgentHandler;
import Paxos.Paxos;

import java.util.Random;

public class NamingFaultMachine {

    public static void main(String[] Args){
        Random rng = new Random();
        Paxos.init(true);
        Paxos.addProces(Math.abs(rng.nextLong()));
        Paxos.addProces(Math.abs(rng.nextLong()));
        Paxos.addProces(Math.abs(rng.nextLong()));
        Paxos.addProces(Math.abs(rng.nextLong()));
        Paxos.addProces(Math.abs(rng.nextLong()));
        Paxos.addProces(Math.abs(rng.nextLong()));
        Paxos.addProces(Math.abs(rng.nextLong()));
        Paxos.addProces(Math.abs(rng.nextLong()));
        Paxos.addProces(Math.abs(rng.nextLong()));

    }

}
