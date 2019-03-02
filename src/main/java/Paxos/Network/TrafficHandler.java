package Paxos.Network;

import java.io.BufferedWriter;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import java.util.Map.Entry;

// handle NetworkManager queues, sending and receiving messages to remote clients
class TrafficHandler implements Runnable{

    public void run(){
	System.out.printf("[TrafficHandler]: Ready to route traffic!\n");
	while(true){
	    for(SocketBox socket : SocketRegistry.getInstance().getAllSockets()){
		try {
		    // take message
		    String message;
		    if(socket.getInputStream().ready()){ // there are data ready to be readden
			message = socket.getInputStream().readLine();
			JsonObject JSONmessage = Json.parse(message).asObject();
			// route the message on the base of the UUID contained in the message
			Long UUID = JSONmessage.get("SENDERID").asLong();

			if(JSONmessage.get("MSGTYPE")!=null){ // internal messages...
			    if(JSONmessage.get("MSGTYPE").asString().equals(MessageType.SUBSCRIBE.toString())){
				// SUBSCRIBE messages can be processed only once
				if(!SocketRegistry.getInstance().getRegistry().values().contains(socket)){
				    // process the message immediately
				    SocketRegistry.getInstance().addElement(UUID, socket); // bind this socketBox to the UUID
				    SocketRegistry.getInstance().getPendingSockets().remove(socket);

				 //   System.out.printf("SUBSRCRIBE message received with UUID: "+UUID+"%n");
				}
			    }
			    else if(JSONmessage.get("MSGTYPE").asString().equals(MessageType.DISCOVER.toString())){
				// process DISCOVER message immediately
				JsonArray connectedProcesses = new JsonArray();
				for(Entry<Long, SocketBox> entry : SocketRegistry.getInstance().getRegistry().entrySet()){
				    connectedProcesses.add(entry.getKey()); // building array with known UUID
				}
				// reply back with the list of connected processes
				JsonObject DISCOVERRESPONSEmessage = new JsonObject();
				DISCOVERRESPONSEmessage.add("MSGTYPE", MessageType.DISCOVERRESPONSE.toString());
				DISCOVERRESPONSEmessage.add("CPLIST", connectedProcesses);
			    
				BufferedWriter tmpWriter = socket.getOutputStream();
				tmpWriter.write(DISCOVERRESPONSEmessage.toString());
				tmpWriter.newLine();
				tmpWriter.flush();			    
			    }
			}
			else{ // route the message to client
			    if(JSONmessage.get("FORWARDTYPE").asString().equals(ForwardType.BROADCAST.toString())){ // append automatically my Message.getJSON();
			        for(SocketBox socketBroadcast : SocketRegistry.getInstance().getRegistry().values()){
			            BufferedWriter tmpWriter = socketBroadcast.getOutputStream();
				    tmpWriter.write(message);
				    tmpWriter.newLine();
				    tmpWriter.flush();
			        }
			    }else{ // unicast transmission
				Long UUIDreceiver = JSONmessage.get("RECIPIENTID").asLong();
				// get the socket binded to this UUID
				SocketBox receiverSocket = SocketRegistry.getInstance().getRegistry().get(UUIDreceiver);
				// sending message...
				BufferedWriter receiverWriter = receiverSocket.getOutputStream();
				receiverWriter.write(message);
				receiverWriter.newLine();
				receiverWriter.flush();
			    }
			}
		    }
		}
		catch (Exception e) {
		    System.out.println("Error " + e.getMessage());
		    e.printStackTrace();
		}

	    }
	    
	    try{
		Thread.sleep(10); // avoid burning the CPU
	    }catch(Exception e){

	    }
	}
    }
   
}
