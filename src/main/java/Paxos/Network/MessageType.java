package Paxos.Network;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonArray;
import java.util.Map.Entry;

public enum MessageType implements TrafficRule{
    // network related messages
    SUBSCRIBE("SUBSCRIBE",(s, m)->{
	    // SUBSCRIBE messages are processed only once
	    if (!SocketRegistry.getInstance().getRegistry().values().contains(s)) {
		// get UUID of the sender
		JsonObject Jmessage = Json.parse(m).asObject();
		Long UUID = Jmessage.get("SENDERID").asLong();
		// bind the socketBox to the UUID of the sender
		SocketRegistry.getInstance().addElement(UUID, s);
		SocketRegistry.getInstance().getPendingSockets().remove(s);
	    }
	}),
    DISCOVERRESPONSE("DISCOVERRESPONSE"),
    DISCOVER("DISCOVER",(s, m)->{
	    JsonArray connectedProcesses = new JsonArray();
	    for(Entry<Long, SocketBox> entry : SocketRegistry.getInstance().getRegistry().entrySet()){
		connectedProcesses.add(entry.getKey()); // building array with known UUID
	    }
	    // reply back with the list of connected processes
	    JsonObject DISCOVERRESPONSEmessage = new JsonObject();
	    DISCOVERRESPONSEmessage.add("MSGTYPE", MessageType.DISCOVERRESPONSE.toString());
            DISCOVERRESPONSEmessage.add("CPLIST", connectedProcesses);
	    
	    s.sendOut(DISCOVERRESPONSEmessage.toString());
	}),

    // naming messages are simply forwarded to the naming service process
    NAMINGREQUEST("NAMINGREQUEST", (s,m) -> SocketRegistry.getInstance().getNamingSocket().sendOut(m)),
    NAMINGSUBSCRIBE("NAMINGSUBSCRIBE", (s,m) -> SocketRegistry.getInstance().setNamingSocket(s)),
    NAMINGREPLY("NAMINGREPLY", (s,m) -> {}),
    NAMINGUPDATE("NAMINGUPDATE", (s,m) -> SocketRegistry.getInstance().getNamingSocket().sendOut(m)),
    
    // paxos protocol related messages
    PAXOS("PAXOS", (s,m) -> MessageType.forwardTo(s,m)),
    PREPAREREQUEST("PREPAREREQUEST", (s,m) -> MessageType.forwardTo(s,m)),
    RESPONDTOPREPAREREQUEST("RESPONDTOPREPAREREQUEST", (s,m) -> MessageType.forwardTo(s,m)),
    ACCEPTREQUEST("ACCEPTREQUEST", (s,m) -> MessageType.forwardTo(s,m));
    
    
    private final String messageType;
    private final TrafficRule rule;
    
    private MessageType(String type, TrafficRule rule){
	this.messageType = type;
	this.rule = rule;
    }

    private MessageType(String type){
	this.messageType = type;
	this.rule = null;
    }
    
    private static void forwardTo(SocketBox socket, String message){	
	JsonObject Jmessage = Json.parse(message).asObject();
	if(Jmessage.get("FORWARDTYPE").asString().equals(ForwardType.BROADCAST.toString())){ // append automatically my Message.getJSON();
	    for(SocketBox socketBroadcast : SocketRegistry.getInstance().getRegistry().values()){
		socketBroadcast.sendOut(message);
	    }
	}else{ // unicast transmission
	    Long UUIDreceiver = Jmessage.get("RECIPIENTID").asLong();
	    // get the socket binded to this UUID
	    SocketBox receiverSocket = SocketRegistry.getInstance().getRegistry().get(UUIDreceiver);
	    // sending message...
	    receiverSocket.sendOut(message);
	}
    }
    
    public String toString(){
	return this.messageType;
    }

    public void applyRule(SocketBox socket, String message){
	this.rule.applyRule(socket, message);
    }

    public boolean match(String message){
	JsonObject Jmessage = Json.parse(message).asObject();
	return Jmessage.get("MSGTYPE").asString().equals(this.messageType);
    }
}
