package Paxos.Network;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.Inet4Address;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.JsonArray;


public class NamingRequestHandler implements Runnable{

    File nodesOnNetworkFile;
    SocketBox socketBox;
    HashSet<MessageType> messageToProcess;
    Timer timer;

    public NamingRequestHandler(String ip, int port, long UUID){
	this.nodesOnNetworkFile = new File("processList.txt");
	
	try{
	    // create file if not exists
	    this.nodesOnNetworkFile.delete();
	    this.nodesOnNetworkFile.createNewFile();
	    
	    Socket connSocket = new Socket(ip, port); // connecting to NetworkInfrastructure
	    this.socketBox = new SocketBox(connSocket);
	    this.socketBox.setUUID(SocketRegistry.getInstance().getMachineUUID());

	    // subscribe the naming service to the local network infrastucture
	    String NAMINGSUBSCRIBEmessage = MessageForgery.forgeNAMINGSUBSCRIBE();
	    this.socketBox.sendOut(NAMINGSUBSCRIBEmessage);

	    // insert the IP of the machine where naming service is running in the available nodes
	    String myIP = Inet4Address.getLocalHost().getHostAddress();
	    recordName(myIP, SocketRegistry.getInstance().getMachineUUID());

	    // populate the set with messages name server has to process
	    messageToProcess = new HashSet<MessageType>();
	    messageToProcess.add(MessageType.NAMINGREQUEST);
	    messageToProcess.add(MessageType.NAMINGUPDATE);
	    
	}
	catch (Exception e) {
	    System.out.println("Error " + e.getMessage());
	    e.printStackTrace();
	}
	
	System.out.printf("[NamingRequestHandler]: Naming server READY%n");

	// keep track of nodes still alive (only the name server should issue ping packets for naming purposes)...
	this.timer = new Timer();
	timer.schedule(new TimerTask(){
		public void run(){
		    for(Entry<String, SocketBox> entry : SocketRegistry.getInstance().getRemoteNodeRegistry().entrySet()){
			String PINGmessage = MessageForgery.forgePING();

			// parse the message to get the ticket identifier
			JsonObject Jmessage = Json.parse(PINGmessage).asObject();
			// process considered alive if response arrives in at most 5 seconds
			Tracker.getInstance().issueTicket(entry.getKey(), 5000, Jmessage.get(MessageField.TICKET.toString()).asLong(), TicketType.PING.toString());

			entry.getValue().sendOut(PINGmessage);
		    }		    
		
		    for(Entry<String, CopyOnWriteArrayList<Ticket>> entry : Tracker.getInstance().getNamingTickets().entrySet()){
			for(Ticket t : entry.getValue()){
			    if(Tracker.getInstance().isExpired(t) && t.ticketType.equals(MessageType.PING.toString())){
				System.out.printf("[Tracker]: I was not able to receive any response from remote node "+entry.getKey()+". Removing any reference to it.%n");	       
				
				// removing the association from socket registry
				SocketRegistry.getInstance().getRemoteNodeRegistry().get(entry.getKey()).close();
				SocketRegistry.getInstance().getRemoteNodeRegistry().remove(entry.getKey());
				
				// remove the name from list of known hosts
				removeName(entry.getKey());
				
				// remove any ticket associated with it
				Tracker.getInstance().getNamingTickets().remove(entry.getKey());
				
				// send a SIGUNLOCK in broadcast
				String SIGUNLOCKmessage = MessageForgery.forgeSIGUNLOCK(ForwardType.BROADCAST, null);
				getSocketBox().sendOut(SIGUNLOCKmessage);
			    }
			}
		    }
		}
	    }, 4000, 4000);	
	
    }

    public void run(){
	String message;
	try{	    
	    while(true){
		if(this.socketBox.getInputStream().ready()){
		    message = this.socketBox.getInputStream().readLine();

		    // handle messages...
		    for(MessageType msg : this.messageToProcess){	
			if(msg.match(message))
			    msg.applyLogic(this, message);
		    }
		}
		Thread.sleep(10);
	    }
       	}catch(Exception e){
	    e.printStackTrace();
	    return;
	}
    }

    public File getNodeFile(){
	return this.nodesOnNetworkFile;
    }

    public SocketBox getSocketBox(){
	return this.socketBox;
    }
    
    public void recordName(String name, long machineUUID){
	try(BufferedReader reader = new BufferedReader(new FileReader(this.nodesOnNetworkFile))){
	    String ip = reader.readLine();
	    while(ip != null){
		if(name.equals(ip)) // name already present
		    return;		
		ip = reader.readLine();
	    }
	}catch(Exception e){
	    return;
	}
	try(FileWriter fileWriter = new FileWriter(this.nodesOnNetworkFile, true)){
	    Long UUID = new Long(machineUUID);
	    String in = name+","+UUID.toString()+"\n";
	    fileWriter.write(in);
	    fileWriter.flush();
        }catch(Exception e){
	    return;
	}
    }

    public void removeName(String name){
	ArrayList<String> tmp = new ArrayList<String>();
	
	try(BufferedReader reader = new BufferedReader(new FileReader(this.nodesOnNetworkFile))){
	    String ip = reader.readLine();
	    while(ip != null){
		String[] ipField = ip.split(",");
		if(!ipField[0].equals(name))
		    tmp.add(ip);

		ip = reader.readLine();
	    }
	}catch(Exception e){
	    return;
	}
	try(FileWriter fileWriter = new FileWriter(this.nodesOnNetworkFile, false)){
	    for(String s : tmp){
		fileWriter.write(s+"\n");
		fileWriter.flush();
	    }
	}catch(Exception e){
	    return;
	}
    }

    public void sendCOORD(){
	String COORDmessage = MessageForgery.forgeCOORD();
	MessageType.forwardTo(this.socketBox, COORDmessage);
	System.out.printf("[NamingRequestHandler]: Broadcasted name server is here%n");
    }

    public void recoverProcessList(){
	// read recovery file
	File recoveryFile = new File("lastProcessStatus.txt");

	try(BufferedReader reader = new BufferedReader(new FileReader(recoveryFile))){	    
	    // clear previously existent file
	    this.nodesOnNetworkFile.delete();
	    this.nodesOnNetworkFile.createNewFile();

	    String procList = reader.readLine();
	    JsonArray procArray = Json.parse(procList).asArray();
	    for(JsonValue entry : procArray){
		JsonObject entryObj = entry.asObject();
		String name = entryObj.get("IP").asString();
		Long UUID = new Long(entryObj.get("UUID").asString());

		recordName(name, UUID);
	    }
		
	}catch(Exception e){
	    e.printStackTrace();
	    return;
	}

	
	System.out.printf("[NamingRequestHandler]: recovered process list according to last NAMINGREPLY received.%n");

    }
}
