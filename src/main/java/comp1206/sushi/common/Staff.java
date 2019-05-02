package comp1206.sushi.common;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.BlockingQueue;

import comp1206.sushi.server.DishStockManager;
import comp1206.sushi.server.Server;

public class Staff extends Model implements Runnable{
	
	private boolean shouldRestock;
	private volatile boolean isRestocking;
	private Server server;
	private String name;
	private DishStockManager stockManager;
	private String status;
	private Number fatigue;
	private Thread kitchenStaff;
	private Thread monitorFatigueThread;
	private BlockingQueue<Dish> dishRestockQueue;
	private Random restockTime = new Random();
	
	public Staff(String name, Server server) {
		this.server = server;
		this.isRestocking = false;
		this.setName(name);
		this.setFatigue(0);
		kitchenStaff = new Thread(this);
		monitorFatigueThread = new Thread(new MonitorFatigueLevel());
		kitchenStaff.start();
		monitorFatigueThread.start();
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
		this.notifyUpdate();
		this.fatigue = fatigue;
	}
	
	public int calculateFatigue(int fatigueNumber) {
		int fatigue = (int)this.fatigue + fatigueNumber;
		if(fatigue >= 100) {
			return 100;
		}
		else {
			return fatigue;
		}
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		notifyUpdate("Staff status",this.status,status);
		this.status = status;
	}
	
	@Override
	public void run() {
		while(true) {
			makeDishes();
		}
	}
	
	public void makeDishes() {
		
		if ((int)fatigue < 100) {
			try {
				isRestocking = true;
				Dish dishTaken = dishRestockQueue.take();
				Map<Ingredient, Number> recipe = dishTaken.getRecipe();
				int currentStockValue = stockManager.getStock(dishTaken);
				int restockThreshold = (int) dishTaken.getRestockThreshold();
				int restockAmount = (int) dishTaken.getRestockAmount();
				if (dishTaken.getRestockType().equals("Extra")) {
					this.setStatus("Restocking " + dishTaken + " ...");
					for (Entry<Ingredient, Number> currentPair: recipe.entrySet()) {
						server.setStock(currentPair.getKey(), -(restockAmount*(int) currentPair.getValue()));
					}
					this.setFatigue(calculateFatigue(restockAmount));
					Thread.sleep(restockTime.nextInt(40001) + 30000);
					stockManager.getDishStockLevels().replace(dishTaken, restockAmount + currentStockValue);
					currentStockValue += restockAmount;
					dishTaken.setRestockStatus(false);	
					isRestocking = false;
					this.setStatus("Idle");
				}
				else {
					while(currentStockValue < restockThreshold) {
						this.setStatus("Restocking " + dishTaken + "...");
						for (Entry<Ingredient, Number> currentPair: recipe.entrySet()) {
							server.setStock(currentPair.getKey(), -(restockAmount*(int) currentPair.getValue()));
						}
						this.setFatigue(calculateFatigue(restockAmount));
						Thread.sleep(restockTime.nextInt(40001) + 30000);
						stockManager.getDishStockLevels().replace(dishTaken, restockAmount + currentStockValue);
						currentStockValue += restockAmount;
					}
					dishTaken.setRestockStatus(false);	
					isRestocking = false;
					this.setStatus("Idle");
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private class MonitorFatigueLevel implements Runnable {

		@Override
		public void run() {
			while(true) {
				if ((int)getFatigue() == 100) {
					int fatigue = (int)getFatigue();
					while (fatigue > 0) {
						setFatigue(calculateFatigue(-1));
						fatigue--;
						try {
							Thread.sleep(1500);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
				else if (isRestocking == false) {
					int fatigue = (int) getFatigue();
					while(fatigue > 0) {
						setFatigue(calculateFatigue(-1));
						fatigue--;
						try {
							Thread.sleep(3000);
						}
						catch(InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
}
