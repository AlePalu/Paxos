package Paxos.Agents;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
        if (currentNumOfVoter > data.getNumOfProces()/2 && data.getCurrentValue() == null) {
            data.setCurrentValue(m.getValue());
            learn(data.getCurrentValue());
        }

    }

    private void learn(String s){
        try {
            data.reset();
            fw.append(s);
            fw.append("\n");
            fw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
