package Paxos.Network;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.Inet4Address;
import java.net.Socket;
import java.util.HashSet;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.Json;

public class NamingRequestHandler implements Runnable{

    File nodesOnNetworkFile;
    SocketBox socketBox;
    HashSet<MessageType> messageToProcess;
    Timer timer;

    
    public NamingRequestHandler(String ip, int port){
	this.nodesOnNetworkFile = new File("processList.txt");
	
	try{
	    // create file if not exists
	    this.nodesOnNetworkFile.createNewFile();
	    
	    Socket connSocket = new Socket(ip, port); // connecting to NetworkInfrastructure
	    this.socketBox = new SocketBox(connSocket);

	    // subscribe the naming service to the local network infrastucture
	    String NAMINGSUBSCRIBEmessage = MessageForgery.forgeNAMINGSUBSCRIBE();
	    this.socketBox.sendOut(NAMINGSUBSCRIBEmessage);

	    // insert the IP of the machine where naming service is running in the available nodes
	    String myIP = Inet4Address.getLocalHost().getHostAddress();
	    recordName(myIP);

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
			Tracker.getInstance().issueTicket(entry.getKey(), 5000, Jmessage.get(MessageField.TICKET.toString()).asLong(), MessageType.PING.toString());

			entry.getValue().sendOut(PINGmessage);
			System.out.printf("invio%n");
		    }
		}
	    }, 5000, 5000);
	
	
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
    
    public void recordName(String name){
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
	    String in = name+"\n";
	    fileWriter.write(in);
	    fileWriter.flush();
        }catch(Exception e){
	    return;
	}
    }
}
