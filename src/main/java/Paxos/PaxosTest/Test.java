package Paxos.PaxosTest;

import  Paxos.Agents.*;

import java.util.Scanner;
import java.util.Random;


public class Test {

    public static void main(String[] args) throws Exception{
        Scanner s = new Scanner(System.in);
        Random rng = new Random();
        AgentHandler a1 = new AgentHandler((long)rng.nextLong(),"./src/main/java/Paxos/PaxosTest/");
        Thread t = new Thread(a1);
        t.start();
        AgentHandler a2 = new AgentHandler((long)rng.nextLong(),"./src/main/java/Paxos/PaxosTest/");
        Thread t2 = new Thread(a2);
        t2.start();
        AgentHandler a3 = new AgentHandler((long)rng.nextLong(),"./src/main/java/Paxos/PaxosTest/");
        Thread t3 = new Thread(a3);
        t3.start();


        s.nextLine();

        Thread.sleep(1000);

       // System.out.println(a1.getid()+" think java sucks");
      //  System.out.println(a3.getid()+" think java is wonderful");


        a3.propose("java is wonderful",250);

        Thread.sleep(3000);
        a2.propose("java sucks",12);

	    // Thread.sleep(1000);
       // a3.propose("java i1s wonder2ful");

    }

}

