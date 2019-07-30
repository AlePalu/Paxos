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

public class NameProber implements Runnable{

    // network broadcast is performed over UDP (no sense to talk about broadcast transmission in TCP, since it is about point-to-point connections)
    private DatagramSocket datagramSocket;
    private int port;
    private byte[] buff;
    private long UUID;

    private SocketBox socketBox;
    
    private HashSet<MessageType> messageToProcess;
    private Random rng;
    
    private static NameProber instance;
    
    private NameProber(){
	this.port = 40001;

	this.rng = new Random();
	Long randomNumber = Math.abs(rng.nextLong());
	this.UUID = randomNumber;
	
	try{
	    // open connection with network infrastructure
	    Socket connSocket = new Socket(Inet4Address.getLocalHost().getHostAddress(), 40000); // connecting to NetworkInfrastructure
	    this.socketBox = new SocketBox(connSocket);
	    this.socketBox.setUUID(SocketRegistry.getInstance().getMachineUUID());

	    String PROBERSUBSCRIBEmessage = MessageForgery.forgePROBERSUBSCRIBE();
	    this.socketBox.sendOut(PROBERSUBSCRIBEmessage);
	    
	    this.datagramSocket = new DatagramSocket(this.port);
	}catch(Exception e){
	    return;
	}
	this.buff = new byte[1024];

	this.messageToProcess = new HashSet<MessageType>();

	this.messageToProcess.add(MessageType.WHEREISNAMING);
	this.messageToProcess.add(MessageType.NAMINGAT);
	this.messageToProcess.add(MessageType.PING);	
    }

    public static NameProber getInstance(){
	if(instance == null)
	    instance = new NameProber();
	return instance;
    }
    
    public void namingProbe(){
	try{
	    System.out.printf("[NameProber]: where is naming?%n");
	    // send in broadcast a WHEREISNAMING message, wait for a response...
	    String WHEREISNAMINGmessage = MessageForgery.forgeWHEREISNAMING(this.UUID);

	    this.socketBox.sendOut(WHEREISNAMINGmessage);
	    
	    // open datagram socket
	    DatagramSocket socket = new DatagramSocket();
	    socket.setBroadcast(true);

	    InetAddress group = InetAddress.getByName("192.168.1.255");
	    DatagramPacket packet = new DatagramPacket(WHEREISNAMINGmessage.getBytes(), WHEREISNAMINGmessage.length(), group, this.port);
	    socket.send(packet);
	}catch(Exception e){
	    e.printStackTrace();
	    return;
	}
    }
    
    public void namingProbeResponse(DatagramPacket packet){
	String namingIP = SocketRegistry.getInstance().getNamingSocket().getSocket().getInetAddress().getHostAddress();
	String NAMINGATmessage = MessageForgery.forgeNAMINGAT(namingIP);

	// send datagram back, with IP address of the name server
	InetAddress addr = packet.getAddress();

	// unicast transmission
	DatagramPacket responsePacket = new DatagramPacket(NAMINGATmessage.getBytes(), NAMINGATmessage.length(), addr, this.port);
	
	try{
	    this.datagramSocket.send(responsePacket);
	}catch(Exception e){
	    e.printStackTrace();
	    return;
	}
    }

    public void run(){
	System.out.printf("[NameProber]: ready to respond to any naming discovery%n");
	while(true){
	    // listen for incoming messages
	    try{
		DatagramPacket packet = new DatagramPacket(this.buff, this.buff.length);
		this.datagramSocket.receive(packet); // block until something is received
	    
		String decodedPacket = new String(packet.getData(), 0, packet.getLength());

		// messages coming from UDP external connections
		for(MessageType msgType : this.messageToProcess){
		    if(msgType.match(decodedPacket)){
			msgType.applyLogic(packet, this);
		    }
		}
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
