package Paxos.Network;

class Main{

    public static void main(String[] args) {
	System.out.printf("starting Main...\n");
	try{
	    ConnectionHandler connectionHandler = new ConnectionHandler(40000);
	    Thread connectionHandlerThread = new Thread(connectionHandler);
	    // start thread
	    connectionHandlerThread.start();
	}catch(Exception e){
	    return;
	}
    }
}
