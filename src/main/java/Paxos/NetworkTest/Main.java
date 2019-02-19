package Paxos.NetworkTest;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

import Paxos.Network.*;

class Main{

    public static void main(String[] args) {
	// get pid of this process
	final RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
	final long pid = runtime.getPid();
	
	NetworkManagerInterface networkManager = NetworkManager.getInstance();
	// connecting to network manager
	try{
	    networkManager.subscribeProcess(pid);
	}catch(Exception e){
	    e.printStackTrace();
	    return;
	}
    }

}
