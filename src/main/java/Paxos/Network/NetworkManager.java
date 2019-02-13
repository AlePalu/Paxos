package Paxos.Network;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import jdk.internal.agent.Agent;

// singleton class responsable to handle all network related stuffs
public class NetworkManager implements NetworkManagerInterface{

    // singleton instance
    private static NetworkManager instance;

    private ConcurrentLinkedQueue<Message> inboundTrafficQueue;
    private ConcurrentLinkedQueue<Message> outboundTrafficQueue;

    private ConcurrentHashMap<Agent, ConcurrentLinkedQueue<Message>> subscribedAgentsMap;
    
    private NetworkManager(){
	this.inboundTrafficQueue = new ConcurrentLinkedQueue<Message>();
	this.outboundTrafficQueue = new ConcurrentLinkedQueue<Message>();
	this.subscribedAgentsMap = new ConcurrentHashMap<Agent, ConcurrentLinkedQueue<Message>>();
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
    
    public Message dequeueMessage(Agent agent){
	return this.subscribedAgentsMap.get(agent).remove();
    }

    public Boolean isThereAnyMessage(){
	
    }

    public void subscribe(Agent agent){
	this.subscribedAgentsMap.put(agent, new ConcurrentLinkedQueue<Message>());
    }
}
