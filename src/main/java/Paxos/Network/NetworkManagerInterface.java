package Paxos.Network;

import java.io.IOException;
import java.util.ArrayList;

public interface NetworkManagerInterface{
    public void enqueueMessage(Message msg);
    public Message dequeueMessage(long pid);
    public Boolean isThereAnyMessage(long pid);
    public void subscribeProcess(long pid) throws IOException;
    public ArrayList<Long> lookupConnectedProcess();
}
