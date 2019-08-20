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
import java.util.concurrent.ConcurrentHashMap;

import com.eclipsesource.json.Json;

class DataTuple<T,M>{

    private T first;
    private M second;

    public DataTuple(T first, M second){
	this.first = first;
	this.second = second;
    }

    public T getFirstValue(){
	return this.first;
    }

    public M getSecondValue(){
	return this.second;
    }
}

class NameProberExecutor implements Runnable{

    private ConcurrentLinkedQueue<DataTuple<String, DatagramPacket>> messageQueue;
    private HashSet<MessageType> messageToProcess;

    private ConcurrentLinkedQueue<DatagramPacket> broadcastOutgoingQueue;
    private ConcurrentLinkedQueue<DatagramPacket> unicastOutgoingQueue;
    
    private DatagramSocket datagramSocket;

    // reliable multicast
    private Integer msgID;
    private ConcurrentHashMap<Integer, DatagramPacket> msgHistory;
    private ConcurrentHashMap<String, Integer> counterTracking;
    private ConcurrentHashMap<String, Integer> retransmissionTracking;
    
    private int port;
    
    public NameProberExecutor(){
	this.port = 40002;
	this.messageQueue = new ConcurrentLinkedQueue<DataTuple<String, DatagramPacket>>();
	this.messageToProcess = new HashSet<MessageType>();

	try{
	    this.broadcastOutgoingQueue = new ConcurrentLinkedQueue<DatagramPacket>();
	    this.datagramSocket = new DatagramSocket(this.port);

	    this.unicastOutgoingQueue = new ConcurrentLinkedQueue<DatagramPacket>();
	}catch(Exception e){
	    e.printStackTrace();
	}
	
	this.messageToProcess.add(MessageType.WHEREISNAMING);
	this.messageToProcess.add(MessageType.NAMINGAT);
	
	// reliable multicast
	this.msgID = 0;
	
	this.msgHistory = new ConcurrentHashMap<Integer, DatagramPacket>();
	this.counterTracking = new ConcurrentHashMap<String, Integer>();
	this.retransmissionTracking = new ConcurrentHashMap<String, Integer>();
    }

    public void process(String message, DatagramPacket datagram){
	DataTuple<String, DatagramPacket> tuple = new DataTuple<String, DatagramPacket> (message, datagram);
	this.messageQueue.add(tuple);
    }
    
    public void sendMsg(DatagramPacket message){	
	String msg = new String(message.getData(), 0, message.getLength());
	
	InetAddress addr = message.getAddress();
	DatagramPacket packet = new DatagramPacket(msg.getBytes(), msg.length(), addr, 40001);

	this.unicastOutgoingQueue.add(packet);
    }

    public void sendBroadcastMsg(DatagramPacket message){	
	String msg = new String(message.getData(), 0, message.getLength());
	try{
	    InetAddress addr = InetAddress.getByName("192.168.1.255");
	    DatagramPacket packet = new DatagramPacket(msg.getBytes(), msg.length(), addr, 40001);

	    //recordMsg(packet);
	    this.broadcastOutgoingQueue.add(packet);
	}catch(Exception e){
	    e.printStackTrace();
	}
    }

    public void run(){
	
	while(true){
	    try{
		if(!this.messageQueue.isEmpty()){ // IN
		    DataTuple<String,DatagramPacket> tuplePacket = this.messageQueue.remove();

		    String decodedPacket = tuplePacket.getFirstValue();
		    DatagramPacket packet = tuplePacket.getSecondValue();

		    // apply message logic
		    for(MessageType msgType : this.messageToProcess){
			if(msgType.match(decodedPacket)){
			    msgType.applyLogic(packet, this);
			}
		    }

		}
		if(!this.broadcastOutgoingQueue.isEmpty()){ // OUT (broadcast transmission)
		    DatagramPacket outboundMessage = broadcastOutgoingQueue.remove();

		    DatagramSocket bSock = new DatagramSocket();
		    bSock.setBroadcast(true);
		    
		    bSock.send(outboundMessage);
		    bSock.close();
		}
		if(!this.unicastOutgoingQueue.isEmpty()){ // OUT (unicast transmission)
		    DatagramPacket outboundMessage = unicastOutgoingQueue.remove();
		    this.datagramSocket.send(outboundMessage);
		}
	    
		Thread.sleep(50);
	    }catch(Exception e){
		continue;
	    }
	}
    }
    
}

public class NameProber implements Runnable{

    // network broadcast is performed over UDP (no sense to talk about broadcast transmission in TCP, since it is about point-to-point connections)
    private int port;
    private byte[] buff;
    private long UUID;
    private SocketBox socketBox;

    private Random rng;

    private NameProberExecutor executor;
    private static NameProber instance;

    private DatagramSocket datagramSocket;
    
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

	    String PROBERSUBSCRIBEmessage = MessageForgery.forgePROBERSUBSCRIBE();
	    this.socketBox.sendOut(PROBERSUBSCRIBEmessage);

	    this.datagramSocket = new DatagramSocket(this.port);
	    this.executor = new NameProberExecutor();	    
	}catch(Exception e){
	    e.printStackTrace();
	    return;
	}
	this.buff = new byte[2048];

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

	    message = Jmessage.toString();
	    // broadcast transmission group
	    InetAddress group = InetAddress.getByName("192.168.1.255");
	    DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), group, this.port);
	    this.executor.sendBroadcastMsg(packet);

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
	    this.executor.sendMsg(responsePacket);

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
	    e.printStackTrace();
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
    
    public void run(){
	System.out.printf("[NameProber]: ready to respond to naming discovery%n");
	
	while(true){
	    // listen for incoming messages
	    try{
		DatagramPacket packet = new DatagramPacket(this.buff, this.buff.length);
		this.datagramSocket.receive(packet); // block until something is received

		String decodedPacket = new String(packet.getData(), 0, packet.getLength());
		
		// messages coming from UDP external connections
		this.executor.process(decodedPacket, packet);
		Thread.sleep(50);

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

    public NameProberExecutor getExecutor(){
	return this.executor;
    }
    
}
