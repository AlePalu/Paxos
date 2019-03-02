package Paxos.Network;

import Paxos.Network.SocketBox;
import Paxos.Network.MessageType;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.ArrayList;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.io.BufferedWriter;
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

    private final Lock lock;
    private final Condition discoverMessageLock;
    
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

		// subscribe the process to the list of connected processes
		JsonObject SUBSCRIBEmessage = new JsonObject();
		SUBSCRIBEmessage.add("SENDERID", UUID); // needed to bind my socketBox to my UUID
		SUBSCRIBEmessage.add("MSGTYPE", MessageType.SUBSCRIBE.toString());
		this.sendMessage(SUBSCRIBEmessage.toString());

		// just send a DISCOVER to get a first list of connected processes...
		JsonObject DISCOVERmessage = new JsonObject();
		DISCOVERmessage.add("MSGTYPE", MessageType.DISCOVER.toString());
		this.sendMessage(DISCOVERmessage.toString());
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
		    BufferedWriter tmpWriter = this.socketBox.getOutputStream();
		    tmpWriter.write(outboundJSONMessage.toString());
		    tmpWriter.newLine();
		    tmpWriter.flush();
		    System.out.printf("message sent to "+this.socketBox.getSocket().getPort()+" [local netwrok server port]%n");
		}
		
		if(this.socketBox.getInputStream().ready()){ // IN
		    // take the message
		    message = this.socketBox.getInputStream().readLine();
		    // handle DISCOVERRESPONSE message immediately
		    JsonObject jsonMsg = Json.parse(message).asObject();
		    if(jsonMsg.get("MSGTYPE")!=null && jsonMsg.get("MSGTYPE").asString().equals(MessageType.DISCOVERRESPONSE.toString())){
			this.connectedProcesses.clear();
			// get the list of UUID
			for(JsonValue UUID : jsonMsg.get("CPLIST").asArray()){
			    this.connectedProcesses.add(UUID.asLong());
			}

			// signal that DISCOVERMESSAGE has been processed
			lock.lock();
			discoverMessageLock.signalAll();
			lock.unlock();
		    }
		    else{
			System.out.printf("message received: "+message+"%n");
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
	JsonObject DISCOVERmessage = new JsonObject();
	DISCOVERmessage.add("MSGTYPE", MessageType.DISCOVER.toString());
	this.sendMessage(DISCOVERmessage.toString());

	// blocking call
	lock.lock();
	try{
	    discoverMessageLock.await();
	}finally{
	    lock.unlock();
	}
    }
}
