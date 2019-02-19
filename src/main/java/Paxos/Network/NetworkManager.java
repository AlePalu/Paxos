package Paxos.Network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

// singleton class responsable to handle all network related stuffs
public class NetworkManager implements NetworkManagerInterface{

    // singleton instance
    private static NetworkManager instance;

    private ConcurrentLinkedQueue<Message> inboundTrafficQueue;
    private ConcurrentLinkedQueue<Message> outboundTrafficQueue;

    private ConcurrentHashMap<Long, ConcurrentLinkedQueue<Message>> subscribedAgentsMap;
    
    private NetworkManager(){
	this.inboundTrafficQueue = new ConcurrentLinkedQueue<Message>();
	this.outboundTrafficQueue = new ConcurrentLinkedQueue<Message>();
	this.subscribedAgentsMap = new ConcurrentHashMap<Long, ConcurrentLinkedQueue<Message>>();
    }

    public static NetworkManager getInstance(){
	if(instance == null)
	    instance = new NetworkManager();

	return instance;
    }

    public void enqueueMessage(Message msg){
	this.outboundTrafficQueue.add(msg);
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
	// open inbound queue
	this.subscribedAgentsMap.put(pid, new ConcurrentLinkedQueue<Message>());
	System.out.printf("binded process with PID: "+pid+" to queue%n");
	// open socket for this process, connecting to local ConnectionHandler
	Socket processSocket = new Socket("127.0.0.1", 4455); // MAKE PORT PARAMETER
	// binding my PID to my port
	Pair<InetAddress, Integer> localLocation = new Pair<InetAddress, Integer> (processSocket.getLocalAddress(), processSocket.getLocalPort());
	ProcessRegistry.getInstance().getRegistry().put(localLocation, pid);
    }
}
