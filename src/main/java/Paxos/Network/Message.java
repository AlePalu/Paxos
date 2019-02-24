package Paxos.Network;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

public class Message{
    // paxos related informations
    private Long senderID;
    private Long recipientID;
    private String value;
    private String agentType; // type of agent to which this message is directed
    
    // needed for internal operation
    private Boolean isBroadcast;

    // used when you want to send a message
    public Message(Long recipientID, String value, AgentType agentType){
	this.recipientID = recipientID;
	this.value = value;
	this.agentType = agentType.toString();

	this.isBroadcast = false;
    }

    // used to build the object from a JSON message, users interact with Message objects not JSON strings!
    public Message(String message){
	JsonObject jsonMessage = Json.parse(message).asObject();
	if(this.recipientID!=null)
	    this.recipientID = jsonMessage.get("RECIPIENTID").asLong();
	this.value = jsonMessage.get("VALUE").asString();
	this.agentType = jsonMessage.get("AGENTTYPE").asString();

	// this is automatically inserted by the routing logic
	this.senderID = jsonMessage.get("SENDERID").asLong();
    }

    public void setAsBroadcast(){
	this.isBroadcast = true;
    }
    
    public Long getRecipientID(){
	return this.recipientID;
    }

    public Long getSenderID(){
	return this.senderID;
    }

    public String getValue(){
	return this.value;
    }

    public String getAgentType(){
	return this.agentType;
    }
    
    public String getJSON(){
	JsonObject jsonMessageFormat = new JsonObject();
	if(this.recipientID!=null) // broadcast messages don't require a recipientID
	    jsonMessageFormat.add("RECIPIENTID", this.recipientID);
	jsonMessageFormat.add("VALUE", this.value);
	jsonMessageFormat.add("AGENTTYPE", this.agentType);
	
	if(this.isBroadcast)
	    jsonMessageFormat.add("FORWARDTYPE", MessageType.BROADCAST.toString());
	else
	    jsonMessageFormat.add("FORWARDTYPE", MessageType.UNICAST.toString());
	
	return jsonMessageFormat.toString();
    }
}
