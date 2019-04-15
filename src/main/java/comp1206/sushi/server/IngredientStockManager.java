package comp1206.sushi.server;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import comp1206.sushi.common.Dish;
import comp1206.sushi.common.Drone;
import comp1206.sushi.common.Ingredient;


public class IngredientStockManager {
	
	private Random restockTime = new Random();
	private Map<Ingredient, Number> ingredientStock;
	private BlockingQueue<Ingredient> ingredientRestockQueue = new ArrayBlockingQueue<Ingredient>(10);
	
	public IngredientStockManager() {
		ingredientStock = new ConcurrentHashMap<>();
	}
	
	public void initializeStockFromConfig(Map<Ingredient, Number> configStock) {
		ingredientStock = configStock;
	}
	
	public Map<Ingredient, Number> getIngredientStockLevel() {
		return ingredientStock;
	}
	
	public Number getIngredientStock(Ingredient ingredient) {
		return ingredientStock.get(ingredient);
	}
	
	public synchronized void setStock(Ingredient ingredient, Number quantity){	
		int ingredientQuantity = (int) ingredientStock.get(ingredient);
		int newIngredientQuantity = ingredientQuantity + (int) + (int) quantity;
		ingredientStock.replace(ingredient, newIngredientQuantity);
		
		if (newIngredientQuantity < (int) ingredient.getRestockThreshold()) {
			try {
				if (ingredientRestockQueue.contains(ingredient)) {
					System.out.println("Nope");
				}
				else {
					ingredientRestockQueue.put(ingredient);
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void restockIngredient(Drone drone) {
		try {
			Ingredient ingredientTaken = ingredientRestockQueue.take();
			int currentStockValue = (int)ingredientStock.get(ingredientTaken);
			int restockThreshold = (int) ingredientTaken.getRestockThreshold();
			int restockAmount = (int) ingredientTaken.getRestockAmount();
			
			while(currentStockValue < restockThreshold) {
				drone.setStatus("Restocking " + ingredientTaken + "...");
				Thread.sleep(restockTime.nextInt(40001) + 30000);
				ingredientStock.replace(ingredientTaken, restockAmount + currentStockValue);
				currentStockValue += restockAmount;
			}
			
			drone.setStatus("Idle");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}