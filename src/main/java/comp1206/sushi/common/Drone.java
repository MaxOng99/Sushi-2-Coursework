package comp1206.sushi.common;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import comp1206.sushi.server.IngredientStockManager;

public class Drone extends Model implements Runnable{
	
	private volatile Number progress;
	private Number speed;
	private Number capacity;
	private Number battery;
	private volatile float distanceTravelled;
	private volatile Postcode source;
	private volatile Postcode destination;
	private Postcode restaurantPostcode;
	private boolean removedFromServer;
	private volatile boolean restockStatus; 
	private volatile String status;
	private IngredientStockManager ingredientManager;
	private BlockingQueue<Order> orderQueue;
	private BlockingQueue<Ingredient> ingredientQueue;
	private Thread droneThread;
	
	public Drone(Number speed) {
		this.speed = speed;
		this.capacity = 1;
		this.battery = 100;
		this.status = "Idle";
		this.progress = null;
		this.droneThread = new Thread(this);
		this.distanceTravelled = 0;
		this.removedFromServer = false;
		this.restockStatus = true;
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
	
	public Number getProgress() {
		return progress;
	}
	
	public void setProgress(Number progress) {
		this.notifyUpdate();
		this.progress = progress;
	}
	
	public Number getSpeed() {
		return speed;
	}
	
	public void setSpeed(Number speed) {
		this.notifyUpdate("Speed", this.speed, speed);
		this.speed = speed;
	}
	
	public Postcode getSource() {
		return source;
	}

	public void setSource(Postcode source) {
		this.notifyUpdate("Source", this.source, source);
		this.source = source;
	}
	
	public boolean getRestockStatus() {
		return restockStatus;
	}
	
	public void setRestockStatus(boolean status) {
		restockStatus = status;
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
	
	public void deleteFromServer() {
		removedFromServer = true;
	}
	
	@Override
	public String getName() {
		return "Drone (" + getSpeed() + " speed)";
	}
	
	@Override
	public void run() {
		while (true) {
			if(!removedFromServer) {
				try {
					restockIngredient();
					deliverOrder();
				}
				catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
			else {
				return;
			}
		}
	}
	
	public void restockIngredient() throws InterruptedException{
		if (this.getStatus().equals("Idle")){
			Ingredient ingredientTaken = null;
			ingredientTaken = ingredientQueue.poll(1, TimeUnit.MILLISECONDS);
			
			if (ingredientTaken == null) {
				return;
			}
			else {
				performRestockProcess(ingredientTaken);
			}
		}
		else {
			return;
		}
	}
	
	public void deliverOrder() throws InterruptedException{
		if (getStatus().equals("Idle")) {
			Order order = null;
			order = orderQueue.poll(1, TimeUnit.MILLISECONDS);
		
			if (order == null) {
				return;
			}
			else {
				performDeliveryProcess(order);
			}	
		}
	}
	
	public void performDeliveryProcess(Order order) {
		if (restaurantPostcode.getName().equals(order.getUser().getPostcode().getName())) {
			order.setStatus("Complete");
			this.setStatus("Idle");
		}
		
		else {
			this.setSource(restaurantPostcode);
			this.setDestination(order.getUser().getPostcode());
			double totalDistance = (double) this.getDestination().getDistance();
			double timeTaken = totalDistance / (Float) this.getSpeed();
			ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);     
	        scheduler.scheduleAtFixedRate(() -> this.outBoundProgress(scheduler, totalDistance), 0, 1, TimeUnit.SECONDS);
			this.setStatus("Delivering Order...");
			try {
				Thread.sleep((long) (timeTaken * 1000));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			order.setStatus("Complete");
			this.setSource(null);
			this.setDestination(null);
		}
	}
	
	public void performRestockProcess(Ingredient ingredientTaken) {
		ingredientTaken.setRestockStatus(true);
		ingredientTaken.setIngredientAvailability(false);
		int currentStockValue = (int)ingredientManager.getStock(ingredientTaken);
		int restockThreshold = (int) ingredientTaken.getRestockThreshold();
		int restockAmount = (int) ingredientTaken.getRestockAmount();
		this.setSource(restaurantPostcode);
		this.setDestination(ingredientTaken.getSupplier().getPostcode());
		double totalDistance = (double) this.getDestination().getDistance();
		double timeTaken = totalDistance / (Float) this.getSpeed();
		this.setStatus("Fetching " + ingredientTaken + "...");
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		if (ingredientTaken.getRestockType().equals("Normal")) {
			while (currentStockValue < restockThreshold) {   
		        scheduler.scheduleAtFixedRate(() -> this.outBoundProgress(scheduler, totalDistance), 0, 1, TimeUnit.SECONDS);
		        try {
		        	Thread.sleep((long) (timeTaken * 1000 * 2));
		        }
		        catch(InterruptedException e) {
		        	e.printStackTrace();
		        }
		        ingredientManager.directRestock(ingredientTaken, currentStockValue + restockAmount);
		        currentStockValue += restockAmount;
			}
			ingredientTaken.setIngredientAvailability(true);
			ingredientTaken.setRestockStatus(false);
			this.setStatus("Idle");
		}
		else if (ingredientTaken.getRestockType().equals("Extra")) {  
	        scheduler.scheduleAtFixedRate(() -> this.outBoundProgress(scheduler, totalDistance), 0, 1, TimeUnit.SECONDS);
	        try {
	        	Thread.sleep((long) (timeTaken * 1000 * 2));
	        }
	        catch(InterruptedException e) {
	        	e.printStackTrace();
	        }
	        ingredientManager.directRestock(ingredientTaken, (int)ingredientManager.getStock(ingredientTaken) + restockAmount);
	        ingredientTaken.setRestockStatus(false);
	        ingredientTaken.setRestockType("Normal");
	        ingredientTaken.setIngredientAvailability(true);
	        this.setStatus("Idle");
		}
	}
	
	public void inBoundProgress(ScheduledExecutorService scheduler, double totalDistance) {
		if (distanceTravelled < totalDistance){
			distanceTravelled = distanceTravelled + (float) this.getSpeed();
			if (distanceTravelled >= totalDistance) {
				this.setStatus("Idle");
				this.setProgress(100);
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				this.setProgress(null);
				this.setSource(null);
				this.setDestination(null);
				this.distanceTravelled = 0;
				scheduler.shutdown();
			}
			else {
				this.setProgress(Math.round((distanceTravelled*100)/totalDistance));
			}	
		}		
	}
	
	public void outBoundProgress(ScheduledExecutorService scheduler, double totalDistance) {
		if (distanceTravelled < totalDistance) {
			distanceTravelled = distanceTravelled + (float) this.getSpeed();
			if (distanceTravelled >= totalDistance) {
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
}
