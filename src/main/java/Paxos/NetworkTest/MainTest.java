package Paxos.NetworkTest;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.util.Random;


import com.eclipsesource.json.JsonObject;

import Paxos.Network.*;

class MainTest {
    
    public static void main(String[] args) {
	// get pid of this process
	//final RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
	//final long pid = runtime.getPid();

	Random rng = new Random();
	Long pid = Math.abs(rng.nextLong());
		
	try{
	    NetworkInterface myProcess = new LocalNetworkProcess(InetAddress.getLocalHost().getHostAddress(), 40000, pid);
	    Thread netThread = new Thread(myProcess);
	    netThread.start();

	    System.out.printf(myProcess+"%n");
		
	    while(true){
		Thread.sleep(1000);
		
		myProcess.updateConnectedProcessesList();
		System.out.println(myProcess.lookupConnectedProcesses());
		
		if(myProcess.isThereAnyMessage()){
		    String msgs = myProcess.receiveMessage();
		    Message receivedMessage = new Message(msgs);

		    System.out.printf("[MessageReceived]: "+msgs+"%n");
		    
		    // example of reply
		    Message msg = new Message(receivedMessage.getSenderID(), "risposta", MessageType.RESPONDTOPREPAREREQUEST, 1);

		    System.out.printf("[MessageSent]: "+msg.getJSON()+"%n");
		    
		    myProcess.sendMessage(msg.getJSON());
		}

	    }
	}catch(Exception w){
	    w.printStackTrace();
	}

      }
}

