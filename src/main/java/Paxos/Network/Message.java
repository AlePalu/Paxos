package Paxos.Network;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

enum ForwardType{
    UNICAST("UNICAST"),
    BROADCAST("BROADCAST");

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
    }

    // used to build the object from a JSON message, users interact with Message objects not JSON strings!
    public Message(String message){
        JsonObject jsonMessage = Json.parse(message).asObject();
        if(this.recipientID!=null)
            this.recipientID = jsonMessage.get("RECIPIENTID").asLong();
        this.messageType = jsonMessage.get("MSGTYPE").asString();
        if(!messageType.equals(MessageType.NAMINGREPLY.toString())) {
            if(!jsonMessage.get("VALUE").isNull())
                this.value = jsonMessage.get("VALUE").asString();
            // this is automatically inserted by the routing logic
            this.senderID = jsonMessage.get("SENDERID").asLong();
        }
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

    public String getMessageType(){
        return this.messageType;
    }

    public String getJSON(){
        JsonObject jsonMessageFormat = new JsonObject();
        if(this.recipientID!=null) // broadcast messages don't require a recipientID
            jsonMessageFormat.add("RECIPIENTID", this.recipientID);
        jsonMessageFormat.add("VALUE", this.value);
        jsonMessageFormat.add("MSGTYPE", this.messageType);

        if(this.isBroadcast)
            jsonMessageFormat.add("FORWARDTYPE", ForwardType.BROADCAST.toString());
        else
            jsonMessageFormat.add("FORWARDTYPE", ForwardType.UNICAST.toString());

        return jsonMessageFormat.toString();
    }
}
