import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


public class Particle extends Rectangle{
    private int x;
    private int y;
	private int size;
	private int velocity;
	private Color color;
	private String id;
	private boolean updated;	
	
	public Particle(int x, int y,int size, int velocity, Color color, String id, boolean updated) {
        super(x, y, size, size);
		this.size = size;
        this.velocity = velocity;
        this.color = color;
        this.id = id;
        this.updated = updated;
	}


}

