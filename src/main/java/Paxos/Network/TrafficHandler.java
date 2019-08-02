package Paxos.Network;

import com.eclipsesource.json.Json;

import java.util.ArrayList;

// handle NetworkManager queues, sending and receiving messages to remote clients
class TrafficHandler implements Runnable{

    private static TrafficHandler instance;
    private ArrayList<String> whiteList;
    
    private TrafficHandler(){
	this.whiteList = new ArrayList<String>();
    }

    public static TrafficHandler getInstance(){
	if(instance == null)
	    instance = new TrafficHandler();

	return instance;
    }
    
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

			// apply traffic rule
			for(MessageType msg : MessageType.values()){
			    if(msg.match(message) && this.whiteList.contains(Json.parse(message).asObject().get(MessageField.MSGTYPE.toString()).asString())){
				//if(Json.parse(message).asObject().get("MSGTYPE").asString().equals("PING"))
				//System.out.println("[IN] " +message);
				msg.applyRule(socket, message);
				break;
			    }
			}
		    }
		}
		Thread.sleep(50); // avoid burning the CPU
	    }catch(Exception e){
	        continue;
	    }
	}
    }

    public void setWhiteList(ArrayList<String> exception){
	this.whiteList.clear();
	// always allow PING messages
	this.whiteList.add(MessageType.PING.toString());
	this.whiteList.addAll(exception);
    }

    public void allowAll(){
	this.whiteList.clear();
	for(MessageType msg : MessageType.values()){
	    this.whiteList.add(msg.toString());
	}
    }
}
