package Paxos.Network;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;

class Ticket{

    Long timestamp;
    Long expirationThreshold;

    public Ticket(Long timestamp, Long expirationThreshold){
	this.timestamp = timestamp;
	this.expirationThreshold = expirationThreshold;
    }
    
}

class Tracker{

    private HashMap<Long, Ticket> trackingList;
    private Timestamp currentTimestamp;

    private Timer timer;
    
    private static Tracker instance;
    
    private Tracker(int delay){
	this.trackingList = new HashMap<Long, Ticket>();
	this.currentTimestamp = new Timestamp(System.currentTimeMillis());

	System.out.printf("[Tracker]: start monitoring for periodic events%n");
	
	// check every delay seconds if there is some ticket expired
	this.timer = new Timer();
	timer.scheduleAtFixedRate(new TimerTask(){
		public void run() {
		    for(Entry<Long, Ticket> entry : trackingList.entrySet()){
			if(Tracker.getInstance().isExpired(entry.getValue())){
			    
			}
		    }
		}
	    },
	    delay*1000, delay*1000);
    }

    public static void init(int delay){
	if(instance == null)
	    instance = new Tracker(delay);
	return;
    }

    public static Tracker getInstance(){
	return instance;
    }
    
    public void issueTicket(Long UUID, Long expirationThreshold){
	Ticket newTicket = new Ticket(this.currentTimestamp.getTime(), expirationThreshold);

	// keep monitoring this ticket
	this.trackingList.put(UUID, newTicket);
    }

    private boolean isExpired(Ticket ticket){
	Long currentTime = this.currentTimestamp.getTime();
	return (currentTime - ticket.timestamp) > ticket.expirationThreshold; 
    }

    public void removeTicket(Long UUID){
	this.trackingList.remove(UUID);
    }
    
}
