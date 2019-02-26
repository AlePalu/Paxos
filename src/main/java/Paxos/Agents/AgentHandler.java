package Paxos.Agents;

import Paxos.Network.LocalNetworkProcess;
import Paxos.Network.Message;
import Paxos.Network.NetworkInterface;
import com.eclipsesource.json.JsonObject;

import java.io.IOException;
import java.util.Random;

public class AgentHandler extends Agent implements Runnable {
    public NetworkInterface network;
    private Aceptor a;
    private Learner l;
    private Proposer p;
    private Long id;


    public AgentHandler(){
        Random r = new Random();
        id = Math.abs(r.nextLong());
        try {
            network = new LocalNetworkProcess("127.0.0.1",40000,id);
            Thread netThread = new Thread(network);
            a = new Aceptor();
            a.id = id;
            l = new Learner("/home/prosdothewolf/Desktop/",id);
            p = new Proposer();
            netThread.start();
            p.updateProcessCount(network.lookupConnectedProcesses().size());
        }catch(IOException e){e.printStackTrace();}
    }

    public AgentHandler(Long n){
        id = n;
        try {
            network = new LocalNetworkProcess("127.0.0.1",40000,n);
            Thread netThread = new Thread(network);
            a = new Aceptor();
            a.id = id;
            l = new Learner("/home/prosdothewolf/Desktop/",n);
            p = new Proposer();
            netThread.start();
            p.updateProcessCount(network.lookupConnectedProcesses().size());
        }catch(IOException e){e.printStackTrace();}
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
               respons(m);
            }
        }
    }

    public void propose(String val){
        Message propose;
        try {
            network.updateConnectedProcessesList();
            p.updateProcessCount(network.lookupConnectedProcesses().size());
        }catch(InterruptedException e){e.printStackTrace();}
        propose = p.propose(val);
        network.sendMessage(propose.getJSON());
    }

    private void respons(Message m){
        Message response = null;
        switch (m.getMessageType()){
            case"PREPAREREQUEST":
                response = a.processPrepareRequest(m);
                break;
            case"RESPONDTOPREPAREREQUEST":
                response = p.processRespondToPrepareRequest(m);
                break;
            case "ACCEPTREQUEST":
                l.learn(m.getValue());
              // reset();
                break;
        }
        if (response != null)
            network.sendMessage(response.getJSON());
    }

    private void reset(){
        p.reset();
        a.reset();
    }

    public long getid(){
        return id;
    }

}
