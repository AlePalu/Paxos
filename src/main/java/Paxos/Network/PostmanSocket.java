package Paxos.Network;

import java.io.PrintWriter;
import java.util.Scanner;

// handle NetworkManager queues, sending and receiving messages to remote clients
class PostmanSocket implements Runnable{

    public void run(){
	// outbound traffic, just send the JSON message on the relative socket
	if(!NetworkManager.getInstance().getOutboundTrafficQueue().isEmpty()){
	    // getting message from queue
	    Message outMessage = NetworkManager.getInstance().getOutboundTrafficQueue().remove();
	    SocketBox outSocket = SocketRegistry.getInstance().getRegistry().get(outMessage.getSenderID());
	    // sending message
	    PrintWriter tmpPrintWriter = SocketRegistry.getInstance().getRegistry().get(outMessage.getSenderID()).getOutputStream();
	    tmpPrintWriter.println(outMessage.getJSON());
	    tmpPrintWriter.flush();
	}

	// inbound traffic, checks if there is some message pending on opened sockets and insert into inboundQueue
	for(SocketBox socket : SocketRegistry.getInstance().getRegistry().values()){
	    if(socket.getInputStream().available() != 0){
		// build message
		Scanner tmpScanner = socket.getInputStream();
		Message newMessage() = new Message(tmpScanner.nextLine(), SocketRegistry.getInstance().getRegistry().keySet(socket).toArray()[0]);

		// inserting message into inbound queue
		NetworkManager.getInstance().getInboundTrafficQueue().add(newMessage);
	    }
	}	
    }
   
}
