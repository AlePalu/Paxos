package Paxos.Agents;

import Paxos.Network.MessageForgery;


public class Proposer {

    private PaxosData data;
    private String proposedValue;
    private long proposeID;

    Proposer(PaxosData data){
        this.data = data;
        this.proposedValue = null;
    }

    String propose(String val, Long proposeID) {
        this.proposeID = proposeID;
        System.out.println("[Proposer "+data.getId() + " ]: make a propose with value: "+val+" and ID: "+ proposeID);
        this.proposedValue = val;
        data.reserCurrentvote();
        return MessageForgery.forgePREPAREREQUEST(proposeID, data.getRound());
    }

    String processRespondToPrepareRequest() {
        System.out.println("[Proposer "+data.getId() + " ]: receive a vote for: "+ this.proposeID);
        data.incProposecuttentvote();
        if (data.getProposecurrentvote() > data.getNumOfProces()/2 && !data.getProposewin()) {
            System.out.println("[Proposer]: my propose win: "+ this.proposedValue);
            data.setProposewin();
            return MessageForgery.forgeACCEPTREQUEST(this.proposeID,this.proposedValue,data.getRound());
        }
        else
            return null;
    }
}