package comp1206.sushi.common;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import comp1206.sushi.server.IngredientStockManager;

public class Drone extends Model implements Runnable{
	
	private Number speed;
	private Number progress;
	
	private Number capacity;
	private Number battery;
	
	private String status;
	
	private Postcode source;
	private Postcode destination;
	private IngredientStockManager ingredientManager;
	private BlockingQueue<Order> orderQueue;
	private BlockingQueue<Ingredient> ingredientQueue;
	private Thread droneThread;
	
	public Drone(Number speed) {
		this.setSpeed(speed);
		this.setCapacity(1);
		this.setBattery(100);
		this.setStatus("Idle");
		this.setProgress(0);
		this.droneThread = new Thread(this);
	}
	
	public void setOrderQueue(BlockingQueue<Order> serverOrderQueue) {
		orderQueue = serverOrderQueue;
		droneThread.start();
		
	}
	
	public void setIngredientStockManager(IngredientStockManager serverIngredientManager) {
		this.ingredientManager = serverIngredientManager;
		ingredientQueue = ingredientManager.getIngredientRestockQueue();
	}
	
	public Number getSpeed() {
		return speed;
	}

	
	public Number getProgress() {
		return progress;
	}
	
	public void setProgress(Number progress) {
		this.notifyUpdate("Progress", this.progress, progress);
		this.progress = progress;
	}
	
	public void setSpeed(Number speed) {
		this.notifyUpdate("Speed", this.speed, speed);
		this.speed = speed;
	}
	
	@Override
	public String getName() {
		return "Drone (" + getSpeed() + " speed)";
	}

	public Postcode getSource() {
		return source;
	}

	public void setSource(Postcode source) {
		this.notifyUpdate("Source", this.source, source);
		this.source = source;
	}

	public Postcode getDestination() {
		return destination;
	}

	public void setDestination(Postcode destination) {
		this.notifyUpdate("Destination", this.destination, destination);
		this.destination = destination;
	}

	public Number getCapacity() {
		return capacity;
	}

	public void setCapacity(Number capacity) {
		this.notifyUpdate("Capacity", this.capacity, capacity);
		this.capacity = capacity;
	}

	public Number getBattery() {
		return battery;
	}

	public void setBattery(Number battery) {
		this.notifyUpdate("Battery", this.battery, battery);
		this.battery = battery;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		notifyUpdate("status",this.status,status);
		this.status = status;
	}

	@Override
	public void run() {
		while (true) {
			restockIngredient();
			deliverOrder();
		}
	}
	
	public void restockIngredient() {
		try {
			Ingredient ingredientTaken = null;
			ingredientTaken = ingredientQueue.poll(1, TimeUnit.NANOSECONDS);
			
			if (ingredientTaken == null) {
				return;
			}
			
			else {
				int currentStockValue = (int)ingredientManager.getIngredientStock(ingredientTaken);
				int restockThreshold = (int) ingredientTaken.getRestockThreshold();
				int restockAmount = (int) ingredientTaken.getRestockAmount();
				//Drone.this.setSource(null);
				Drone.this.setDestination(ingredientTaken.getSupplier().getPostcode());
				
				while(currentStockValue < restockThreshold) {
					Drone.this.setStatus("Restocking " + ingredientTaken + "...");
					Thread.sleep(20000);
					ingredientManager.getIngredientStockLevel().replace(ingredientTaken, restockAmount + currentStockValue);
					currentStockValue += restockAmount;
				}
				
				Drone.this.setSource(null);
				Drone.this.setStatus("Idle");
			}
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void deliverOrder() {
		try {
			Order order = null;
			order = orderQueue.poll(1, TimeUnit.SECONDS);
			
			if (order == null) {
				return;
			}
			else {
				Drone.this.setStatus("Delivering delivering...");
				Drone.this.setDestination(order.getUser().getPostcode());
				Thread.sleep(20000);
				Drone.this.setStatus("Completed Delivery");
			}
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
