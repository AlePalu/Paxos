package Paxos.Network;

@FunctionalInterface
public interface CustomMessageLogic{
    void applyLogic(Object... o);
}
