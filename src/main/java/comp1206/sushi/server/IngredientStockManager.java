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
	
	public BlockingQueue<Ingredient> getIngredientRestockQueue() {
		return ingredientRestockQueue;
	}
	
	public void directRestock(Ingredient ingredient, Number quantitiy) {
		ingredientStock.replace(ingredient, quantitiy);
	}
	
	public void setStock(Ingredient ingredient, Number quantity){	
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
}