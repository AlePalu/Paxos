package Paxos.Network;

class Pair<TypeA,TypeB>{

    private TypeA first;
    private TypeB second;

    public Pair(TypeA first,TypeB second){
	this.first = first;
	this.second = second;
    }

    public TypeA getFirst(){
	return this.first;
    }

    public TypeB getSecond(){
	return this.second;
    }
    
}
