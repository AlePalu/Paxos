package Paxos.Network;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

class SocketBox{

    private Socket socket;
    private Scanner socketInputStream;
    private PrintWriter socketOutputStream;

    public SocketBox(Socket socket) throws IOException{
	this.socket = socket;
	this.socketInputStream = new Scanner(this.socket.getInputStream());
	this.socketOutputStream = new PrintWriter(this.socket.getOutputStream());
    }

    public Socket getSocket(){
	return this.socket;
    }

    public Scanner getInputStream(){
	return this.socketInputStream;
    }

    public PrintWriter getOutputStream(){
	return this.socketOutputStream;
    }

    public void close() throws IOException{
	this.socket.close();
	this.socketInputStream.close();
	this.socketOutputStream.close();
    }
}
