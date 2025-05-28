import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class App {

	static Dimension sizeN = new Dimension(1000,1000);
	Sandbox box = new Sandbox(100, 100, 800, 800);

	public static void main(String[] args) {
		Simulation sim = new Simulation(sizeN);
	}

    public void paint(Graphics g) {
        Color newthing = new Color(0,0,0);
        g.setColor(newthing);
        g.fillRect(box.x, box.y, box.width, box.height);
        g.setColor(newthing);
        g.drawRect(box.x, box.y, box.width, box.height);
    }

}
