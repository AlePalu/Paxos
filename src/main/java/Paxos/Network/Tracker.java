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

import java.net.Inet4Address;

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

enum TicketType{    
    NAMING("NAMING", (o) -> {
	    Ticket t = (Ticket) o[0];
	    Entry<Long, CopyOnWriteArrayList<Ticket>> entry = (Entry<Long, CopyOnWriteArrayList<Ticket>>) o[1];
	    System.out.printf("[Tracker]: No response received from the naming server, considered it death.\n");

	    // removing association with naming service
	    if(SocketRegistry.getInstance().getNamingSocket() != null)
		SocketRegistry.getInstance().getNamingSocket().close();
	    SocketRegistry.getInstance().setNamingSocket(null);
	    
	    // remove any ticket associated with it
	    Tracker.getInstance().getTrackingList().remove(entry.getKey());
				    
	    // if something was wrong in the naming request, the client is still waiting for a response. Unlock it
	    String SIGUNLOCKmessage = MessageForgery.forgeSIGUNLOCK(ForwardType.BROADCAST, "NAMEFAULT");
	    t.socket.sendOut(SIGUNLOCKmessage);
	    // now is responsability of the processes to perform the election...
	}),
    ELECT("ELECT", (o) -> {
	    if(SocketRegistry.getInstance().getNamingSocket() == null){ // only if there is NOT an already running name server
		System.out.printf("[Tracker]: No BULLYSUPPRESS message received. Broadcasting this node is the new name server%n");
		System.out.printf("[Tracker]: Starting new name server%n");

		try{
		    // reallow all traffic
		    TrafficHandler.getInstance().allowAll();
					
		    NamingRequestHandler namingHandler = new NamingRequestHandler(Inet4Address.getLocalHost().getHostAddress(), 40000, SocketRegistry.getInstance().getMachineUUID());
		    Thread namingThread = new Thread(namingHandler);
		    namingThread.start();

		    namingHandler.recoverProcessList();

		    // Sending COORD message
		    namingHandler.sendCOORD();
					
		    Tracker.getInstance().removeTicket("ELECT");
		}catch(Exception e){
		    return;
	    }
	    }
	}),
    PING("PING", (o) -> {
	    Entry<Long, CopyOnWriteArrayList<Ticket>> entry = (Entry<Long, CopyOnWriteArrayList<Ticket>>) o[1];
	    System.out.printf("[Tracker]: I was not able to receive any response from "+entry.getKey()+". Removing any reference to it.%n");

	    // removing the association from socket registry
	    SocketRegistry.getInstance().getRegistry().get(entry.getKey()).close();
	    SocketRegistry.getInstance().getRegistry().remove(entry.getKey());
	    SocketRegistry.getInstance().getLocalUUID().remove(entry.getKey());

	    // remove any ticket associated with it
	    Tracker.getInstance().getTrackingList().remove(entry.getKey());	    
	}),
    DISCOVER("DISCOVER", (o) -> {
	    Ticket t = (Ticket) o[0];
	    Entry<Long, CopyOnWriteArrayList<Ticket>> entry = (Entry<Long, CopyOnWriteArrayList<Ticket>>) o[1];
	    
	    Tracker.getInstance().removeTicket("DISCOVER");
	    
	    // if something was wrong in the naming request, the client is still waiting for a response. Unlock it
	    String SIGUNLOCKmessage = MessageForgery.forgeSIGUNLOCK(ForwardType.UNICAST, "NAMEFAULT");
	    MessageType.forwardTo(t.socket, SIGUNLOCKmessage);
	    
	}),
    NAMINGDISCOVER("NAMINGDISCOVER", (o) -> {
	    // start naming service
	    try{
		NamingRequestHandler namingHandler = new NamingRequestHandler(Inet4Address.getLocalHost().getHostAddress(), 40000, SocketRegistry.getInstance().getMachineUUID());
		Thread namingThread = new Thread(namingHandler);
		namingThread.start();
	    }catch(Exception e){
		e.printStackTrace();
	    }
	    // remove any pending ticket (if any)
	    Tracker.getInstance().removeTicket(NameProber.getInstance().getUUID(), null, "NAMINGDISCOVER");
	});
    
    private String ticketType;
    private CustomMessageLogic ticketLogic;
    
    private TicketType(String ticketType, CustomMessageLogic rule){
	this.ticketType = ticketType;
	this.ticketLogic = rule;
    }

    public String toString(){
	return this.ticketType;
    }

    public void applyLogic(Object... args){
	this.ticketLogic.applyLogic(args);
    }

    public boolean match(Ticket t){
	return t.ticketType.equals(this.ticketType);
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
				for(TicketType ticketType : TicketType.values()){
				    if(ticketType.match(t))
					ticketType.applyLogic(t, entry);
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
			Tracker.getInstance().issueTicket(entry, 5000, Jmessage.get(MessageField.TICKET.toString()).asLong(), TicketType.PING.toString(), null);
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
    
    public void removeTicket(Long UUID, Long ticketUUID, String ticketType){
	// identify the ticket
	Ticket ticket = null;
	for(Ticket t : this.trackingList.get(UUID)){
		
	    if (ticketUUID == null){ // remove any ticket of this type
		if(t.ticketType.equals(ticketType))
		    this.trackingList.get(UUID).remove(t);
	    }else{
		if(t.UUID.equals(ticketUUID)){
		    ticket = t;
		    this.trackingList.get(UUID).remove(t);
		    break;
		}
	    }
	}
	
	// remove any ticket still present having a timestamp older than this one    
	for(Ticket t : this.trackingList.get(UUID)){
	    if(t.timestamp < ticket.timestamp && t.ticketType.equals(ticketType)){
		this.trackingList.get(UUID).remove(t);
	    }
	}
    }

    public void removeTicket(String nodeUUID, Long ticketUUID, String ticketType){
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
	     if(t.timestamp < ticket.timestamp && t.ticketType.equals(ticketType)){
		 this.nodeList.get(nodeUUID).remove(t);
	     }
	 }
    }

    // remove ticket on the base of its type
    public void removeTicket(String ticketType){
	for(Entry<Long, CopyOnWriteArrayList<Ticket>> entry : trackingList.entrySet()){	
	    for(Ticket t : this.trackingList.get(entry.getKey())){
		if(t.ticketType.equals(ticketType)){
		    this.trackingList.get(entry.getKey()).remove(t);
		}
	    }
	}

	for(Entry<String, CopyOnWriteArrayList<Ticket>> entry : nodeList.entrySet()){	
	    for(Ticket t : this.nodeList.get(entry.getKey())){
		if(t.ticketType.equals(ticketType)){
		    this.nodeList.get(entry.getKey()).remove(t);
		}
	    }
	}
	
    }

    
    
}
