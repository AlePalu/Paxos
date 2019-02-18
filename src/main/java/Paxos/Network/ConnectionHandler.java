package Paxos.Network;

import java.io.IOException;
import java.net.InetAddress;
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
	while(true){
	    try{
		// waiting for new connection
		Socket newSocket = incomingConnectionSocket.accept();
		// open output/input communication
		SocketBox socketBox = new SocketBox(newSocket);
		// add the client to the opened socket registry
		Pair<InetAddress, Integer> clientIdentifier = new Pair(newSocket.getInetAddress(), newSocket.getPort()); // remote client process identifier
		try{
		    SocketRegistry.getInstance().addElement(clientIdentifier, socketBox); // bind the remote process to the just opened socket
		}catch(IllegalStateException exception){
		    return; // this socket has already been binded to a remote process
		}
	    }catch(IOException exception){

	    }
        }
    }
    
}
