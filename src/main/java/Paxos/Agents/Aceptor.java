package Paxos.Agents;

import Paxos.Network.Message;
import Paxos.Network.MessageType;

public class Aceptor {

    private Long bound;
    private PaxosData data;

    public Aceptor(PaxosData data){
        this.bound = (long)0;
        this.data = data;

    }

    Message processPrepareRequest(Message m){
        if(m.getSenderID()>=bound && data.getCurrentValue() == null) {
            bound = m.getSenderID();
            return new Message(m.getSenderID(), null, MessageType.RESPONDTOPREPAREREQUEST);
        }
        return null;
    }

    Message processAcceptRequest(Message m) {
        Message respond;
        if (m.getSenderID() < bound) {
            return null;
        }
        respond = new Message(null, m.getValue(), MessageType.DECISION);
        respond.setAsBroadcast();
        return respond;
    }


}

