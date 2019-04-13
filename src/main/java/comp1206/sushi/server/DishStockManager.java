package comp1206.sushi.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import comp1206.sushi.common.Dish;
import comp1206.sushi.common.Staff;

public class DishStockManager {
	
	Lock stockManagerLock = new ReentrantLock();
	
	private Map<Dish, Number> dishStock;	
	
	public DishStockManager() {
		dishStock = new HashMap<>();
	}
	public Map<Dish, Number> getDishStockLevels() {
		return dishStock;
	}
	
	
	public Map<Dish, Number> getStockWithLock() {
		stockManagerLock.lock();
		
		
		return dishStock;
	}
	
	public void checkStockAndRestock(Staff staff) {
		stockManagerLock.lock();
		for (Entry<Dish, Number> dishQttyPair: dishStock.entrySet()) {
			lock(dishQttyPair);
			Dish currentDish = dishQttyPair.getKey();
			Number currentQuantity = dishQttyPair.getValue();
			while((int) currentQuantity < (int) currentDish.getRestockThreshold()) {
				staff.setStatus("Restocking...");
				System.out.println(staff.getStatus());
				currentQuantity = (int) currentQuantity + (int) currentDish.getRestockAmount();
				setStock(currentDish, currentDish.getRestockAmount());
				try {
					Thread.sleep(5500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		staff.setStatus("Idle");
		stockManagerLock.unlock();
	}
	
	
	public void setStock(Dish dish, Number quantity){
		int dishQuantity = (int) dishStock.get(dish);
		int newDishQuantity = (int) quantity;
		dishStock.replace(dish, dishQuantity + newDishQuantity);
	}
}