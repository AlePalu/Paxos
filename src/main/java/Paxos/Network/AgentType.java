package Paxos.Network;

public enum AgentType{
    PROPOSER("PROPOSER"),
    ACCEPTOR("ACCEPTOR"),
    LEARNER("LEARNER");

    private final String type;

    AgentType(final String type){
	this.type = type;
    }

    public String toString(){
	return this.type;
    }
}
