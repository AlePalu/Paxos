package Paxos.Network;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.Json;

class Ticket{

    Long timestamp;
    int expirationThreshold;
    Long UUID;

    public Ticket(Long timestamp, int expirationThreshold, Long UUID){
	this.timestamp = timestamp;
	this.expirationThreshold = expirationThreshold;
	this.UUID = UUID;
    }
    
}

class Tracker{

    private HashMap<Long, ArrayList<Ticket>> trackingList;
    private Timer timer;
    
    private static Tracker instance;
    
    private Tracker(int delay){
	this.trackingList = new HashMap<Long, ArrayList<Ticket>>();

	System.out.printf("[Tracker]: ready to handle periodic events%n");
        
	this.timer = new Timer();

	// check if there is some ticket expired
	timer.scheduleAtFixedRate(new TimerTask(){
		public void run() {
		    for(Entry<Long, ArrayList<Ticket>> entry : trackingList.entrySet()){
			for(Ticket t : entry.getValue()){
			    if(Tracker.getInstance().isExpired(t)){
				System.out.printf("[Tracker]: ticket for process "+entry.getKey()+" expired.%n");
				
			    }
			}
		    }
		}

	    },delay*100, delay*100);

	// periodically keep track of processes still alive
	timer.scheduleAtFixedRate(new TimerTask(){
		public void run() {
		    System.out.printf("[Tracker]: start polling cycle...%n");
		    for(Entry<Long, SocketBox> entry : SocketRegistry.getInstance().getRegistry().entrySet()){
			String PINGmessage = MessageForgery.forgePING(entry.getKey());

			// parse the message to get the ticket identifier
			JsonObject Jmessage = Json.parse(PINGmessage).asObject();
			// process considered alive if response arrives in at most 10 seconds
			Tracker.getInstance().issueTicket(entry.getKey(), 10000, Jmessage.get(MessageField.TICKET.toString()).asLong());
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
    
    public void issueTicket(Long UUID, int expirationThreshold, Long ticketUUID){
	Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
	Ticket newTicket = new Ticket(currentTimestamp.getTime(), expirationThreshold, ticketUUID);

	// create a field for this UUID
	if(!trackingList.keySet().contains(UUID))
	    this.trackingList.put(UUID, new ArrayList<Ticket>());
	
	// keep monitoring this ticket
	this.trackingList.get(UUID).add(newTicket);
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
    
}
