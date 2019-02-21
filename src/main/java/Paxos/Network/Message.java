package Paxos.Network;

import java.net.InetAddress;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

enum ForwardDirection{
    IN("IN"),
    OUT("OUT");

    private final String direction;

    private ForwardDirection(String direction){
	this.direction = direction;
    }

    public String toString(){
	return this.direction;
    }
}

public class Message{
    // paxos related informations
    private Integer ID;
    private Integer value;

    // needed for internal operation
    private MessageType messageType; // specify if the message must be considered as unicast or broadcast one
    private Pair<InetAddress, Integer> recipient; // needed for reply
    private String agentType; // type of agent to which this message is directed
    private ForwardDirection forwardDirection;

    
    public Message(Integer ID, Integer value, MessageType type, AgentType agentType, Pair<InetAddress,Integer> sender){
	this.ID = ID;
	this.value = value;
	this.messageType = type;
	this.recipient = sender;
	this.agentType = agentType.toString();
    }

    // given a JSON string, builds the message
    public Message(String message, Pair<InetAddress, Integer> sender){
	JsonObject jsonMessage = Json.parse(message).asObject();
	this.ID = jsonMessage.getInt("ID", 0);
	this.value = jsonMessage.getInt("value", 0);
	this.agentType = jsonMessage.get("agentType").asString();
	
	// keeping track of the sender... needed for reply
	this.recipient = sender;
    }

    public Message(String message){
	JsonObject jsonMessage = Json.parse(message).asObject();
	this.ID = jsonMessage.get("ID").asInt();
	this.value = jsonMessage.get("value").asInt();
	this.agentType = jsonMessage.get("agentType").asString();
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

    public Pair<InetAddress, Integer> getRecipient(){
	return this.recipient;
    }

    public String getAgentType(){
	return this.agentType;
    }

    public void setForwardDirection(ForwardDirection direction){
	this.forwardDirection = direction;
    }

    public ForwardDirection getForwardDirection(){
	return this.forwardDirection;
    }
    
    public String getJSON(){
	JsonObject jsonMessageFormat = new JsonObject();
	jsonMessageFormat.add("ID", this.ID);
	jsonMessageFormat.add("value", this.value);
	jsonMessageFormat.add("agentType", this.agentType);
	jsonMessageFormat.add("forwardDirection", this.forwardDirection.toString());
	
	return jsonMessageFormat.asString();
    }

    public void sendTo(Pair<InetAddress, Integer> recipient){
	this.recipient = recipient;
    }
    
}
