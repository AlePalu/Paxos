package Paxos.Agents;

import Paxos.Network.Message;
import Paxos.Network.MessageForgery;

public class Aceptor {

    private PaxosData data;

    public Aceptor(PaxosData data){
        this.data = data;

    }

    String processPrepareRequest(Message m){
        System.out.println("[Acceptor "+data.getId() + " ]: receive propose with ID: "+ m.getProposeID());
        if(m.getProposeID() >= data.getAcceptorBound() && data.getCurrentValue() == null) {
            System.out.println("[Acceptor "+data.getId() + " ]: make a promise for ID: "+ m.getProposeID());
            data.setAcceptorBound(m.getProposeID());
            return MessageForgery.forgeRESPONDTOPREPAREREQUEST(m.getSenderID(),m.getProposeID(),m.getRound());
        }
        return null;
    }

    String processAcceptRequest(Message m) {
        System.out.println("[Acceptor "+data.getId() + " ]: receive Value: "+m.getValue()+ " with ID: "+ m.getProposeID());
        if (m.getProposeID() < data.getAcceptorBound()) {
            System.out.println("[Acceptor "+data.getId() + " ]: i can't accept");
            return null;
        }
        return MessageForgery.forgeDECISION(m.getProposeID(),m.getValue(),m.getRound());
    }


}

