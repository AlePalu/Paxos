package Paxos.Network;

import java.io.PrintWriter;
import java.util.Scanner;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import java.util.Map.Entry;


// handle NetworkManager queues, sending and receiving messages to remote clients
class TrafficHandler implements Runnable{

    public void run(){
	System.out.printf("queue handler is ready\n");
	while(true){
	    for(SocketBox socket : SocketRegistry.getInstance().getAllSockets()){
		try {
		    if(socket.getSocket().getInputStream().available() != 0){ // there are data ready to be readden
			// take message
			Scanner tmpScanner = socket.getInputStream();
			String message = tmpScanner.nextLine();

			JsonObject JSONmessage = Json.parse(message).asObject();
			// route the message on the base of the UUID contained in the message
			Long UUID = JSONmessage.get("SENDERID").asLong();
			
			// is this a SUBSCRIBE message?
			if(JSONmessage.get("MSGTYPE").asString().equals(MessageType.SUBSCRIBE.toString())){
			    // SUBSCRIBE messages can be processed only once
			    if(!SocketRegistry.getInstance().getRegistry().values().contains(socket)){
				// process the message immediately
				SocketRegistry.getInstance().addElement(UUID, socket); // bind this socketBox to the UUID
				SocketRegistry.getInstance().getPendingSockets().remove(socket);

				System.out.printf("SUBSRCRIBE message received with UUID: "+UUID+"%n");
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
			    
			    PrintWriter tmpPrintWriter = socket.getOutputStream();
			    tmpPrintWriter.println(DISCOVERRESPONSEmessage.toString());
			    tmpPrintWriter.flush();			    
			}
			else{// route the message
			    if(JSONmessage.get("FORWARDTYPE").asString().equals(MessageType.BROADCAST.toString())){
				System.out.printf("broadcasting message...%n");
				for(SocketBox socketBroadcast : SocketRegistry.getInstance().getRegistry().values()){
				    if(!socketBroadcast.equals(socket)){ // not send back to the sending socket
					PrintWriter tmpPrintWriter = socketBroadcast.getOutputStream();
					tmpPrintWriter.println(message);
					tmpPrintWriter.flush();
				    }
				}
			    }else{ // unicast transmission
				Long UUIDreceiver = JSONmessage.get("RECEIVERID").asLong();
				// get the socket binded to this UUID
				SocketBox receiverSocket = SocketRegistry.getInstance().getRegistry().get(UUIDreceiver);
				// sending message...
				PrintWriter receiverPrintWriter = receiverSocket.getOutputStream();
				receiverPrintWriter.println(message);
				receiverPrintWriter.flush();
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
