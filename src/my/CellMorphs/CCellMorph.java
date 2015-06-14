/*
 * CCellMorph.java
 *
 * Created on 7. Juni 2007, 12:14
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package my.CellMorphs;

import java.awt.image.*;
import java.awt.*;
import java.util.*;
import java.io.*; // Serializable
//import my.CellArts.*;

/**
 *
 * @author Adrian Imfeld
 */
public class CCellMorph implements Serializable {
    
    // Render types
    public static final int RTState = 0;
    public static final int RTNeighborCnt = 1;
    public static final int RTImage = 2;
    public static final int RTNeighborhood = 3;
    
    private int CellsWidth;
    private int CellsHeight;
    private int ImgWidth;
    private int ImgHeight;
    private int[][] Cells;
    private int[][] Neighbors;
    private int[] PixBuf;
    private MemoryImageSource MemImgSource;
    private CGenome Genes; // Only genes are saved to file
    
    Random r = new Random();
    
    public int StepCnt = 0;
    
    /**
     * Creates a new instance of CCellMorph
     */
    public CCellMorph(int width, int height) {
        Genes = new CGenome();
        Resize(width, height);
    }
    
    /** Constructor for creating mutations */
    public CCellMorph(int width, int height, CGenome g) {
        Genes = g;
        Resize(width, height);
    }
    
    /**
     * Resizes the CCellMorph, adjusting array sizes
     */
    public void Resize(int width, int height) {
        // Also asure minimal size to prevent array index out of bound exceptions
        CellsWidth = Math.max(width / Genes.GetGene(Genes.GCellSize) + 1, Genes.NHMaxSize + 2);
        CellsHeight = Math.max(height / Genes.GetGene(Genes.GCellSize) + 1, Genes.NHMaxSize + 2);
        
        ImgWidth = CellsWidth * Genes.GetGene(Genes.GCellSize);
        ImgHeight = CellsHeight * Genes.GetGene(Genes.GCellSize);
        Cells = new int[CellsWidth][CellsHeight];
        Neighbors = new int[CellsWidth][CellsHeight];
        PixBuf = new int[ImgWidth * ImgHeight];
        MemImgSource = new MemoryImageSource(ImgWidth, ImgHeight, PixBuf, 0, ImgWidth);
        Init();
    }
    
    public void Render(int RenderType) {
        int intensity, red, green, blue, color = 0, bufstart, dx, dy;
        int cellsize = Genes.GetGene(Genes.GCellSize);
        
        int GAlpha = Genes.GetGene(Genes.GAlpha);
        int GRedPeak = Genes.GetGene(Genes.GRedPeak);
        int GGreenPeak = Genes.GetGene(Genes.GGreenPeak);
        int GBluePeak = Genes.GetGene(Genes.GBluePeak);
        int GRedBreadth = Genes.GetGene(Genes.GRedBreadth);
        int GGreenBreadth = Genes.GetGene(Genes.GGreenBreadth);
        int GBlueBreadth = Genes.GetGene(Genes.GBlueBreadth);
        int GDoAlpha = Genes.GetGene(Genes.GDoAlpha);
        int GNHRadius = Genes.GetGene(Genes.GNHRadius);
        
        //int side = Genes.GetGene(Genes.GNHRadius) * 2 + 1;
        int nhsize = GetNHSize();
        
        for (int cy = 0; cy < CellsHeight; cy++) {
            for (int cx = 0; cx < CellsWidth; cx++) {
                
                if (RenderType == RTState) {
                    color = Cells[cx][cy] == 1 ? 0xffffffff : 0xff000000;
                } else if (RenderType == RTNeighborCnt) {
                    intensity = (int)(Neighbors[cx][cy] / (float)nhsize * 255); // Percent of living cells in neighborhood
                    color = 0xff000000 | (intensity << 16) | (intensity << 8) | intensity;
                } else if (RenderType == RTImage) {
                    intensity = (int)(Neighbors[cx][cy] / (float)nhsize * 255); // Typecast (truncating) to int instead of rounding (probable performance gain)
                    red = Math.max(Math.min(255 * (GRedBreadth - Math.abs(intensity - GRedPeak)) / GRedBreadth, 255), 0);
                    green = Math.max(Math.min(255 * (GGreenBreadth - Math.abs(intensity - GGreenPeak)) / GGreenBreadth, 255), 0);
                    blue = Math.max(Math.min(255 * (GBlueBreadth - Math.abs(intensity - GBluePeak)) / GBlueBreadth, 255), 0);
                    
                    if (GDoAlpha == 1)
                        color = (GAlpha << 24) | (red << 16) | (green << 8) | blue;
                    else
                        color = 0xff000000 | (red << 16) | (green << 8) | blue;
                } else if (RenderType == RTNeighborhood) {
                    color = 0xff000000;
                    dx = -(CellsWidth / 2 - cx); // distance from center
                    dy = -(CellsHeight / 2 - cy);
                    if (Math.abs(dx) <= GNHRadius && Math.abs(dy) <= GNHRadius &&
                            Genes.Neighborhood[CGenome.NHMaxSize / 2 + dx][CGenome.NHMaxSize / 2 + dy])
                        color = 0xffffffff;
                }
                
                bufstart = cy * cellsize * ImgWidth + cx * cellsize;
                for (int py = 0; py < cellsize; py++) {
                    for (int px = 0; px < cellsize; px++) {
                        PixBuf[bufstart + px] = color;
                    }
                    bufstart += ImgWidth;
                }
                
            }
        }
    }
    
    public Image GetImage() {
        return Toolkit.getDefaultToolkit().createImage(MemImgSource);
    }
    
    public int GetGene(int Gene) {
        return Genes.GetGene(Gene);
    }
    
    // returns the size of the neighborhood
    public int GetNHSize() {
        int nhsize = 0; // Size of neighborhood
        int GNHRadius = Genes.GetGene(CGenome.GNHRadius);
        int mx = CGenome.NHMaxSize / 2;
        int my = CGenome.NHMaxSize / 2;
        for (int y = -GNHRadius; y <= GNHRadius; y++)
            for (int x = -GNHRadius; x <= GNHRadius; x++)
                if (Genes.Neighborhood[mx + x][my + y])
                    nhsize++;
        return nhsize;
    }
    
    public CGenome CopyGenes() {
        return Genes.Copy();
    }
    
    
    public CCellMorph Mutate(int Mutations) {
        CCellMorph Child = new CCellMorph(ImgWidth, ImgHeight, Genes.Mutate(Mutations));
        return Child;
    }
    
    public void Init() {
        StepCnt = 0;
        
        // Clear first
        for (int y = 0; y < CellsHeight; y++)
            for (int x = 0; x < CellsWidth; x++)
                Cells[x][y] = 0;
        
        // Random initialization
        if (Genes.GetGene(Genes.GInitMode) == Genes.IMRandom)
            for (int y = 0; y < CellsHeight; y++)
                for (int x = 0; x < CellsWidth; x++)
                    Cells[x][y] = (1 + Math.abs(r.nextInt()) % 100 <= Genes.GetGene(Genes.GRandomInitDensity)) ? 1 : 0;
        
        // Initialization using InitField
        if (Genes.GetGene(Genes.GInitMode) == Genes.IMInitField)
            for (int y = 0; y < Genes.InitFieldSize; y++)
                for (int x = 0; x < Genes.InitFieldSize; x++)
                    if (Genes.GetInitField(x, y) == 1)
                        Cells[CellsWidth / 2 - Genes.InitFieldSize / 2 + x][CellsHeight / 2 - Genes.InitFieldSize / 2 + y] = 1;
        
        // Update neighborcount
        for (int y = 0; y < CellsHeight; y++)
            for (int x = 0; x < CellsWidth; x++)
                Neighbors[x][y] = NeighborCount(x, y);
    }
    
    public void Save(ObjectOutputStream os) throws FileNotFoundException, IOException {
        Genes.Save(os);
        //os.writeObject(Genes);
    }
    
    public static CCellMorph Load(ObjectInputStream is, int ImgWidth, int ImgHeight) throws FileNotFoundException, IOException, ClassNotFoundException {
        return new CCellMorph(ImgWidth, ImgHeight, new CGenome(is));
    }
    
    // Old CellArts file type (*.ca) without generation history
    public static CCellMorph LoadCA(ObjectInputStream is, int ImgWidth, int ImgHeight) throws FileNotFoundException, IOException, ClassNotFoundException {
        my.CellArts.CGenome g = (my.CellArts.CGenome)is.readObject();
        return new CCellMorph(ImgWidth, ImgHeight, new CGenome(g));
    }

    
    public void Step(){
        int ncnt;
        int GIsolation = Genes.GetGene(Genes.GIsolation);
        int GOvercrowding = Genes.GetGene(Genes.GOvercrowding);
        int GBirthMin = Genes.GetGene(Genes.GBirthMin);
        int GBirthMax = Genes.GetGene(Genes.GBirthMax);
        int DoNoise = Genes.GetGene(Genes.GDoNoise);
        int GNoiseAmount = Genes.GetGene(Genes.GNoiseAmount);
        
        // System.arraycopy() and .clone() only create shallow copies
//        for (int y = 0; y < CellsHeight; y++) {
//            for (int x = 0; x < CellsWidth; x++) {
//                NewCells[x][y] = Cells[x][y];
//            }
//        }
        StepCnt++;
        
        // Mark dying and newborn cells
        for (int y = 0; y < CellsHeight; y++) {
            for (int x = 0; x < CellsWidth; x++) {
                ncnt = NeighborCount(x, y);
                Neighbors[x][y] = ncnt;
                if (Cells[x][y] == 1 && (ncnt <= GIsolation || ncnt >= GOvercrowding)) // Death
                    Cells[x][y] = 2;
                else if (Cells[x][y] == 0 && ncnt >= GBirthMin && ncnt <= GBirthMax) // Birth
                    Cells[x][y] = -1;
            }
        }
        
        // Remove dead, create newborns
        for (int y = 0; y < CellsHeight; y++) {
            for (int x = 0; x < CellsWidth; x++) {
                if (Cells[x][y] == -1)
                    Cells[x][y] = 1;
                else if (Cells[x][y] == 2)
                    Cells[x][y] = 0;
            }
        }
        
        // Apply noise
        if (DoNoise == 1)
            for (int y = 0; y < CellsHeight; y++)
                for (int x = 0; x < CellsWidth; x++)
                    if (1 + Math.abs(r.nextInt()) % 10000 <= GNoiseAmount)   // Apply noise
                        Cells[x][y] = Cells[x][y] == 1 ? 0 : 1;
    }
    
    
    private int NeighborCount(int CellX, int CellY) {
        int Radius = Genes.GetGene(Genes.GNHRadius);
        int NHcenterX = Genes.NHMaxSize / 2;
        int NHcenterY = NHcenterX;
        
        int cnt = 0;
        for (int dy = -Radius; dy <= Radius; dy++) {
            for (int dx = -Radius; dx <= Radius; dx++) {
                if (Genes.Neighborhood[NHcenterX + dx][NHcenterY + dy] &&
                        Cells[PeriodicNeighbor(CellX + dx, CellsWidth)][PeriodicNeighbor(CellY + dy, CellsHeight)] > 0)
                    cnt++;
            }
        }
        
        return cnt;
    }
    
    private static int PeriodicNeighbor(int pos, int size) {
        if (pos >= 0 && pos < size)
            return pos;
        else if (pos < 0)
            return size + pos;
        else //if (pos >= size)
            return pos - size;
    }
    
    
}
