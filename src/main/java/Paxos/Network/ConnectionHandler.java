package Paxos.Network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


// responsable to accept new connection and to track opened connection to remote processes for later communications.
class ConnectionHandler implements Runnable{

    private ServerSocket incomingConnectionSocket;
    
    public ConnectionHandler(int welcomePort) throws IOException{
	this.incomingConnectionSocket = new ServerSocket(welcomePort);
    }

    // thread routine
    public void run(){

	System.out.printf("[ConnectionHandler]: Ready to accept connections on port: "+this.incomingConnectionSocket.getLocalPort()+"\n");

	// start thread for queue handling
	TrafficHandler trafficHandler = new TrafficHandler();
	Thread trafficHandlerThread = new Thread(trafficHandler);
	trafficHandlerThread.start();

	while(true){
	    try{
		// waiting for new connection
		Socket newSocket = incomingConnectionSocket.accept();
	        // open output/input communication
		SocketBox socketBox = new SocketBox(newSocket);
		// add the client to the opened socket registry, but still pending to be binded to a UUID
		SocketRegistry.getInstance().getPendingSockets().add(socketBox);

		System.out.printf("[ConnectionHandler]: new client connected from "+newSocket.getInetAddress().getHostAddress()+"%n");	
	    }catch(IOException exception){
		continue;
	    }
        }
    }
    
}
