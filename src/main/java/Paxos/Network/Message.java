package Paxos.Network;

class Message{
    private Integer ID;
    private Integer value;

    public Message(Integer ID, Integer value){
	this.ID = ID;
	this.value = value;
    }
    
    public Integer getID(){
	return this.ID;
    }

    public Integer getValue(){
	return this.value;
    }

    
    
}
