package Paxos.Agents;

import Paxos.Network.Message;
import Paxos.Network.MessageType;


public class Proposer extends Agent {
    private int numOfProcess;
    private int currentNumOfVoter = 0;
    private boolean win = false;

    void updateProcessCount(int n) {
        this.numOfProcess = n;
    }

    Message propose(String val) {
        Message m;
        m = new Message(null, val, MessageType.PREPAREREQUEST);
        m.setAsBroadcast();
        return m;
    }

    Message processRespondToPrepareRequest(Message m) {
        Message respons;
        currentNumOfVoter++;
       // System.out.println("voti= "+ currentNumOfVoter + "for " + m.getValue());
        if (currentNumOfVoter > numOfProcess/2 && !win) {
            win = true;
            respons = new Message(null, m.getValue(), MessageType.ACCEPTREQUEST);
            respons.setAsBroadcast();
            return  respons;
        }
        else
            return null;
    }

    void reset(){
        currentNumOfVoter = 0;
        win = false;
    }
}