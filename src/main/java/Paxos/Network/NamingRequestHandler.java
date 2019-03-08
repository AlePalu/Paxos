package Paxos.Network;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.net.Inet4Address;
import java.net.Socket;
import java.io.PrintWriter;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

public class NamingRequestHandler implements Runnable{

    File processesOnNetworkFile;
    BufferedReader buffFileReader;
    BufferedWriter buffFileWriter;
    FileReader fileReader;
    SocketBox socketBox;
    JsonArray tmpJsonArray;


    RandomAccessFile file;
    
    public NamingRequestHandler(String ip, int port){
	this.processesOnNetworkFile = new File("processList.txt");
	
	try{
	    // create file if not exists
	    this.processesOnNetworkFile.createNewFile();
	    
	    Socket connSocket = new Socket(ip, port); // connecting to NetworkInfrastructure
	    this.socketBox = new SocketBox(connSocket);

	    // subscribe the naming service to the local network infrastucture
	    JsonObject NAMINGSUBSCRIBEmessage = new JsonObject();
	    NAMINGSUBSCRIBEmessage.add("MSGTYPE", MessageType.NAMINGSUBSCRIBE.toString());
	    this.socketBox.sendOut(NAMINGSUBSCRIBEmessage.toString());

	    // insert the IP of the machine where naming service is running in the available nodes
	    FileWriter fileWriter = new FileWriter(this.processesOnNetworkFile,false);
	    String myIP = Inet4Address.getLocalHost().getHostAddress();
	    fileWriter.write(myIP+"\n");
	    fileWriter.flush();
	    fileWriter.close();
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
			FileWriter fw = new FileWriter(this.processesOnNetworkFile, true);
		   	fw.close();
		    }
		    if(JSONmessage.get("MSGTYPE").asString().equals(MessageType.NAMINGUPDATE.toString())){
			FileWriter fw = new FileWriter(this.processesOnNetworkFile, true);
			// adding new client
			String newName = JSONmessage.get("NAME").asString();
			// write on file
			fw.write(newName+"\n");
			fw.flush();
		        fw.close();
		    }
		}
		Thread.sleep(10);
	    }
       	}catch(Exception e){
	    e.printStackTrace();
	    return;
	}
    }    
}
