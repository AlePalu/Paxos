package Paxos.NetworkTest;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

import com.eclipsesource.json.JsonObject;

import Paxos.Network.*;

class MainTest {
    
    public static void main(String[] args) {
	// get pid of this process
	final RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
	final long pid = runtime.getPid();

	try{
	    NetworkInterface myProcess = new LocalNetworkProcess("127.0.0.1", 40000, pid);
	    Thread netThread = new Thread(myProcess);
	    netThread.start();

	    myProcess.updateConnectedProcessesList();
	    while(true){
		Thread.sleep(500);
		myProcess.updateConnectedProcessesList();

		System.out.printf(myProcess.lookupConnectedProcesses().toString()+"%n");
		
		/*Message msg = new Message(null, "ciao" , MessageType.PAXOS);
		msg.setAsBroadcast();
		myProcess.sendMessage(msg.getJSON());*/

		/*		Message msg = new Message(null, "prova", MessageType.NAMINGREQUEST);
		myProcess.sendMessage(msg.getJSON());

		JsonObject nn = new JsonObject();
		nn.add("MSGTYPE", MessageType.NAMINGUPDATE.toString());
		myProcess.sendMessage(nn.toString());*/
		/*
		if(myProcess.isThereAnyMessage()){
		    Message receivedMessage = new Message(myProcess.receiveMessage());
		    // example of reply
		    msg = new Message(receivedMessage.getSenderID(), "risposta", MessageType.PAXOS);
		    myProcess.sendMessage(msg.getJSON());
		    }*/
	    }
	}catch(Exception w){
	    w.printStackTrace();
	}
      }
}

