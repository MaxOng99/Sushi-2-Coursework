package comp1206.sushi.common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Basket implements Serializable{
	
	private static final long serialVersionUID = -1803572769884618746L;
	private float cost;
	private Map<Dish, Number> basketMap;
	
	public Basket() {
		cost = 0;
		basketMap = new HashMap<>();
	}
	
	
	public String toString() {
		return basketMap.toString();
	}
	
	public void addDishToBasket(Dish dish, Number quantity) {
		if (basketMap.containsKey(dish)) {
			cost -= (Integer) dish.getPrice() * (Integer) basketMap.get(dish);
		}
		cost += ((Float) dish.getPrice() * (Integer) quantity);
		basketMap.put(dish, quantity);
		
	}
	
	public void updateDishInBasket(Dish dish, Number quantity) {
		float cost = 0;
		setCost(cost);
		
		if ( (Integer) quantity == 0) {
			basketMap.remove(dish);
			for(Entry<Dish, Number> item: basketMap.entrySet()) {
				cost += (Float) item.getKey().getPrice() * (Integer) item.getValue();
			}			
		} 
		else {
			basketMap.put(dish, quantity);
			for(Entry<Dish, Number> item: basketMap.entrySet()) {
				cost += (Float) item.getKey().getPrice() * (Integer) item.getValue();
			}
		}
		setCost(cost);
	}
	
	public void setCost(float newCost) {
		cost = newCost;
	}
	public Number getCost() {
		return cost;
	}
	
	public Map<Dish, Number> getBasketMap() {
		return basketMap;
	}
}
