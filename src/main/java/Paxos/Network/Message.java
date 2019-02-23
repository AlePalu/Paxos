package Paxos.Network;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

public class Message{
    // paxos related informations
    private Integer senderID;
    private Integer recipientID;
    private Integer value;
    private String agentType; // type of agent to which this message is directed
    
    // needed for internal operation
    private MessageType messageType;
    private Boolean isBroadcast;

    // used when you want to send a message
    public Message(Integer recipientID, Integer value, AgentType agentType, MessageType type){
	this.recipientID = recipientID;
	this.value = value;
	this.messageType = type;
	this.agentType = agentType.toString();
    }

    // used to build the object from a JSON message, users interact with Message objects not JSON strings!
    public Message(String message){
	JsonObject jsonMessage = Json.parse(message).asObject();
	this.recipientID = jsonMessage.get("RECIPIENTID").asInt();
	this.value = jsonMessage.get("VALUE").asInt();
	this.agentType = jsonMessage.get("AGENTTYPE").asString();

	// this is automatically inserted by the routing logic
	this.senderID = jsonMessage.get("SENDERID").asInt();
    }

    public void setAsBroadcast(){
	this.isBroadcast = true;
    }
    
    public Integer getRecipientID(){
	return this.recipientID;
    }

    public Integer getSenderID(){
	return this.senderID;
    }

    public Integer getValue(){
	return this.value;
    }

    public String getAgentType(){
	return this.agentType;
    }
    
    public String getJSON(){
	JsonObject jsonMessageFormat = new JsonObject();
	jsonMessageFormat.add("RECIPIENTID", this.recipientID);
	jsonMessageFormat.add("VALUE", this.value);
	jsonMessageFormat.add("AGENTTYPE", this.agentType);
	jsonMessageFormat.add("MSGTYPE", this.messageType.toString());

	if(this.isBroadcast)
	    jsonMessageFormat.add("FORWARDTYPE", MessageType.BROADCAST.toString());
	else
	    jsonMessageFormat.add("FORWARDTYPE", MessageType.UNICAST.toString());
	
	return jsonMessageFormat.asString();
    }
}
