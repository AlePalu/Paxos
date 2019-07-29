package Paxos.Network;

import java.net.Inet4Address;
import java.net.Socket;

import java.util.UUID;

class Main{
    
    public static void main(String[] args) {
	
	if(args.length == 0){
	    System.out.printf("[Main]: No naming node IP inserted. Aborting.%n");
	    return;
	}
	
	String namingNodeIP = args[0];
	
	System.out.printf("[Main]: started\n");

	UUID randomUUID = UUID.randomUUID();
	long UUID = randomUUID.getMostSignificantBits() & Long.MAX_VALUE;
	
	System.out.printf("[Main]: machine UUID: "+UUID+"\n");
	SocketRegistry.getInstance().setMachineUUID(UUID);
	
	System.out.printf("[Main]: supplied naming service IP : " + namingNodeIP + "\n");	
	
	try{
	    ConnectionHandler connectionHandler = new ConnectionHandler(40000);
	    Thread connectionHandlerThread = new Thread(connectionHandler);
	    // bringing up network infrastructure
	    connectionHandlerThread.start();
	    
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

	    // initialize the tracker, 2 second period
	    Tracker.init(2);
	    
	}catch(Exception e){
		e.printStackTrace();
	    return;
	}
    }
}
