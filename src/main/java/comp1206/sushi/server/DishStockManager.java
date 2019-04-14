package comp1206.sushi.server;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import comp1206.sushi.common.Dish;
import comp1206.sushi.common.Staff;


public class DishStockManager {
	
	private Random restockTime = new Random();
	private Map<Dish, Number> dishStock;
	private BlockingQueue<Dish> queue = new ArrayBlockingQueue<Dish>(10);
	
	public DishStockManager() {
		dishStock = new ConcurrentHashMap<>();
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
				if (queue.contains(dish)) {
					System.out.println("Nope");
				}
				else {
					queue.put(dish);
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void queueRestockWay(Staff staff) {
		try {
			Dish dishTaken = queue.take();
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