/*
 * CGenome.java
 *
 * Created on 11. Juni 2007, 11:25
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package my.CellMorphs;

import java.util.*;
import java.io.*; // Serializable
/**
 *
 * @author Adrian Imfeld
 */
public class CGenome {
    
    // static final long serialVersionUID = -0000000000000000001L; // no serialization in .cm files (see comment in my.CellArts.CGenome
    
    private static final int MutationCount = 20; 
    public static final int NHMaxSize = 11;       // Maximal size of considered neighborhood (array size = NeighborhoodSize^2)
    
    public static final int IMInitField = 0;     // State of GInitMode
    public static final int IMRandom = 1;        // State of GInitMode
    
    public static final int GInitMode =          0;  // binary: IMInitField, IMRandom
    public static final int GInitField =         1;  // Pseudo-gene for InitField mutations
    public static final int GRandomInitDensity = 2;  // Density of initial random seed (percent)
    public static final int GDoNoise =           3;  // binary: 0 = no, 1 = yes
    public static final int GNoiseAmount =       4;  // Noise probability per cell per step (value/10000)
    public static final int GCellSize =          5;  // Resolution of a cell in pixels
    public static final int GNHRadius =          6;  // Radius of neighborhood
    public static final int GNeighborhood =      7;  // Pseudo-gene for neighborhood mutations
    public static final int GIsolation =         8;  // Game of Life rule
    public static final int GOvercrowding =      9;  // Game of Life rule
    public static final int GBirthMin =         10;  // Game of Life rule
    public static final int GBirthMax =         11;  // Game of Life rule
    public static final int GRedPeak =          12;  // Maximum sensitivity of red channel (0..255)
    public static final int GGreenPeak =        13;  // Maximum sensitivity of green channel (0..255)
    public static final int GBluePeak =         14;  // Maximum sensitivity of blue channel (0..255)
    public static final int GRedBreadth =       15;  // Breadth of linear sensitivity function (_/\_)
    public static final int GGreenBreadth =     16;  // Breadth of linear sensitivity function (_/\_)
    public static final int GBlueBreadth =      17;  // Breadth of linear sensitivity function (_/\_)
    public static final int GDoAlpha =          18;  // binary: Transparency, 0 = no, 1 = yes
    public static final int GAlpha =            19;  // Transparency: 0 = invisible, 255 = opaque
    public static final int GCount =            20;
    
    private static final int InitVal =           0;
    private static final int MinVal =            1;
    private static final int MaxVal =            2;
    private static final int StepVal =           3; // Amount of mutation per step
    private static final int MutFreq =           4; // Mutation frequency
    
    private static final int[][] GeneDefs =
    { // InitVal,    MinVal,     MaxVal,   StepVal,  MutFreq
        {      0,         0,          1,         1,     10},    // GInitieldMode
        {      0,         0,          0,         0,     10},    // GInitield (Pseudo gene)
        {     20,         0,        100,        10,     10},    // GRandomInitDensity
        {      0,         0,          1,         1,     10},    // GDoNoise
        {      1,         0,        100,         2,     10},    // GNoiseAmount
        {      4,         1,         20,         1,     10},    // GCellSize
        {      1,         1,    NHMaxSize/2,     1,     10},    // GNHRadius
        {      0,         0,          0,         0,     10},    // GNeighborhood
        {      1,         0,       1000,         1,     10},    // GIsolation
        {      4,         0,       1000,         1,     10},    // GOvercrowding
        {      3,         0,       1000,         1,     10},    // GBirthMin
        {      3,         0,       1000,         1,     10},    // GBirthMax
        {    230,         0,        255,        15,     10},    // GRedPeak
        {    150,         0,        255,        15,     10},    // GGreenPeak
        {     70,         0,        255,        15,     10},    // GBluePeak
        {     60,         1,       1000,        15,     10},    // GRedBreadth
        {     60,         1,       1000,        15,     10},    // GGreenBreadth
        {     60,         1,       1000,        15,     10},    // GBlueBreadth
        {      0,         0,          1,         1,     10},    // GDoAlpha
        {    128,         0,        255,        40,     10}     // GAlpha
    };
    
    private int[] Genes;
    
    public static final int InitFieldSize = 7;  // Could be boolean
    private int[][] InitField = // Must be quadratic with side InitFieldSize
    {
        {0,0,0,0,0,0,0},
        {0,0,0,0,0,0,0},
        {0,0,0,1,0,0,0},
        {0,0,1,1,1,0,0},
        {0,0,0,1,0,0,0},
        {0,0,0,0,0,0,0},
        {0,0,0,0,0,0,0}
    };
    
    public boolean[][] Neighborhood;     // A quadratic array to store neighborhood
//    {
//        {true,false,false},
//        {true,true,true},
//        {false,false,false}       
//    };
    
    // If a mutation of init field occurs, the type of mutation is chosen according to 
    // the following frequencies (which must sum up to 100%):
    private static final int AsymIFMutFreq = 20; // Asymmetric: A single cell is flipped
    private static final int XSymIFMutFreq = 35; // X-symmetric: two x-axis-symmetric cells are flipped
    private static final int XYSymIFMutFreq = 45; // XY-symmetric: four xy-symmetric cells are flipped
    
    // The same thing for the neighborhood field:
    private static final int AsymNHMutFreq = 20; // Asymmetric: A single cell is flipped
    private static final int XSymNHMutFreq = 35; // X-symmetric: two x-axis-symmetric cells are flipped
    private static final int XYSymNHMutFreq = 45; // XY-symmetric: four xy-symmetric cells are flipped
    
    
    private static Random r = new Random(); // Random number generator
    
    /**
     * Creates a new instance of CGenome
     */
    public CGenome() {
        Genes = new int[GCount];
        for (int y = 0; y < GCount; y++) {
            Genes[y] = GeneDefs[y][InitVal];
        }
        
        // Create classic game of life neighborhood
        Neighborhood = new boolean[NHMaxSize][NHMaxSize]; // default value = false
        for (int y = -1; y <= 1; y++)
            for (int x = -1; x <= 1; x++)
                if (x != 0 || y != 0)
                    Neighborhood[NHMaxSize / 2 + x][NHMaxSize / 2 + y] = true;
    }
    
    // Constructor for loading from file
    public CGenome(ObjectInputStream is) throws FileNotFoundException, IOException, ClassNotFoundException {
        Genes = new int[GCount];
        Neighborhood = new boolean[NHMaxSize][NHMaxSize];
        Load(is);
    }
    
    public CGenome(my.CellArts.CGenome g) {
        Genes = new int[GCount];
        Neighborhood = new boolean[NHMaxSize][NHMaxSize];

        for (int i = 0; i < Genes.length; i++)
            Genes[i] = g.GetGene(i);
        
        for (int y = 0; y < InitFieldSize; y++)
            for (int x = 0; x < InitFieldSize; x++)
                InitField[x][y] = g.GetInitField(x, y);
        
        for (int y = 0; y < NHMaxSize; y++)
            for (int x = 0; x < NHMaxSize; x++)                
                Neighborhood[x][y] = g.Neighborhood[x][y];
    }
    
    public int GetGene(int Gene) {
        return Genes[Gene];
        //return (int)(Genes[Gene]);
    }
    
    public int GetInitField(int x, int y) {
        return InitField[x][y];
    }
    
    
    // Returns a deep copy of itself
    public CGenome Copy() {
        CGenome Copy = new CGenome();
        for (int i = 0; i < Genes.length; i++)
            Copy.Genes[i] = Genes[i];
        
        for (int y = 0; y < InitFieldSize; y++)
            for (int x = 0; x < InitFieldSize; x++)
                Copy.InitField[x][y] = InitField[x][y];
        
        for (int y = 0; y < NHMaxSize; y++)
            for (int x = 0; x < NHMaxSize; x++)
                Copy.Neighborhood[x][y] = Neighborhood[x][y];
        
        return Copy;
    }
    
    public void Save(ObjectOutputStream os) throws FileNotFoundException, IOException {
        for (int i = 0; i < Genes.length; i++)
            os.writeInt(Genes[i]);

        for (int y = 0; y < InitFieldSize; y++)
            for (int x = 0; x < InitFieldSize; x++)
                os.writeInt(InitField[x][y]);
        
        for (int y = 0; y < NHMaxSize; y++)
            for (int x = 0; x < NHMaxSize; x++)
                os.writeBoolean(Neighborhood[x][y]);
    }
    
    public void Load(ObjectInputStream is) throws FileNotFoundException, IOException, ClassNotFoundException {
        for (int i = 0; i < Genes.length; i++)
            Genes[i] = is.readInt();

        for (int y = 0; y < InitFieldSize; y++)
            for (int x = 0; x < InitFieldSize; x++)
                InitField[x][y] = is.readInt();
        
        for (int y = 0; y < NHMaxSize; y++)
            for (int x = 0; x < NHMaxSize; x++)
                Neighborhood[x][y] = is.readBoolean();
    }
    
    
    // int Mutations: How many genes change per mutation
    public CGenome Mutate(int Mutations) {
        // Copy genes
        CGenome Child = Copy();
        
        int totfreq = 0; // Total frequency
        for (int y = 0; y < GCount; y++)
            totfreq += (int)GeneDefs[y][MutFreq];
        
        // Create mutations
        for (int m = 0; m < Mutations; m++) {
            
            // Select gene according to the probabilities of GeneDefs[Gene][MutFreq]
            int dice = Math.abs(r.nextInt()) % totfreq;
            int cumfreq = 0;
            int iGene = 0;
            for (int y = 0; y < GCount; y++) {
                cumfreq += (int)GeneDefs[y][MutFreq];
                if (dice < cumfreq) {
                    iGene = y;
                    break;
                }
            }
            
            if (iGene == GInitField) { // InitField mutations
                int dx = Math.abs(r.nextInt()) % InitFieldSize - InitFieldSize / 2;
                int dy = Math.abs(r.nextInt()) % InitFieldSize - InitFieldSize / 2;
                int IFMutationType = 1 + (Math.abs(r.nextInt()) % 100);
                int mx = InitFieldSize / 2;
                int my = InitFieldSize / 2;
                
                int newval = Child.InitField[mx + dx][my + dy] == 1 ? 0 : 1; // Flip value
                Child.InitField[mx + dx][my + dy] = newval; // Asymmetric mutation
                
                if (IFMutationType > AsymIFMutFreq && dx != 0) // dy-Axis-symmetric mutation
                    Child.InitField[mx - dx][my + dy] = newval;
                
                if (IFMutationType > AsymIFMutFreq + XSymIFMutFreq &&
                        dy != 0){ // dx-and-dy-Axis-symmetric mutation
                    Child.InitField[mx + dx][my - dy] = newval;
                    if (dx != 0)
                        Child.InitField[mx - dx][my - dy] = newval;
                }
            } else if (iGene == GNeighborhood) { // Neighborhood mutations
                int dx = Math.abs(r.nextInt()) % (GetGene(GNHRadius) * 2 + 1) - GetGene(GNHRadius);
                int dy = Math.abs(r.nextInt()) % (GetGene(GNHRadius) * 2 + 1) - GetGene(GNHRadius);
                int NHMutationType = 1 + (Math.abs(r.nextInt()) % 100);
                int mx = NHMaxSize / 2;
                int my = NHMaxSize / 2;
                
                boolean newval = !Child.Neighborhood[mx + dx][my + dy];
                Child.Neighborhood[mx + dx][my + dy] = newval;
                
                if (NHMutationType > AsymNHMutFreq && dx != 0)
                    Child.Neighborhood[mx - dx][my + dy] = newval;
                
                if (NHMutationType > AsymNHMutFreq + XSymNHMutFreq && dy != 0) {
                    Child.Neighborhood[mx + dx][my - dy] = newval;
                    if (dx != 0)
                        Child.Neighborhood[mx - dx][my - dy] = newval;              
                }
                                
            } else { // Normal gene mutations
                if (Math.abs(r.nextInt()) % 2 == 0) {
                    if (Child.Genes[iGene] + GeneDefs[iGene][StepVal] <= GeneDefs[iGene][MaxVal])
                        Child.Genes[iGene] = Child.Genes[iGene] + GeneDefs[iGene][StepVal];
                } else {
                    if (Child.Genes[iGene] - GeneDefs[iGene][StepVal] >= GeneDefs[iGene][MinVal])
                        Child.Genes[iGene] = Child.Genes[iGene] - GeneDefs[iGene][StepVal];
                }       
            }
        }
        
        return Child;
    }
    
}
