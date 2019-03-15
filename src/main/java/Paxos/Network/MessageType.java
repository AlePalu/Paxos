package Paxos.Network;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonValue;

import java.util.Map.Entry;
import java.net.Inet4Address;
import java.net.Socket;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

public enum MessageType implements TrafficRule{ 
	SUBSCRIBE("SUBSCRIBE",
		  (s, m)->{
		      // SUBSCRIBE messages are processed only once
		      if (!SocketRegistry.getInstance().getRegistry().values().contains(s)) {
			  // get UUID of the sender
			  JsonObject Jmessage = Json.parse(m).asObject();
			  Long UUID = Jmessage.get(MessageField.SENDERID.toString()).asLong();
			  // bind the socketBox to the UUID of the sender
			  SocketRegistry.getInstance().addElement(UUID, s);
			  SocketRegistry.getInstance().getPendingSockets().remove(s);
		      }
		  }),
	// messages used to keep track of processes currently active on network
	DISCOVERREPLY("DISCOVERREPLY",
		      (s, m) -> {
			  // local DISCOVERREPLY messages are sent directly to the process which originated the request, only remote ones are processed here
			  MessageType.forwardTo(s,m);
		      },
		      (o) -> {
			  LocalNetworkProcess process = (LocalNetworkProcess) o[0];
			  String message = (String) o[1];
			  JsonObject Jmessage = Json.parse(message).asObject();
			  // get the list of UUID
			  for(JsonValue UUID : Jmessage.get(MessageField.CPLIST.toString()).asArray()){
			        process.lookupConnectedProcesses().add(UUID.asLong());
			  }
			  // keep note that a DISCOVERREPLY from this node has been processed
			  process.getPendingDiscoverList().remove(Jmessage.get(MessageField.NAME.toString()).asString());
			
			  // signal that all DISCOVERREPLY have been processed
			  if(process.getPendingDiscoverList().isEmpty()){
			      process.lock.lock();
			      process.discoverMessageLock.signalAll();
			      process.lock.unlock();
			  }		
		      }),
	DISCOVER("DISCOVER",
		 (s, m)->{
		     JsonArray connectedProcesses = new JsonArray();
		     for(Entry<Long, SocketBox> entry : SocketRegistry.getInstance().getRegistry().entrySet()){
			 connectedProcesses.add(entry.getKey()); // building array with known UUID
		     }

		     // parse the message to get the ID of the sender
		     JsonObject Jmessage = Json.parse(m).asObject();
		
		     // reply back with the list of connected processes
		     String DISCOVERREPLYmessage = MessageForgery.forgeDISCOVERREPLY(connectedProcesses, Jmessage.get(MessageField.SENDERID.toString()).asLong());
	        
		     try{
			 if(Inet4Address.getLocalHost().getHostAddress().equals(Jmessage.get(MessageField.NAME.toString()).asString())) // forward locally
			     s.sendOut(DISCOVERREPLYmessage);
			 else // to remote node
			     SocketRegistry.getInstance().getRemoteNodeRegistry().get(Jmessage.get(MessageField.NAME.toString()).asString()).sendOut(DISCOVERREPLYmessage);
		     }catch(Exception e){

		     }
		 }),
	DISCOVERREQUEST("DISCOVERREQUEST",
			(s, m)->{
			    // reply with the list of local processes
			    JsonArray connectedProcesses = new JsonArray();
			    for(Entry<Long, SocketBox> entry : SocketRegistry.getInstance().getRegistry().entrySet()){
				connectedProcesses.add(entry.getKey()); // building array with known UUID
			    }
	
			    String DISCOVERREPLYmessage = MessageForgery.forgeDISCOVERREPLY(connectedProcesses);
			    s.sendOut(DISCOVERREPLYmessage);

			    // parse the message to get the ID of the sender
			    JsonObject Jmessage = Json.parse(m).asObject();
			    // sends DISCOVER messages to all known remote nodes
			    for (Entry<String, SocketBox> remoteSocket : SocketRegistry.getInstance().getRemoteNodeRegistry().entrySet()) {
				String DISCOVERmessage = MessageForgery.forgeDISCOVER(Jmessage.get(MessageField.SENDERID.toString()).asLong()); 
				remoteSocket.getValue().sendOut(DISCOVERmessage);
			    }
			}),
	// messages used for naming services
	NAMINGREQUEST("NAMINGREQUEST",
		      (s,m) -> SocketRegistry.getInstance().getNamingSocket().sendOut(m),
		      (o) -> {
			  NamingRequestHandler process = (NamingRequestHandler) o[0];
			  String message = (String) o[1];

			  // parse the message
			  JsonObject Jmessage = Json.parse(message).asObject();
			  
			  JsonArray nodesList = new JsonArray();
			  try(BufferedReader reader = new BufferedReader(new FileReader(process.getNodeFile()))){
			      String ip = reader.readLine();
			      while(ip != null){
				  nodesList.add(ip);
				  ip = reader.readLine();
			      }
			  }catch(Exception e){

			  }

			  // send response back
			  String NAMINGREPLYmessage;
			  if(Jmessage.get(MessageField.SENDERID.toString()) != null){ // request originated by a remote process
			      NAMINGREPLYmessage = MessageForgery.forgeNAMINGREPLY(nodesList,
										   Jmessage.get(MessageField.SENDERID.toString()).asLong(),
										   Jmessage.get(MessageField.NAME.toString()).asString());
			  }else{ // request originated by a remote network infrastructure
			      NAMINGREPLYmessage = MessageForgery.forgeNAMINGREPLY(nodesList,
										   Jmessage.get(MessageField.NAME.toString()).asString());
			  }
			  process.getSocketBox().sendOut(NAMINGREPLYmessage);
		      }),
	NAMINGSUBSCRIBE("NAMINGSUBSCRIBE",
			(s,m) -> SocketRegistry.getInstance().setNamingSocket(s)),
	NAMINGREPLY("NAMINGREPLY",
		    (s,m) -> {
			JsonObject Jmessage = Json.parse(m).asObject();
			String IP = Jmessage.get(MessageField.NAME.toString()).asString();
			try{
			    if(Inet4Address.getLocalHost().getHostAddress().equals(IP)){ // I'm the recipient of the message
				// update list of known remote hosts
				JsonArray nodeList = Jmessage.get(MessageField.NODELIST.toString()).asArray();
				SocketRegistry.getInstance().getRemoteNodeList().clear();
				for(JsonValue node : nodeList){
				    SocketRegistry.getInstance().getRemoteNodeList().add(node.asString());
				}

				// open connection with new remote nodes
				ArrayList<String> remoteNodes = SocketRegistry.getInstance().getRemoteNodeList();
				remoteNodes.remove(Inet4Address.getLocalHost().getHostAddress());
			        for(String remoteIP : remoteNodes){
				    if(!SocketRegistry.getInstance().getRemoteNodeRegistry().keySet().contains(remoteIP)){
					// open connection with remote node
					Socket socket = new Socket(remoteIP, 40000);
					SocketBox socketBox = new SocketBox(socket);
					SocketRegistry.getInstance().getRemoteNodeRegistry().put(remoteIP, socketBox);
				    }
				}
				
				// remove inactive sockets binded to inactive IPs
				for(Entry<String, SocketBox> remoteSocket : SocketRegistry.getInstance().getRemoteNodeRegistry().entrySet()){
				    // this IP is not recongnized as active by the name server, remove it...
				    if(!SocketRegistry.getInstance().getRemoteNodeList().contains(remoteSocket.getKey())){
					SocketRegistry.getInstance().getRemoteNodeRegistry().remove(remoteSocket.getKey());
				    }
				}
				
				// if request was originated by a local process, notify the request has been processed
				if(Jmessage.get(MessageField.RECIPIENTID.toString()) != null){
				    // avoid duplicating NAME field in response
				    Jmessage.remove(MessageField.NAME.toString());
				    Long receiver = Jmessage.get(MessageField.RECIPIENTID.toString()).asLong();
				    SocketRegistry.getInstance().getRegistry().get(receiver).sendOut(Jmessage.toString());
				}
			    }else{
				// forward the message to the remote sender
				SocketRegistry.getInstance().getRemoteNodeRegistry().get(IP).sendOut(m);
			    }
			}catch(Exception e){
			    return;
			}
		    },
		    (o) -> {
			LocalNetworkProcess process = (LocalNetworkProcess) o[0];
			String message = (String) o[1];
			// parse the message
			JsonObject Jmessage = Json.parse(message).asObject();

			process.getPendingDiscoverList().clear();
			// keep track of the DISCOVERREPLY we must wait for
			for(JsonValue ip : Jmessage.get(MessageField.NODELIST.toString()).asArray()){
			    process.getPendingDiscoverList().add(ip.asString());
			}
			// signal naming is updated
			process.lock.lock();
			process.namingLock.signalAll();
			process.lock.unlock();
		    }),
	NAMINGUPDATE("NAMINGUPDATE",
		     (s,m) -> {
			 // bind the IP address to this socket
			 JsonObject Jmessage = Json.parse(m).asObject();
			 String IP = Jmessage.get(MessageField.NAME.toString()).asString();
			 SocketRegistry.getInstance().getRemoteNodeRegistry().put(IP, s);
			 
			 // forward message to naming service
			 SocketRegistry.getInstance().getNamingSocket().sendOut(m);
		     },
		     (o) -> {
			 NamingRequestHandler process = (NamingRequestHandler) o[0];
			 String message = (String) o[1];

			 // parse the message
			 JsonObject Jmessage = Json.parse(message).asObject();
			 String newName = Jmessage.get(MessageField.NAME.toString()).asString();
			 // add node to the list of available nodes
			 process.recordName(newName);

			 System.out.printf("[NamingRequestHandler]: remote node "+newName+" registered%n");
		     }),
	// messages used to keep the internal state updated
	//PING("PING"),
	
	// paxos protocol related messages are simply forwarded to the correct process, no internal processing nor packet inspection is done by the network stack
        PREPAREREQUEST("PREPAREREQUEST", (s,m) -> MessageType.forwardTo(s,m)),
	RESPONDTOPREPAREREQUEST("RESPONDTOPREPAREREQUEST", (s,m) -> MessageType.forwardTo(s,m)),
	ACCEPTREQUEST("ACCEPTREQUEST", (s,m) -> MessageType.forwardTo(s,m)),
	DECISION("DECISION", (s,m) -> MessageType.forwardTo(s,m));


	private final String messageType;
	private final TrafficRule rule;
	private final CustomMessageLogic messageLogic;
	
	private MessageType(String type, TrafficRule rule){
	    this.messageType = type;
	    this.rule = rule;
	    this.messageLogic = null;
	}

	private MessageType(String type, TrafficRule rule, CustomMessageLogic messageLogic){
	    this.messageType = type;
	    this.rule = rule;
	    this.messageLogic = messageLogic;
	}

	private static void forwardTo(SocketBox socket, String message){
	    JsonObject Jmessage = Json.parse(message).asObject();
	    if(Jmessage.get(MessageField.FORWARDTYPE.toString()).asString().equals(ForwardType.BROADCAST.toString())){
		for(SocketBox socketBroadcast : SocketRegistry.getInstance().getRegistry().values()){
		    socketBroadcast.sendOut(message);
		}
	    }else{ // unicast transmission
		Long UUIDreceiver = Jmessage.get(MessageField.RECIPIENTID.toString()).asLong();
		// get the socket binded to this UUID
		SocketBox receiverSocket = SocketRegistry.getInstance().getRegistry().get(UUIDreceiver);
		receiverSocket.sendOut(message);
	    }
	}

	public String toString(){
	    return this.messageType;
	}

	public void applyRule(SocketBox socket, String message){
	    this.rule.applyRule(socket, message);
	}

	public void applyLogic(Object... args){
	    this.messageLogic.applyLogic(args);
	}
	
	public boolean match(String message){
	    JsonObject Jmessage = Json.parse(message).asObject();
	    return Jmessage.get(MessageField.MSGTYPE.toString()).asString().equals(this.messageType);
	}
}
