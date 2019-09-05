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

    synchronized void processDecisionRequest(Message m){
            currentNumOfVoter++;
        System.out.println("[Learner "+ data.getId()+"] receive a decision with ID: "+m.getProposeID()+" and Value: "+m.getValue());
        if (currentNumOfVoter > data.getNumOfProces()/2 && !data.getLearnerwin()) {
            System.out.println("[Learner "+ data.getId()+"] majority for: "+m.getProposeID()+" and Value: "+m.getValue());
            data.setLearnerwin();
            data.setCurrentValue(m.getValue());
            learn(data.getCurrentValue());
        }

    }

    synchronized private void reset(){
        System.out.println("[Learner "+ data.getId()+"] new Round");
        currentNumOfVoter = 0;
        data.reset();
        data.nextRound();
    }

    synchronized private void learn(String s){
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
