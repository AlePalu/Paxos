package Paxos.Network;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.Socket;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

public class NamingRequestHandler implements Runnable{

    File processesOnNetworkFile;
    BufferedReader fileReader;
    BufferedWriter fileWriter;
    SocketBox socketBox;

    public NamingRequestHandler(String ip, int port){
	this.processesOnNetworkFile = new File("processList.txt");
	try{
	    // create file if not exists
	    this.processesOnNetworkFile.createNewFile();
	    
	    Socket connSocket = new Socket(ip, port); // connecting to NetworkInfrastructure
	    this.socketBox = new SocketBox(connSocket);

	    // keep track of naming service socket
	    SocketRegistry.getInstance().setNamingSocket(this.socketBox);
	    
	    // open file output and input stream 
	    this.fileReader = new BufferedReader(new FileReader(this.processesOnNetworkFile));
	    this.fileWriter = new BufferedWriter(new FileWriter(this.processesOnNetworkFile));
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
	    if(this.socketBox.getInputStream().ready()){
		message = this.socketBox.getInputStream().readLine();

		JsonObject JSONmessage = Json.parse(message).asObject();

		if(JSONmessage.get("MSGTYPE").asString().equals(MessageType.NAMINGREQUEST.toString())){
		    // respond to naming request
		    JsonArray processesOnNetwork = Json.parse(fileReader).asArray();

		    // send message back
		    BufferedWriter tmpWriter = this.socketBox.getOutputStream();
		    tmpWriter.write(processesOnNetwork.toString());
		    tmpWriter.newLine();
		    tmpWriter.flush();			
		}
		if(JSONmessage.get("MSGTYPE").asString().equals(MessageType.NAMINGSUBSCRIBE.toString())){
		    JsonArray processesOnNetwork = Json.parse(fileReader).asArray();
		    // adding new client
		    processesOnNetwork.add(JSONmessage.get("NAME").asString());

		    // write on file
		    this.fileWriter.write(processesOnNetwork.toString());
	        }
	    }
	
	    // close connection
	    this.socketBox.close();
	}catch(Exception e){
	    return;
	}
    }
}
