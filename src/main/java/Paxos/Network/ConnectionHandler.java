package Paxos.Network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;


// responsable to accept new connection and to track opened connection to remote processes for later communications.
class ConnectionHandler implements Runnable{

    private ServerSocket incomingConnectionSocket;
    private ConcurrentHashMap<Pair<InetAddress,Integer>,SocketBox> socketRegistry;

    
    public ConnectionHandler(int welcomePort) throws IOException{
	this.incomingConnectionSocket = new ServerSocket(welcomePort);
	this.socketRegistry = new ConcurrentHashMap<Pair<InetAddress,Integer>,SocketBox>();
    }

    // thread routine
    public void run() throws IOException{
	while(true){
	    // waiting for new connection
	    Socket newSocket = incomingConnectionSocket.accept();

	    // open output/input communication
	    SocketBox socketBox = new SocketBox(newSocket);

	    // add the client to the opened socket registry
	    Pair<InetAddress, Integer> clientIdentifier = new Pair(newSocket.getInetAddress(), newSocket.getPort()); // remote client process identifier
	    socketRegistry.put(clientIdentifier, socketBox); // bind the remote process to the just opened socket
	}
    }

    public ConcurrentHashMap<Pair<InetAddress,Integer>, SocketBox> getSocketRegistry(){
	return this.socketRegistry;
    }
    
}
