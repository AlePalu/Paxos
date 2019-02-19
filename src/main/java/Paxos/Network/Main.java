package Paxos.Network;

class Main{

    public static void main(String[] args) {
	System.out.printf("starting Main...\n");
	try{
	    ConnectionHandler connectionHandler = new ConnectionHandler(4455);
	    // start thread
	    connectionHandler.run();
	}catch(Exception e){
	    return;
	}
    }
}
