package Paxos.Network;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

enum ForwardType{
    UNICAST("UNICAST"),
    BROADCAST("BROADCAST"),
    LOCALBROADCAST("LOCALBROADCAST");

    private String type;

    private ForwardType(String type){
        this.type = type;
    }

    public String toString(){
        return this.type;
    }
}

public class Message{
    // paxos related informations
    private Long senderID;
    private Long recipientID;
    private Long proposeID;
    private String value;
    private String messageType; // type of agent to which this message is directed
    private Long round;


    // needed for internal operation
    private Boolean isBroadcast;

    // used to build the object from a JSON message, users interact with Message objects not JSON strings!
    public Message(String message){
        JsonObject jsonMessage = Json.parse(message).asObject();
        if(this.recipientID!=null)
            this.recipientID = jsonMessage.get(MessageField.RECIPIENTID.toString()).asLong();

	    this.messageType = jsonMessage.get(MessageField.MSGTYPE.toString()).asString();

	    if(!jsonMessage.get(MessageField.VALUE.toString()).isNull())
	        this.value = jsonMessage.get(MessageField.VALUE.toString()).asString();
	    if(!jsonMessage.get(MessageField.PROPOSEID.toString()).isNull())
		this.proposeID = jsonMessage.get(MessageField.PROPOSEID.toString()).asLong();

	    this.round = jsonMessage.get(MessageField.ROUND.toString()).asLong();

	    // this is automatically inserted by the routing logic
	    this.senderID = jsonMessage.get(MessageField.SENDERID.toString()).asLong();
    }


    public void setAsBroadcast(){
        this.isBroadcast = true;
    }

    public Long getRecipientID(){
        return this.recipientID;
    }

    public Long getProposeID(){
        return proposeID;
    }

    public Long getSenderID(){
        return this.senderID;
    }

    public String getValue(){
        return this.value;
    }

    public String getMessageType(){
        return this.messageType;
    }

    public Long getRound(){
        return this.round;
    }
}
