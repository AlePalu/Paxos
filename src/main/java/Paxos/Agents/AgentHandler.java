package Paxos.Agents;

import Paxos.Network.LocalNetworkProcess;
import Paxos.Network.Message;
import Paxos.Network.NetworkInterface;
import com.eclipsesource.json.JsonObject;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

public class AgentHandler extends Agent implements Runnable {
    private Aceptor a = new Aceptor();
    private Learner l = new Learner("/home/prosdothewolf/Desktop/test.txt");
    private Proposer p = new Proposer();
    private NetworkInterface network;
    final RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
    final long pid = runtime.getPid();


    public AgentHandler(){
        try {
            network = new LocalNetworkProcess("127.0.0.1",3705,pid);
            int n = network.lookupConnectedProcesses().size();
            p.updateProcessCount(n);
        }catch(IOException e){e.printStackTrace();}
    }

    public void makeProposer(int val){

    }

    @Override
    public void run(){
        Message m;
        JsonObject jsonMsg;
        String s;
        while(true) {
           try{
               Thread.sleep(10);
           }
           catch(InterruptedException e){
               e.printStackTrace();
           }

           if (network.isThereAnyMessage()) {
               s = network.receiveMessage();
               m = new Message(s);
               dispac(m);
            }
        }
    }

    private void dispac(Message m){
        //TODO
        //switch ()
    }

    public void propose(int val){
        Message m = p.propose(val);
        network.sendMessage(m.getJSON());
    }


/*
    public static void main(String[] args){
        AgentHandler a = new AgentHandler();
    }
*/


}
