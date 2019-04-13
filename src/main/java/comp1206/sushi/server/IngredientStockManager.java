package comp1206.sushi.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import comp1206.sushi.common.Ingredient;
import comp1206.sushi.common.UpdateEvent;
import comp1206.sushi.common.UpdateListener;

public class IngredientStockManager {

	private Map<Ingredient, Number> ingredientStock = new HashMap<>();
	private List<UpdateListener> updateListeners = new ArrayList<UpdateListener>();
	
	public IngredientStockManager(Map<Ingredient, Number> ingredientStockFromConfig) {
		this.ingredientStock = new ConcurrentHashMap<>(ingredientStockFromConfig);
	}
	
	public void addUpdateListener(UpdateListener listener) {
		updateListeners.add(listener);
	}
	
	public void notifyUpdate() {
		//Call the updated method on every update listener
		for(UpdateListener listener : updateListeners) {
			listener.updated(new UpdateEvent());
		}		
	}
	
	public Map<Ingredient, Number> getIngredientStockLevels() {
		return ingredientStock;
	}
	
	public void setStock(Ingredient ingredient, Number quantity) {
		for (Entry<Ingredient, Number> ingredientStockPair: ingredientStock.entrySet()) {
			
			if (ingredientStockPair.getKey().equals(ingredient)) {
				ingredientStockPair.setValue(quantity);
			}
		}
	}
}