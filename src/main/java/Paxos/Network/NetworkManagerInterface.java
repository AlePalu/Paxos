package Paxos.Network;

import jdk.internal.agent.Agent;

public interface NetworkManagerInterface{
    public void enqueueMessage(Message msg);
    public Message dequeueMessage(Agent agent);
    public Boolean isThereAnyMessage(Agent agent);
    public void subscribe(Agent agent);
}
