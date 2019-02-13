import java.util.HashMap;
import java.util.Queue;

import jdk.internal.agent.Agent;

// singleton class responsable to handle all network related stuffs
class NetworkManager implements NetworkManagerInterface{

    // singleton instance
    private static NetworkManager instance;

    private Queue<Message> inboundTrafficQueue;
    private Queue<Messge> outboundTrafficQueue;

    private HashMap<Agent, Queue<Message>> subscribedAgentsMap;
    
    private NetworkManager(){
	this.inboundTrafficQueue = new Queue<Message>();
	this.outboundTrafficQueue = new Queue<Message>();
	this.subscribedAgentsMap = new Queue<Agent, Queue<Message>>();
    }

    public static NetworkManager getInstance(){
	if(instance == null)
	    instance = new NetworkManager();

	return instance;
    }

    public void enqueueMessage(Message msg){
	this.outboundTrafficQueue.add(msg);
    }

    public Queue<Message> getInboundTrafficQueue(){
	return this.inboundTrafficQueue;
    }

    public Queue<Message> getOutboundTrafficQueue(){
	return this.outboundTrafficQueue;
    }
    
    public Message dequeueMessage(){
	
    }

    public Boolean isThereAnyMessage(){

    }

    public void subscribe(){

    }
    
}
