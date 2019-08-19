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


    // needed for internal operation
    private Boolean isBroadcast;

    // used when you want to send a message
    public Message(Long recipientID, String value, MessageType messageType){
        this.recipientID = recipientID;
        this.value = value;
        this.messageType = messageType.toString();
        this.isBroadcast = false;
       // this.round = round;
    }



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

    public String getJSON(){
        JsonObject jsonMessageFormat = new JsonObject();
        if(this.recipientID!=null) // broadcast messages don't require a recipientID
            jsonMessageFormat.add(MessageField.RECIPIENTID.toString(), this.recipientID);
        jsonMessageFormat.add(MessageField.VALUE.toString(), this.value);
        jsonMessageFormat.add(MessageField.MSGTYPE.toString(), this.messageType);

        if(this.isBroadcast)
            jsonMessageFormat.add(MessageField.FORWARDTYPE.toString(), ForwardType.BROADCAST.toString());
        else
            jsonMessageFormat.add(MessageField.FORWARDTYPE.toString(), ForwardType.UNICAST.toString());

        return jsonMessageFormat.toString();
    }
}
