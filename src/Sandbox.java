import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;

public class Sandbox extends Rectangle {

    private Particle[][] particles;

	public Sandbox(Particle[][] particles) { 
        super(100,100,1000,1000);
        this.particles = particles;
	}

    public void gravity() {
        for (int i = 0; i<this.particles.length; i++) {
            for (int j = 0; j<this.particles[0].length; j++) {
                particles[i][j].setVelocity(particles[i][j].getVelocity()+10);
            }
        }
    }
}
