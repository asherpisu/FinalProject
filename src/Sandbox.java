import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Sandbox {
	private int width;
	private int length;
	private Simulation sim;
	
	public Sandbox(int width, int length, Simulation sim) {
		this.width=width;
		this.length=length;
		this.sim = sim;
		prepareRect();
	}
	
	
	private void prepareRect() {
		Rectangle rect = new Rectangle(100,100, width, length);
	}
}
