package Paxos.Network;

import Paxos.Network.SocketBox;
import Paxos.Network.MessageType;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Random;

import java.util.Timer;
import java.util.TimerTask;

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

    public Timer timer;
    
    // required by naming server fault detection
    public Boolean nameFault;
    
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

		// message to process
		this.messageToProcess = new HashSet<MessageType>();
		this.messageToProcess.add(MessageType.DISCOVERREPLY);
		this.messageToProcess.add(MessageType.NAMINGREPLY);
		this.messageToProcess.add(MessageType.PING);
		this.messageToProcess.add(MessageType.SIGUNLOCK);
		this.messageToProcess.add(MessageType.SUBSCRIBE); // get my machineUUID from here
		this.messageToProcess.add(MessageType.COORD);
		
		// subscribe the process to the list of connected processes
		String SUBSCRIBEmessage = MessageForgery.forgeSUBSCRIBE();
		this.sendMessage(SUBSCRIBEmessage);
		
		this.nameFault = false;
    }

    public void run(){
	String message;
	boolean match = false;

	while(true){
	    try {
		if(!this.outboundQueue.isEmpty()){ // OUT
		    // automatically add my UUID
		    String outboundMessage = outboundQueue.remove();
		    JsonObject outboundJSONMessage = Json.parse(outboundMessage).asObject();
		    outboundJSONMessage.add(MessageField.SENDERID.toString(), this.UUID);

		    // send message on socket
		    this.socketBox.sendOut(outboundJSONMessage.toString());
		}

		if(this.socketBox.getInputStream().ready()){ // IN
		    // take the message
		    message = this.socketBox.getInputStream().readLine();

		    for(MessageType msgType : this.messageToProcess){
				if(msgType.match(message)){
				    msgType.applyLogic(this, message);
				    match = true;
				}
		    }
		    if(!match) {
			this.inboundQueue.add(message);
		    }
		    match = false;
		}
		Thread.sleep(50); // avoid burning CPU
	    }
	    catch (Exception e) {
		e.printStackTrace();
	    }

	}
    }
    
    public void sendMessage(String message){
	this.outboundQueue.add(message);
    }

    public String receiveMessage(){
	return this.inboundQueue.poll();
    }

    public boolean isThereAnyMessage(){
	return !this.inboundQueue.isEmpty();
    }

    public ArrayList<Long> lookupConnectedProcesses(){
	return this.connectedProcesses;
    }

    // this force sending of DISCOVER message. Be carefull this call blocks the caller until the DISCOVERRESPONSE has been processed
    public void updateConnectedProcessesList() throws InterruptedException{
	System.out.printf("[NetworkStack]: performing naming update...\n");
	// force update of known node on network
	String NAMINGREQUESTmessage = MessageForgery.forgeNAMINGREQUEST();
       	this.sendMessage(NAMINGREQUESTmessage);

	// wait for naming processing...
	
	// set up a sentinel to eventually unlock the process in case of race conditions
	this.timer = new Timer();
	try{
	    timer.scheduleAtFixedRate(new TimerTask(){
		    public void run(){
			if(nameFault){
			    lock.lock();
			    namingLock.signalAll();
			    lock.unlock();
			}
		    }
	    },1000,1000);
	}catch(Exception e){
	    // timer was already canceled when delay elapsed, simply ignore this exception
	}
	lock.lock();
	try {
	    namingLock.await();
	}finally{
	    lock.unlock();
	}

	// delete the timer
	timer.cancel();
	timer.purge();
	
        if(!nameFault){
	    System.out.printf("[NetworkStack]: discovering currently connected processes on network...\n");
	    // reset the current list of known processes
	    this.connectedProcesses.clear();

	    String DISCOVERREQUESTmessage = MessageForgery.forgeDISCOVERREQUEST();
	    this.sendMessage(DISCOVERREQUESTmessage);

	    // waiting for processes discovering...
	    // set up a sentinel to eventually unlock the process in case of race conditions
	    this.timer = new Timer();
	    try{
		timer.scheduleAtFixedRate(new TimerTask(){
			public void run(){
			    if(nameFault){
				lock.lock();
				discoverMessageLock.signalAll();
				lock.unlock();
			    }
			}
		    },1000,1000);
		// what to do in the unlucky case where no process has discovered name server was death??
		timer.schedule(new TimerTask(){
			public void run(){
			    lock.lock();
			    // unlock the process
			    discoverMessageLock.signalAll();
			    lock.unlock();
			    System.out.printf("[NetworkStack]: got stucked while discovering processes, maybe something was wrong...%n");
			}
		    }, 7000);
	    }catch(Exception e){
		// timer was already canceled when delay elapsed, simply ignore this exception
	    }
	    
	    lock.lock();
	    try{
		discoverMessageLock.await();
	    }finally{
		lock.unlock();
	    }

	    // delete the timer
	    timer.cancel();
	    timer.purge();
	}
	if(nameFault){
	    /* name server fault.
	       Possible naming failures are handled by a bully election mechanism by which at the end a new name server is elected. 
	       Since election requires to elect a new physical node where run a new name server, the local process does not handle the election, but simply signals to its network infrastructure to start it. */

	    System.out.printf("[NetworkStack]: name server fault! starting election of new name server...\n");
	    
	    String BULLYREQUESTmessage = MessageForgery.forgeBULLYREQUEST();
	    this.sendMessage(BULLYREQUESTmessage);

	    // waiting system to be recovered
	    lock.lock();
	    try {
		namingLock.await();
	    }finally{
		lock.unlock();
	    }
	    
	}
    }

    public HashSet<String> getPendingDiscoverList(){
	return this.pendingDISCOVERREPLY;
    }
    
    public long getProcessUUID(){
	return this.UUID;
    }

    public SocketBox getSocketBox(){
	return this.socketBox;
    }
}
