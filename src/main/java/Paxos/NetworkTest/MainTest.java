package Paxos.NetworkTest;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;

import com.eclipsesource.json.JsonObject;

import Paxos.Network.*;

class MainTest {
    
    public static void main(String[] args) {
	// get pid of this process
	final RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
	final long pid = runtime.getPid();

	try{
	    NetworkInterface myProcess = new LocalNetworkProcess(InetAddress.getLocalHost().getHostAddress(), 40000, 20);
	    Thread netThread = new Thread(myProcess);
	    netThread.start();

		NetworkInterface myProcess2 = new LocalNetworkProcess(InetAddress.getLocalHost().getHostAddress(), 40000, 15);
		Thread netThread2 = new Thread(myProcess2);
		netThread2.start();


		int howManyMessages;
		
		Message msg = new Message(null, "ciao" , MessageType.PREPAREREQUEST);

		msg.setAsBroadcast();
		myProcess.sendMessage(msg.getJSON());
		
		while(true){
		Thread.sleep(2000);
		System.out.println("Start");
		/*
		myProcess.updateConnectedProcessesList();
		System.out.println(myProcess.lookupConnectedProcesses());
		
		if(myProcess.isThereAnyMessage()){
		    String msgs = myProcess.receiveMessage();
		    Message receivedMessage = new Message(msgs);

		    System.out.printf("[MessageReceived]: "+msgs+"%n");
		    
		    // example of reply
		    msg = new Message(receivedMessage.getSenderID(), "risposta", MessageType.RESPONDTOPREPAREREQUEST);

		    System.out.printf("[MessageSent]: "+msg.getJSON()+"%n");
		    
		    myProcess.sendMessage(msg.getJSON());
		    }*/

	    }
	}catch(Exception w){
	    w.printStackTrace();
	}

      }
}

