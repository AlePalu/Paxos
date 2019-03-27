package Paxos.Network;

import com.eclipsesource.json.Json;

// handle NetworkManager queues, sending and receiving messages to remote clients
class TrafficHandler implements Runnable{

    public void run(){
	System.out.printf("[TrafficHandler]: Ready to route traffic!\n");
	while(true){
	    try{
		for(SocketBox socket : SocketRegistry.getInstance().getAllSockets()){
		    // take message
		    String message;
		    if(socket.getInputStream().ready()){ // there are data ready to be readden
			// get the message from socket
			message = socket.getInputStream().readLine();

			if(!Json.parse(message).asObject().get("MSGTYPE").asString().equals("PING"))
				System.out.println("[OUT] " +message);
	
			// apply traffic rule
			for(MessageType msg : MessageType.values()){
			    if(msg.match(message)){
				msg.applyRule(socket, message);
				break;
			    }
			}
		    }
		}
		Thread.sleep(10); // avoid burning the CPU
	    }catch(Exception e){
	        continue;
	    }
	}
    }
}
