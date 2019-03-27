package Paxos.Network;

import java.util.ArrayList;

public interface NetworkInterface extends Runnable{
    public void sendMessage(String msg);
    public String receiveMessage();
    public boolean isThereAnyMessage();
    public ArrayList<Long> lookupConnectedProcesses();
    public void updateConnectedProcessesList() throws InterruptedException;

    public void run();
}
