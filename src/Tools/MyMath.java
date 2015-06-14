/*
 * MyMath.java
 *
 * Created on 26. Juni 2007, 16:14
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package Tools;

/**
 *
 * @author Adrian Imfeld
 */
public class MyMath {    
    
    /**
     * Fits a value within a range. MyMath.Constrain(val, min, max) is equivalent to
     * Math.max(Math.min(val, max), min)
     */
    public static int Constrain(int val, int min, int max) {
        return Math.max(Math.min(val, max), min);
    }
}
