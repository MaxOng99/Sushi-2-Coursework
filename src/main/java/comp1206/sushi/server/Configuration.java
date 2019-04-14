package comp1206.sushi.server;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import comp1206.sushi.common.Dish;
import comp1206.sushi.common.Ingredient;
import comp1206.sushi.common.Order;
import comp1206.sushi.common.Postcode;
import comp1206.sushi.common.Restaurant;
import comp1206.sushi.common.Staff;
import comp1206.sushi.common.Supplier;
//import org.apache.commons.lang3.StringUtils;
import comp1206.sushi.common.User;

public class Configuration {
	
	private Server server;
	private String filename;
	private Postcode restaurantPostcode;
	private String restaurantName;
	private Map<Ingredient, Number> ingredientStock;
	private Map<Dish, Number> dishStock;
	private DishStockManager dishStockManager;
	private List<User> users;
	private List<Dish> dishes;
	
	public Configuration(String filename, Server server, DishStockManager serverStockManager) {
		this.filename=filename;
		this.server = server;	
		this.ingredientStock = new HashMap<>();
		this.dishStock = new HashMap<>();
		this.users = new ArrayList<>();
		this.dishes = new ArrayList<>();
		this.dishStockManager = serverStockManager;
		removeServerData();
		
		try {
			readLine();
		}
		catch(FileNotFoundException lol) {
			System.err.println(lol.getMessage());
		}
	}
	
	public void removeServerData() {
		server.getDishes().clear();
		server.getDrones().clear();
		server.getStaff().clear();
		server.getIngredients().clear();
		server.getSuppliers().clear();
		server.getUsers().clear();
		server.getOrders().clear();
		server.getPostcodes().clear();
		this.dishStockManager.getDishStockLevels().clear();
	}
	
	public void readLine() throws FileNotFoundException {
		BufferedReader bf = new BufferedReader(new FileReader(filename));
		String line;
	
		try {
			while((line = bf.readLine()) != null) {
				String[] splitted = line.split(":");
				
				if (line.contains("POSTCODE")) {
					server.addPostcode(splitted[1]);
				}
				
				else if (line.contains("RESTAURANT")) {
					restaurantName = splitted[1];
					
					for (Postcode current: server.getPostcodes()) {
						if (current.getName().equals(splitted[2])) {
							restaurantPostcode = current;
						}
					}
				}
				
				else if (line.contains("SUPPLIER")) {
					
					for (Postcode postcode: server.getPostcodes()) {
						if (postcode.getName().equals(splitted[2])) {			
							server.addSupplier(splitted[1], postcode);
							break;
						}
					}
				}
				
				else if (line.contains("STAFF")) {
					server.addStaff(splitted[1]);
				}
				
				else if (line.contains("DRONE")) {
					
					if (validateNumeric(splitted[1]) == true) {
						float dronSpeed = Float.parseFloat(splitted[1]);
						server.addDrone(dronSpeed);
					}
				}
				
				else if (line.contains("INGREDIENT")) {
					for (Supplier supplier: server.getSuppliers()) {
						if (supplier.getName().equals(splitted[3])) {
							
							if (validateNumeric(splitted[4]) && validateNumeric(splitted[5]) && validateNumeric(splitted[6])) {
								int restockThreshold = Integer.parseInt(splitted[4]);
								int restockAmount = Integer.parseInt(splitted[5]);
								int weight = Integer.parseInt(splitted[6]);
								
								server.addIngredient(splitted[1], splitted[2], supplier, restockThreshold, restockAmount, weight);
								break;
							}
						}
					}
				}
				
				else if (line.contains("DISH")) {
					float price = Float.parseFloat(splitted[3]);
					int restockThreshold = Integer.parseInt(splitted[4]);
					int restockAmount = Integer.parseInt(splitted[5]);
					String[] recipe = splitted[6].split(",");
					Map<Ingredient, Number> dishRecipe = new HashMap<>();
					Dish dishToAdd = server.addDish(splitted[1], splitted[2], price, restockThreshold, restockAmount);
					dishes.add(dishToAdd);
					for (String current: recipe) {
						String[] ingredientQttyPair = current.split(" * ");
						float quantity = Float.parseFloat(ingredientQttyPair[0]);
						
						for (Ingredient currentIngredient: server.getIngredients()) {
							if (currentIngredient.getName().equals(ingredientQttyPair[1])) {
								dishRecipe.put(currentIngredient, quantity);
								break;
							}
						}
					}
					
					server.setRecipe(dishToAdd, dishRecipe);
				}
				
				else if (line.contains("STOCK")) {
					for (Dish dish: server.getDishes()) {
						if (dish.getName().equals(splitted[1])) {
							if (validateNumeric(splitted[2])) {
								int dishStock = Integer.parseInt(splitted[2]);
								//this.dishStock.put(dish, dishStock);
								this.dishStockManager.getDishStockLevels().put(dish, dishStock);
								break;
							}
						}
					}
					
					for (Ingredient ingredient: server.getIngredients()) {
						if (ingredient.getName().equals(splitted[1])) {
							if (validateNumeric(splitted[2])) {
								int ingredientStock = Integer.parseInt(splitted[2]);
								this.ingredientStock.put(ingredient, ingredientStock);
								break;
							}
						}
					}
				}
				
				else if (line.contains("USER")) {
					Postcode userPostcode = null;
					for (Postcode current: server.getPostcodes()) {
						if (current.getName().equals(splitted[4])) {
							userPostcode = current;
							break;
						}
					}
					User userFromConfig = new User(splitted[1], splitted[2], splitted[3],userPostcode);
					users.add(userFromConfig);
					server.getUsers().add(userFromConfig);
				}
				
				else if (line.contains("ORDER")) {
					for (User current: users) {
						String userName = splitted[1];
						if (current.getName().equals(userName)) {
							String[] orders = splitted[2].split(",");
							for (String currentOrder: orders) {
								String[] orderQuantityPair = currentOrder.split(" * ");
								int quantity = Integer.parseInt(orderQuantityPair[0]);
								server.addOrder(new Order(current));
								
							}
						}
					}
				}
			}
		}
		catch(IOException e) {
			System.err.print(e.getMessage());
		}
	}
	
	public Map<Dish, Number> getDishStock() {
		return this.dishStock;
	}
	
	public Map<Ingredient, Number> getIngredientStock() {
		return this.ingredientStock;
	}
	public boolean validateNumeric(String value) {
		/*
		if (isNumeric(value)) {
			return true;
		}
		else {
			return false;
		}
		*/
		return true;
	}
	
	public Restaurant getRestaurant() {
		Restaurant restaurant = new Restaurant(restaurantName, restaurantPostcode);
		return restaurant;
	}
	
	public DishStockManager getDishStockManager() {
		return dishStockManager;
	}
}
