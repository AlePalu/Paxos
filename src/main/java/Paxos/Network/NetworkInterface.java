package Paxos.Network;

import java.io.IOException;
import java.util.ArrayList;

public interface NetworkInterface{
    public void sendMessage(String msg);
    public String reeciveMessage();
    public Boolean isThereAnyMessage();
    public ArrayList<Long> lookupConnectedProcess();
}
