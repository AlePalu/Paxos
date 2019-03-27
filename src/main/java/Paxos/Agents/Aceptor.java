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
       // System.out.println("io sono"+ data.getId()+ " il mio bound é"+bound);
        if(m.getSenderID()>=bound && data.getCurrentValue() == null) {
            System.out.println("io sono"+ data.getId()+ " nuovo bound"+m.getSenderID());
            bound = m.getSenderID();
            return new Message(m.getSenderID(), null, MessageType.RESPONDTOPREPAREREQUEST);
        }
        return null;
    }

    Message processAcceptRequest(Message m) {
        Message respond;
        System.out.println("io sono" + data.getId() + " bound" + bound + " proposdta " + m.getSenderID());
        if (m.getSenderID() < bound) {
            System.out.println("io sono" + data.getId() + " e non rispondo a "+ m.getValue());
            return null;
        }
        System.out.println("io sono" + data.getId() + " e rispondo a "+ m.getValue());
        respond = new Message(null, m.getValue(), MessageType.DECISION);
        respond.setAsBroadcast();
        return respond;
    }


}

