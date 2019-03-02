package Paxos.Network;

import java.net.Inet4Address;

class Main{

    public static void main(String[] args) {

	if(args.length == 0){
	    System.out.printf("[Main]: No naming node IP inserted. Aborting.%n");
	    return;
	}
	
	String namingNodeIP = args[0];
	
	System.out.printf("[Main]: started\n");
	System.out.printf("[Main]: supplied naming service IP : " + namingNodeIP + "\n");

	
	try{
	    ConnectionHandler connectionHandler = new ConnectionHandler(40000);
	    Thread connectionHandlerThread = new Thread(connectionHandler);
	    // start thread
	    connectionHandlerThread.start();

	    if(Inet4Address.getLocalHost().getHostAddress().equals(namingNodeIP)){
		System.out.printf("[Main]: Naming service will run on this node. Starting Naming service...\n");
		NamingRequestHandler namingHandler = new NamingRequestHandler("127.0.0.1", 40000);
		Thread namingThread = new Thread(namingHandler);
		namingThread.start();
	    }else{
		System.out.printf("[Main]: Naming service is remote. Using the supplied IP as Naming service reference...\n");
		
	    }
	    
	}catch(Exception e){
	    return;
	}
    }
}
