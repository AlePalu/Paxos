package Paxos.Agents;

import java.io.*;

import Paxos.Network.Message;

public class Learner {

    private FileWriter fw;
    private BufferedReader fr;
    private PaxosData data;
    private int currentNumOfVoter;
    private boolean win = false;

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
            fr  = new BufferedReader(new FileReader(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void processDecisionRequest(Message m){
        currentNumOfVoter++;
        if (currentNumOfVoter > data.getNumOfProces()/2 && !win) {
            this.win = true;
            data.setCurrentValue(m.getValue());
            learn(data.getCurrentValue());
        }

    }

    private void reset(){
        data.reset();
        win = false;

    }

    private void learn(String s){
        try {
            if(!fr.readLine().equals(data.getCurrentValue())) {
                fw.append(s);
                fw.append("\n");
                fw.flush();
            }
            reset();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
