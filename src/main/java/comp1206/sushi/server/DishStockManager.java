package comp1206.sushi.server;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import comp1206.sushi.common.Dish;
import comp1206.sushi.common.Drone;
import comp1206.sushi.common.Ingredient;
import comp1206.sushi.common.Staff;


public class DishStockManager {
	
	private IngredientStockManager ingredientManager;
	private Random restockTime = new Random();
	private Map<Dish, Number> dishStock;
	private BlockingQueue<Dish> dishRestockQueue = new ArrayBlockingQueue<Dish>(10);
	private BlockingQueue<Dish> dishDeliveryQueue = new ArrayBlockingQueue<Dish>(10);
	
	public DishStockManager(IngredientStockManager serverIngredientManager) {
		ingredientManager = serverIngredientManager;
	}
	
	public void initializeStockFromConfig(Map<Dish, Number> configStock) {
		dishStock = configStock;
	}
	
	public Map<Dish, Number> getDishStockLevels() {
		return dishStock;
	}
	
	public synchronized void setStock(Dish dish, Number quantity){	
		int dishQuantity = (int) dishStock.get(dish);
		int newDishQuantity = dishQuantity + (int) + (int) quantity;
		dishStock.replace(dish, newDishQuantity);
		
		if (newDishQuantity < (int) dish.getRestockThreshold()) {
			try {
				if (dishRestockQueue.contains(dish)) {
					System.out.println("no");
				}
				else {
					dishRestockQueue.put(dish);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public boolean checkAbleToRestock(Dish dish) {
		Map<Ingredient, Number> recipe = dish.getRecipe();
		boolean ableToRestock = false;
		for (Entry<Ingredient, Number> current: recipe.entrySet()) {
			if (!((int) ingredientManager.getIngredientStock(current.getKey()) > (int) current.getValue())) {
				System.out.println("Lacking " + current);
				break;
			}
			else {
				ableToRestock = true;
			}
		}
		return ableToRestock;
	}
	
	public void makeDishes(Staff staff) {
		try {
			Dish dishTaken = dishRestockQueue.take();
			int currentStockValue = (int)dishStock.get(dishTaken);
			int restockThreshold = (int) dishTaken.getRestockThreshold();
			int restockAmount = (int) dishTaken.getRestockAmount();
			
			while(currentStockValue < restockThreshold) {
				staff.setStatus("Restocking " + dishTaken + "...");
				Thread.sleep(restockTime.nextInt(40001) + 30000);
				dishStock.replace(dishTaken, restockAmount + currentStockValue);
				currentStockValue += restockAmount;
			}
			
			staff.setStatus("Idle");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void deliverDish(Drone drone) {
		
	}
}