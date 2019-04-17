package comp1206.sushi.common;
import java.util.Random;
import java.util.concurrent.BlockingQueue;

import comp1206.sushi.server.DishStockManager;

public class Staff extends Model implements Runnable{

	private String name;
	private DishStockManager stockManager;
	private String status;
	private Number fatigue;
	private Thread kitchenStaff;
	private BlockingQueue<Dish> dishRestockQueue;
	private Random restockTime = new Random();
	
	public Staff(String name) {
		this.setName(name);
		this.setFatigue(0);
		kitchenStaff = new Thread(this);
		kitchenStaff.start();
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
			int currentStockValue = stockManager.getDishStock(dishTaken);
			int restockThreshold = (int) dishTaken.getRestockThreshold();
			int restockAmount = (int) dishTaken.getRestockAmount();
			
			while(currentStockValue < restockThreshold) {
				this.setStatus("Restocking " + dishTaken + "...");
				Thread.sleep(restockTime.nextInt(40001) + 30000);
				stockManager.getDishStockLevels().replace(dishTaken, restockAmount + currentStockValue);
				currentStockValue += restockAmount;
			}
			dishTaken.setRestockStatus(false);
			this.setStatus("Idle");
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
