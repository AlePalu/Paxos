package Paxos.Agents;

import Paxos.Network.MessageForgery;


public class Proposer {

    private int currentNumOfVoter;
    private boolean win;
    private PaxosData data;
    private String proposedValue;

    Proposer(PaxosData data){
        this.data = data;
        this.currentNumOfVoter =0;
        this.win = false;
        this.proposedValue = null;
    }

    String propose(String val, Long proposeID) {
        //System.out.println("[Proposer "+data.getId() + " ]: make a propose with value: "+val+"and ID: "+ proposeID);
        this.proposedValue = val;
        this.currentNumOfVoter = 0;
        return MessageForgery.forgePREPAREREQUEST(proposeID);
    }

    String processRespondToPrepareRequest() {
        //System.out.println("[Proposer "+data.getId() + " ]: receive a vote for: "+ this.proposedValue);
        currentNumOfVoter++;
        if (currentNumOfVoter > data.getNumOfProces()/2 && !this.win) {
            //System.out.println("[Proposer]: my propose win: "+ this.proposedValue);
            //data.setwin();
            win = true;
            return MessageForgery.forgeACCEPTREQUEST(data.getId(),this.proposedValue);
        }
        else
            return null;
    }
}