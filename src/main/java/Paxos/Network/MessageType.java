package Paxos.Network;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonValue;

import java.util.Map.Entry;
import java.net.Inet4Address;
import java.net.Socket;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

public enum MessageType implements TrafficRule{ 
	SUBSCRIBE("SUBSCRIBE",
		  (s, m)->{
		      // SUBSCRIBE messages are processed only once
		      if (!SocketRegistry.getInstance().getRegistry().values().contains(s)){
			  JsonObject Jmessage = Json.parse(m).asObject();
			  try {
			      Long UUID = Jmessage.get(MessageField.SENDERID.toString()).asLong();
			      if(Jmessage.get(MessageField.NAME.toString()).asString().equals(Inet4Address.getLocalHost().getHostAddress()) &&
					      !Jmessage.get(MessageField.FORWARDTYPE.toString()).asString().equals(ForwardType.LOCALBROADCAST.toString())){ // local message

				  // associate to this SocketBox the machineUUID
				  s.setUUID(SocketRegistry.getInstance().getMachineUUID());
				  
				  // bind the socketBox to the UUID of the sender
				  SocketRegistry.getInstance().addElement(UUID, s);
				  SocketRegistry.getInstance().getPendingSockets().remove(s);

				  // forward to remote node
				  Jmessage.remove(MessageField.FORWARDTYPE.toString());
				  Jmessage.add(MessageField.FORWARDTYPE.toString(), ForwardType.BROADCAST.toString());
				  MessageType.forwardTo(s, Jmessage.toString());
			      }else{ // the message comes from a remote node
				  SocketRegistry.getInstance().getRegistry().put(UUID,SocketRegistry.getInstance().getRemoteNodeRegistry().get(Jmessage.get(MessageField.NAME.toString()).asString()));
			      }
			  }
			  catch (Exception e) {
			      e.printStackTrace();
			  }
		      }
		  },
		  (o) -> {
		      // get machineUUID from the SUBSCRIBE, which is echoed to all processes on network
		      LocalNetworkProcess process = (LocalNetworkProcess) o[0];
		      String message = (String) o[1];
		      JsonObject Jmessage = Json.parse(message).asObject();

		      Long senderID = new Long(Jmessage.get(MessageField.SENDERID.toString()).asLong());
		      
		      // this is my SUBSCRIBE
		      if(senderID.equals(process.getProcessUUID())){
			  long machineUUID = Jmessage.get(MessageField.MACHINEUUID.toString()).asLong();
			  process.getSocketBox().setUUID(machineUUID);
		      }
		  }),
	// messages used to keep track of processes currently active on network
	DISCOVERREPLY("DISCOVERREPLY",
		      (s, m) -> {
			  // local DISCOVERREPLY messages are sent directly to the process which originated the request, only remote ones are processed here

			  // bind the UUID of remote processes to the socket of the remote machine where are hosted
			  JsonObject Jmessage = Json.parse(m).asObject();
			  SocketBox remoteSocketBox = SocketRegistry.getInstance().getRemoteNodeRegistry().get(Jmessage.get(MessageField.NAME.toString()).asString());

			  ArrayList<Long> activeProcesses = new ArrayList<Long>();
			  for(JsonValue UUID : Jmessage.get(MessageField.CPLIST.toString()).asArray()){
			      SocketRegistry.getInstance().getRegistry().put(UUID.asLong(), remoteSocketBox);
			      activeProcesses.add(UUID.asLong());
			  }

			  // remove UUID of processes binded to this remote machine which are not contained in the DISCOVERREPLY (death processes)
			  for(Entry<Long, SocketBox> entry : SocketRegistry.getInstance().getRegistry().entrySet()){
			      if(entry.getValue().equals(remoteSocketBox) && !activeProcesses.contains(entry.getKey())){ // remove this binding
				  SocketRegistry.getInstance().getRegistry().remove(entry.getKey());
			      }
			  }
			  // forward the message to the process
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
		     for(Long entry : SocketRegistry.getInstance().getLocalUUID()){
			 connectedProcesses.add(entry); // building array with known UUID
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
			 if(SocketRegistry.getInstance().getRemoteNodeRegistry().get(Jmessage.get(MessageField.NAME.toString()).asString()) == null){
			     // open connection with remote node
			     String remoteIP = Jmessage.get(MessageField.NAME.toString()).asString();

			     try{
				 Socket socket = new Socket(remoteIP, 40000);
				 SocketBox socketBox = new SocketBox(socket);
			     	 // set the machineUUID for this connection, the mine. All outgoing traffic must be signed with my machineUUID
				 socketBox.setUUID(SocketRegistry.getInstance().getMachineUUID());
				 SocketRegistry.getInstance().getRemoteNodeRegistry().put(remoteIP, socketBox);
			     }catch(Exception ex){
				 return;
			     }
			     
			     // now can reply
			     SocketRegistry.getInstance().getRemoteNodeRegistry().get(remoteIP).sendOut(DISCOVERREPLYmessage);
			 }
		     }
		 }),
	DISCOVERREQUEST("DISCOVERREQUEST",
			(s, m)->{
			    // reply with the list of local processes
			    JsonArray connectedProcesses = new JsonArray();
			    for(Long entry : SocketRegistry.getInstance().getLocalUUID()){
				connectedProcesses.add(entry); // building array with known UUID
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
		      (s,m) -> {
			  // issue a ticket for the naming request
			  Random rng = new Random();
			  Long randomNumber = Math.abs(rng.nextLong());
			  JsonObject Jmessage = Json.parse(m).asObject();
			  // keep track of this naming request...
			  try{
			      if(Jmessage.get(MessageField.NAME.toString()).asString().equals(Inet4Address.getLocalHost().getHostAddress()))
				  Tracker.getInstance().issueTicket(Jmessage.get(MessageField.SENDERID.toString()).asLong(), 5000, randomNumber, TicketType.NAMING.toString(), s);
			  }catch(Exception e){
			      return;
			  }
			  // add the ticket to the request
			  if(Jmessage.get(MessageField.TICKET.toString()) == null)
			      Jmessage.add(MessageField.TICKET.toString(), randomNumber);
			  
			  SocketRegistry.getInstance().getNamingSocket().sendOut(Jmessage.toString());
		      },
		      (o) -> {
			  NamingRequestHandler process = (NamingRequestHandler) o[0];
			  String message = (String) o[1];
			  
			  // parse the message
			  JsonObject Jmessage = Json.parse(message).asObject();

			  // build up the list of known remote nodes
			  JsonArray nodesList = new JsonArray();
			  try(BufferedReader reader = new BufferedReader(new FileReader(process.getNodeFile()))){
			      String ip = reader.readLine();
			      while(ip != null){
				  // each remote node is identified by a JSON object, containing IP and UUID of the remote machine
				  JsonObject Jmachine = new JsonObject();
				  // retrieve informations from file string
				  String[] infos = ip.split(",");
				  // packing into JsonObject...
				  Jmachine.add("IP", infos[0]);
				  Jmachine.add("UUID", infos[1]);
				  nodesList.add(Jmachine);
				  ip = reader.readLine();
			      }
			  }catch(Exception e){
			      e.printStackTrace();
			  }

			  // send response back
			  String NAMINGREPLYmessage;
			  if(Jmessage.get(MessageField.SENDERID.toString()) != null){ // request originated by a remote process
			      NAMINGREPLYmessage = MessageForgery.forgeNAMINGREPLY(nodesList,
										   Jmessage.get(MessageField.SENDERID.toString()).asLong(),
										   Jmessage.get(MessageField.NAME.toString()).asString(),
										   Jmessage.get(MessageField.TICKET.toString()).asLong());
			  }else{ // request originated by a remote network infrastructure
			      NAMINGREPLYmessage = MessageForgery.forgeNAMINGREPLY(nodesList,
										   Jmessage.get(MessageField.NAME.toString()).asString(),
										   Jmessage.get(MessageField.TICKET.toString()).asLong());
			  };

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
				    // each node is encoded in a JsonObject
				    JsonObject Jmachine = node.asObject();				    
				    SocketRegistry.getInstance().getRemoteNodeList().add(Jmachine.get("IP").asString());
				}

				// write nodelist to recovery file
				File recoveryFile = new File("lastProcessStatus.txt");
				try(FileWriter fileWriter = new FileWriter(recoveryFile, false)){
				    fileWriter.write(nodeList.toString());
				    fileWriter.flush();
				}catch(Exception e){
				    System.out.printf("There was some problem in writing recovery file!%n");
				}


				// open connection with new remote nodes
				ArrayList<String> remoteNodes = SocketRegistry.getInstance().getRemoteNodeList();
				remoteNodes.remove(Inet4Address.getLocalHost().getHostAddress());
			        for(String remoteIP : remoteNodes){
				    if(!SocketRegistry.getInstance().getRemoteNodeRegistry().keySet().contains(remoteIP)){
					// open connection with remote node
					Socket socket = new Socket(remoteIP, 40000);
					SocketBox socketBox = new SocketBox(socket);
					// set the machineUUID for this connection, the mine. All outgoing traffic must be signed with my machineUUID
					socketBox.setUUID(SocketRegistry.getInstance().getMachineUUID());				
					SocketRegistry.getInstance().getRemoteNodeRegistry().put(remoteIP, socketBox);
				    }
				}

				// keep track of the UUID of the remote machine. Required for fault tolerance
				for(JsonValue node : nodeList){
				    // each node is encoded in a JsonObject
				    JsonObject Jmachine = node.asObject();
				    String rIP = Jmachine.get("IP").asString();
				    Long remoteUUID = new Long(Jmachine.get("UUID").asString());
				    
				    if(!rIP.equals(Inet4Address.getLocalHost().getHostAddress()))
					SocketRegistry.getInstance().getRemoteNodeRegistry().get(rIP).setRemoteUUID(remoteUUID);
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

			    // remove the ticket associated with this request
			    Long ticket = Jmessage.get(MessageField.TICKET.toString()).asLong();
			    if(IP.equals(Inet4Address.getLocalHost().getHostAddress()) &&
			       Jmessage.get(MessageField.RECIPIENTID.toString()) != null){
				
				Long sender = Jmessage.get(MessageField.RECIPIENTID.toString()).asLong();
				Tracker.getInstance().removeTicket(sender, ticket, TicketType.NAMING.toString());
			    }
			}catch(Exception e){
			    e.printStackTrace();
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
			    String machineIP = ip.asObject().get("IP").asString();
	        	    process.getPendingDiscoverList().add(machineIP);
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
			 long machineUUID = Jmessage.get(MessageField.MACHINEUUID.toString()).asLong();

			 // add node to the list of available nodes
			 process.recordName(newName, machineUUID);

			 System.out.printf("[NamingRequestHandler]: remote node "+newName+" registered%n");
		     }),
	// messages used to keep the internal state updated
	PING("PING",
	     (s,m) -> {
		 // remove the ticket, since the message has been received
		 JsonObject Jmessage = Json.parse(m).asObject();
		 if(Jmessage.get(MessageField.RECIPIENTID.toString()) != null){ // PING message originated by a proces
		     Tracker.getInstance().removeTicket(Jmessage.get(MessageField.SENDERID.toString()).asLong(), Jmessage.get(MessageField.TICKET.toString()).asLong(), TicketType.PING.toString());
		 }else{ // PING message originated by name server
		     // reply back with the same message
		     String sender = Jmessage.get(MessageField.NAME.toString()).asString();
		     Jmessage.remove(MessageField.NAME.toString()); // force the substitution of the field name with the name of who is replying back

		     // open connection if sender is unknown
		     if(SocketRegistry.getInstance().getRemoteNodeRegistry().get(sender) == null){
			 try{
			     Socket socket = new Socket(sender, 40000);
			     SocketBox socketBox = new SocketBox(socket);
			     // set the machineUUID for this connection, the mine. All outgoing traffic must be signed with my machineUUID
			     socketBox.setUUID(SocketRegistry.getInstance().getMachineUUID());
			     SocketRegistry.getInstance().getRemoteNodeRegistry().put(sender, socketBox);
			 }catch(Exception ex){
			     return;
			 }
		     }
		     
		     SocketRegistry.getInstance().getRemoteNodeRegistry().get(sender).sendOut(Jmessage.toString());
		     // remove the ticket
		     Tracker.getInstance().removeTicket(sender, Jmessage.get(MessageField.TICKET.toString()).asLong(), TicketType.PING.toString());
		 }
	     },
	     (o) -> {
		 // reply back with the same message
		 LocalNetworkProcess process = (LocalNetworkProcess) o[0];
		 String message = (String) o[1];

		 process.sendMessage(message);
	     }),
	SIGUNLOCK("SIGUNLOCK",
		     (s,m) -> {
			 MessageType.forwardTo(s,m);
		     },
		     (o) -> {
			 LocalNetworkProcess process = (LocalNetworkProcess) o[0];
			 String message = (String) o[1];

			 JsonObject Jmessage = Json.parse(message).asObject();
			 // there was a name fault??
			 if(Jmessage.get(MessageField.SIGTYPE.toString()) != null){
			     String sigType = Jmessage.get(MessageField.SIGTYPE.toString()).asString();
			     if(sigType.equals("NAMEFAULT")){
				 process.nameFault = true;
			     }
			 }
			 
			 // simply nullify any pending discover by unlocking the process
			 process.lock.lock();
			 process.namingLock.signalAll();
			 process.discoverMessageLock.signalAll();
			 process.lock.unlock();

			 // remove any timer waiting on a DISCOVER
			 process.timer.cancel();
			 process.timer.purge();
		     }),
	COORD("COORD",
	     (s,m) -> {
		  // COORD is transmitted by naming server. keep the new name server reference
		  SocketRegistry.getInstance().setNamingSocket(s);
		  System.out.printf("[Tracker]: new name server located at: "+s.getSocket().getInetAddress().getHostAddress()+"\n");

		  // allow all traffic
		  TrafficHandler.getInstance().allowAll();

		  // remove any ticket still present, if any
		  Tracker.getInstance().removeTicket(TicketType.NAMING.toString());
		  Tracker.getInstance().removeTicket(TicketType.ELECT.toString());
		  
		  // unlock clients waiting for naming recovery
		  MessageType.forwardTo(null, m);
	      },(o) -> {
		  LocalNetworkProcess process = (LocalNetworkProcess) o[0];
		  process.nameFault = false;
		  
		  process.lock.lock();
		  process.namingLock.signalAll();
		  process.discoverMessageLock.signalAll();
		  process.lock.unlock();		  
	      }),
	ELECT("ELECT",
	      (s,m) -> {
		  // on reception of an ELECT, someone has discovered name server has crashed.
		  // If no process of mine have issued a BULLYREQUEST, I still keep the old naming reference, and this could cause issue in the election mechanism, rmeove it
		  try{
		      SocketRegistry.getInstance().getNamingSocket().close();
		      SocketRegistry.getInstance().setNamingSocket(null);
		  }catch(NullPointerException e){
		      // reference already removed, no need to worry
		  }
		  
		  JsonObject Jmessage = Json.parse(m).asObject();
		  // get UUID of the sender
		  long senderMachineUUID = Jmessage.get(MessageField.MACHINEUUID.toString()).asLong();
		  // if my UUID is greater than the sender UUID, suppress its election
		  if(SocketRegistry.getInstance().getMachineUUID() > senderMachineUUID){
		      String BULLYSUPPRESSmessage = MessageForgery.forgeBULLYSUPPRESS();
		      s.sendOut(BULLYSUPPRESSmessage);

		      Random rng = new Random();
		      Long randomNumber = Math.abs(rng.nextLong());
		      Tracker.getInstance().issueTicket(SocketRegistry.getInstance().getMachineUUID(), 5000, randomNumber, TicketType.ELECT.toString(), s);
			     
		      // start a new election, sending an ELECT to all nodes having UUID greater than mine
		      String ELECTmessage = MessageForgery.forgeELECT();
		      for(Entry<String, SocketBox> remoteSocket : SocketRegistry.getInstance().getRemoteNodeRegistry().entrySet()){
			  if(remoteSocket.getValue().getRemoteUUID() > SocketRegistry.getInstance().getMachineUUID()){
			      remoteSocket.getValue().sendOut(ELECTmessage);
			      System.out.printf(ELECTmessage+"%n");
			  }
		      }

		      ArrayList<String> allowedTraffic = new ArrayList<String>();
		      allowedTraffic.add("ELECT");
		      allowedTraffic.add("BULLYSUPPRESS");
		      allowedTraffic.add("COORD");

		      TrafficHandler.getInstance().setWhiteList(allowedTraffic);
		  } 
	      }, (o) -> {
		  // inform local process of the name fault, in case in this node still no on process has discovered it.
		  LocalNetworkProcess process = (LocalNetworkProcess) o[0];
		  process.nameFault = true;
	      }),
	BULLYSUPPRESS("BULLYSUPPRESS",
		      (s,m) -> {
			  // I will not be the new leader, remove any ELECT token
			  Tracker.getInstance().removeTicket(TicketType.ELECT.toString());
		      }),
	BULLYREQUEST("BULLYREQUEST",
		     (s,m) -> {
			 // Emit a token to track the elect request.
			 // When an elect token expires, I will be the new leader (If an ELECT token is still present then I've not received any BULLYSUPPRESS message, hence I'm the node still alive having the highest UUID on network)
			 Random rng = new Random();
			 Long randomNumber = Math.abs(rng.nextLong());
			 Tracker.getInstance().issueTicket(SocketRegistry.getInstance().getMachineUUID(), 5000, randomNumber, MessageType.ELECT.toString(), s);
			 
			 // send an ELECT to all pyhisical nodes having an UUID grater than mine
			 String ELECTmessage = MessageForgery.forgeELECT();
			 for(Entry<String, SocketBox> remoteSocket : SocketRegistry.getInstance().getRemoteNodeRegistry().entrySet()){
			     if(remoteSocket.getValue().getRemoteUUID() > SocketRegistry.getInstance().getMachineUUID()){
				 remoteSocket.getValue().sendOut(ELECTmessage);
			     }
			 }

			 // name server is not up, this is a critical state of the infrastructure, and no messages other than the ones to recover the system should be transmitted.
			 // block any outcoming and incoming message not related to failure recovery. 
			 ArrayList<String> allowedTraffic = new ArrayList<String>();
			 allowedTraffic.add(MessageType.ELECT.toString());
			 allowedTraffic.add(MessageType.BULLYSUPPRESS.toString());
			 allowedTraffic.add(MessageType.COORD.toString());
			 
			 TrafficHandler.getInstance().setWhiteList(allowedTraffic);			 
		     }),
	
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

	public static void forwardTo(SocketBox socket, String message){
	    JsonObject Jmessage = Json.parse(message).asObject();
	    // broadcasting messages to check
	    HashSet<String> forwardType = new HashSet<String>();
	    forwardType.add(ForwardType.BROADCAST.toString());
	    forwardType.add(ForwardType.LOCALBROADCAST.toString());

	    String actualForwardType = Jmessage.get(MessageField.FORWARDTYPE.toString()).asString();
	    
	    if(forwardType.contains(actualForwardType)){
		ArrayList<SocketBox> sockets = new ArrayList<SocketBox>();

		for(Long UUID : SocketRegistry.getInstance().getLocalUUID()){
		    sockets.add(SocketRegistry.getInstance().getRegistry().get(UUID));
		}

		if(actualForwardType.equals(ForwardType.BROADCAST.toString())){ // truly broadcast trasmission
		    // add remote nodes
		    sockets.addAll(SocketRegistry.getInstance().getRemoteNodeRegistry().values());
 
		    // avoid flooding the network with broadcast messages, make this message a LOCALBROADCAST one
		    Jmessage.remove(MessageField.FORWARDTYPE.toString());
		    Jmessage.add(MessageField.FORWARDTYPE.toString(), ForwardType.LOCALBROADCAST.toString());
		}
		for(SocketBox socketBroadcast : sockets){
		    socketBroadcast.sendOut(Jmessage.toString());
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
