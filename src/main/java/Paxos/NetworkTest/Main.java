package Paxos.NetworkTest;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

import com.eclipsesource.json.JsonObject;
import Paxos.Network.*;

class Main{
    
    public static void main(String[] args) {
	// get pid of this process
	final RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
	final long pid = runtime.getPid();

	try{
	    LocalNetworkProcess myProcess = new LocalNetworkProcess("127.0.0.1", 4455, pid);
	    Thread netThread = new Thread(myProcess);
	    netThread.start();

	    JsonObject jo = new JsonObject();
	    jo.add("ID", pid);
	    jo.add("MSGTYPE", MessageType.SUBSCRIBE.toString());
	    myProcess.sendMessage(jo.toString());
	    
	    while(true){
		Thread.sleep(1000);
		JsonObject jj = new JsonObject();
		jj.add("ID", pid);
		jj.add("MSGTYPE", MessageType.DISCOVER.toString());
		jj.add("FORWARDTYPE", MessageType.BROADCAST.toString());
	        myProcess.sendMessage(jj.toString());
	    }
	}catch(Exception w){
	    w.printStackTrace();
	}
      }
}

