package Paxos.Network;

public enum MessageType{
    // network related messages
    SUBSCRIBE("SUBSCRIBE"),
    DISCOVER("DISCOVER"),
    DISCOVERRESPONSE("DISCOVERRESPONSE"),
    NAMINGREQUEST("NAMINGREQUEST"),
    NAMINGSUBSCRIBE("NAMINGSUBSCRIBE"),
    
    // paxos protocol related messages
    PAXOS("PAXOS"),
    PREPAREREQUEST("PREPAREREQUEST"),
    RESPONDTOPREPAREREQUEST("RESPONDTOPREPAREREQUEST"),
    ACCEPTREQUEST("ACCEPTREQUEST");

    
    private final String messageType;

    private MessageType(String type){
	this.messageType = type;
    }

    public String toString(){
	return this.messageType;
    }
}
