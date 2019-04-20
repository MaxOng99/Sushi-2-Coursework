package comp1206.sushi.common;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.BlockingQueue;

import comp1206.sushi.server.DishStockManager;
import comp1206.sushi.server.Server;

public class Staff extends Model implements Runnable{
	
	private boolean shouldRestock;
	private Server server;
	private String name;
	private DishStockManager stockManager;
	private String status;
	private Number fatigue;
	private Thread kitchenStaff;
	private BlockingQueue<Dish> dishRestockQueue;
	private Random restockTime = new Random();
	
	public Staff(String name, Server server) {
		this.server = server;
		this.setName(name);
		this.setFatigue(0);
		kitchenStaff = new Thread(this);
		kitchenStaff.start();
	}
	
	public void setRestockStatus(boolean status) {
		this.shouldRestock = status;
	}
	
	public boolean shouldRestock() {
		return this.shouldRestock;
	}
	public String getName() {
		return name;
	}

	public void setDishStckManager(DishStockManager serverStockManager) {
		stockManager = serverStockManager;
		dishRestockQueue = stockManager.getDishRestockQueue();
	}
	
	public void setName(String name) {
		this.notifyUpdate("Name", this.name, name);
		this.name = name;
	}

	public Number getFatigue() {
		return fatigue;
	}

	public void setFatigue(Number fatigue) {
		this.notifyUpdate("Fatigue", this.fatigue, fatigue);
		this.fatigue = fatigue;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		notifyUpdate("Staff status",this.status,status);
		this.status = status;
	}
	
	public void makeDishes() {
	
		try {
			Dish dishTaken = dishRestockQueue.take();
			System.out.println(dishTaken.getName());
			Map<Ingredient, Number> recipe = dishTaken.getRecipe();
			int currentStockValue = stockManager.getStock(dishTaken);
			int restockThreshold = (int) dishTaken.getRestockThreshold();
			int restockAmount = (int) dishTaken.getRestockAmount();
			
			if (dishTaken.getRestockType().equals("Extra")) {
				this.setStatus("Restocking " + dishTaken + " ...");
				for (Entry<Ingredient, Number> currentPair: recipe.entrySet()) {
					server.setStock(currentPair.getKey(), -(restockAmount*(float) currentPair.getValue()));
				}
				Thread.sleep(restockTime.nextInt(40001) + 30000);
				stockManager.getDishStockLevels().replace(dishTaken, restockAmount + currentStockValue);
				
				currentStockValue += restockAmount;
				dishTaken.setRestockStatus(false);	
				this.setStatus("Idle");
			}
			else {
				while(currentStockValue < restockThreshold) {
					this.setStatus("Restocking " + dishTaken + "...");
					for (Entry<Ingredient, Number> currentPair: recipe.entrySet()) {
						server.setStock(currentPair.getKey(), -(restockAmount*(float) currentPair.getValue()));
					}
					Thread.sleep(restockTime.nextInt(40001) + 30000);
					stockManager.getDishStockLevels().replace(dishTaken, restockAmount + currentStockValue);
					
					currentStockValue += restockAmount;
				}
				dishTaken.setRestockStatus(false);	
				this.setStatus("Idle");
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
	
		}
		
	}
	@Override
	public void run() {
	
		while(true) {
			makeDishes();
		}
	}
}
