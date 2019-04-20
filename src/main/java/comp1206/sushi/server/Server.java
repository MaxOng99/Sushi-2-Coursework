package comp1206.sushi.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
	private ArrayList<ServerMailBox> mailBoxes = new ArrayList<>();
	private ArrayList<Dish> dishes = new ArrayList<Dish>();
	private ArrayList<Drone> drones = new ArrayList<Drone>();
	private ArrayList<Ingredient> ingredients = new ArrayList<Ingredient>();
	private ArrayList<Order> orders = new ArrayList<Order>();
	private ArrayList<Staff> staff = new ArrayList<Staff>();
	private ArrayList<Supplier> suppliers = new ArrayList<Supplier>();
	private ArrayList<User> users = new ArrayList<User>();
	private ArrayList<Postcode> postcodes = new ArrayList<Postcode>();
	private ArrayList<UpdateListener> listeners = new ArrayList<UpdateListener>();
	private IngredientStockManager ingredientManager = new IngredientStockManager(this);
	private DishStockManager dishManager = new DishStockManager(ingredientManager, this);
	private BlockingQueue<Order> orderDeliveryQueue = new LinkedBlockingQueue<>();
	private BlockingQueue<Order> waitingForDishRestock = new LinkedBlockingQueue<>();
	private Object lockOrder = new Object();
	private Object dishLock = new Object();
	
	
	
	public Server() {
		logger.info("Starting up server...");
		restaurant = new Restaurant("Southampton Sushi", new Postcode("SO17 1BJ"));
		Thread listenForClientThread = new Thread(new ClientListener(this, logger));
		listenForClientThread.start();
	}
	
	@Override
	public void loadConfiguration(String filename) {
		Configuration config = new Configuration(filename, this, dishManager, ingredientManager);	
		System.out.println("Loaded configuration: " + filename);
		
		for (ServerMailBox mailBox: mailBoxes) {
			try {
				if (!mailBox.getRegisteredUsers().isEmpty()) {
					for(User user: mailBox.getRegisteredUsers()) {
						this.users.add(user);
					}
				}
				mailBox.sendInitialDataToClient();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void setRestaurantFromConfig(Restaurant restaurantFromConfig) {
		this.restaurant = restaurantFromConfig;
	}
	
	public void setDishesFromConfig(ArrayList<Dish> dishesFromConfig) {
		this.dishes = dishesFromConfig;
		for (Dish dish: dishes) {
			dish.addUpdateListener(this);
		}
	}
	
	public void setStaffsFromConfig(ArrayList<Staff> staffsFromConfig) {
		this.staff = staffsFromConfig;
		for (Staff currentStaff: staff) {
			currentStaff.setDishStckManager(dishManager);
		}
	}
	public void setDronesFromConfig(ArrayList<Drone> dronesFromConfig) {
		this.drones = dronesFromConfig;
		for (Drone drone: drones) {
			drone.addUpdateListener(this);
			drone.setIngredientStockManager(ingredientManager);
			drone.setOrderQueue(orderDeliveryQueue);
			drone.setRestaurantPostcode(getRestaurantPostcode());
		}
	}
	
	public boolean checkLackingDish(Dish dish, int numberOfDishesOrdered) {
		boolean lackDishes;
		if (dishManager.getStock(dish) < numberOfDishesOrdered && dishManager.getStock(dish) >= (int) dish.getRestockThreshold()) {
			dish.setRestockType("Extra");
			lackDishes = true;
			return true;
		}
		else if (dishManager.getStock(dish) < numberOfDishesOrdered && dishManager.getStock(dish) < (int) dish.getRestockThreshold()) {
			dish.setRestockType("Normal");
			lackDishes = true;
		}
		else {
			lackDishes = false;
		}
		
		return lackDishes;
	}
	public void setOrders(ArrayList<Order> ordersFromConfig) {
		Thread checkAbleToDeliverThread = new Thread(new CheckAbleToDeliver());
		checkAbleToDeliverThread.start();
		this.orders = ordersFromConfig;
		for (Order order: this.orders) {
			order.setStatus("Incomplete");
			order.addUpdateListener(this);
			this.notifyUpdate();
			Map<Dish, Number> dishesOrdered = order.getUser().getBasket().getBasketMap();
			if (ableToDeliverOrder(order)) {
				System.out.println("Able to delive order lol");
				try {
					orderDeliveryQueue.put(order);
					for (Entry<Dish, Number> currentOrder: dishesOrdered.entrySet()) {
						
						for (Dish dish: getDishes()) {
							String dishOrderedName = currentOrder.getKey().getName();
							int currentDishStockDeducted = (int)currentOrder.getValue();
							if (dish.getName().equals(dishOrderedName)) {
								setStock(dish, -currentDishStockDeducted);
								break;
							}
						}
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			else {
				try {
					for (Entry<Dish, Number> currentOrder: dishesOrdered.entrySet()) {
						for (Dish dish: getDishes()) {
							if (dish.getName().equals(currentOrder.getKey().getName())) {
								if (checkLackingDish(dish, (int)currentOrder.getValue()) == true) {
									if (dish.getRestockType().equals("Extra")) {
										System.out.println("added to required extra stock dishlist");
										dishManager.requireExtraStock(dish);
									}
									else {
										dishManager.getLackIngredientList().put(dish);
									}
									break;
								}
							}
						}
					}
					waitingForDishRestock.put(order);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	@Override
	public List<Dish> getDishes() {
		return this.dishes;
	}
	
	public void addMailBoxes(ServerMailBox mailBox) {
		mailBoxes.add(mailBox);
	}
	
	public void informClientsOfDishUpdates(Dish dish) {
		for (ServerMailBox current: mailBoxes) {
			try {
				current.sendNewDish(dish);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
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
		dish.setAvailability(false);
		this.informClientsOfDishUpdates(dish);
		this.notifyUpdate();
	}

	@Override
	public Map<Dish, Number> getDishStockLevels() {
		return dishManager.getDishStockLevels();
	}
	
	@Override
	public void setRestockingIngredientsEnabled(boolean enabled) {
		for (Drone drone: this.drones) {
			drone.setRestockStatus(false);
		}
	}

	@Override
	public void setRestockingDishesEnabled(boolean enabled) {
		for (Staff staff: this.staff) {
			staff.setRestockStatus(false);
		}
		
	}
	
	@Override
	public void setStock(Dish dish, Number stock) {
		synchronized(dishLock) {
			dishManager.setStock(dish, stock);
			this.notifyUpdate();
		}
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
		mock.setIngredientStockManager(ingredientManager);
		mock.setOrderQueue(orderDeliveryQueue);
		mock.setRestaurantPostcode(getRestaurantPostcode());
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
		Staff mock = new Staff(name, this);
		mock.setStatus("Idle");
		mock.setDishStckManager(dishManager);
		mock.addUpdateListener(this);
		this.staff.add(mock);
		return mock;
	}
	
	public boolean ableToDeliverOrder(Order order) {
		boolean ableToDeliver = false;
		Map<Dish, Number> dishesOrdered = order.getUser().getBasket().getBasketMap();
		for (Entry<Dish, Number> currentEntry: dishesOrdered.entrySet()) {
			
			for (Dish dish: getDishes()) {
				if (dish.getName().equals(currentEntry.getKey().getName())) {
					if (dishManager.getStock(dish) >= (int) currentEntry.getValue()) {
						ableToDeliver = true;
					}
					else {
						return false;
					}
				}
			}
		}
		return ableToDeliver;
	}
	
	public void addOrder(Order order) {
		
		synchronized(lockOrder) {
			order.setStatus("Incomplete");
			order.addUpdateListener(this);
			orders.add(order);
			this.notifyUpdate();
			Map<Dish, Number> dishesOrdered = order.getUser().getBasket().getBasketMap();
			if (ableToDeliverOrder(order)) {
				try {
					orderDeliveryQueue.put(order);
					for (Entry<Dish, Number> currentOrder: dishesOrdered.entrySet()) {
						
						for (Dish dish: getDishes()) {
							String dishOrderedName = currentOrder.getKey().getName();
							int currentDishStockDeducted = (int)currentOrder.getValue();
							if (dish.getName().equals(dishOrderedName)) {
								setStock(dish, -currentDishStockDeducted);
								break;
							}
						}
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			else {
				try {
					for (Entry<Dish, Number> currentOrder: dishesOrdered.entrySet()) {
						for (Dish dish: getDishes()) {
							if (dish.getName().equals(currentOrder.getKey().getName())) {
								if (checkLackingDish(dish, (int)currentOrder.getValue()) == true) {
									if (dish.getRestockType().equals("Extra")) {
										System.out.println("added to required extra stock dishlist");
										dishManager.requireExtraStock(dish);
									}
									else {
										dishManager.getLackIngredientList().put(dish);
									}
									break;
								}
							}
						}
					}
					waitingForDishRestock.put(order);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
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
		if(quantity == Float.valueOf(0)) {
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
		dishManager.addDishToStockManager(dish, 0);
		this.setStock(dish, 0);
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
		this.notifyUpdate();
		String modelName = updateEvent.model.getName();
		String updateProperty = updateEvent.property;
		Object oldValue = updateEvent.oldValue;
		Object newValue = updateEvent.newValue;
		
		if (oldValue != newValue) {
			System.out.println(updateProperty+" of "+modelName+" has changed from "+oldValue+" to "+newValue);
			logger.info(updateProperty+" of "+modelName+" has changed from "+oldValue+" to "+newValue);
		}
	}
	
	class CheckAbleToDeliver implements Runnable{

		@Override
		public void run() {
			while(true) {
				try {
					Order order = waitingForDishRestock.take();
					if (ableToDeliverOrder(order)) {
						orderDeliveryQueue.put(order);
						Map<Dish, Number> dishesOrdered = order.getUser().getBasket().getBasketMap();
						for (Entry<Dish, Number> currentOrder: dishesOrdered.entrySet()) {
							
							for (Dish dish: getDishes()) {
								String dishOrderedName = currentOrder.getKey().getName();
								int currentDishStockDeducted = (int)currentOrder.getValue();
								if (dish.getName().equals(dishOrderedName)) {
									setStock(dish, -currentDishStockDeducted);
									break;
								}
							}
						}
					}
					else {
						waitingForDishRestock.put(order);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
