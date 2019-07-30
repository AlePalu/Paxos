package Paxos.Network;

import java.net.Inet4Address;
import java.net.Socket;

import java.util.UUID;

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
	    Thread.sleep(200);
	    
	    if(args.length == 0){ // dynamically discover where naming node is
		System.out.printf("[Main]: no name IP supplied, starting NameProber service%n");
		Thread namingProberThread = new Thread(NameProber.getInstance());
		namingProberThread.start();
		Thread.sleep(200);
	
		NameProber.getInstance().namingProbe();
	    }else{ // manually supplied naming
		String namingNodeIP = args[0];
		System.out.printf("[Main]: supplied naming service IP : " + namingNodeIP + "\n");	

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
		    if(Inet4Address.getLocalHost().getHostAddress().equals(namingNodeIP)){
			System.out.printf("[Main]: Naming service will run on this node. Starting Naming service...\n");

			// give time to network infrastructure to go up
			Thread.sleep(200);
		
			NamingRequestHandler namingHandler = new NamingRequestHandler(Inet4Address.getLocalHost().getHostAddress(), 40000, UUID);
			Thread namingThread = new Thread(namingHandler);
			namingThread.start();
		    }else{
			System.out.printf("[Main]: Naming service is remote. Using the supplied IP as Naming service reference...\n");		
			// open connection with naming service node
			Socket namingSocket = new Socket(namingNodeIP, 40000);
			SocketBox namingSocketBox = new SocketBox(namingSocket);
			namingSocketBox.setUUID(SocketRegistry.getInstance().getMachineUUID());
			SocketRegistry.getInstance().setNamingSocket(namingSocketBox);

			String NAMINGUPDATEmessage = MessageForgery.forgeNAMINGUPDATE(Inet4Address.getLocalHost().getHostAddress());
			SocketRegistry.getInstance().getNamingSocket().sendOut(NAMINGUPDATEmessage);

			String NAMINGREQUESTmessage = MessageForgery.forgeNAMINGREQUEST();
			SocketRegistry.getInstance().getNamingSocket().sendOut(NAMINGREQUESTmessage);
		    }
		}
	    }
	    
	}catch(Exception e){
		e.printStackTrace();
	    return;
	}
    }
}
