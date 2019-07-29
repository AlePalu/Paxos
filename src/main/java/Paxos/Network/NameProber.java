package Paxos.Network;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonValue;

import java.util.HashSet;

public class NameProber implements Runnable{

    // network broadcast is performed over UDP (no sense to talk about broadcast transmission in TCP, since it is about point-to-point connections)
    private DatagramSocket datagramSocket;
    private int port;
    private byte[] buff;

    private HashSet<MessageType> messageToProcess;
    
    public NameProber(int port){
	this.port = port;
	try{
	    this.datagramSocket = new DatagramSocket(port);
	}catch(Exception e){
	    return;
	}
	this.buff = new byte[1024];

	this.messageToProcess = new HashSet<MessageType>();

	this.messageToProcess.add(MessageType.WHEREISNAMING);
	this.messageToProcess.add(MessageType.NAMINGAT);
    }
    
    public void namingProbe(){
	try{
	    // send in broadcast a WHEREISNAMING message, wait for a response...
	    String WHEREISNAMINGmessage = MessageForgery.forgeWHEREISNAMING();

	    // open datagram socket
	    DatagramSocket socket = new DatagramSocket();
	    socket.setBroadcast(true);

	    InetAddress group = InetAddress.getByName("192.168.1.255");
	    DatagramPacket packet = new DatagramPacket(WHEREISNAMINGmessage.getBytes(), WHEREISNAMINGmessage.length(), group, this.port);
	    socket.send(packet);
	    socket.close();
	}catch(Exception e){
	    e.printStackTrace();
	    return;
	}
    }

    public void run(){
	while(true){
	    // listen for incoming messages
	    try{
		DatagramPacket packet = new DatagramPacket(this.buff, this.buff.length);
		this.datagramSocket.receive(packet); // block until something is received
	    
		String decodedPacket = new String(packet.getData(), 0, packet.getLength());

		// apply logic according to message type
		for(MessageType msgType : this.messageToProcess){
		    if(msgType.match(decodedPacket)){
			msgType.applyLogic(this, decodedPacket);
		    }
		}

		// avoid burning CPU
		Thread.sleep(50);
	    }catch(Exception e){
		e.printStackTrace();
		return;
	    }
	}
    }
    
}
