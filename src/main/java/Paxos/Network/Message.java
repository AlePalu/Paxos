package Paxos.Network;

import com.eclipsesource.json.*;

enum MessageType{
    UNICAST, BROADCAST
}

class Message{
    private Integer ID;
    private Integer value;
    private MessageType messageType;
    
    public Message(Integer ID, Integer value, MessageType type){
	this.ID = ID;
	this.value = value;
	this.messageType = type;
    }
    
    public Integer getID(){
	return this.ID;
    }

    public Integer getValue(){
	return this.value;
    }

    public MessageType getMessageType(){
	return this.messageType;
    }

    public String serializeMessage(){

	JsonValue jsonValue = new JsonValue();

	return;
    }
    
}
