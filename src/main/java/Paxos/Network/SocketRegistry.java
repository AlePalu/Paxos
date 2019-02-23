package Paxos.Network;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

class SocketRegistry{

    private static SocketRegistry instance;
    private ConcurrentHashMap<Long, SocketBox> registry;
    private ConcurrentLinkedQueue<SocketBox> pendingSockets;
    
    private SocketRegistry(){
	this.registry = new ConcurrentHashMap<Long, SocketBox>();
	this.pendingSockets = new ConcurrentLinkedQueue<SocketBox>();
    }

    public static SocketRegistry getInstance(){
	if(instance == null)
	    instance = new SocketRegistry();

	return instance;
    }

    public ConcurrentHashMap<Long, SocketBox> getRegistry(){
	return this.registry;
    }

    public ConcurrentLinkedQueue<SocketBox> getPendingSockets(){
	return this.pendingSockets;
    }

    public ArrayList<SocketBox> getAllSockets(){
	ArrayList<SocketBox> tmpArrayList = new ArrayList(this.pendingSockets);
	tmpArrayList.addAll(this.registry.values());
	return tmpArrayList;
    }
    
    public void addElement(Long processUUID, SocketBox socketBox) throws IllegalStateException{
	// ensure 1:1 mapping
	if(!this.registry.values().contains(socketBox)) // this socketBox is not mapped to a key, can add...
	    this.registry.put(processUUID, socketBox);
	else
	    throw new IllegalStateException();
    }
    
}
