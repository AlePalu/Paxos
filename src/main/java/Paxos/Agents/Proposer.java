package Paxos.Agents;

import Paxos.Network.MessageForgery;


public class Proposer {

    private int currentNumOfVoter;
    private PaxosData data;
    private String proposedValue;
    private long proposeID;

    Proposer(PaxosData data){
        this.data = data;
        this.currentNumOfVoter =0;
        this.proposedValue = null;
    }

    String propose(String val, Long proposeID) {
        this.proposeID = proposeID;
        System.out.println("[Proposer "+data.getId() + " ]: make a propose with value: "+val+" and ID: "+ proposeID);
        this.proposedValue = val;
        this.currentNumOfVoter = 0;
       
        return MessageForgery.forgePREPAREREQUEST(proposeID, data.getRound());
    }

    String processRespondToPrepareRequest() {
        System.out.println("[Proposer "+data.getId() + " ]: receive a vote for: "+ this.proposeID);
        currentNumOfVoter++;
        if (currentNumOfVoter > data.getNumOfProces()/2 && !data.getProposewin()) {
            System.out.println("[Proposer]: my propose win: "+ this.proposedValue);
            //data.setwin();
            data.setProposewin();
            return MessageForgery.forgeACCEPTREQUEST(this.proposeID,this.proposedValue,data.getRound());
        }
        else
            return null;
    }
}