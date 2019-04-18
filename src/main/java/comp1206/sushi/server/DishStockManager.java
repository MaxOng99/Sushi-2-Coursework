package comp1206.sushi.server;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import comp1206.sushi.common.Dish;
import comp1206.sushi.common.Ingredient;


public class DishStockManager {
	
	private Server server;
	private IngredientStockManager ingredientManager;
	private Random restockTime = new Random();
	private Map<Dish, Number> dishStock;
	private BlockingQueue<Dish> dishRestockQueue;
	private BlockingQueue<Dish> lackIngredientList;
	private Thread checkAbleToRestock;
	
	public DishStockManager(IngredientStockManager serverIngredientManager, Server server) {
		this.server = server;
		ingredientManager = serverIngredientManager;
		dishRestockQueue = new LinkedBlockingQueue<>();
		lackIngredientList = new LinkedBlockingQueue<>();
		checkAbleToRestock = new Thread(new CheckAbleToRestock());
		checkAbleToRestock.start();
	}
	
	public void addDishToStockManager(Dish dish, Number quantity) {
		dishStock.put(dish, quantity);
	}
	
	public void initializeStockFromConfig(Map<Dish, Number> configStock) {
		dishStock = configStock;
		for (Entry<Dish, Number> currentDishStockPair: dishStock.entrySet()) {
			Dish currentDish = currentDishStockPair.getKey();
			Number currentDishStock = currentDishStockPair.getValue();
			if ((int)currentDishStockPair.getValue() < (int)currentDish.getRestockThreshold()) {
				currentDish.setAvailability(false);
				server.informClientsOfDishUpdates(currentDish);
				if (ableToRestock(currentDish) == true) {
					try {
						dishRestockQueue.put(currentDish);
						currentDish.setRestockStatus(true);
						server.informClientsOfDishUpdates(currentDish);
						System.out.println("Added to Restocking Queue");
					}
					
					catch(InterruptedException e) {
						e.printStackTrace();
					}
				}
				else {
					if (!lackIngredientList.contains(currentDish)) {
						try {
							lackIngredientList.put(currentDish);
							System.out.println("Added to lackOfIngredientQueue");
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
	
	public int getDishStock(Dish dish) {
		return (int) dishStock.get(dish);
	}
	
	public Map<Dish, Number> getDishStockLevels() {
		return dishStock;
	}
	
	public BlockingQueue<Dish> getDishRestockQueue() {
		return dishRestockQueue;
	}
	
	public boolean ableToRestock(Dish dish) {
		Map<Ingredient, Number> recipe = dish.getRecipe();
		boolean ableToRestock = false;
		for (Entry<Ingredient, Number> current: recipe.entrySet()) {
			Ingredient currentIngredient = current.getKey();
			if (currentIngredient.getIngredientAvailability() == true) {
				if (!(ingredientManager.getIngredientStock(current.getKey()) > (int) current.getValue())) {
					System.out.println("Lacking " + current);
				}
				else {
					ableToRestock = true;
				}
			}
		}
		return ableToRestock;
	}
	
	public void setStock(Dish dish, Number quantity){	
		int dishQuantity = (int) dishStock.get(dish);
		int newDishQuantity = dishQuantity + (int) quantity;
		dishStock.replace(dish, newDishQuantity);
		System.out.println(dish.getName() + " " + newDishQuantity + " " + dish.getRestockThreshold());
		if (newDishQuantity < (int) dish.getRestockThreshold()) {
			System.out.println(dish.getName() + " " + newDishQuantity);
			try {
				if (dishRestockQueue.contains(dish) || ableToRestock(dish) == false) {
					lackIngredientList.add(dish);
					System.out.println("Not enough ingredients");
				}
				else {
					if (dish.beingRestocked() == false) {
						dish.setRestockStatus(true);
						dish.setAvailability(false);
						dishRestockQueue.put(dish);
						server.informClientsOfDishUpdates(dish);
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	class CheckAbleToRestock implements Runnable {

		@Override
		public void run() {
			while(true) {
				Dish dish = null;
				try {
					dish = lackIngredientList.take();
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				if (dish!=null) {
					try {
						if (ableToRestock(dish)) {
							dishRestockQueue.put(dish);
						}
						else {
							lackIngredientList.put(dish);
						}
					}
					catch(InterruptedException e2) {
						e2.printStackTrace();
					}
					
				}
				
			}
		}
		
	}
}