package Paxos.Network;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.Inet4Address;
import java.net.Socket;
import java.util.HashSet;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

public class NamingRequestHandler implements Runnable{

    File nodesOnNetworkFile;
    SocketBox socketBox;
    HashSet<MessageType> messageToProcess;

    
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

	    messageToProcess = new HashSet<MessageType>();
	    messageToProcess.add(MessageType.NAMINGREQUEST);
	    messageToProcess.add(MessageType.NAMINGUPDATE);
	    
	}
	catch (Exception e) {
	    System.out.println("Error " + e.getMessage());
	    e.printStackTrace();
	}
	
	System.out.printf("[NamingRequestHandler]: Naming server READY%n");
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
	    fileWriter.close();
        }catch(Exception e){
	    return;
	}
    }
}
