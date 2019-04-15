package comp1206.sushi.server;

import java.net.ServerSocket;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import comp1206.sushi.common.Dish;
import comp1206.sushi.common.Drone;
import comp1206.sushi.common.Ingredient;
import comp1206.sushi.common.Order;
import comp1206.sushi.common.Postcode;
import comp1206.sushi.common.Restaurant;
import comp1206.sushi.common.Staff;
import comp1206.sushi.common.Supplier;
import comp1206.sushi.common.UpdateEvent;
import comp1206.sushi.common.UpdateListener;
import comp1206.sushi.common.User;
 
public class Server implements ServerInterface, UpdateListener{

   private static final Logger logger = LogManager.getLogger("Server");
	
	private Restaurant restaurant = null;
	private ArrayList<ServerMessageManager> msgManagers = new ArrayList<>();
	private ArrayList<Dish> dishes = new ArrayList<Dish>();
	private ArrayList<Drone> drones = new ArrayList<Drone>();
	private ArrayList<Ingredient> ingredients = new ArrayList<Ingredient>();
	private ArrayList<Order> orders = new ArrayList<Order>();
	private ArrayList<Staff> staff = new ArrayList<Staff>();
	private ArrayList<Supplier> suppliers = new ArrayList<Supplier>();
	private ArrayList<User> users = new ArrayList<User>();
	private ArrayList<Postcode> postcodes = new ArrayList<Postcode>();
	private ArrayList<UpdateListener> listeners = new ArrayList<UpdateListener>();
	
	private IngredientStockManager ingredientManager = new IngredientStockManager();
	private DishStockManager dishManager = new DishStockManager(ingredientManager);
	
	public Server() {
		logger.info("Starting up server...");
		loadConfiguration("C:\\Users\\BoBoRen\\Documents\\Test3.txt");
		Thread listenForClientThread = new Thread(new ClientListener());
		listenForClientThread.start();
	}
	
	public void setRestaurant(Restaurant restaurant) {
		this.restaurant = restaurant;
	}
	
	public class ClientListener implements Runnable{
		
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
			while (true) {
				ServerMessageManager newMsgManager = new ServerMessageManager(socket = serverSock.accept(), Server.this);
				msgManagers.add(newMsgManager);
				newMsgManager.notifyClient();
				executors.execute((newMsgManager));
				
				if (socket!= null && socket.isConnected()) {
					logger.info("Connection to client " + socket.getInetAddress());
				}
			}
		}
	}
	
	@Override
	public List<Dish> getDishes() {
		return this.dishes;
	}

	@Override
	public Dish addDish(String name, String description, Number price, Number restockThreshold, Number restockAmount) {
		Dish newDish = new Dish(name,description,price,restockThreshold,restockAmount);
		newDish.addUpdateListener(this);
		this.dishes.add(newDish);
		this.notifyUpdate();
		return newDish;
	}
	
	@Override
	public void removeDish(Dish dish) {
		this.dishes.remove(dish);
		this.notifyUpdate();
	}

	@Override
	public Map<Dish, Number> getDishStockLevels() {
		return dishManager.getDishStockLevels();
	}
	
	@Override
	public void setRestockingIngredientsEnabled(boolean enabled) {
		//Notify drones that they do not need to restock anymore ingredients
	}

	@Override
	public void setRestockingDishesEnabled(boolean enabled) {
		//Notify staff that they should not restock anymore dishes
		
	}
	
	@Override
	public void setStock(Dish dish, Number stock) {
		dishManager.setStock(dish, stock);
		this.notifyUpdate();
	}

	@Override
	public void setStock(Ingredient ingredient, Number stock) {
		ingredientManager.setStock(ingredient, stock);
		this.notifyUpdate();
	}

	@Override
	public List<Ingredient> getIngredients() {
		return this.ingredients;
	}

	@Override
	public Ingredient addIngredient(String name, String unit, Supplier supplier,
			Number restockThreshold, Number restockAmount, Number weight) {
		Ingredient mockIngredient = new Ingredient(name,unit,supplier,restockThreshold,restockAmount,weight);
		mockIngredient.addUpdateListener(this);
		this.ingredients.add(mockIngredient);
		this.notifyUpdate();
		return mockIngredient;
	}

	@Override
	public void removeIngredient(Ingredient ingredient) {
		int index = this.ingredients.indexOf(ingredient);
		this.ingredients.remove(index);
		this.notifyUpdate();
	}

	@Override
	public List<Supplier> getSuppliers() {
		return this.suppliers;
	}

	@Override
	public Supplier addSupplier(String name, Postcode postcode) {
		Supplier mock = new Supplier(name,postcode);
		this.suppliers.add(mock);
		this.notifyUpdate();
		return mock;
	}


	@Override
	public void removeSupplier(Supplier supplier) {
		int index = this.suppliers.indexOf(supplier);
		this.suppliers.remove(index);
		this.notifyUpdate();
	}

	@Override
	public List<Drone> getDrones() {
		return this.drones;
	}

	@Override
	public Drone addDrone(Number speed) {
		Drone mock = new Drone(speed);
		mock.addUpdateListener(this);
		this.drones.add(mock);
		return mock;
	}

	@Override
	public void removeDrone(Drone drone) {
		int index = this.drones.indexOf(drone);
		this.drones.remove(index);
		this.notifyUpdate();
	}

	@Override
	public List<Staff> getStaff() {
		return this.staff;
	}

	@Override
	public Staff addStaff(String name) {
		Staff mock = new Staff(name);
		mock.setStatus("Idle");
		mock.setDishStckManager(dishManager);
		mock.addUpdateListener(this);
		this.staff.add(mock);
		return mock;
	}
	
	public void addOrder(Order order) {
		order.setStatus("Incomplete");
		order.addUpdateListener(this);
		orders.add(order);
	}
	@Override
	public void removeStaff(Staff staff) {
		this.staff.remove(staff);
		this.notifyUpdate();
	}

	@Override
	public List<Order> getOrders() {
		return this.orders;
	}

	@Override
	public void removeOrder(Order order) {
		int index = this.orders.indexOf(order);
		this.orders.remove(index);
		this.notifyUpdate();
	}
	
	@Override
	public Number getOrderCost(Order order) {
		return order.getCost();
	}

	@Override
	public Map<Ingredient, Number> getIngredientStockLevels() {
		return ingredientManager.getIngredientStockLevel();
	}

	@Override
	public Number getSupplierDistance(Supplier supplier) {
		return supplier.getDistance();
	}

	@Override
	public Number getDroneSpeed(Drone drone) {
		return drone.getSpeed();
	}

	@Override
	public Number getOrderDistance(Order order) {
		return order.getDistance();
	}

	@Override
	public void addIngredientToDish(Dish dish, Ingredient ingredient, Number quantity) {
		if(quantity == Integer.valueOf(0)) {
			removeIngredientFromDish(dish,ingredient);
		} else {
			dish.getRecipe().put(ingredient,quantity);
		}
	}

	@Override
	public void removeIngredientFromDish(Dish dish, Ingredient ingredient) {
		dish.getRecipe().remove(ingredient);
		this.notifyUpdate();
	}

	@Override
	public Map<Ingredient, Number> getRecipe(Dish dish) {
		return dish.getRecipe();
	}

	@Override
	public List<Postcode> getPostcodes() {
		return this.postcodes;
	}

	@Override
	public Postcode addPostcode(String code) {
		Postcode mock = new Postcode(code);
		
		if (restaurant!= null) {
			mock.calculateDistance(restaurant);
		}
		
		mock.addUpdateListener(this);
		this.postcodes.add(mock);
		this.notifyUpdate();
		return mock;
	}

	@Override
	public void removePostcode(Postcode postcode) throws UnableToDeleteException {
		this.postcodes.remove(postcode);
		this.notifyUpdate();
	}
	
	public void addUser(User user) {
		users.add(user); 
	}
	
	@Override
	public List<User> getUsers() {
		return this.users;
	}
	
	@Override
	public void removeUser(User user) {
		this.users.remove(user);
		this.notifyUpdate();
	}

	@Override
	public void loadConfiguration(String filename) {
		Configuration config = new Configuration(filename, this, dishManager, ingredientManager);
		
		for (ServerMessageManager current: msgManagers) {
			current.notifyClient();
		}
		
		System.out.println("Loaded configuration: " + filename);
	}

	@Override
	public void setRecipe(Dish dish, Map<Ingredient, Number> recipe) {
		for(Entry<Ingredient, Number> recipeItem : recipe.entrySet()) {
			addIngredientToDish(dish,recipeItem.getKey(),recipeItem.getValue());
		}
		this.notifyUpdate();
	}

	@Override
	public boolean isOrderComplete(Order order) {
		if (order.getStatus().equals("Complete")) {
			return true;
		}
		
		else {
			return false;
		}
	}

	@Override
	public String getOrderStatus(Order order) {
		return order.getStatus();
	}
	
	@Override
	public String getDroneStatus(Drone drone) {
		return drone.getStatus();
	}
	
	@Override
	public String getStaffStatus(Staff staff) {
		return staff.getStatus();
	}

	@Override
	public void setRestockLevels(Dish dish, Number restockThreshold, Number restockAmount) {
		dish.setRestockThreshold(restockThreshold);
		dish.setRestockAmount(restockAmount);
		this.notifyUpdate();
	}

	@Override
	public void setRestockLevels(Ingredient ingredient, Number restockThreshold, Number restockAmount) {
		ingredient.setRestockThreshold(restockThreshold);
		ingredient.setRestockAmount(restockAmount);
		this.notifyUpdate();
	}

	@Override
	public Number getRestockThreshold(Dish dish) {
		return dish.getRestockThreshold();
	}

	@Override
	public Number getRestockAmount(Dish dish) {
		return dish.getRestockAmount();
	}

	@Override
	public Number getRestockThreshold(Ingredient ingredient) {
		return ingredient.getRestockThreshold();
	}

	@Override
	public Number getRestockAmount(Ingredient ingredient) {
		return ingredient.getRestockAmount();
	}

	@Override
	public void addUpdateListener(UpdateListener listener) {
		this.listeners.add(listener);
	}
	
	@Override
	public void notifyUpdate() {
		this.listeners.forEach(listener -> listener.updated(new UpdateEvent()));
	}

	@Override
	public Postcode getDroneSource(Drone drone) {
		return drone.getSource();
	}

	@Override
	public Postcode getDroneDestination(Drone drone) {
		return drone.getDestination();
	}

	@Override
	public Number getDroneProgress(Drone drone) {
		return drone.getProgress();
	}

	@Override
	public String getRestaurantName() {
		return restaurant.getName();
	}

	@Override
	public Postcode getRestaurantPostcode() {
		return restaurant.getLocation();
	}
	
	@Override
	public Restaurant getRestaurant() {
		return restaurant;
	}
	
	@Override
	public void updated(UpdateEvent updateEvent) {
		
		String modelName = updateEvent.model.getName();
		String updateProperty = updateEvent.property;
		Object oldValue = updateEvent.oldValue;
		Object newValue = updateEvent.newValue;
		
		if (oldValue != newValue) {
			System.out.println(updateProperty+" of "+modelName+" has changed from "+oldValue+" to "+newValue);
			logger.info(updateProperty+" of "+modelName+" has changed from "+oldValue+" to "+newValue);
		}
	}
}
