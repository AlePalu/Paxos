package Paxos.Network;

public enum MessageType{
    UNICAST("UNICAST"),
    BROADCAST("BROADCAST"),
    SUBSCRIBE("SUBSCRIBE");

    private final String messageType;

    private MessageType(String type){
	this.messageType = type;
    }

    public String toString(){
	return this.messageType;
    }
}
