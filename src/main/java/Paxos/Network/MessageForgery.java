package Paxos.Network;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

enum MessageField{
    MSGTYPE("MSGTYPE"),
    CPLIST("CPLIST"),
    RECIPIENTID("RECIPIENTID"),
    FORWARDTYPE("FORWARDTYPE"),
    SENDERID("SENDERID"),
    NAME("NAME"),
    VALUE("VALUE"),
    NODELIST("NODELIST");
    
    private String name;
    
    private MessageField(String name){
	this.name = name;
    }

    public String toString(){
	return this.name;
    }
}

class MessageForgery{

    public static String forgeDISCOVERREPLY(JsonArray connectedProcesses, Long recipientID){
	JsonObject Jmessage = new JsonObject();
	Jmessage.add(MessageField.MSGTYPE.toString(), MessageType.DISCOVERREPLY.toString());
	Jmessage.add(MessageField.CPLIST.toString(), connectedProcesses);
	Jmessage.add(MessageField.RECIPIENTID.toString(), recipientID);
	Jmessage.add(MessageField.FORWARDTYPE.toString(), ForwardType.UNICAST.toString());
	return Jmessage.toString();
    }

    public static String forgeDISCOVERREPLY(JsonArray connectedProcesses){
	JsonObject Jmessage = new JsonObject();
	Jmessage.add(MessageField.MSGTYPE.toString(), MessageType.DISCOVERREPLY.toString());
	Jmessage.add(MessageField.CPLIST.toString(), connectedProcesses);
	Jmessage.add(MessageField.FORWARDTYPE.toString(), ForwardType.UNICAST.toString());
	return Jmessage.toString();
    }

    public static String forgeDISCOVER(Long senderID){
	JsonObject Jmessage = new JsonObject();
	Jmessage.add(MessageField.MSGTYPE.toString(), MessageType.DISCOVER.toString());
	Jmessage.add(MessageField.SENDERID.toString(), senderID);
	return Jmessage.toString();
    }

    public static String forgeDISCOVERREQUEST(){
	JsonObject Jmessage = new JsonObject();
	Jmessage.add(MessageField.MSGTYPE.toString(), MessageType.DISCOVERREQUEST.toString());
	return Jmessage.toString();
    }
    
    public static String forgeSUBSCRIBE(){
	JsonObject Jmessage = new JsonObject();
	Jmessage.add(MessageField.MSGTYPE.toString(), MessageType.SUBSCRIBE.toString());
	return Jmessage.toString();
    }    

    public static String forgeNAMINGREQUEST(){
	JsonObject Jmessage = new JsonObject();
	Jmessage.add(MessageField.MSGTYPE.toString(), MessageType.NAMINGREQUEST.toString());
	return Jmessage.toString();
    }

    public static String forgeNAMINGUPDATE(String name){
	JsonObject Jmessage = new JsonObject();
	Jmessage.add(MessageField.MSGTYPE.toString(), MessageType.NAMINGUPDATE.toString());
	Jmessage.add(MessageField.NAME.toString(), name);
	return Jmessage.toString();
    }

    public static String forgeNAMINGSUBSCRIBE(){
	JsonObject Jmessage = new JsonObject();
	Jmessage.add(MessageField.MSGTYPE.toString(), MessageType.NAMINGSUBSCRIBE.toString());
	return Jmessage.toString();
    }

    public static String forgeNAMINGREPLY(JsonArray nodeList, Long recipientID, String name){
	JsonObject Jmessage = new JsonObject();
	Jmessage.add(MessageField.MSGTYPE.toString(), MessageType.NAMINGREPLY.toString());
	Jmessage.add(MessageField.NODELIST.toString(), nodeList);
	Jmessage.add(MessageField.RECIPIENTID.toString(), recipientID);
	Jmessage.add(MessageField.NAME.toString(), name);
	return Jmessage.toString();
    }
}