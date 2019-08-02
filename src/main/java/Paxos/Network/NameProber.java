package Paxos.Network;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.Socket;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonValue;

import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

class NameProberExecutor implements Runnable{

    private ConcurrentLinkedQueue<DatagramPacket> messageQueue;
    private HashSet<MessageType> messageToProcess;
    
    public NameProberExecutor(){
	this.messageQueue = new ConcurrentLinkedQueue<DatagramPacket>();
	this.messageToProcess = new HashSet<MessageType>();

	this.messageToProcess.add(MessageType.WHEREISNAMING);
	this.messageToProcess.add(MessageType.NAMINGAT);
	this.messageToProcess.add(MessageType.NPELECT);
	this.messageToProcess.add(MessageType.NPBULLYSUPPRESS);
	this.messageToProcess.add(MessageType.COORD);
	this.messageToProcess.add(MessageType.NAMESTATUS);
    }

    public void putMsg(DatagramPacket message){
	this.messageQueue.add(message);
    }

    public void run(){
	while(true){
	    if(!this.messageQueue.isEmpty()){
		DatagramPacket packet = this.messageQueue.remove();
		String decodedPacket = new String(packet.getData(), 0, packet.getLength());
		
		// apply message logic
		for(MessageType msgType : this.messageToProcess){
		    if(msgType.match(decodedPacket)){
			msgType.applyLogic(packet, this);
		    }
		}
	    }
	    try{
		Thread.sleep(50);
	    }catch(Exception e){
		continue;
	    }
	}
    }
    
}

public class NameProber implements Runnable{

    // network broadcast is performed over UDP (no sense to talk about broadcast transmission in TCP, since it is about point-to-point connections)
    private DatagramSocket datagramSocket;
    private int port;
    private byte[] buff;
    private long UUID;

    private SocketBox socketBox;
    
    private Random rng;

    private NameProberExecutor executor;
    
    private static NameProber instance;

    private NameProber(){
        this.port = 40001;

	this.rng = new Random();
	Long randomNumber = Math.abs(rng.nextLong());
	this.UUID = randomNumber;
	
	try{
	    // open connection with network infrastructure
	    Socket connSocket = new Socket(Inet4Address.getLocalHost().getHostAddress(), 40000);
	    this.socketBox = new SocketBox(connSocket);
	    this.socketBox.setUUID(SocketRegistry.getInstance().getMachineUUID());

        try{
            // open connection with network infrastructure
            Socket connSocket = new Socket(Inet4Address.getLocalHost().getHostAddress(), 40000); // connecting to NetworkInfrastructure
            this.socketBox = new SocketBox(connSocket);
            this.socketBox.setUUID(SocketRegistry.getInstance().getMachineUUID());

	this.executor = new NameProberExecutor();

	Thread execThread = new Thread(this.executor);
	execThread.start();
    }

    public static NameProber getInstance(){
        if(instance == null)
            instance = new NameProber();
        return instance;
    }

    public void sendUDPBroadcast(String message){
	try{
	    JsonObject Jmessage = Json.parse(message).asObject();
	    // add IP of local machine and UUID, exactly like sendOut() method in SocketBox
	    Jmessage.add(MessageField.NAME.toString(), Inet4Address.getLocalHost().getHostAddress());
	    Jmessage.add(MessageField.MACHINEUUID.toString(), this.socketBox.getUUID());

	    // open datagram socket
	    DatagramSocket socket = new DatagramSocket();
	    socket.setBroadcast(true);

	    message = Jmessage.toString();
	    // broadcast transmission group
	    InetAddress group = InetAddress.getByName("192.168.1.255");
	    DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), group, this.port);
	    socket.send(packet);
	}catch(Exception e){
	    e.printStackTrace();
	    return;
	}
    }

    public void respondUDPUnicast(DatagramPacket receivedPacket, String message){
	try{
	    JsonObject Jmessage = Json.parse(message).asObject();
	    // add IP of local machine and UUID, exactly like sendOut() method in SocketBox
	    Jmessage.add(MessageField.NAME.toString(), Inet4Address.getLocalHost().getHostAddress());
	    Jmessage.add(MessageField.MACHINEUUID.toString(), this.socketBox.getUUID());

	    // send datagram back, with IP address of the name server
	    InetAddress addr = receivedPacket.getAddress();
	    String response = Jmessage.toString();
	    
	    // unicast transmission
	    DatagramPacket responsePacket = new DatagramPacket(response.getBytes(), response.length(), addr, this.port);
	    this.datagramSocket.send(responsePacket);
	}catch(Exception e){
	    e.printStackTrace();
	    return;
	}
    }
    
    public void namingProbe(){
	System.out.printf("[NameProber]: where is naming?%n");
	// send in broadcast a WHEREISNAMING message, wait for a response...
	try{
	    String WHEREISNAMINGmessage = MessageForgery.forgeWHEREISNAMING(this.UUID, Inet4Address.getLocalHost().getHostAddress());

	    // inform network infrastructure of the request
	    this.socketBox.sendOut(WHEREISNAMINGmessage);

	    // send the message in broadcast
	    this.sendUDPBroadcast(WHEREISNAMINGmessage);
	}catch(Exception e){

	}
    }
    
    public void namingProbeResponse(DatagramPacket packet){
	try{
	    String namingIP = SocketRegistry.getInstance().getNamingSocket().getSocket().getInetAddress().getHostAddress();
	    String NAMINGATmessage = MessageForgery.forgeNAMINGAT(namingIP, Inet4Address.getLocalHost().getHostAddress());
	    this.respondUDPUnicast(packet, NAMINGATmessage);
	}catch(Exception e){

	}
    }

    public void bullyElection(){
	System.out.printf("[NameProber]: starting election of name server...%n");
	// start election, sending an ELECT in broadcast
	String NPELECTmessage = MessageForgery.forgeNPELECT();
	this.sendUDPBroadcast(NPELECTmessage);

	// now We have to wait for a possible NPBULLYSUPPRESS... this is performed by the central Tracking system
	this.socketBox.sendOut(NPELECTmessage);
    }
    
    public void run(){
	System.out.printf("[NameProber]: ready to respond to naming discovery%n");
	while(true){
	    // listen for incoming messages
	    try{
		DatagramPacket packet = new DatagramPacket(this.buff, this.buff.length);
		this.datagramSocket.receive(packet); // block until something is received
		
		// messages coming from UDP external connections
		this.executor.putMsg(packet);
	    }catch(Exception e){
		e.printStackTrace();
		return;
	    }
	}
    }

    public Long getUUID(){
        return this.UUID;
    }

    public SocketBox getSocketBox(){
        return this.socketBox;
    }

}
