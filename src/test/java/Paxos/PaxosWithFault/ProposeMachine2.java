package Paxos.PaxosWithFault;

import Paxos.Agents.AgentHandler;
import Paxos.Paxos;

import java.util.Random;
import java.util.Scanner;

public class ProposeMachine2 {
    public static void main(String[] Args){
        Random rng = new Random();
        Scanner s = new Scanner(System.in);

        Paxos.init(false);
        AgentHandler a1 = Paxos.addProces(Math.abs(rng.nextLong()));
        AgentHandler a2 = Paxos.addProces(Math.abs(rng.nextLong()));
        AgentHandler a3 = Paxos.addProces(Math.abs(rng.nextLong()));

        s.nextLine();

        Paxos.propose((long)12,"Polimi",a1);
        Paxos.propose((long)80,"Siamo Belli",a2);
    }
}
