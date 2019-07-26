package Paxos.Network;

import java.util.Random;

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
    NODELIST("NODELIST"),
    TICKET("TICKET"),
    MACHINEUUID("MACHINEUUID"),
    SIGTYPE("SIGTYPE"),
    
    ROUND("ROUND");
    
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
	Jmessage.add(MessageField.FORWARDTYPE.toString(), ForwardType.UNICAST.toString());
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

    public static String forgeNAMINGREPLY(JsonArray nodeList, Long recipientID, String name, Long ticket){
	JsonObject Jmessage = new JsonObject();
	Jmessage.add(MessageField.MSGTYPE.toString(), MessageType.NAMINGREPLY.toString());
	Jmessage.add(MessageField.NODELIST.toString(), nodeList);
	if(recipientID != null)
	    Jmessage.add(MessageField.RECIPIENTID.toString(), recipientID);
	Jmessage.add(MessageField.NAME.toString(), name);
	Jmessage.add(MessageField.TICKET.toString(), ticket);
	return Jmessage.toString();
    }

    public static String forgeNAMINGREPLY(JsonArray nodeList, String name, Long ticket){
	return forgeNAMINGREPLY(nodeList, null, name, ticket);
    }

    public static String forgePING(Long recipientID){
	JsonObject Jmessage = new JsonObject();
	Jmessage.add(MessageField.MSGTYPE.toString(), MessageType.PING.toString());
	if(recipientID != null)
	    Jmessage.add(MessageField.RECIPIENTID.toString(), recipientID);
	
	Random rng = new Random();
	Long randomNumber = Math.abs(rng.nextLong());
	Jmessage.add(MessageField.TICKET.toString(), randomNumber);

	return Jmessage.toString();
    }

    public static String forgePING(){
	return forgePING(null);
    }

    public static String forgeSIGUNLOCK(ForwardType forward, JsonObject sigType){
	JsonObject Jmessage = new JsonObject();
	Jmessage.add(MessageField.MSGTYPE.toString(), MessageType.SIGUNLOCK.toString());

	if(sigType != null)
	    Jmessage.add(MessageField.SIGTYPE.toString(), sigType);
	
	if(forward.equals(ForwardType.BROADCAST)) // broadcast transmission
	    Jmessage.add(MessageField.FORWARDTYPE.toString(), ForwardType.BROADCAST.toString());
	else
	    Jmessage.add(MessageField.FORWARDTYPE.toString(), ForwardType.UNICAST.toString());
	return Jmessage.toString();
    }

    
    public static String forgeBULLYREQUEST(){
	JsonObject Jmessage = new JsonObject();
	Jmessage.add(MessageField.MSGTYPE.toString(), MessageType.BULLYREQUEST.toString());
	return Jmessage.toString();
    }

    public static String forgeELECT(){
	JsonObject Jmessage = new JsonObject();
	Jmessage.add(MessageField.MSGTYPE.toString(), MessageType.ELECT.toString());
	return Jmessage.toString();
    }

    public static String forgeBULLYSUPPRESS(){
	JsonObject Jmessage = new JsonObject();
	Jmessage.add(MessageField.MSGTYPE.toString(), MessageType.BULLYSUPPRESS.toString());
	return Jmessage.toString();
    }
    
}
