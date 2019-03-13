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

    // pure network related stuffs
    private SocketBox socketBox;
    private ConcurrentLinkedQueue<String> inboundQueue;
    private ConcurrentLinkedQueue<String> outboundQueue;
    private long UUID;
    private HashSet<MessageType> messageToProcess;
    
    // list of connected processes on network, updated by DISCOVER mechanism
    private ArrayList<Long> connectedProcesses;

    // required by DISCOVER mechanism
    private HashSet<String> pendingDISCOVERREPLY;    
    public final Lock lock;
    public final Condition discoverMessageLock;
    public final Condition namingLock;
    
    public LocalNetworkProcess(String ip, int port, long UUID) throws IOException{
		Socket processSocket = new Socket(ip, port); // connect to network infrastructure
		this.socketBox = new SocketBox(processSocket);

		// initialize internal state
		this.UUID = UUID;
		this.inboundQueue = new ConcurrentLinkedQueue<String>();
		this.outboundQueue = new ConcurrentLinkedQueue<String>();
		this.connectedProcesses = new ArrayList<Long>();
		this.lock = new ReentrantLock();
		this.discoverMessageLock = lock.newCondition();
		this.namingLock = lock.newCondition();
	
		this.pendingDISCOVERREPLY = new HashSet<String>();
		
		this.messageToProcess = new HashSet<MessageType>();
		this.messageToProcess.add(MessageType.DISCOVERREPLY);
		this.messageToProcess.add(MessageType.NAMINGREPLY);
		
		// subscribe the process to the list of connected processes
		String SUBSCRIBEmessage = MessageForgery.forgeSUBSCRIBE();
		this.sendMessage(SUBSCRIBEmessage);

    }

    public void run(){
	String message;
	
	while(true){
	    try {
		if(!this.outboundQueue.isEmpty()){ // OUT
		    // automatically add my UUID
		    String outboundMessage = outboundQueue.remove();
		    JsonObject outboundJSONMessage = Json.parse(outboundMessage).asObject();
		    outboundJSONMessage.add(MessageField.SENDERID.toString(), this.UUID);
 
		    // send message on socket
		    this.socketBox.sendOut(outboundJSONMessage.toString());
		    System.out.printf("[OUT]: "+outboundMessage+" sent to "+this.socketBox.getSocket().getPort()+" [local netwrok server port]%n");
		}
		
		if(this.socketBox.getInputStream().ready()){ // IN
		    // take the message
		    message = this.socketBox.getInputStream().readLine();

		    System.out.printf("[IN ]: "+message+"\n");

		    for(MessageType msgType : this.messageToProcess){
			if(msgType.match(message))
			    msgType.applyLogic(this, message);
			else
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
	String NAMINGREQUESTmessage = MessageForgery.forgeNAMINGREQUEST();
       	this.sendMessage(NAMINGREQUESTmessage);
	// wait for naming processing...
	lock.lock();
	try {
	    namingLock.await();
	}finally{
	    lock.unlock();
	}

	// reset the current list of known processes
	this.connectedProcesses.clear();
	
	String DISCOVERREQUESTmessage = MessageForgery.forgeDISCOVERREQUEST();
	this.sendMessage(DISCOVERREQUESTmessage);
	// wait for disover processing...
	lock.lock();
	try{
	    discoverMessageLock.await();
	}finally{
	    lock.unlock();
	}
    }


    public HashSet<String> getPendingDiscoverList(){
	return this.pendingDISCOVERREPLY;
    }
}
