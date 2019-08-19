package Paxos.Agents;

import Paxos.Network.Message;
import Paxos.Network.MessageForgery;

public class Aceptor {

    private Long bound;
    private PaxosData data;

    public Aceptor(PaxosData data){
        this.bound = (long)0;
        this.data = data;

    }

    String processPrepareRequest(Message m){
        System.out.println("[Acceptor "+data.getId() + " ]: receive propose with ID: "+ m.getProposeID());
        if(m.getProposeID() >= bound && data.getCurrentValue() == null) {
            System.out.println("[Acceptor "+data.getId() + " ]: make a promise for ID: "+ m.getProposeID());
            bound = m.getProposeID();
            return MessageForgery.forgeRESPONDTOPREPAREREQUEST(m.getSenderID(),m.getProposeID());
        }
        return null;
    }

    String processAcceptRequest(Message m) {
        System.out.println("[Acceptor "+data.getId() + " ]: receive Value: "+m.getValue()+ " with ID: "+ m.getProposeID());
        if (m.getProposeID() < bound) {
            System.out.println("[Acceptor "+data.getId() + " ]: i can't accept");
            return null;
        }
        return MessageForgery.forgeDECISION(m.getProposeID(),m.getValue());
    }


}

