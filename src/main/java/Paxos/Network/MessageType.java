package Paxos.Network;

public enum MessageType{
    SUBSCRIBE("SUBSCRIBE"),
    DISCOVER("DISCOVER"),
    DISCOVERRESPONSE("DISCOVERRESPONSE"),
    PREPAREREQUEST("PREPAREREQUEST"),
    RESPONDTOPREPAREREQUEST("RESPONDTOPREPAREREQUEST"),
    ACCEPTREQUEST("ACCEPTREQUEST"),
    DECISION("DECISION");

    
    private final String messageType;

    private MessageType(String type){
	this.messageType = type;
    }

    public String toString(){
	return this.messageType;
    }
}
