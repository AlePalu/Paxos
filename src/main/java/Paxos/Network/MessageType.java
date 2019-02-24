package Paxos.Network;

public enum MessageType{
    SUBSCRIBE("SUBSCRIBE"),
    DISCOVER("DISCOVER"),
    DISCOVERRESPONSE("DISCOVERRESPONSE"),
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
