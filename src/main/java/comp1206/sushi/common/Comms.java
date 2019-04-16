package comp1206.sushi.common;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

import comp1206.sushi.client.ClientMailBox;
import comp1206.sushi.server.ServerMailBox;

public class Comms{
	
	private Socket socket;
	private ObjectOutputStream output;
	private ObjectInputStream input;
	private InetAddress clientIP;
	
	public Comms(Socket socket) {
		this.socket = socket;
		
		try {
			output = new ObjectOutputStream(this.socket.getOutputStream());
			input = new ObjectInputStream(this.socket.getInputStream());
		}
		catch(IOException e) {
			System.out.println("Something went wrong");
		}	
	}
	
	public Comms(Socket socket, InetAddress clientIP) {
		this(socket);
		this.clientIP = socket.getInetAddress();
	}
	
	public void sendMessage(Object object) throws IOException {
		output.writeObject(object);
		output.flush();
		output.reset();
	}
	
	public Object receiveMessage(ServerMailBox serverMails) {
		Object objectReceived = null;
		
		try {
			objectReceived = input.readObject();
		}
		catch(IOException e) {
			try {
				System.out.println("Loss connection to client " + clientIP);
				socket.close();
				output.close();
				input.close();
				clientIP = null;
			}
			catch(IOException e2) {
				e.printStackTrace();
			}   
		}
		catch(ClassNotFoundException cnfe) {
			cnfe.printStackTrace();
		}
		
		return objectReceived;
	}
	
	public Object receiveMessage(ClientMailBox clientMail) {
		Object objectReceived = null;
		
		try {
			objectReceived = input.readObject();
		}
		catch(IOException e) {
			try {
				socket.close();
				output.close();
				input.close();
				System.out.println("Loss connection to Server");
			}
			catch(IOException e2) {
				e.printStackTrace();
			}
		}
		catch(ClassNotFoundException cnfe) {
			cnfe.printStackTrace();
		}
		
		return objectReceived;
	}
	
	public ObjectInputStream getInputStream() {
		return input;
	}
	
	public ObjectOutputStream getOutputStream() {
		return output;
	}
	
	public InetAddress getClientIP() {
		return clientIP;
	}
}
