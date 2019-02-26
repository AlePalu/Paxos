package Paxos.Agents;

import Paxos.Network.Message;
import Paxos.Network.MessageType;

public class Aceptor {

    private Long bound = null;
    private String currentValue = null;
    Long id;

    Message processPrepareRequest(Message m){
        if(currentValue == null){
            bound = m.getSenderID();
            currentValue = m.getValue();
        }

        if(m.getSenderID()>=bound && currentValue != null) {
          // System.out.println(id+" vota " + m.getValue());
            bound = m.getSenderID();
           // System.out.println(id + " ha bound: "+bound);
            return new Message(m.getSenderID(), m.getValue(), MessageType.RESPONDTOPREPAREREQUEST);
        }
      //  System.out.println("voto no");
        return null;
    }

    void reset(){
        bound = null;
        currentValue = null;
    }
}

