package Paxos.Network;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.Socket;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

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
	try{
	    this.socketOutputStream.write(message);
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
