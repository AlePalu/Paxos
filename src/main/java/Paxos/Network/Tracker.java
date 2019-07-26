package Paxos.Network;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.Json;

import Paxos.Network.*;

class Ticket{

    Long timestamp;
    int expirationThreshold;
    Long UUID;
    String namingUUID;
    String ticketType;
    LocalNetworkProcess process;
    SocketBox socket;
    
    public Ticket(Long timestamp, int expirationThreshold, Long UUID, String ticketType, SocketBox socket){
	this.timestamp = timestamp;
	this.expirationThreshold = expirationThreshold;
	this.UUID = UUID;
	this.ticketType = ticketType;
	this.socket = socket;
    }

    // used for name server tickets
    public Ticket(Long timestamp, int expirationThreshold, String namingUUID, String ticketType){
	this.timestamp = timestamp;
	this.expirationThreshold = expirationThreshold;
	this.namingUUID = namingUUID;
	this.ticketType = ticketType;
    } 
}

class Tracker{

    private ConcurrentHashMap<Long, CopyOnWriteArrayList<Ticket>> trackingList;
    private ConcurrentHashMap<String, CopyOnWriteArrayList<Ticket>> nodeList;
    
    private Timer timer;
    
    private static Tracker instance;
    
    private Tracker(int delay){
	this.trackingList = new ConcurrentHashMap<Long, CopyOnWriteArrayList<Ticket>>();
	this.nodeList = new ConcurrentHashMap<String, CopyOnWriteArrayList<Ticket>>();
	
	System.out.printf("[Tracker]: ready to handle periodic events%n");
        
	this.timer = new Timer();

	// check if there is some ticket expired
	timer.scheduleAtFixedRate(new TimerTask(){
		public void run() {
		    for(Entry<Long, CopyOnWriteArrayList<Ticket>> entry : trackingList.entrySet()){
			for(Ticket t : entry.getValue()){
			    if(Tracker.getInstance().isExpired(t)){
				if(t.ticketType.equals(MessageType.PING.toString())){
				    System.out.printf("[Tracker]: I was not able to receive any response from "+entry.getKey()+". Removing any reference to it.%n");

				    // removing the association from socket registry
				    SocketRegistry.getInstance().getRegistry().get(entry.getKey()).close();
				    SocketRegistry.getInstance().getRegistry().remove(entry.getKey());
				    SocketRegistry.getInstance().getLocalUUID().remove(entry.getKey());

				    // remove any ticket associated with it
				    Tracker.getInstance().getTrackingList().remove(entry.getKey());
				}
				// no response received from the naming server, considered it death. Start election of new naming server
				if(t.ticketType.equals(MessageType.NAMINGREQUEST.toString())){ 
				    System.out.printf("[Tracker]: No response received from the naming server, considered it death.");

				    // removing association with naming service
				    SocketRegistry.getInstance().getNamingSocket().close();

				    // remove any ticket associated with it
				    Tracker.getInstance().getTrackingList().remove(entry.getKey());
				    
				    // SIGUNLOCK only unlock processess waiting for some event, add info about the fault
				    JsonObject sigType = new JsonObject();
				    sigType.add("NAMEFAULT", "true");

				    // if something was wrong in the naming request, the client is still waiting for a response. Unlock it
				    MessageForgery.forgeSIGUNLOCK(ForwardType.BROADCAST, sigType);
				    // now is responsability of the processes to perform the election...
				}
				// if this expires, no one have sent me a BULLYSUPPRESS, then I'll be the new name server
				if(t.ticketType.equals(MessageType.ELECT.toString())){
				    
				}
			    }
			}
		    }	    
		}

	    }, delay*100, delay*100);

	// periodically keep track of processes still alive
	timer.scheduleAtFixedRate(new TimerTask(){
		public void run() {
		    // polling local processes...
		    for(Long entry : SocketRegistry.getInstance().getLocalUUID()){
			String PINGmessage = MessageForgery.forgePING(entry);

			// parse the message to get the ticket identifier
			JsonObject Jmessage = Json.parse(PINGmessage).asObject();
			// process considered alive if response arrives in at most 5 seconds
			Tracker.getInstance().issueTicket(entry, 5000, Jmessage.get(MessageField.TICKET.toString()).asLong(), MessageType.PING.toString(), null);
			// send the message
			SocketRegistry.getInstance().getRegistry().get(entry).sendOut(PINGmessage);
		    }
		}
		
	    }, delay*200, delay*200);
	
    }

    public static void init(int delay){
	if(instance == null)
	    instance = new Tracker(delay);
	return;
    }

    public static Tracker getInstance(){
	return instance;
    }
    
    public void issueTicket(Long UUID, int expirationThreshold, Long ticketUUID, String ticketType, SocketBox socket){
	Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
	Ticket newTicket = new Ticket(currentTimestamp.getTime(), expirationThreshold, ticketUUID, ticketType, socket);

	// create a field for this UUID
	if(!trackingList.keySet().contains(UUID))
	    this.trackingList.put(UUID, new CopyOnWriteArrayList<Ticket>());
	
	// keep monitoring this ticket
	try{
	    this.trackingList.get(UUID).add(newTicket);
	}catch(Exception e){
	    e.printStackTrace();
	}
    }

    public void issueTicket(String nameUUID, int expirationThreshold, Long ticketUUID, String ticketType){
	Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
	Ticket newTicket = new Ticket(currentTimestamp.getTime(), expirationThreshold, ticketUUID, ticketType, null);

	// create a field for this node
	if(!nodeList.keySet().contains(nameUUID))
	    this.nodeList.put(nameUUID, new CopyOnWriteArrayList<Ticket>());
	
	// keep monitoring this ticket
	try{
	    this.nodeList.get(nameUUID).add(newTicket);
	}catch(Exception e){
	    e.printStackTrace();
	}
    }

    public ConcurrentHashMap<String, CopyOnWriteArrayList<Ticket>> getNamingTickets(){
	return this.nodeList;
    }

    public ConcurrentHashMap<Long, CopyOnWriteArrayList<Ticket>> getTrackingList(){
	return this.trackingList;
    }
    
    public boolean isExpired(Ticket ticket){
	Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
	Long currentTime = currentTimestamp.getTime();
        return (currentTime - ticket.timestamp) > ticket.expirationThreshold; 
    }

    public void removeTicket(Long UUID, Long ticketUUID){
	// identify the ticket
	Ticket ticket = null;
	for(Ticket t : this.trackingList.get(UUID)){
	    if(t.UUID.equals(ticketUUID)){
		ticket = t;
		this.trackingList.get(UUID).remove(t);
		break;
	    }
	}
	
	// remove any ticket still present having a timestamp older than this one    
	for(Ticket t : this.trackingList.get(UUID)){
	    if(t.timestamp < ticket.timestamp){
		this.trackingList.get(UUID).remove(t);
	    }
	}
    }

     public void removeTicket(String nodeUUID, Long ticketUUID){
	 // identify the ticket
	 Ticket ticket = null;
	 for(Ticket t : this.nodeList.get(nodeUUID)){
	     if(t.UUID.equals(ticketUUID)){
		 ticket = t;
		 this.nodeList.get(nodeUUID).remove(t);
		 break;
	     }
	 }

	 // remove any ticket still present having a timestamp older than this one
	 for(Ticket t : this.nodeList.get(nodeUUID)){
	     if(t.timestamp < ticket.timestamp){
		 this.nodeList.get(nodeUUID).remove(t);
	     }
	 }
    }

    // remove ticket on the base of its type
    public void removeTicket(Long UUID, String ticketType){
	for(Ticket t : this.trackingList.get(UUID)){
	    if(t.ticketType.equals(ticketType)){
		this.trackingList.get(UUID).remove(t);
	    }
	}
    }

    
    
}
