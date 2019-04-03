package Paxos.Agents;

import java.io.*;

import Paxos.Network.Message;

public class Learner {

    private FileWriter fw;
    private PaxosData data;
    private int currentNumOfVoter;

    Learner(PaxosData data, String path){
        this.currentNumOfVoter=0;
        this.data = data;
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

    void processDecisionRequest(Message m){
        currentNumOfVoter++;
        if (currentNumOfVoter > data.getNumOfProces()/2 && data.getRound() == m.getRound()) {
            data.setCurrentValue(m.getValue());
            learn(data.getCurrentValue());
        }

    }

    private void reset(){
        data.reset();
        data.nextRound();
    }

    private void learn(String s){
        try {
            fw.append(s);
            fw.append("\n");
            fw.flush();
            reset();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
