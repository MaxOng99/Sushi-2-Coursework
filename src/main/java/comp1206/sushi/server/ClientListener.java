package comp1206.sushi.server;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.Logger;

public class ClientListener implements Runnable{
	
	private Server server;
	private Logger logger;
	
	public ClientListener(Server server, Logger logger) {
		this.server = server;
		this.logger = logger;
	}
		public void run() {
			try {
				listenForClients();
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		public void listenForClients() throws Exception{
			ServerSocket serverSock = new ServerSocket(49920);
			ExecutorService executors = Executors.newFixedThreadPool(10);
			Socket socket = null;
			InetAddress clientIP = null;
			while (true) {
				ServerMailBox newMailBox = new ServerMailBox(server, socket = serverSock.accept(), socket.getInetAddress());
				server.addMailBoxes(newMailBox);
				clientIP = socket.getInetAddress();
				executors.execute((newMailBox));
				
				if (socket!= null && socket.isConnected()) {
					logger.info("Connected to client " + clientIP);
				}
			}
		}
	}