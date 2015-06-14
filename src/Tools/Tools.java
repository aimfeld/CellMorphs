/*
 * Tools.java
 *
 * Created on 1. Juli 2007, 16:39
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package Tools;

import java.awt.Font;
/**
 *
 * @author Adrian Imfeld
 */
public class Tools {
    
    /**
     * Lädt einen Font mit Hilfe einer übergebenen ClassLoaders. Funktioniert
     * auch, wenn der Font in einem jar-Archiv ist.
     * Typischerweise sieht ein Aufruf etwa so aus:
     * Font LogicFont12Plain = Tools.LoadFont(LogicAnalyzer.class.getClassLoader(), "LogicFont.ttf", Font.PLAIN, 12)
     * Falls der Font nicht geladen werden kann, wird ein "Dialog" Font zurückgegeben.
     */
    public static Font LoadFont(ClassLoader cl, String name, int Style, int Size) {
        Font font = null;
        try {
            font = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, cl.getResourceAsStream(name));
            font = font.deriveFont((float)Size); // Grösse setzen
            font = font.deriveFont(Style);
        } catch (Exception e) {
            return new Font("Dialog", Style, Size);
        }
        return font;
    }
}
