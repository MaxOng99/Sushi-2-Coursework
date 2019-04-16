package comp1206.sushi.server;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import comp1206.sushi.common.Drone;
import comp1206.sushi.common.Ingredient;


public class IngredientStockManager {
	
	private Server server;
	private Random restockTime = new Random();
	private Map<Ingredient, Number> ingredientStock;
	private BlockingQueue<Ingredient> ingredientRestockQueue;
	
	public IngredientStockManager(Server server) {
		this.server = server;
		ingredientStock = new ConcurrentHashMap<>();
		ingredientRestockQueue = new LinkedBlockingQueue<>();
	}
	
	public void initializeStockFromConfig(Map<Ingredient, Number> configStock) {
		ingredientStock = configStock;
		for (Entry<Ingredient, Number> currentIngredientStockPair: ingredientStock.entrySet()) {
			if ((int)currentIngredientStockPair.getValue() < (int)currentIngredientStockPair.getKey().getRestockThreshold()) {
				try {
					ingredientRestockQueue.put(currentIngredientStockPair.getKey());
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public Map<Ingredient, Number> getIngredientStockLevel() {
		return ingredientStock;
	}
	
	public int getIngredientStock(Ingredient ingredient) {
		return (int) ingredientStock.get(ingredient);
	}
	
	public synchronized void setStock(Ingredient ingredient, Number quantity){	
		int ingredientQuantity = (int) ingredientStock.get(ingredient);
		int newIngredientQuantity = (int) (ingredientQuantity + (Float) quantity);
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
	
	public BlockingQueue<Ingredient> getIngredientRestockQueue() {
		return ingredientRestockQueue;
	}
	
	public void restockIngredient(Drone drone) {
		try {
			Ingredient ingredientTaken = ingredientRestockQueue.take();
			int currentStockValue = (int)ingredientStock.get(ingredientTaken);
			int restockThreshold = (int) ingredientTaken.getRestockThreshold();
			int restockAmount = (int) ingredientTaken.getRestockAmount();
			drone.setSource(server.getRestaurantPostcode());
			drone.setDestination(ingredientTaken.getSupplier().getPostcode());
			
			while(currentStockValue < restockThreshold) {
				drone.setStatus("Restocking " + ingredientTaken + "...");
				Thread.sleep(8000);
				ingredientStock.replace(ingredientTaken, restockAmount + currentStockValue);
				currentStockValue += restockAmount;
			}
			
			drone.setSource(null);
			drone.setStatus("Idle");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}