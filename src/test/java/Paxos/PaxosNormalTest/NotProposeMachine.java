package Paxos.PaxosNormalTest;

import Paxos.Agents.AgentHandler;
import Paxos.Paxos;

import java.util.Random;

public class NotProposeMachine {

        public static void main(String[] Args) {
            Random rng = new Random();
            Paxos.init(false);
            AgentHandler a1 = Paxos.addProces(Math.abs(rng.nextLong()));
            AgentHandler a2 = Paxos.addProces(Math.abs(rng.nextLong()));
            AgentHandler a3 = Paxos.addProces(Math.abs(rng.nextLong()));
        }
}
