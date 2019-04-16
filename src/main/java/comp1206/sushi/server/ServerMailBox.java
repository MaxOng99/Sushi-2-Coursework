package comp1206.sushi.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
	
	public ServerMailBox(Server server, Socket socket, InetAddress clientIP) {
		this.server = server;
		serverSideComm = new Comms(socket, clientIP);
		this.clientIP = clientIP;
	}
	
	public void sendInitialDataToClient() throws IOException{
		List<Dish> serverDishes = server.getDishes();
		serverSideComm.sendMessage(serverDishes);
		
		Restaurant serverRestaurant = server.getRestaurant();
		serverSideComm.sendMessage(serverRestaurant);
	}
	
	public void sendNewDish(Dish dish) throws IOException {
		serverSideComm.sendMessage(dish);
	}
	
	public void addUser(User user) {
		server.addUser(user);
	}
	
	public void addNewOrder(Order order) {
		User userOfNewOrder = order.getUser();
		
		for (User user: server.getUsers()) {
			if (user.getName().equals(userOfNewOrder.getName()) && user.getPassword().equals(userOfNewOrder.getPassword())) {
				server.addOrder(order);
				Basket basket = order.getBasket();
				user.updateBasket(basket);
				break;
			}
		}
		
		Map<Dish, Number> dishesOrdered = order.getUser().getBasket().getBasketMap();
		for (Entry<Dish, Number> dishQttyPair: dishesOrdered.entrySet()) {
			
			for (Dish dish: server.getDishes()) {
				if (dish.getName().equals(dishQttyPair.getKey().getName())) {
					server.setStock(dish, -(int) dishQttyPair.getValue());
					
					Map<Ingredient, Number> recipe = dish.getRecipe();
					for(Entry<Ingredient, Number> currentEntry: recipe.entrySet()) {
						server.setStock(currentEntry.getKey(), -(Float)currentEntry.getValue());
					}
				}
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

	
	@Override
	public void run() {
		try {
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
