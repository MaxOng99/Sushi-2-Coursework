package comp1206.sushi.common;
import comp1206.sushi.server.DishStockManager;

public class Staff extends Model implements UpdateListener, Runnable{

	private String name;
	private DishStockManager stockManager;
	private String status;
	private Number fatigue;
	private Thread kitchenStaff;
	
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
		notifyUpdate("status",this.status,status);
		this.status = status;
	}
	
	@Override
	public void updated(UpdateEvent updateEvent) {
		
	}
	
	@Override
	public void run() {
		
		while(true) {
			stockManager.queueRestockWay(this);
		}
	}
}
