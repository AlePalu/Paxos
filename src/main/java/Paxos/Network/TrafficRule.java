package Paxos.Network;

@FunctionalInterface
public interface TrafficRule{
    void applyRule(SocketBox socket, String message);
}


