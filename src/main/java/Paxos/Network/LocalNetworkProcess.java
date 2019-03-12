package Paxos.Network;

import Paxos.Network.SocketBox;
import Paxos.Network.MessageType;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Map.Entry;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// interface used by processes to communicate with the main NetworkManager server (in a client-server fashion)
public class LocalNetworkProcess implements Runnable, NetworkInterface{

    private SocketBox socketBox;
    private ConcurrentLinkedQueue<String> inboundQueue;
    private ConcurrentLinkedQueue<String> outboundQueue;
    private long UUID;
    
    private ArrayList<Long> connectedProcesses;

    // used for DISCOVER messages
    private Set<String> pendingDISCOVERREPLY;
    
    private final Lock lock;
    private final Condition discoverMessageLock;
    private final Condition namingLock;
    
    public LocalNetworkProcess(String ip, int port, long UUID) throws IOException{
		Socket processSocket = new Socket(ip, port); // connect to NetworkManager server
		this.socketBox = new SocketBox(processSocket);

		// initialize internal state
		this.UUID = UUID;
		this.inboundQueue = new ConcurrentLinkedQueue<String>();
		this.outboundQueue = new ConcurrentLinkedQueue<String>();
		this.connectedProcesses = new ArrayList<Long>();

		this.lock = new ReentrantLock();
		this.discoverMessageLock = lock.newCondition();
		this.namingLock = lock.newCondition();
		
		// subscribe the process to the list of connected processes
		JsonObject SUBSCRIBEmessage = new JsonObject();
		//SUBSCRIBEmessage.add("SENDERID", UUID); // needed to bind my socketBox to my UUID
		SUBSCRIBEmessage.add("MSGTYPE", MessageType.SUBSCRIBE.toString());
		this.sendMessage(SUBSCRIBEmessage.toString());

		this.pendingDISCOVERREPLY = new HashSet<String>();
    }

    public void run(){
	String message;
	
	while(true){
	    try {
	        // handling messages...

		if(!this.outboundQueue.isEmpty()){ // OUT
		    // automatically add my UUID
		    String outboundMessage = outboundQueue.remove();
		    JsonObject outboundJSONMessage = Json.parse(outboundMessage).asObject();
		    outboundJSONMessage.add("SENDERID", this.UUID);
 
		    // send message on socket
		    this.socketBox.sendOut(outboundJSONMessage.toString());
		    System.out.printf("[OUT]: "+outboundMessage+" sent to "+this.socketBox.getSocket().getPort()+" [local netwrok server port]%n");
		}
		
		if(this.socketBox.getInputStream().ready()){ // IN
		    // take the message
		    message = this.socketBox.getInputStream().readLine();

		    System.out.printf("[IN ]: "+message+"\n");

		    // handle DISCOVERRESPONSE immediately
		    JsonObject jsonMsg = Json.parse(message).asObject();
		    if(jsonMsg.get("MSGTYPE").asString().equals(MessageType.DISCOVERRESPONSE.toString())){
		        // get the list of UUID
			for(JsonValue UUID : jsonMsg.get("CPLIST").asArray()){
			    this.connectedProcesses.add(UUID.asLong());
			}
			this.pendingDISCOVERREPLY.remove(jsonMsg.get("NAME").asString());
			
			// signal that all DISCOVERREPLY have been processed
			if(this.pendingDISCOVERREPLY.isEmpty()){
			    lock.lock();
			    discoverMessageLock.signalAll();
			    lock.unlock();
			}
		    }else if(jsonMsg.get("MSGTYPE").asString().equals(MessageType.NAMINGREPLY.toString())){
			this.pendingDISCOVERREPLY.clear();
			// keep track of the DISCOVERREPLY we must wait for
			for(JsonValue ip : jsonMsg.get("NODELIST").asArray()){
			    this.pendingDISCOVERREPLY.add(ip.asString());
			}
			// signal naming is updated
			lock.lock();
			namingLock.signalAll();
			lock.unlock();
		    }
		    else{
			this.inboundQueue.add(message);
		    }
		}

		Thread.sleep(10); // avoid burning CPU
	    }
	    catch (Exception e) {
		System.out.println("Error " + e.getMessage());
		e.printStackTrace();
	    }

	}
    }

    public void sendMessage(String message){
	this.outboundQueue.add(message);
    }

    public String receiveMessage(){
	return this.inboundQueue.remove();
    }

    public Boolean isThereAnyMessage(){
	return !this.inboundQueue.isEmpty();
    }

    public ArrayList<Long> lookupConnectedProcesses(){
	return this.connectedProcesses;
    }

    // this force sending of DISCOVER message. Be carefull this call blocks the caller until the DISCOVERRESPONSE has been processed
    public void updateConnectedProcessesList() throws InterruptedException{
	// force update of known node on network
	JsonObject NAMINGREQUESTmessage = new JsonObject();
	NAMINGREQUESTmessage.add("MSGTYPE", MessageType.NAMINGREQUEST.toString());
	this.sendMessage(NAMINGREQUESTmessage.toString());

	// wait for naming processing...
	lock.lock();
	try {
	    namingLock.await();
	}finally{
	    lock.unlock();
	}

	// reset the current list of known processes
	this.connectedProcesses.clear();
	
	JsonObject DISCOVERREQUESTmessage = new JsonObject();
	DISCOVERREQUESTmessage.add("MSGTYPE", MessageType.DISCOVERREQUEST.toString());

	this.sendMessage(DISCOVERREQUESTmessage.toString());

	System.out.printf("must wait for: "+this.pendingDISCOVERREPLY.toString()+"%n");
	
	// wait for disover processing...
	lock.lock();
	try{
	    discoverMessageLock.await();
	}finally{
	    lock.unlock();
	}
    }
}
