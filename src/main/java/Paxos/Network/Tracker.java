package Paxos.Network;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.Json;

class Ticket{

    Long timestamp;
    int expirationThreshold;
    Long UUID;
    String namingUUID;
    String ticketType;
    
    public Ticket(Long timestamp, int expirationThreshold, Long UUID, String ticketType){
	this.timestamp = timestamp;
	this.expirationThreshold = expirationThreshold;
	this.UUID = UUID;
	this.ticketType = ticketType;
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

    private ConcurrentHashMap<Long, ArrayList<Ticket>> trackingList;
    private ConcurrentHashMap<String, ArrayList<Ticket>> nodeList;
    
    private Timer timer;
    
    private static Tracker instance;
    
    private Tracker(int delay){
	this.trackingList = new ConcurrentHashMap<Long, ArrayList<Ticket>>();
	this.nodeList = new ConcurrentHashMap<String, ArrayList<Ticket>>();
	
	System.out.printf("[Tracker]: ready to handle periodic events%n");
        
	this.timer = new Timer();

	// check if there is some ticket expired
	timer.scheduleAtFixedRate(new TimerTask(){
		public void run() {
		    for(Entry<Long, ArrayList<Ticket>> entry : trackingList.entrySet()){
			for(Ticket t : entry.getValue()){
			    if(Tracker.getInstance().isExpired(t)){
				if(t.ticketType.equals(MessageType.PING.toString())){
				    System.out.printf("[Tracker]: I was not able to receive any response from "+entry.getKey()+". Removing any reference to it.%n");

				    // removing the association from socket registry
				    SocketRegistry.getInstance().getRegistry().get(entry.getKey()).close();
				    SocketRegistry.getInstance().getRegistry().remove(entry.getKey());

				    // remove any ticket associated with it
				    trackingList.remove(entry.getKey());
				}	
			    }
			}
		    }

		    for(Entry<String, ArrayList<Ticket>> entry : nodeList.entrySet()){
			for(Ticket t : entry.getValue()){
			    if(Tracker.getInstance().isExpired(t) && t.ticketType.equals(MessageType.PING.toString())){
				System.out.printf("[Tracker]: I was not able to receive any response from remote node "+entry.getKey()+". Removing any reference to it.%n");
				
				// removing the association from socket registry
				SocketRegistry.getInstance().getRemoteNodeRegistry().get(entry.getKey()).close();
				SocketRegistry.getInstance().getRemoteNodeRegistry().remove(entry.getKey());
				
				// remove any ticket associated with it
				nodeList.remove(entry.getKey());
			    }
			}
		    }
		    
		}

	    },delay*100, delay*100);

	// periodically keep track of processes still alive
	timer.scheduleAtFixedRate(new TimerTask(){
		public void run() {
		    // polling local processes...
		    for(Entry<Long, SocketBox> entry : SocketRegistry.getInstance().getRegistry().entrySet()){
			String PINGmessage = MessageForgery.forgePING(entry.getKey());

			// parse the message to get the ticket identifier
			JsonObject Jmessage = Json.parse(PINGmessage).asObject();
			// process considered alive if response arrives in at most 5 seconds
			Tracker.getInstance().issueTicket(entry.getKey(), 5000, Jmessage.get(MessageField.TICKET.toString()).asLong(), MessageType.PING.toString());
			// send the message
			entry.getValue().sendOut(PINGmessage);
		    }
		}
		
	    }, delay*2000, delay*2000);
	
    }

    public static void init(int delay){
	if(instance == null)
	    instance = new Tracker(delay);
	return;
    }

    public static Tracker getInstance(){
	return instance;
    }
    
    public void issueTicket(Long UUID, int expirationThreshold, Long ticketUUID, String ticketType){
	Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
	Ticket newTicket = new Ticket(currentTimestamp.getTime(), expirationThreshold, ticketUUID, ticketType);

	// create a field for this UUID
	if(!trackingList.keySet().contains(UUID))
	    this.trackingList.put(UUID, new ArrayList<Ticket>());
	
	// keep monitoring this ticket
	this.trackingList.get(UUID).add(newTicket);
    }

     
    public void issueTicket(String nameUUID, int expirationThreshold, Long ticketUUID, String ticketType){
	Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
	Ticket newTicket = new Ticket(currentTimestamp.getTime(), expirationThreshold, ticketUUID, ticketType);

	// create a field for this node
	if(!nodeList.keySet().contains(nameUUID))
	    this.nodeList.put(nameUUID, new ArrayList<Ticket>());
	
	// keep monitoring this ticket
	this.nodeList.get(nameUUID).add(newTicket);
    }

    private boolean isExpired(Ticket ticket){
	Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
	Long currentTime = currentTimestamp.getTime();
        return (currentTime - ticket.timestamp) > ticket.expirationThreshold; 
    }

    public void removeTicket(Long UUID, Long ticketUUID){
	for(Ticket t : this.trackingList.get(UUID)){
	    if(t.UUID.equals(ticketUUID)){
		this.trackingList.get(UUID).remove(t);
	    }
	}
    }

     public void removeTicket(String nodeUUID, Long ticketUUID){
	for(Ticket t : this.nodeList.get(nodeUUID)){
	    if(t.UUID.equals(ticketUUID)){
		this.nodeList.get(nodeUUID).remove(t);
	    }
	}
    }

    
    
}
