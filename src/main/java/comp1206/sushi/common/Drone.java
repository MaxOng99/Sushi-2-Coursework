package comp1206.sushi.common;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import comp1206.sushi.server.IngredientStockManager;

public class Drone extends Model implements Runnable{
	
	private Number speed;
	private Number progress;
	
	private Number capacity;
	private Number battery;
	
	private volatile String status;
	private volatile float distanceTravelled;
	private Postcode restaurantPostcode;
	private volatile Postcode source;
	private volatile Postcode destination;
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
		this.distanceTravelled = 0;
	}
	
	public void setRestaurantPostcode(Postcode restaurantPostcode) {
		this.restaurantPostcode = restaurantPostcode;
		this.setSource(restaurantPostcode);
		droneThread.start();
	}
	
	public void setOrderQueue(BlockingQueue<Order> serverOrderQueue) {
		orderQueue = serverOrderQueue;
		
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
		this.notifyUpdate();
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
	
	public void returnToRestaurant() {
		double sourceLocation = (double) getSource().getDistance();
		double timeTaken = sourceLocation / (Float) this.getSpeed();
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);     
        scheduler.scheduleAtFixedRate(() -> this.inBoundProgress(scheduler, sourceLocation), 0, 1, TimeUnit.SECONDS);
        try {
			Thread.sleep((long) timeTaken * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void restockIngredient() {
		
		if (this.getStatus().equals("Idle")){
			try {
				Ingredient ingredientTaken = null;
				ingredientTaken = ingredientQueue.poll(1, TimeUnit.NANOSECONDS);
				
				if (ingredientTaken == null) {
					return;
				}
				
				else {
					if (getSource() != restaurantPostcode) {
						returnToRestaurant();
						int currentStockValue = (int)ingredientManager.getIngredientStock(ingredientTaken);
						int restockThreshold = (int) ingredientTaken.getRestockThreshold();
						int restockAmount = (int) ingredientTaken.getRestockAmount();
						this.setSource(restaurantPostcode);
						this.setDestination(ingredientTaken.getSupplier().getPostcode());
						double totalDistance = (double) this.getDestination().getDistance();
						double timeTaken = totalDistance / (Float) this.getSpeed();
						
						while (currentStockValue < restockThreshold) {
							this.setStatus("Restocking " + ingredientTaken + "...");
							ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);     
					        scheduler.scheduleAtFixedRate(() -> this.outBoundProgress(scheduler, totalDistance), 0, 1, TimeUnit.SECONDS);
					        Thread.sleep((long) (timeTaken * 1000 * 2));
					        ingredientManager.directRestock(ingredientTaken, currentStockValue + restockAmount);
					        currentStockValue += restockAmount;
						}
					}
				
					else {
						int currentStockValue = (int)ingredientManager.getIngredientStock(ingredientTaken);
						int restockThreshold = (int) ingredientTaken.getRestockThreshold();
						int restockAmount = (int) ingredientTaken.getRestockAmount();
						this.setSource(restaurantPostcode);
						this.setDestination(ingredientTaken.getSupplier().getPostcode());
						double totalDistance = (double) this.getDestination().getDistance();
						double timeTaken = totalDistance / (Float) this.getSpeed();
						
						while (currentStockValue < restockThreshold) {
							this.setStatus("Restocking " + ingredientTaken + "...");
							ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);     
					        scheduler.scheduleAtFixedRate(() -> this.outBoundProgress(scheduler, totalDistance), 0, 1, TimeUnit.SECONDS);
					        Thread.sleep((long) (timeTaken * 1000 * 2));
					        ingredientManager.directRestock(ingredientTaken, currentStockValue + restockAmount);
					        currentStockValue += restockAmount;
						}
					}
				}
				
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		else {
			return;
		}
		
	}
	
	public void deliverOrder() {
		
		if (getStatus().equals("Idle")) {
			try {
				Order order = null;
				order = orderQueue.poll(1, TimeUnit.NANOSECONDS);
				
				if (order == null) {
					return;
				}
				else {
					this.setSource(restaurantPostcode);
					this.setDestination(order.getUser().getPostcode());
					double totalDistance = (double) this.getDestination().getDistance();
					
					if (totalDistance == 0) {
						this.setStatus("Idle");
					}
					
					else {
						double timeTaken = totalDistance / (Float) this.getSpeed();
						ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);     
				        scheduler.scheduleAtFixedRate(() -> this.outBoundProgress(scheduler, totalDistance), 0, 1, TimeUnit.SECONDS);
						this.setStatus("Delivering Order...");
						Thread.sleep((long) (timeTaken * 1000));
						order.setStatus("Complete");
					}
				}
				
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
			
	public void outBoundProgress(ScheduledExecutorService scheduler, double totalDistance) {
		if (distanceTravelled < totalDistance) {
			distanceTravelled = distanceTravelled + (float) this.getSpeed();
			if (distanceTravelled > totalDistance) {
				this.setProgress(100);
				this.setStatus("Returning To Restaurant");
				this.setSource(this.getDestination());
				this.setDestination(restaurantPostcode);
				this.distanceTravelled = 0;
				scheduler.shutdown();
				ScheduledExecutorService inBoundScheduler = Executors.newScheduledThreadPool(1);     
		        inBoundScheduler.scheduleAtFixedRate(() -> this.inBoundProgress(inBoundScheduler, totalDistance), 0, 1, TimeUnit.SECONDS);
			}
			
			else {
				this.setProgress(Math.round((distanceTravelled*100)/totalDistance));
			}	
		}
	}
	
	public void inBoundProgress(ScheduledExecutorService scheduler, double totalDistance) {
		
		if (distanceTravelled < totalDistance){
			distanceTravelled = distanceTravelled + (float) this.getSpeed();
			if (distanceTravelled > totalDistance) {
				this.setProgress(100);
				this.setStatus("Idle");
				this.distanceTravelled = 0;
				scheduler.shutdown();
			}
			else {
				this.setProgress(Math.round((distanceTravelled*100)/totalDistance));
			}	
		}		
	}
	
	@Override
	public void run() {
		while (true) {
			restockIngredient();
			deliverOrder();
		}
	}
}
