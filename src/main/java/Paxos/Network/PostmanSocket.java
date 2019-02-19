package Paxos.Network;

import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.Scanner;

// handle NetworkManager queues, sending and receiving messages to remote clients
class PostmanSocket implements Runnable{

    public void run(){
	System.out.printf("queue handler is ready\n");
	while(true){
	    // outbound traffic, just send the JSON message on the relative socket
	    if(!NetworkManager.getInstance().getOutboundTrafficQueue().isEmpty()){
		// getting message from queue
		Message outMessage = NetworkManager.getInstance().getOutboundTrafficQueue().remove();
		if(outMessage.getMessageType().equals(MessageType.BROADCAST)){ // message must be sent to all process on network
		    for(SocketBox outSocket : SocketRegistry.getInstance().getRegistry().values()){
			PrintWriter tmpPrintWriter = outSocket.getOutputStream();
			tmpPrintWriter.println(outMessage.getJSON());
			tmpPrintWriter.flush();
		    }
		}else{ // unicast
		    SocketBox outSocket = SocketRegistry.getInstance().getRegistry().get(outMessage.getRecipient());
		    // sending message
		    PrintWriter tmpPrintWriter = SocketRegistry.getInstance().getRegistry().get(outMessage.getRecipient()).getOutputStream(); // this works for reply
		    tmpPrintWriter.println(outMessage.getJSON());
		    tmpPrintWriter.flush();
		}
	    }

	    // inbound traffic, checks if there is some message pending on opened sockets and insert into inboundQueue
	    for(SocketBox socket : SocketRegistry.getInstance().getRegistry().values()){
		try{
		    if(socket.getSocket().getInputStream().available() != 0){
			// build message
			Scanner tmpScanner = socket.getInputStream();
			Pair<InetAddress, Integer> address = (Pair<InetAddress, Integer>)SocketRegistry.getInstance().getRegistry().keySet(socket).toArray()[0];
			Message newMessage = new Message(tmpScanner.nextLine(), address);

			// inserting message into inbound queue
			long PID = ProcessRegistry.getInstance().getRegistry().get(address);
			NetworkManager.getInstance().getInboundTrafficQueue(PID).add(newMessage); // dispatching message to local process
		    }
		}catch(Exception e){

		}
	    }
	}
    }
   
}
