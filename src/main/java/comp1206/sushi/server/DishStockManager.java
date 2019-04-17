package comp1206.sushi.server;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import comp1206.sushi.common.Dish;
import comp1206.sushi.common.Ingredient;


public class DishStockManager {

	private IngredientStockManager ingredientManager;
	private Random restockTime = new Random();
	private Map<Dish, Number> dishStock;
	private BlockingQueue<Dish> dishRestockQueue;
	private List<Dish> lackIngredientList;
	private Thread checkAbleToRestock;
	
	public DishStockManager(IngredientStockManager serverIngredientManager, Server server) {
		ingredientManager = serverIngredientManager;
		dishRestockQueue = new LinkedBlockingQueue<>();
		lackIngredientList = Collections.synchronizedList(new ArrayList<Dish>());
		checkAbleToRestock = new Thread(new CheckAbleToRestock());
		checkAbleToRestock.start();
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
			if (!(ingredientManager.getIngredientStock(current.getKey()) > (Float) current.getValue())) {
				System.out.println("Lacking " + current);
			}
			else {
				ableToRestock = true;
			}
		}
		return ableToRestock;
	}
	
	public void setStock(Dish dish, Number quantity){	
		int dishQuantity = (int) dishStock.get(dish);
		int newDishQuantity = dishQuantity + (int) quantity;
		dishStock.replace(dish, newDishQuantity);
		if (newDishQuantity < (int) dish.getRestockThreshold()) {
			try {
				if (dishRestockQueue.contains(dish) || ableToRestock(dish) == false) {
					lackIngredientList.add(dish);
					System.out.println("Not enough ingredients");
				}
				else {
					if (dish.getRestockStatus() == false) {
						dish.setRestockStatus(true);
						dishRestockQueue.put(dish);
					}
					
					else {
						System.out.println("Dish being restock");
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
				if (!lackIngredientList.isEmpty()) {
					for (Dish dish: lackIngredientList) {
						if (ableToRestock(dish)) {
							try {
								dishRestockQueue.put(dish);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						else {
							return;
						}
					}
				}
			}
		}
		
	}
}