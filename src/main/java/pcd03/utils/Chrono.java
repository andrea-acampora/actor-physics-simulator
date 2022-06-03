package pcd03.utils;

public class Chrono {

	private boolean running;
	private long startTime;

	public Chrono() {
		this.running = false;
	}
	
	public void start() {
		this.running = true;
		this.startTime = System.currentTimeMillis();
	}
	
	public void stop() {
		this.startTime = getTime();
		this.running = false;
	}
	
	public long getTime(){
		if (this.running) {
			return 	System.currentTimeMillis() - this.startTime;
		} else {
			return this.startTime;
		}
	}
}
