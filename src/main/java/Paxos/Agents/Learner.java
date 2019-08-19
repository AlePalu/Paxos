package Paxos.Agents;

import java.io.*;

import Paxos.Network.Message;

public class Learner {

    private FileWriter fw;
    private PaxosData data;
    private int currentNumOfVoter;
    private boolean win;

    Learner(PaxosData data, String path){
        this.currentNumOfVoter=0;
        this.data = data;
        this.win = false;
        path = path + data.getId()+".txt";
        try {
            File file = new File(path);
            if (!file.exists()) {
                file.createNewFile();
            }
            fw  = new FileWriter(file,true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    synchronized void processDecisionRequest(Message m){
        //if(data.getRound() == m.getRound())
            currentNumOfVoter++;
        System.out.println("[Learner "+ data.getId()+"] receive a decision with ID: "+m.getProposeID()+" and Value: "+m.getValue());
        if (currentNumOfVoter > data.getNumOfProces()/2 && !win/*&& data.getRound() == m.getRound()*/) {
            //System.out.println("round "+data.getRound()+" round messaggio "+m.getRound() +" "+m.getValue()+" sono il "+currentNumOfVoter);
            System.out.println("[Learner "+ data.getId()+"] majority for: "+m.getProposeID()+" and Value: "+m.getValue());
            win = true;
            data.setCurrentValue(m.getValue());
            learn(data.getCurrentValue());
        }

    }

    synchronized private void reset(){
        System.out.println("aggiorno round e sono "+currentNumOfVoter+" con valore "+ data.getCurrentValue());
        currentNumOfVoter = 0;
        data.reset();
        data.nextRound();
    }

    synchronized private void learn(String s){
        try {
            fw.append(s);
            fw.append("\n");
            fw.flush();
           // reset();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
