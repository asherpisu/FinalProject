import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


public class Simulation {
	private static Frame frame;
	private int width;
	private int length;
	private Dimension d;
	

	
	public Simulation(int width, int length) {
		this.width=width;
		this.length=length;
		prepareGUI();
	}
	
	public Simulation(Dimension size) {
		this.d=size;
		prepareGUI(size);
	}

	public static Frame getFrame() {
		return frame;
	}
	
	private void prepareGUI() {
		frame = new Frame("Program");
		frame.setSize(width, length);
		frame.setVisible(true);
		frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e)
            {
                System.exit(0);
            }
        });
		
	}
	
	private void prepareGUI(Dimension d) {
		frame = new Frame("Program");
		frame.setSize(d);
		frame.setVisible(true);
		frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e)
            {
                System.exit(0);
            }
        });
	}


}

