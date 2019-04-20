package comp1206.sushi.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import comp1206.sushi.common.Basket;
import comp1206.sushi.common.Comms;
import comp1206.sushi.common.Dish;
import comp1206.sushi.common.Ingredient;
import comp1206.sushi.common.Order;
import comp1206.sushi.common.Restaurant;
import comp1206.sushi.common.User;

public class ServerMailBox implements Runnable{
	
	private Server server;
	private Comms serverSideComm;
	private InetAddress clientIP;
	private List<Order> orders;
	private List<User> registeredUsers;
	private boolean reloadedConfiguration;
	
	public ServerMailBox(Server server, Socket socket, InetAddress clientIP) {
		this.reloadedConfiguration = false;
		this.registeredUsers = new ArrayList<User>();
		this.server = server;
		serverSideComm = new Comms(socket, clientIP);
		this.clientIP = clientIP;
		this.orders = new CopyOnWriteArrayList<>();
	}
	
	public List<User> getRegisteredUsers() {
		return registeredUsers;
	}
	
	public void sendInitialDataToClient() throws IOException{
		
		if (clientIP != null) {
			Restaurant serverRestaurant = server.getRestaurant();
			serverSideComm.sendMessage(server.getDishes());
			serverSideComm.sendMessage(serverRestaurant);
		}
		else {
			return;
		}
		
	}
	
	public void sendNewDish(Dish dish) throws IOException {
		serverSideComm.sendMessage(dish);
	}
	
	public void addUser(User user) {
		this.registeredUsers.add(user);
		server.addUser(user);
	}
	
	public void addNewOrder(Order order) {
		User userOfNewOrder = order.getUser();
		String username = userOfNewOrder.getName();
		String password = userOfNewOrder.getPassword();
		for (User user: server.getUsers()) {
			if (user.getName().equals(username) && user.getPassword().equals(password)) {
				server.addOrder(order);
				System.out.println("Yee har");
				this.orders.add(order);
				Basket basket = order.getBasket();
				user.updateBasket(basket);
				break;   
			}
		}
	}
	
	public void cancelOrder(Order order) {
		for (Order current: server.getOrders()) {
			if(current.getName().equals(order.getName())) {
				current.setStatus("Canceled");
				break;
			}
		}
	}
	
	public void verifyCredentials(String credentials) throws IOException{
		String[] credential = credentials.split(":");
		
		for (User user: server.getUsers()) {
			if (credential[0].equals(user.getUsername()) && credential[1].equals(user.getPassword())) {
				serverSideComm.sendMessage(user);
			}
			
			else {
				serverSideComm.sendMessage("Verification Failed");
			}
		}
	}
	
	class InformCompleteOrder implements Runnable {
		@Override
		public void run() {
			
			while(true) {
				if (!ServerMailBox.this.orders.isEmpty()) {
					for (Order order: ServerMailBox.this.orders) {
						if (server.isOrderComplete(order)) {
							try {
								serverSideComm.sendMessage(order);
								ServerMailBox.this.orders.remove(order);
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}
					}
				}
			}	
		}
		
	}
	
	@Override
	public void run() {
		try {
			Thread informOrderThread = new Thread(new InformCompleteOrder());
			informOrderThread.start();
			sendInitialDataToClient();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		
		while (true) {
			if (serverSideComm.getClientIP() != null) {
				Object objectReceived = serverSideComm.receiveMessage(this);
				if (objectReceived instanceof User) {
					addUser((User) objectReceived);
				}
				
				else if (objectReceived instanceof Order) {
					Order order = (Order) objectReceived;
					
					if (order.getStatus().equals("Incomplete")) {
						addNewOrder(order);
						System.out.println("Added new Order from server mailBox");
					}
					
					else if (order.getStatus().equals("Canceled")) {
						for (Order current: server.getOrders()) {
							if(current.getName().equals(order.getName())) {
								current.setStatus("Canceled");
								break;
							}
						}
					}
				}	
				
				else if (objectReceived instanceof String) {
					String credentials = (String) objectReceived;
					if (credentials.contains(":")) {
						try {
							verifyCredentials(credentials);
						}
						
						catch(IOException verificationFailed) {
							verificationFailed.printStackTrace();
						}
					}
				}
			}
			else {
				break;
			}
		}
	}
}
