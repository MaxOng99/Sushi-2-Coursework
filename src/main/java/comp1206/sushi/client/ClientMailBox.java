package comp1206.sushi.client;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.ListIterator;

import comp1206.sushi.common.Comms;
import comp1206.sushi.common.Dish;
import comp1206.sushi.common.Order;
import comp1206.sushi.common.Restaurant;
import comp1206.sushi.common.User;

public class ClientMailBox implements Runnable{
	
	private Client client;
	private Comms clientSideComm;
	private User registeredUser;
	private ArrayList<Dish> serverDishes = new ArrayList<>();
	
	public ClientMailBox(Client client, Socket socket) {
		this.client = client;
		this.clientSideComm = new Comms(socket);
	}
	
	public void setRegisteredUser(User registeredUser) {
		this.registeredUser = registeredUser;
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
	
	public ArrayList<Dish> getDishes(){
		return this.serverDishes;
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
					ArrayList<Dish> serverDishes = (ArrayList<Dish>) objectReceived;
					if (registeredUser == null) {
						this.serverDishes = ((ArrayList<Dish>) objectReceived);	
					}
					else {
						client.setDishes(serverDishes);
						client.notifyUpdate();
						System.out.println("I am running");
					}
				}
				
				else if (objectReceived instanceof Restaurant) {
					Restaurant serverRestaurant = (Restaurant) objectReceived;
					setRestaurantInClient(serverRestaurant);
				}
				
				else if(objectReceived instanceof String) {
					String input = (String) objectReceived;
					if (input.equals("Verification Failed")) {
						client.setRegisteredUser(null);
					}
				}
				
				else if (objectReceived instanceof Order) {
					Order order = (Order) objectReceived;
					
					for (Order userOrder: registeredUser.getOrders()) {
						if (order.getName().equals(userOrder.getName())) {
							userOrder.setStatus("Complete");
						}
					}
				}
				
				else if (objectReceived instanceof Dish) {
					Dish dishReceived = (Dish) objectReceived;
					if (dishReceived.getAvailability() == true) {
						client.addNewDish(dishReceived);
					}
					
					else {
						ListIterator<Dish> dishIt = client.getDishes().listIterator();
						while(dishIt.hasNext()) {
							Dish dish = dishIt.next();
							if (dish.getName().equals(dishReceived.getName())){
								dishIt.remove();
								client.notifyUpdate();
							}
						}
					}
				}
			}
		}
	}
}
