package Paxos.NetworkTest;

import Paxos.Network.LocalNetworkProcess;
import Paxos.Network.NetworkInterface;

import java.net.InetAddress;
import java.util.Random;

public class Test {

    public static void main(String[] args) {

        Random rng = new Random();
        Long pid = Math.abs(rng.nextLong());

        try{
            NetworkInterface myProcess = new LocalNetworkProcess(InetAddress.getLocalHost().getHostAddress(), 40000, pid);
            Thread netThread = new Thread(myProcess);
            netThread.start();

            while(true){
                Thread.sleep(1000);
                myProcess.updateConnectedProcessesList();
                System.out.println(myProcess.lookupConnectedProcesses().size());

            }
        }catch(Exception w){
            w.printStackTrace();
        }

    }


}
