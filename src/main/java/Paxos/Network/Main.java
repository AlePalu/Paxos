package Paxos.Network;

import java.net.Inet4Address;
import java.net.Socket;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

class Main{
    
    public static void main(String[] args) {		
	System.out.printf("[Main]: started\n");

	UUID randomUUID = UUID.randomUUID();
	long UUID = randomUUID.getMostSignificantBits() & Long.MAX_VALUE;
	
	System.out.printf("[Main]: machine UUID: "+UUID+"\n");
	SocketRegistry.getInstance().setMachineUUID(UUID);
	
	try{
	    ConnectionHandler connectionHandler = new ConnectionHandler(40000);
	    Thread connectionHandlerThread = new Thread(connectionHandler);
	    // bringing up network infrastructure
	    connectionHandlerThread.start();

	    // initialize the tracker, 2 second period
	    Tracker.init(2);
	    Thread.sleep(50);
	    
	    if(args.length == 0){ // dynamically discover where naming node is
		System.out.printf("[Main]: starting NameProber service to search for name server...%n");
		Thread namingProberThread = new Thread(NameProber.getInstance());
		namingProberThread.start();

		// randomly wait for a given timeout (makes difficult to have triky race conditions)
		int randomDelay = ThreadLocalRandom.current().nextInt(0,5);
		Thread.sleep(200 + randomDelay*500);
		
		NameProber.getInstance().namingProbe();
		Thread.sleep(1000);
		if(SocketRegistry.getInstance().getNamingSocket() == null){
		    System.out.printf("[NameProber]: No name server active, waiting for system recovery...%n");
		}
		while(SocketRegistry.getInstance().getNamingSocket() == null){ // wait until the name server is ready
		    NameProber.getInstance().namingProbe();
		    Thread.sleep(1000);
		}
	    }else{ // manually supplied naming
		String inputField = args[0];
		if(!inputField.equals("-n")){
		    System.out.printf("[Main]: bad argument. Aborting%n");
		    return;
		}else{
		    System.out.printf("[Main]: this node will act as first name server\n");	

		    // always need to start naming probe service, to handle join of new nodes
		    Thread namingProberThread = new Thread(NameProber.getInstance());
		    namingProberThread.start();
		    Thread.sleep(200);
	
		    System.out.printf("[Main]: checking if an already running name service is present on network...%n");
		    NameProber.getInstance().namingProbe();
		    Thread.sleep(2000); // wait name service discover to be completed

		    if(SocketRegistry.getInstance().getNamingSocket() != null){
			System.out.printf("[Main]: NameProber found an active name service. Not launching a new one.%n");
		    }else{
			 System.out.printf("[Main]: Naming service will run on this node. Starting Naming service...\n");

			 // give time to network infrastructure to go up
			 Thread.sleep(200);
		
			 NamingRequestHandler namingHandler = new NamingRequestHandler(Inet4Address.getLocalHost().getHostAddress(), 40000, UUID);
			 Thread namingThread = new Thread(namingHandler);
			 namingThread.start();			
		    }
		}
	    }
	}catch(Exception e){
	    e.printStackTrace();
	    return;
	}
    }
}
