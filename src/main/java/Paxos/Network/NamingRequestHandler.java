package Paxos.Network;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.Inet4Address;
import java.net.Socket;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

public class NamingRequestHandler implements Runnable{

    File nodesOnNetworkFile;
    SocketBox socketBox;
    
    public NamingRequestHandler(String ip, int port){
	this.nodesOnNetworkFile = new File("processList.txt");
	
	try{
	    // create file if not exists
	    this.nodesOnNetworkFile.createNewFile();
	    
	    Socket connSocket = new Socket(ip, port); // connecting to NetworkInfrastructure
	    this.socketBox = new SocketBox(connSocket);

	    // subscribe the naming service to the local network infrastucture
	    JsonObject NAMINGSUBSCRIBEmessage = new JsonObject();
	    NAMINGSUBSCRIBEmessage.add("MSGTYPE", MessageType.NAMINGSUBSCRIBE.toString());
	    this.socketBox.sendOut(NAMINGSUBSCRIBEmessage.toString());

	    // insert the IP of the machine where naming service is running in the available nodes
	    String myIP = Inet4Address.getLocalHost().getHostAddress();
	    recordName(myIP);
	}
	catch (Exception e) {
	    System.out.println("Error " + e.getMessage());
	    e.printStackTrace();
	}
	
	System.out.printf("[NamingRequestHandler]: Naming server READY%n");
    }

    public void run(){
	String message;
	// handling messages...
	try{	    
	    while(true){
		if(this.socketBox.getInputStream().ready()){
		    message = this.socketBox.getInputStream().readLine();
		    JsonObject JSONmessage = Json.parse(message).asObject();
	    
		    if(JSONmessage.get("MSGTYPE").asString().equals(MessageType.NAMINGREQUEST.toString())){
			JsonArray nodesList = new JsonArray();
			try(BufferedReader reader = new BufferedReader(new FileReader(this.nodesOnNetworkFile))){
			    String ip = reader.readLine();
			    while(ip != null){
				nodesList.add(ip);
			        ip = reader.readLine();
			    }
			}

			// send response back
			JsonObject NAMINGREPLYmessage = new JsonObject();
			NAMINGREPLYmessage.add("MSGTYPE", MessageType.NAMINGREPLY.toString());
			NAMINGREPLYmessage.add("NODELIST", nodesList);
			NAMINGREPLYmessage.add("RECIPIENTID", JSONmessage.get("SENDERID").asLong());
			NAMINGREPLYmessage.add("NAME", JSONmessage.get("NAME").asString());
			
			this.socketBox.sendOut(NAMINGREPLYmessage.toString());
		    }
		    if(JSONmessage.get("MSGTYPE").asString().equals(MessageType.NAMINGUPDATE.toString())){
			String newName = JSONmessage.get("NAME").asString();
			// add node to the list of available nodes
			recordName(newName);
		    }
		}
		Thread.sleep(10);
	    }
       	}catch(Exception e){
	    e.printStackTrace();
	    return;
	}
    }

    // REFACTORIZZARE
    private void recordName(String name){
	try(BufferedReader reader = new BufferedReader(new FileReader(this.nodesOnNetworkFile))){
	    String ip = reader.readLine();
	    while(ip != null){
		if(name.equals(ip)) // name already present
		    return;
		
		ip = reader.readLine();
	    }
	}catch(Exception e){

	}
	try(FileWriter fileWriter = new FileWriter(this.nodesOnNetworkFile, true)){
	    String in = name+"\n";
	    fileWriter.write(in);
	    fileWriter.flush();
	    fileWriter.close();
        }catch(Exception e){
	    
	}
    }
}
