package Paxos.Network;

import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;

class SocketRegistry{

    private static SocketRegistry instance;
    private ConcurrentHashMap<Pair<InetAddress, Integer>, SocketBox> registry;
    
    private SocketRegistry(){
	this.registry = new ConcurrentHashMap<Pair<InetAddress, Integer>, SocketBox>();
    }

    public static SocketRegistry getInstance(){
	if(instance == null)
	    instance = new SocketRegistry();

	return instance;
    }

    public ConcurrentHashMap<Pair<InetAddress, Integer>, SocketBox> getRegistry(){
	return this.registry;
    }

    public void addElement(Pair<InetAddress, Integer> process, SocketBox socketBox) throws IllegalStateException{
	// ensure 1:1 mapping
	if(this.registry.keySet(socketBox).isEmpty()) // this socketBox is not mapped to a key, can add...
	    this.registry.put(process, socketBox);
	else
	    throw new IllegalStateException();
    }
    
}
