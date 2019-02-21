package Paxos.Network;

import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;

class ProcessRegistry{

    private ConcurrentHashMap<Pair<InetAddress, Integer>, Long> registry;
    private static ProcessRegistry instance;

    private ProcessRegistry(){
	this.registry = new ConcurrentHashMap<Pair<InetAddress, Integer>, Long>();
    }

    public static ProcessRegistry getInstance(){
	if(instance == null)
	    instance = new ProcessRegistry();

	return instance;
    }

    public ConcurrentHashMap<Pair<InetAddress, Integer>, Long> getRegistry(){
	System.out.printf("getRegistry "+registry.size()+"\n");
	return this.registry;
    } 

    public void addElement(Pair<InetAddress, Integer> location, Long identifier){
	this.registry.put(location, identifier);
	System.out.printf("[PP] "+this.registry.toString()+"%n");
    }

}
