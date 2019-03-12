package Paxos.Network;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.Socket;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import java.net.Inet4Address;

public class SocketBox{

    private Socket socket;

    private BufferedReader socketInputStream;
    private BufferedWriter socketOutputStream;
    
    public SocketBox(Socket socket) throws IOException{
	this.socket = socket;
	this.socketInputStream = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
	this.socketOutputStream = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));
    }

    public Socket getSocket(){
	return this.socket;
    }

    public BufferedReader getInputStream(){
	return this.socketInputStream;
    }

    public BufferedWriter getOutputStream(){
	return this.socketOutputStream;
    }

    public void sendOut(String message){
	JsonObject outboundJSONMessage = Json.parse(message).asObject();
	try{
	    // automatically add the IP of the machine where the local process is running
	    JsonObject Jmessage = Json.parse(message).asObject();
	    if (Jmessage.get("NAME")==null) // if a name field is already present, don't append a new one (this cause problems with name server)
		outboundJSONMessage.add("NAME", Inet4Address.getLocalHost().getHostAddress());
	    
	    // send the message
	    this.socketOutputStream.write(outboundJSONMessage.toString());
	    this.socketOutputStream.newLine();
	    this.socketOutputStream.flush();
	}catch(Exception e){
	    
	}
    }
    
    public void close() throws IOException{
	this.socket.close();
	this.socketInputStream.close();
	this.socketOutputStream.close();
    }
}
