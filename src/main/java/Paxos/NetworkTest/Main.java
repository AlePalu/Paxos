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
	    jo.add("ID", 5);
	    jo.add("value", 88);
	    jo.add("agentType", "ciccio");
	    myProcess.sendMessage(jo.toString());
	}catch(Exception e){

	}
	//System.out.printf("connected processes: " + networkManager.lookupConnectedProcess().toString() + "%n");

	while(true){
	    try{
		Thread.sleep(1000);
	    }catch(Exception e){
		e.printStackTrace();
	    }
	    //System.out.printf(networkManager.lookupConnectedProcess().toString()+"%n");
	    
	}
    }

}
