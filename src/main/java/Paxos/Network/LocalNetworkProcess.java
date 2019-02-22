package Paxos.Network;

import Paxos.Network.SocketBox;

import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;


// interface used by processes to communicate with the main NetworkManager server (in a client-server fashion)
public class LocalNetworkProcess implements Runnable{

    private SocketBox socketBox;
    private ConcurrentLinkedQueue<String> inboundQueue;
    private ConcurrentLinkedQueue<String> outboundQueue;
    private long UUID;

    public LocalNetworkProcess(String ip, int port, long UUID) throws IOException{
	Socket processSocket = new Socket(ip, port); // connect to NetworkManager server
	this.socketBox = new SocketBox(processSocket);

	this.UUID = UUID;
	this.inboundQueue = new ConcurrentLinkedQueue<String>();
	this.outboundQueue = new ConcurrentLinkedQueue<String>();
    }

    public void run(){
	while(true){
	    try {
	        // handling messages...
		
		if(!this.outboundQueue.isEmpty()){ // OUT
		    // send message on socket
		    PrintWriter tmpPrintWriter = this.socketBox.getOutputStream();
		    tmpPrintWriter.println(outboundQueue.remove());
		    tmpPrintWriter.flush();
		    System.out.printf("message sent to "+this.socketBox.getSocket().getPort()+"%n");
		}

		if(this.socketBox.getSocket().getInputStream().available() != 0){ // IN
		    Scanner tmpScanner = this.socketBox.getInputStream();
		    String msg = tmpScanner.nextLine();
		    System.out.printf("message received: "+msg+"%n");
		    this.inboundQueue.add(msg);
		}

		Thread.sleep(10); // avoid burning CPU
	    }
	    catch (Exception e) {
		System.out.println("Error " + e.getMessage());
		e.printStackTrace();
	    }

	}
    }

    public void sendMessage(String message){
	System.out.printf("message added: "+message+"\n");
	this.outboundQueue.add(message);
    }

    public String receiveMessage(){
	return this.inboundQueue.remove();
    }
}
