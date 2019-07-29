package Paxos.Network;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

class SocketRegistry{

    private static SocketRegistry instance;

    // contains all bindings UUID-socketBox, either local and remotes
    private ConcurrentHashMap<Long, SocketBox> registry;
    private ConcurrentLinkedQueue<SocketBox> pendingSockets;

    // contains the list of local UUID (required by DISCOVER, each network infrastructure is responsable for its own local processes)
    private ArrayList<Long> localUUID;
    
    // contains binding IP-socketBox for remote machine
    private ConcurrentHashMap<String, SocketBox> remoteNodeRegistry;

    // updated via NAMINGREPLY
    private ArrayList<String> remoteNodeList;
    
    private SocketBox namingSocket;

    private long machineUUID;
    
    private SocketRegistry(){
	this.registry = new ConcurrentHashMap<Long, SocketBox>();
	this.pendingSockets = new ConcurrentLinkedQueue<SocketBox>();
	this.localUUID = new ArrayList<Long>();
	
	// required to resolve remote addresses
	this.remoteNodeRegistry = new ConcurrentHashMap<String, SocketBox>();
	this.remoteNodeList = new ArrayList<String>();
    }

    public static SocketRegistry getInstance(){
	if(instance == null)
	    instance = new SocketRegistry();

	return instance;
    }

    public ConcurrentHashMap<Long, SocketBox> getRegistry(){
	return this.registry;
    }

    public ConcurrentHashMap<String, SocketBox> getRemoteNodeRegistry(){
	return this.remoteNodeRegistry;
    }    
    
    public ArrayList<String> getRemoteNodeList(){
	return this.remoteNodeList;
    }
    
    public ConcurrentLinkedQueue<SocketBox> getPendingSockets(){
	return this.pendingSockets;
    }

    public ArrayList<Long> getLocalUUID(){
	return this.localUUID;
    }
    
    public ArrayList<SocketBox> getAllSockets(){
	ArrayList<SocketBox> tmpArrayList = new ArrayList(this.pendingSockets);
	tmpArrayList.addAll(this.registry.values());
	tmpArrayList.add(this.namingSocket);
	return tmpArrayList;
    }

    public void setNamingSocket(SocketBox namingSocket){
	this.namingSocket = namingSocket;
    }

    public SocketBox getNamingSocket(){
	return this.namingSocket;
    }
    
    public void addElement(Long processUUID, SocketBox socketBox) throws IllegalStateException{
	// ensure 1:1 mapping
	if(!this.registry.values().contains(socketBox)){ // this socketBox is not mapped to a key, can add...
	    this.registry.put(processUUID, socketBox);

	    // add the process to the known list of active local processes
	    this.localUUID.add(processUUID);
	}
	else
	    throw new IllegalStateException();
    }

    public void setMachineUUID(long machineUUID){
	this.machineUUID = machineUUID;
    }

    public long getMachineUUID(){
	return this.machineUUID;
    }
    
}
