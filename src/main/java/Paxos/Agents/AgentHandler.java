package Paxos.Agents;

import Paxos.Network.LocalNetworkProcess;
import Paxos.Network.Message;
import Paxos.Network.NetworkInterface;

import java.net.Inet4Address;


public class AgentHandler implements Runnable {
    public NetworkInterface network;
    private Aceptor a;
    private Learner l;
    private Proposer p;
    private PaxosData data;


   /* public AgentHandler(){
        Random r = new Random();
        long id = Math.abs(r.nextLong());
        try {
            network = new LocalNetworkProcess("127.0.0.1",40000,id);
            Thread netThread = new Thread(network);
            a = new Aceptor(id);
            l = new Learner("/home/prosdothewolf/Desktop/",id);
            p = new Proposer();
            netThread.start();
            p.updateProcessCount(network.lookupConnectedProcesses().size());
        }catch(IOException e){e.printStackTrace();}
    }*/

    public AgentHandler(Long n, String path){
        try {
            network = new LocalNetworkProcess(Inet4Address.getLocalHost().getHostAddress(),40000,n);
            Thread netThread = new Thread(network);
            netThread.start();
            network.updateConnectedProcessesList();
            //wait set up network
            Thread.sleep(100);
            this.data = new PaxosData(network.lookupConnectedProcesses().size(),n);
            a = new Aceptor(this.data);
            l = new Learner(this.data,path);
            p = new Proposer(this.data);
        }catch(Exception e){e.printStackTrace();}
    }

    @Override
    public void run(){
        Message m;
        String s;
        while(true) {
           try{
               Thread.sleep(10);
           }
           catch(InterruptedException e){
               e.printStackTrace();
           }
           if(network.isThereAnyMessage()) {
               s = network.receiveMessage();
               m = new Message(s);
               respons(m);
	   }
        }
    }

    public void propose(String val){
        Message propose;
        try {
            network.updateConnectedProcessesList();
            this.data.setNumOfProces(network.lookupConnectedProcesses().size());
        }catch(InterruptedException e){e.printStackTrace();}
        propose = p.propose(val);
        network.sendMessage(propose.getJSON());
    }

    private void respons(Message m){
        System.out.println("[Paxos IN] "+ m);
        Message response = null;
        switch (m.getMessageType()){
            case"PREPAREREQUEST":
                response = a.processPrepareRequest(m);
                break;
            case"RESPONDTOPREPAREREQUEST":
                response = p.processRespondToPrepareRequest();
                break;
            case "ACCEPTREQUEST":
                response = a.processAcceptRequest(m);
            case "DECISION":
                l.processDecisionRequest(m);
                break;
        }
        if (response != null) {
            System.out.println("[Paxos Out] "+ m);
            network.sendMessage(response.getJSON());
        }
    }

    public long getid(){
        return data.getId();
    }

}
