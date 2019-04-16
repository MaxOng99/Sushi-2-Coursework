package comp1206.sushi.server;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import comp1206.sushi.common.Dish;
import comp1206.sushi.common.Ingredient;
import comp1206.sushi.common.Staff;


public class DishStockManager {

	private IngredientStockManager ingredientManager;
	private Random restockTime = new Random();
	private Map<Dish, Number> dishStock;
	private BlockingQueue<Dish> dishRestockQueue;
	
	public DishStockManager(IngredientStockManager serverIngredientManager, Server server) {
		ingredientManager = serverIngredientManager;
		dishRestockQueue = new LinkedBlockingQueue<>();
	}
	
	public void initializeStockFromConfig(Map<Dish, Number> configStock) {
		dishStock = configStock;
		for (Entry<Dish, Number> currentDishStockPair: dishStock.entrySet()) {
			if ((int)currentDishStockPair.getValue() < (int)currentDishStockPair.getKey().getRestockThreshold()) {
				try {
					dishRestockQueue.put(currentDishStockPair.getKey());
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public Map<Dish, Number> getDishStockLevels() {
		return dishStock;
	}
	
	public boolean checkAbleToRestock(Dish dish) {
		Map<Ingredient, Number> recipe = dish.getRecipe();
		boolean ableToRestock = false;
		for (Entry<Ingredient, Number> current: recipe.entrySet()) {
			if (!(ingredientManager.getIngredientStock(current.getKey()) > (Float) current.getValue())) {
				System.out.println("Lacking " + current);
			}
			else {
				ableToRestock = true;
			}
		}
		return ableToRestock;
	}
	
	public synchronized void setStock(Dish dish, Number quantity){	
		int dishQuantity = (int) dishStock.get(dish);
		int newDishQuantity = dishQuantity + (int) quantity;
		dishStock.replace(dish, newDishQuantity);
		if (newDishQuantity < (int) dish.getRestockThreshold()) {
			try {
				if (dishRestockQueue.contains(dish) || checkAbleToRestock(dish) == false) {
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
}