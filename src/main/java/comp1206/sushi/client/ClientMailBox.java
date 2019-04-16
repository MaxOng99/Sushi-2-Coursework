package comp1206.sushi.client;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import comp1206.sushi.common.Comms;
import comp1206.sushi.common.Dish;
import comp1206.sushi.common.Order;
import comp1206.sushi.common.Restaurant;
import comp1206.sushi.common.User;

public class ClientMailBox implements Runnable{
	
	private Client client;
	private Comms clientSideComm;
	
	public ClientMailBox(Client client, Socket socket) {
		this.client = client;
		this.clientSideComm = new Comms(socket);
	}
	
	public void requestRegistration(User newUser) throws IOException{
		clientSideComm.sendMessage(newUser);
	}
	
	public void requestLogin(String credentials) throws IOException {
		clientSideComm.sendMessage(credentials);
	}
	
	public void notifyNewOrder(Order newOrder) throws IOException {
		clientSideComm.sendMessage(newOrder);
	}
	
	public void requestOderCancelation(Order orderToCancel) throws IOException {
		clientSideComm.sendMessage(orderToCancel);
	}
	
	public void setDishesInClient(ArrayList<Dish> serverDishes) {
		client.setDishes(serverDishes);
	}
	
	public void setRestaurantInClient(Restaurant restaurant) {
		client.setRestaurantAndPostcodes(restaurant);
	}
	
	@Override
	public void run() {
		while (true) {
			
			Object objectReceived = clientSideComm.receiveMessage(this);
			
			if (objectReceived == null) {
				return;
			}
			
			else {
				if (objectReceived instanceof User) {
					client.setRegisteredUser((User) objectReceived);
				}
				
				if (objectReceived instanceof ArrayList<?>) {
					setDishesInClient((ArrayList<Dish>) objectReceived);
				}
				
				else if (objectReceived instanceof Restaurant) {
					Restaurant serverRestaurant = (Restaurant) objectReceived;
					setRestaurantInClient(serverRestaurant);
				}
				
				else if (objectReceived instanceof Dish) {
					Dish newDish = (Dish) objectReceived;
					client.addNewDish(newDish);
				}
				
				else if(objectReceived instanceof String) {
					String input = (String) objectReceived;
					if (input.equals("Verification Failed")) {
						client.setRegisteredUser(null);
					}
				}
			}
		}
	}
}
