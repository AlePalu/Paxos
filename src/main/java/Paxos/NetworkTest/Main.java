package Paxos.NetworkTest;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

import Paxos.Network.*;

class Main{
    
    public static void main(String[] args) {
	// get pid of this process
	final RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
	final long pid = runtime.getPid();

	try{
	    NetworkInterface myProcess = new LocalNetworkProcess("127.0.0.1", 4455, pid);
	    Thread netThread = new Thread(myProcess);
	    netThread.start();
    
	    while(true){
		Thread.sleep(1000);
		System.out.printf("sending DISCOVER...%n");
		myProcess.updateConnectedProcessesList();
		System.out.printf("received response%n");
		System.out.printf("connected process list: "+myProcess.lookupConnectedProcesses().toString()+"%n");
	    }
	}catch(Exception w){
	    w.printStackTrace();
	}
      }
}

