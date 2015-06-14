/*
 * CCellMorphCanvas.java
 *
 * Created on 19. Juni 2007, 11:29
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package my.CellMorphs;

import java.awt.*;
import javax.swing.*;
/**
 *
 * @author Adrian Imfeld
 */

// Mixing awt and swing components is bad (http://java.sun.com/products/jfc/tsc/articles/mixing/index.html).
// - There's a nasty resize bug, an awt.canvas blocks a surrounding panel from getting smaller.
// - A JMenu will be hidden behind an awt.canvas.
// So we derive from swing.JPanel instead of awt.Canvas
public class CCellMorphCanvas extends JPanel {
    
    private Image CMImage = null;
    
    /**
     * Creates a new instance of CCellMorphCanvas
     */
    public CCellMorphCanvas() {
    }
    
    /** The paintComponent() method is called e.g. when the window is shown after being hidden.
     * Automatic updating is reason for overriding this method, because it is called
     * automatically.
     */
    public void paintComponent(Graphics g) {
        if (CMImage != null && g != null) {
            //g.clearRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.drawImage(CMImage, 0, 0, null);
        }
    }
    
    public void SetImage(Image Img) {
        CMImage = Img;
//        Graphics gr = getGraphics();
//        if (Img != null && gr != null)
//            gr.drawImage(Img, 0, 0, null);     
    }
}

