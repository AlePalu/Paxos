package Paxos.Agents;

import Paxos.Network.LocalNetworkProcess;
import Paxos.Network.Message;
import Paxos.Network.NetworkInterface;
import java.security.SecureRandom;

import java.net.Inet4Address;


public class AgentHandler implements Runnable {
    public NetworkInterface network;
    private Aceptor a;
    private Learner l;
    private Proposer p;
    private PaxosData data;


   public AgentHandler(String path){
       SecureRandom r = new SecureRandom();
        long id = Math.abs(r.nextLong());
        try {
            network = new LocalNetworkProcess(Inet4Address.getLocalHost().getHostAddress(),40000,id);
            Thread netThread = new Thread(network);
            netThread.start();
            network.updateConnectedProcessesList();
            //wait set up network
            Thread.sleep(100);
            this.data = new PaxosData(network.lookupConnectedProcesses().size(),id);
            a = new Aceptor(this.data);
            l = new Learner(this.data,path);
            p = new Proposer(this.data);
        }catch(Exception e){e.printStackTrace();}
    }

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
               Thread.sleep(100);
           }
           catch(InterruptedException e){
               e.printStackTrace();
           }
           if(network.isThereAnyMessage()) {
               s = network.receiveMessage();
               m = new Message(s);
               //System.out.println(data.getId()+" receve"+s);
               respons(m);
	   }
        }
    }

    public void propose(String val, long proposeID){
        String propose;
        try {
            network.updateConnectedProcessesList();
            this.data.setNumOfProces(network.lookupConnectedProcesses().size());
        }catch(InterruptedException e){e.printStackTrace();}
        propose = p.propose(val,proposeID);
        //System.out.println(data.getId()+" send propose"+propose);
        network.sendMessage(propose);
    }

    private void respons(Message m){
        String response = null;
        try {
            switch (m.getMessageType()) {
                case "PREPAREREQUEST":
                    response = a.processPrepareRequest(m);
                    break;
                case "RESPONDTOPREPAREREQUEST":
                    response = p.processRespondToPrepareRequest();
                    break;
                case "ACCEPTREQUEST":
                    response = a.processAcceptRequest(m);
                    break;
                case "DECISION":
                    l.processDecisionRequest(m);
                    break;
            }
            if (response != null) {
                //System.out.println(data.getId()+" send"+response);
                network.sendMessage(response);
            }
        }catch(Exception e){}
    }

    public long getid(){
        return data.getId();
    }

}
