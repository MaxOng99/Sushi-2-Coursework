package comp1206.sushi.server;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import comp1206.sushi.common.Basket;
import comp1206.sushi.common.Dish;
import comp1206.sushi.common.Drone;
import comp1206.sushi.common.Ingredient;
import comp1206.sushi.common.Order;
import comp1206.sushi.common.Postcode;
import comp1206.sushi.common.Restaurant;
import comp1206.sushi.common.Supplier;
//import org.apache.commons.lang3.StringUtils;
import comp1206.sushi.common.User;

public class Configuration {
	
	private Server server;
	private String filename;
	private Postcode restaurantPostcode;
	private String restaurantName;
	private DishStockManager dishStockManager;
	private IngredientStockManager ingredientStockManager;
	private Map<Dish, Number> configDishStock;
	private Map<Ingredient, Number> configIngredientStock;
	private List<User> users;
	private ArrayList<Dish> dishes;
	private ArrayList<Drone> drones;
	
	public Configuration(String filename, Server server, DishStockManager serverStockManager, IngredientStockManager serverIngredientManager) {
		this.filename=filename;
		this.server = server;	
		this.users = new ArrayList<>();
		this.dishes = new ArrayList<>();
		this.dishStockManager = serverStockManager;
		this.ingredientStockManager = serverIngredientManager;
		this.configDishStock = new HashMap<>();
		this.configIngredientStock = new HashMap<>();
		this.drones = new ArrayList<>();
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
					server.addPostcode(splitted[2]);
					restaurantPostcode = new Postcode(splitted[2]);
					Restaurant restaurant = new Restaurant(restaurantName, restaurantPostcode);
					server.setRestaurant(restaurant);
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
						float droneSpeed = Float.parseFloat(splitted[1]);
						drones.add(new Drone(droneSpeed));
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
					Dish dishToAdd = new Dish(splitted[1], splitted[2], price, restockThreshold, restockAmount);
					dishes.add(dishToAdd);
					for (String current: recipe) {
						String[] ingredientQttyPair = current.split(" \\* ");
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
					for (Dish dish: dishes) {
						if (dish.getName().equals(splitted[1])) {
							if (validateNumeric(splitted[2])) {
								int dishStock = Integer.parseInt(splitted[2]);
								configDishStock.put(dish, dishStock);
								break;
							}
						}
					}
					
					for (Ingredient ingredient: server.getIngredients()) {
						if (ingredient.getName().equals(splitted[1])) {
							if (validateNumeric(splitted[2])) {
								int ingredientStock = Integer.parseInt(splitted[2]);
								configIngredientStock.put(ingredient, ingredientStock);
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
							Basket userBasket = current.getBasket();
							String[] orders = splitted[2].split(",");
							for (String currentOrder: orders) {
								String[] orderQuantityPair = currentOrder.split(" \\* ");
								for (Dish currentDish: dishes) {
									if (currentDish.getName().equals(orderQuantityPair[1])) {
										int quantity = Integer.parseInt(orderQuantityPair[0]);
										userBasket.addDishToBasket(currentDish, quantity);
									}
								}
							}
							server.addOrder(new Order(current));
						}
					}
				}
			}
			
			server.setDishesFromConfig(dishes);
			server.setDronesFromConfig(drones);
			ingredientStockManager.initializeStockFromConfig(configIngredientStock);
			dishStockManager.initializeStockFromConfig(configDishStock);
		}
		catch(IOException e) {
			System.err.print(e.getMessage());
		}
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
}
