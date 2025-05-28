import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.*;
import java.awt.geom.*;

public class Simulation extends Frame {
       
   public Simulation(){
      super("Sandbox");
      prepareGUI();
   }

   public static void main(String[] args){
      Simulation sim = new Simulation();  
      sim.setVisible(true);
   }

   private void prepareGUI(){
      setSize(400,400);
      addWindowListener(new WindowAdapter() {
         public void windowClosing(WindowEvent windowEvent){
            System.exit(0);
         }        
      }); 
   }    

   @Override
   public void paint(Graphics g) {
      Rectangle2D shape = new Rectangle2D.Float();
      shape.setFrame(100, 150, 200,100);
      Graphics2D g2 = (Graphics2D) g; 
      g2.draw (shape);
      Font font = new Font("Serif", Font.PLAIN, 24);
      g2.setFont(font);
   }
}

