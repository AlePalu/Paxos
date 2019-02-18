package Paxos.Network;

import java.net.InetAddress;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

class Message{
    private Integer ID;
    private Integer value;
    private MessageType messageType;
    private Pair<InetAddress, Integer> sender;

    
    public Message(Integer ID, Integer value, MessageType type){
	this.ID = ID;
	this.value = value;
	this.messageType = type;
    }

    // given a JSON string, builds the message
    public Message(String message, Pair<InetAddress, Integer> sender){
	JsonObject jsonMessage = Json.parse(message).asObject();
	this.ID = jsonMessage.getInt("ID", 0);
	this.value = jsonMessage.getInt("value", 0);

	// keeping track of the sender... needed for reply
	this.sender = sender;
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

    public Pair<InetAddress, Integer> getSenderID(){
	return this.sender;
    }
    
    public String getJSON(){
	JsonObject jsonMessageFormat = new JsonObject();
	jsonMessageFormat.add("ID", this.ID);
	jsonMessageFormat.add("value", this.value);
	
	return jsonMessageFormat.asString();
    }
    
}
