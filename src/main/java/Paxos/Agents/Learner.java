package Paxos.Agents;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Learner {

    FileWriter fw;


    Learner(String path){
        try {
            File file = new File(path);
            if (!file.exists()) {
                file.createNewFile();
                System.out.println("qui");
            }
           fw  = new FileWriter(file,true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void learn(String s){
        try {
            fw.append(s);
            fw.append("\n");
            fw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
