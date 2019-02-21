package Paxos.Network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.eclipsesource.json.JsonObject;

import java.util.ArrayList;


// singleton class responsable to handle all network related stuffs
public class NetworkManager implements NetworkManagerInterface{

    // singleton instance
    private static NetworkManager instance;

    private LinkedBlockingQueue<Message> inboundInternalTrafficQueue;
    private LinkedBlockingQueue<Message> outboundTrafficQueue;

    private ConcurrentHashMap<Long, LinkedBlockingQueue<Message>> subscribedAgentsMap;

    private ProcessRegistry processRegistry;
    
    private NetworkManager(){
	this.inboundInternalTrafficQueue = new LinkedBlockingQueue<Message>();
	this.outboundTrafficQueue = new LinkedBlockingQueue<Message>();
	this.subscribedAgentsMap = new ConcurrentHashMap<Long, LinkedBlockingQueue<Message>>();
	this.processRegistry = ProcessRegistry.getInstance();
    }

    public static NetworkManager getInstance(){
	if(instance == null)
	    instance = new NetworkManager();

	return instance;
    }

    public void enqueueMessage(Message msg){
	this.outboundTrafficQueue.add(msg);
    }

    public Queue<Message> getInboundInternalTraficQueue(){
	return this.inboundInternalTrafficQueue;
    }
    
    public Queue<Message> getInboundTrafficQueue(long pid){
	return this.subscribedAgentsMap.get(pid);
    }

    public Queue<Message> getOutboundTrafficQueue(){
	return this.outboundTrafficQueue;
    }
    
    public Message dequeueMessage(long pid){
	return this.subscribedAgentsMap.get(pid).remove();
    }

    public Boolean isThereAnyMessage(long pid){
	return !this.subscribedAgentsMap.get(pid).isEmpty();
    }

    // use the PID of the process as local identifier
    public void subscribeProcess(long pid) throws IOException{
	JsonObject SUBSCRIBEmessage = new JsonObject();
	SUBSCRIBEmessage.add("TYPE", MessageType.SUBSCRIBE.toString());
	SUBSCRIBEmessage.add("UUID", pid);
    }

    public ArrayList<Long> lookupConnectedProcess(){
	return new ArrayList<Long> (this.processRegistry.getRegistry().values());
    }
    
}
