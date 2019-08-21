package Paxos;

import Paxos.Agents.AgentHandler;
import Paxos.Network.Main;

import java.util.Random;

public class Paxos {

    public static boolean isnetworksSarted = false;

    public static void init(Boolean isNaming){
        String[] args = new String[1];
        args[0]="-n";
        if(isNaming)
            Main.main(args);
        else
            Main.main(null);
        isnetworksSarted = true;
        try{Thread.sleep(2000);}
        catch(Exception e){}
        System.out.println("network set up");
    }

    public static AgentHandler addProces(){
        if(!isnetworksSarted) {
            System.out.println("please start network");
            return null;
        }
        Random rng = new Random();
        AgentHandler agent = new AgentHandler((long)Math.abs(rng.nextLong()),"./src/main/java/Paxos/");
        Thread t = new Thread(agent);
        t.start();
        return agent;
    }

    public static AgentHandler addProces(String path){
        if(!isnetworksSarted) {
            System.out.println("please start network");
            return null;
        }
        Random rng = new Random();
        AgentHandler agent = new AgentHandler((long)Math.abs(rng.nextLong()),path);
        Thread t = new Thread(agent);
        t.start();
        return agent;
    }

    public static AgentHandler addProces(Long ID){
        if(!isnetworksSarted) {
            System.out.println("please start network");
            return null;
        }
        AgentHandler agent = new AgentHandler(ID,"./src/main/java/Paxos/");
        Thread t = new Thread(agent);
        t.start();
        return agent;
    }

    public static AgentHandler addProces(Long ID, String path){
        if(!isnetworksSarted) {
            System.out.println("please start network");
            return null;
        }
        AgentHandler agent = new AgentHandler(ID,path);
        Thread t = new Thread(agent);
        t.start();
        return agent;
    }

    public static void propose(Long proposID, String value, AgentHandler agent){
        agent.propose(value,proposID);
    }

    public static String readConsensus(AgentHandler agent){
        return agent.readConsensus();
    }
}
