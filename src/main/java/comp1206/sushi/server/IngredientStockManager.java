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
		for (Entry<Ingredient, Number> currentEntry: ingredientStock.entrySet()) {
			Ingredient currentIngredient = currentEntry.getKey();
			float currentStock = (float) currentEntry.getValue();
			if (currentStock < (float) currentIngredient.getRestockThreshold()) {
				try {
					currentIngredient.setRestockStatus(true);
					currentIngredient.setIngredientAvailability(false);
					ingredientRestockQueue.put(currentIngredient);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			else {
				currentIngredient.setIngredientAvailability(true);
			}
		}
	}
	
	public Map<Ingredient, Number> getIngredientStockLevel() {
		return ingredientStock;
	}
	
	public Number getStock(Ingredient ingredient) {
		return ingredientStock.get(ingredient);
	}
	
	public BlockingQueue<Ingredient> getIngredientRestockQueue() {
		return ingredientRestockQueue;
	}
	
	public void directRestock(Ingredient ingredient, Number quantitiy) {
		ingredientStock.replace(ingredient, quantitiy);
	}
	
	public void setStock(Ingredient ingredient, Number quantity){	
		float ingredientQuantity = (float) ingredientStock.get(ingredient);
		float newIngredientQuantity = ingredientQuantity + (float) quantity;
		
		if (newIngredientQuantity < 0) {
			ingredientStock.replace(ingredient, (float)0);
		}
		else {
			ingredientStock.replace(ingredient, newIngredientQuantity);
		}
		if (newIngredientQuantity < (float) ingredient.getRestockThreshold()) {
			System.out.println("gonna go restock ingredients if condiition tru");
			ingredient.setIngredientAvailability(false);
			try {
				if (ingredientRestockQueue.contains(ingredient) || ingredient.beingRestocked() == true) {
					System.out.println(ingredient + " being restocked already");
				}
				else {
					ingredientRestockQueue.put(ingredient);
					ingredient.setRestockStatus(true);
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else {
			ingredient.setIngredientAvailability(true);
		}
	}
}