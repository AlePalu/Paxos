package Paxos.Agents;

import Paxos.Network.Message;
import Paxos.Network.MessageType;


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

    Message propose(String val) {
        Message m;
        this.proposedValue = val;
        m = new Message(null, null, MessageType.PREPAREREQUEST,data.getRound());
        m.setAsBroadcast();
        return m;
    }

    Message processRespondToPrepareRequest() {
        Message respond;
        currentNumOfVoter++;
        if (currentNumOfVoter > data.getNumOfProces()/2 && !data.isWin()) {
            data.setwin();
            respond = new Message(null, this.proposedValue, MessageType.ACCEPTREQUEST,data.getRound());
            respond.setAsBroadcast();
            return respond;
        }
        else
            return null;
    }
}