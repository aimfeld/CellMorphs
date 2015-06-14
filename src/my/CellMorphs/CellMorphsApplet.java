/*
 * CellMorphsApplet.java
 *
 * Created on 1. Juli 2007, 17:34
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package my.CellMorphs;

import java.awt.*;
import java.awt.image.*;
import javax.swing.*;
import java.awt.event.*;
import java.io.*; // File, OutputStreams
import java.util.Vector;
import Tools.*;
import my.CellMorphs.*;

/**
 *
 * @author  Adrian Imfeld
 */
public class CellMorphsApplet extends javax.swing.JApplet {
    
    private static int RenderType;
    private static int Version = 11;
    
    private static final int CellMorphCount = 6;
    private static final int CellMorphParent = 0;
    
    private static MemoryImageSource mis;
    private static Timer timer;
    
    private static CCellMorph[] CellMorphs;
    private static Vector Generations; /* Contains the current parent's genome and the ones of all its ancestors,
                                          the last element being the genome of the current parent. */
    private static int DisplayedGeneration;
    
    private static JPanel[] CellMorphPanels;
    private static JPanel[] SelectPanels;
    private static CCellMorphCanvas[] Canvases;
    
    private static int Selected = -1;
    
    private ActionListener taskPerformer = new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
            Step();
            Draw();
            
        }
    };
    
    private static final String CAFileExt = "ca"; // Old CellArts files without generation history (serialized)
    private static final String CMFileExt = "cm"; // New CellMorphs files with generation history (not serialized)
    private static final String[] Exts= {CMFileExt, CAFileExt};
    private static ExampleFileFilter FileFilter = new ExampleFileFilter(Exts, "CellMorph Files");
    
    /**
     * Creates new form CellMorphsUI
     */
    public CellMorphsApplet() {
        initComponents();
        
        RenderType = jComboBoxRender.getSelectedIndex();
        
        timer = new Timer(0, taskPerformer);
        
        DisplayedGeneration = 1;
        Generations = new Vector();
        CellMorphs = new CCellMorph[CellMorphCount];
        
        // Set up panel and canvas arrays for easier access in loops
        CellMorphPanels = new JPanel[CellMorphCount];
        CellMorphPanels[0] = jPanelCM1;
        CellMorphPanels[1] = jPanelCM2;
        CellMorphPanels[2] = jPanelCM3;
        CellMorphPanels[3] = jPanelCM4;
        CellMorphPanels[4] = jPanelCM5;
        CellMorphPanels[5] = jPanelCM6;
        Canvases = new CCellMorphCanvas[CellMorphCount];
        Canvases[0] = CMCanvas1;
        Canvases[1] = CMCanvas2;
        Canvases[2] = CMCanvas3;
        Canvases[3] = CMCanvas4;
        Canvases[4] = CMCanvas5;
        Canvases[5] = CMCanvas6;
        SelectPanels = new JPanel[CellMorphCount];
        SelectPanels[0] = jPanelSelect1;
        SelectPanels[1] = jPanelSelect2;
        SelectPanels[2] = jPanelSelect3;
        SelectPanels[3] = jPanelSelect4;
        SelectPanels[4] = jPanelSelect5;
        SelectPanels[5] = jPanelSelect6;
        
        Restart();
    }
    
// Do one step for all automata
    private void Step() {
        for (int i = 0; i < CellMorphCount; i++) {
            CellMorphs[i].Step();
        }
    }
    
// Completely restart CellArts
    private void Restart() {
        Generations.clear();
        SetNewParent(new CCellMorph(CMCanvas1.getWidth(), CMCanvas1.getHeight()), false);
        Generations.add(CellMorphs[CellMorphParent].CopyGenes());
        DisplayedGeneration = 1;
        Erase();
        Draw();
        UpdateControls();
        UpdateInfo();
        jButtonRunActionPerformed(null);
    }
    
// Draw all automata
    private void Draw() {
        for (int i = 0; i < CellMorphCount; i++) {
            CellMorphs[i].Render(RenderType);
            Canvases[i].SetImage(CellMorphs[i].GetImage()); // Make paint() work
            
            // For some reason, drawImage() is a lot faster if called here than in e.g.
            // CCellMorphCanvas.SetImage()...
            Graphics gr = Canvases[i].getGraphics();
            if(gr != null)
                gr.drawImage(CellMorphs[i].GetImage(), 0, 0, null);
        }
        
        jLabelStepCnt.setText(String.valueOf(CellMorphs[CellMorphParent].StepCnt)); // StepCnt is synchronous for all CellMorphs
//        if (jMenu.isPopupMenuVisible())
//            jMenu.repaint();
        
    }
    
// Erase Canvases
    private void Erase() {
        for (int i = 0; i < CellMorphCount; i++) { // Start with black background
            //Canvases[i].getGraphics().clearRect(0, 0, Canvases[i].getWidth(), Canvases[i].getHeight());
            Canvases[i].repaint(); // Calls CCellMorphCanvas.paint()
        }
        
//        EventQueue systemQueue = Toolkit.getDefaultToolkit().getSystemEventQueue();
//        while (systemQueue.peekEvent() != null)
//            try {
//
//                Thread.sleep(10);
//            } catch(InterruptedException e) {
//            }
    }
    private void SetNewParent(CCellMorph NewParent, boolean Resize) {
        CellMorphs[CellMorphParent] = NewParent;
        if (Resize)
            CellMorphs[CellMorphParent].Resize(Canvases[CellMorphParent].getWidth(), Canvases[CellMorphParent].getHeight());
        
        for (int i = 1; i < CellMorphCount; i++) { // Create new mutations
            CellMorphs[i] = CellMorphs[CellMorphParent].Mutate(jSliderMutations.getValue());
            CellMorphs[i].Resize(Canvases[i].getWidth(), Canvases[i].getHeight());
        }
    }
    
    private void Select(int iCellMorph, boolean ShowInfoOnly) {
        if (Selected == iCellMorph && !ShowInfoOnly) {     // Select new parent and create mutations
            if (DisplayedGeneration == Generations.size() || JOptionPane.showConfirmDialog(this, "You are selecting an ancestor of the most recent cellular automaton. By selecting a the ancestor, you will start a new lineage and lose all later generations. Do you want to continue?", "Select?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                Generations.setSize(DisplayedGeneration); // Discard all later generations
                SetNewParent(CellMorphs[Selected], true);
                
                if (Selected != CellMorphParent) { // Selecting the same parent again just creates new mutations without advancing the lineage
                    Generations.add(CellMorphs[CellMorphParent].CopyGenes()); // Add new parent's genome to '
                    DisplayedGeneration++;
                }
                
                //Panels[Selected].setBorder(javax.swing.BorderFactory.createTitledBorder(null, Panels[Selected].getAccessibleContext().getAccessibleName(), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 0, 12)));
                SelectPanels[Selected].setBackground(CellMorphPanels[Selected].getBackground());
                Selected = -1; // unselect
                Erase();
                Draw();
            }
        } else {                    // Mark as selected
            // unselect
            if (Selected != -1)
                SelectPanels[Selected].setBackground(CellMorphPanels[Selected].getBackground());
            //Panels[Selected].setBorder(javax.swing.BorderFactory.createTitledBorder(null, Panels[Selected].getAccessibleContext().getAccessibleName(), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 0, 12)));
            
            
            // select
            Selected = iCellMorph;
            SelectPanels[Selected].setBackground(Color.RED);
            //Panels[iCellMorph].setBorder(javax.swing.BorderFactory.createTitledBorder(null, Panels[iCellMorph].getAccessibleContext().getAccessibleName(), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 0, 12), new java.awt.Color(255, 0, 0)));
            //Panels[Selected].setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 0, 0), 3));
            
        }
        
        UpdateControls();
        UpdateInfo();
    }
    
    // Updates button states, checkboxes, comboboxes etc.
    private void UpdateControls() {
        jButtonRun.setEnabled(!timer.isRunning() && RenderType != CCellMorph.RTNeighborhood);
        jButtonStop.setEnabled(timer.isRunning() && RenderType != CCellMorph.RTNeighborhood);
        jButtonStep.setEnabled(!timer.isRunning() && RenderType != CCellMorph.RTNeighborhood);
        if (jComboBoxRender.getSelectedIndex() != RenderType)
            jComboBoxRender.setSelectedIndex(RenderType);
        jButtonBigView.setEnabled(Selected != -1);
        jButtonSave.setEnabled(Selected != -1);
        jMenuItemSave.setEnabled(Selected != -1);
        
        jSpinnerGeneration.setModel(new SpinnerNumberModel(DisplayedGeneration, 1, Generations.size(), 1));
        jSpinnerGeneration.setEnabled(Generations.size() > 1);
    }
    
    private void UpdateInfo() {
        if (Selected != -1) {
            CCellMorph ca = CellMorphs[Selected];
            jLabelNHRadius.setText(String.valueOf(ca.GetGene(CGenome.GNHRadius)));
            jLabelNHSize.setText(String.valueOf(ca.GetNHSize()));
            
            jLabelIsolation.setText(String.valueOf(ca.GetGene(CGenome.GIsolation)));
            jLabelOvercrowding.setText(String.valueOf(ca.GetGene(CGenome.GOvercrowding)));
            jLabelBirthMin.setText(String.valueOf(ca.GetGene(CGenome.GBirthMin)));
            jLabelBirthMax.setText(String.valueOf(ca.GetGene(CGenome.GBirthMax)));
            
            jLabelInitMode.setText(ca.GetGene(CGenome.GInitMode) == CGenome.IMInitField ? "Init Field" : "Random");
            jLabelNoise.setText(ca.GetGene(CGenome.GDoNoise) == 1 ? "Yes" : "No");
            jLabelAlpha.setText(ca.GetGene(CGenome.GDoAlpha) == 1 ? "Yes" : "No");
        }
        
        for (int i = 0; i < jPanelInfo.getComponentCount(); i++) {
            Component c = jPanelInfo.getComponent(i);
            if (c != jLabelStep && c != jLabelStepCnt)
                c.setVisible(Selected != -1);
        }
    }
    
    
    private void Save() {
        int returnVal = jFileChooser.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                File f = jFileChooser.getSelectedFile();
                String ext = FileFilter.getExtension(f);
                if (ext == null || ext.equals(CMFileExt) == false)    // Extension anhängen, falls nicht vorhanden
                    f = new File(f.getAbsolutePath()+"."+CMFileExt);
                
                FileOutputStream fs = new FileOutputStream(f);
                ObjectOutputStream os = new ObjectOutputStream(fs);
                
                os.writeInt(Version);
                CellMorphs[Selected].Save(os);
                os.writeInt(RenderType);
                
                // Write generation history
                Vector v = (Vector)Generations.clone(); // We only need the history up to the current displayed generation
                v.setSize(DisplayedGeneration); // This doesn't change Generations.size(), tested
                if (Selected != CellMorphParent) // Complete the lineage for children
                    v.add(CellMorphs[Selected].CopyGenes());
                CGenome g;
                os.writeInt(v.size());
                for (int i = 0; i < v.size(); i++) {
                    g = (CGenome)v.get(i);
                    g.Save(os);
                }
                os.close();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, e.toString());
            }
            JOptionPane.showMessageDialog(this, "File has been saved.");
        }
    }
    
    private void Load() {
        int returnVal = jFileChooser.showOpenDialog(this);
        CCellMorph ca;
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                File f = jFileChooser.getSelectedFile();
                FileInputStream fs = new FileInputStream(f);
                ObjectInputStream is = new ObjectInputStream(fs);
                
                if (FileFilter.getExtension(f).equals(CAFileExt)) { // Load *.ca 
                    SetNewParent(CCellMorph.LoadCA(is, CMCanvas1.getWidth(), CMCanvas1.getHeight()), false);
                    RenderType = is.readInt();
                    Generations.clear();
                    Generations.add(CellMorphs[CellMorphParent].CopyGenes());
                    DisplayedGeneration = 1;
                } else { // Load *.cm 
                    int vers = is.readInt();
                    SetNewParent(CCellMorph.Load(is, CMCanvas1.getWidth(), CMCanvas1.getHeight()), false);                    
                    RenderType = is.readInt();
                    
                    Generations.clear();
                    int size = is.readInt();
                    
                    for (int i = 0; i < size; i++) {
                        CGenome g = new CGenome(is);
                        Generations.add(g);
                    }
                    DisplayedGeneration = Generations.size();
                }
                
                is.close();
            } catch (Exception e) {
                //System.err.println(e.toString());
                JOptionPane.showMessageDialog(this, e.toString());
            }
        }
        Select(CellMorphParent, true);
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">                          
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jFileChooser = new javax.swing.JFileChooser();
        jPanelInfo = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jLabelNHRadius = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabelNHSize = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabelIsolation = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabelOvercrowding = new javax.swing.JLabel();
        jLabelBirthMin = new javax.swing.JLabel();
        jLabelBirthMax = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabelInitMode = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        jLabelNoise = new javax.swing.JLabel();
        jLabel22 = new javax.swing.JLabel();
        jLabelAlpha = new javax.swing.JLabel();
        jLabelStep = new javax.swing.JLabel();
        jLabelStepCnt = new javax.swing.JLabel();
        jPanelControls = new javax.swing.JPanel();
        jButtonStep = new javax.swing.JButton();
        jSliderSpeed = new javax.swing.JSlider();
        jButtonRun = new javax.swing.JButton();
        jButtonStop = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jButtonSave = new javax.swing.JButton();
        jButtonLoad = new javax.swing.JButton();
        jButtonBigView = new javax.swing.JButton();
        jButtonReInit = new javax.swing.JButton();
        jComboBoxRender = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jSliderMutations = new javax.swing.JSlider();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jSpinnerGeneration = new javax.swing.JSpinner();
        jLabel7 = new javax.swing.JLabel();
        jButtonRestart = new javax.swing.JButton();
        jPanelCellMorphs = new javax.swing.JPanel();
        jPanelCM1 = new javax.swing.JPanel();
        jPanelSelect1 = new javax.swing.JPanel();
        CMCanvas1 = new my.CellMorphs.CCellMorphCanvas();
        jPanelCM2 = new javax.swing.JPanel();
        jPanelSelect2 = new javax.swing.JPanel();
        CMCanvas2 = new my.CellMorphs.CCellMorphCanvas();
        jPanelCM3 = new javax.swing.JPanel();
        jPanelSelect3 = new javax.swing.JPanel();
        CMCanvas3 = new my.CellMorphs.CCellMorphCanvas();
        jPanelCM4 = new javax.swing.JPanel();
        jPanelSelect4 = new javax.swing.JPanel();
        CMCanvas4 = new my.CellMorphs.CCellMorphCanvas();
        jPanelCM5 = new javax.swing.JPanel();
        jPanelSelect5 = new javax.swing.JPanel();
        CMCanvas5 = new my.CellMorphs.CCellMorphCanvas();
        jPanelCM6 = new javax.swing.JPanel();
        jPanelSelect6 = new javax.swing.JPanel();
        CMCanvas6 = new my.CellMorphs.CCellMorphCanvas();
        jMenuBar = new javax.swing.JMenuBar();
        jMenu = new javax.swing.JMenu();
        jMenuItemHelp = new javax.swing.JMenuItem();
        jMenuItemRestart = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        jMenuItemSave = new javax.swing.JMenuItem();
        jMenuItemLoad = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        jMenuItemExit = new javax.swing.JMenuItem();

        jFileChooser.setCurrentDirectory(null);
        jFileChooser.setDialogType(javax.swing.JFileChooser.SAVE_DIALOG);
        jFileChooser.setFileFilter(FileFilter);

        //setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        //setTitle("CellMorphs v1.1");
        //setIconImage(new javax.swing.ImageIcon("D:\\Projects\\Java NetBeans\\CellMorphs\\pics\\monkey_icon.png").getImage());
        setMinimumSize(new java.awt.Dimension(800, 720));
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        jPanelInfo.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Info", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 0, 12)));
        jLabel6.setText("Neighborhood Radius:");

        jLabelNHRadius.setText("0");

        jLabel8.setText("Neighborhood Size:");

        jLabelNHSize.setText("0");

        jLabel10.setText("Death by Isolation <=");

        jLabelIsolation.setText("0");

        jLabel12.setText("Death by Overcrowding >=");

        jLabel13.setText("Birthing Minimum:");

        jLabel14.setText("Birthing Maximum:");

        jLabelOvercrowding.setText("0");

        jLabelBirthMin.setText("0");

        jLabelBirthMax.setText("0");

        jLabel15.setText("Init:");

        jLabelInitMode.setText("bla");

        jLabel20.setText("Noise:");

        jLabelNoise.setText("bla");

        jLabel22.setText("Transparency:");

        jLabelAlpha.setText("bla");

        jLabelStep.setText("Step:");

        jLabelStepCnt.setText("0");

        javax.swing.GroupLayout jPanelInfoLayout = new javax.swing.GroupLayout(jPanelInfo);
        jPanelInfo.setLayout(jPanelInfoLayout);
        jPanelInfoLayout.setHorizontalGroup(
            jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelInfoLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelInfoLayout.createSequentialGroup()
                        .addComponent(jLabelStep)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabelStepCnt))
                    .addGroup(jPanelInfoLayout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabelNHRadius))
                    .addGroup(jPanelInfoLayout.createSequentialGroup()
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabelNHSize))
                    .addGroup(jPanelInfoLayout.createSequentialGroup()
                        .addComponent(jLabel10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabelIsolation))
                    .addGroup(jPanelInfoLayout.createSequentialGroup()
                        .addComponent(jLabel12)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabelOvercrowding))
                    .addGroup(jPanelInfoLayout.createSequentialGroup()
                        .addComponent(jLabel13)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabelBirthMin))
                    .addGroup(jPanelInfoLayout.createSequentialGroup()
                        .addComponent(jLabel14)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabelBirthMax))
                    .addGroup(jPanelInfoLayout.createSequentialGroup()
                        .addComponent(jLabel15)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabelInitMode))
                    .addGroup(jPanelInfoLayout.createSequentialGroup()
                        .addComponent(jLabel20)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabelNoise))
                    .addGroup(jPanelInfoLayout.createSequentialGroup()
                        .addComponent(jLabel22)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabelAlpha)))
                .addContainerGap(67, Short.MAX_VALUE))
        );
        jPanelInfoLayout.setVerticalGroup(
            jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelInfoLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelStep)
                    .addComponent(jLabelStepCnt))
                .addGap(18, 18, 18)
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(jLabelNHRadius))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(jLabelNHSize))
                .addGap(18, 18, 18)
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(jLabelIsolation))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel12)
                    .addComponent(jLabelOvercrowding))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel13)
                    .addComponent(jLabelBirthMin))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel14)
                    .addComponent(jLabelBirthMax))
                .addGap(22, 22, 22)
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel15)
                    .addComponent(jLabelInitMode))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel20)
                    .addComponent(jLabelNoise))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelInfoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel22)
                    .addComponent(jLabelAlpha))
                .addContainerGap(14, Short.MAX_VALUE))
        );

        jPanelControls.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Controls", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 0, 12)));
        jButtonStep.setText("Step");
        jButtonStep.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonStepActionPerformed(evt);
            }
        });

        jSliderSpeed.setMaximum(32);
        jSliderSpeed.setValue(24);
        jSliderSpeed.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSliderSpeedStateChanged(evt);
            }
        });

        jButtonRun.setText("Run");
        jButtonRun.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRunActionPerformed(evt);
            }
        });

        jButtonStop.setText("Stop");
        jButtonStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonStopActionPerformed(evt);
            }
        });

        jLabel1.setText("Speed: -");

        jButtonSave.setText("Save Selected");
        jButtonSave.setToolTipText("");
        jButtonSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSaveActionPerformed(evt);
            }
        });

        jButtonLoad.setText("Load");
        jButtonLoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonLoadActionPerformed(evt);
            }
        });

        jButtonBigView.setText("Big View");
        jButtonBigView.setToolTipText("Enlarge the currently selected CellMorph");
        jButtonBigView.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonBigViewActionPerformed(evt);
            }
        });

        jButtonReInit.setText("Reinitialze");
        jButtonReInit.setToolTipText("Watch the current CellMorphs develop again");
        jButtonReInit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonReInitActionPerformed(evt);
            }
        });

        jComboBoxRender.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Cell State", "Neighbor Count", "Image", "Neighborhood Shape" }));
        jComboBoxRender.setSelectedIndex(2);
        jComboBoxRender.setToolTipText("Different display modes");
        jComboBoxRender.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jComboBoxRenderItemStateChanged(evt);
            }
        });

        jLabel2.setText("Render:");

        jLabel3.setText("+");

        jSliderMutations.setMajorTickSpacing(5);
        jSliderMutations.setMaximum(20);
        jSliderMutations.setMinorTickSpacing(1);
        jSliderMutations.setPaintTicks(true);
        jSliderMutations.setToolTipText("Number of genes mutated in children when a new parent is selected");
        jSliderMutations.setValue(3);

        jLabel4.setText("Mutations: -");

        jLabel5.setText("+");

        jSpinnerGeneration.setToolTipText("Review the evolutionary history");
        jSpinnerGeneration.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinnerGenerationStateChanged(evt);
            }
        });

        jLabel7.setText("Generation:");

        jButtonRestart.setText("Restart");
        jButtonRestart.setToolTipText("Completely restart CellMorphs");
        jButtonRestart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRestartActionPerformed(evt);
            }
        });

        jButtonRestart.getAccessibleContext().setAccessibleName("jButtonRestart");

        javax.swing.GroupLayout jPanelControlsLayout = new javax.swing.GroupLayout(jPanelControls);
        jPanelControls.setLayout(jPanelControlsLayout);
        jPanelControlsLayout.setHorizontalGroup(
            jPanelControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelControlsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelControlsLayout.createSequentialGroup()
                        .addGroup(jPanelControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel4))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSliderSpeed, javax.swing.GroupLayout.DEFAULT_SIZE, 136, Short.MAX_VALUE)
                            .addComponent(jSliderMutations, javax.swing.GroupLayout.DEFAULT_SIZE, 136, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, 9, Short.MAX_VALUE)
                            .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, 9, Short.MAX_VALUE)))
                    .addGroup(jPanelControlsLayout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jComboBoxRender, 0, 173, Short.MAX_VALUE))
                    .addGroup(jPanelControlsLayout.createSequentialGroup()
                        .addComponent(jButtonRun, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonStop)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonStep))
                    .addGroup(jPanelControlsLayout.createSequentialGroup()
                        .addGroup(jPanelControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jButtonRestart, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButtonLoad, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButtonBigView, javax.swing.GroupLayout.Alignment.LEADING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jButtonSave, javax.swing.GroupLayout.DEFAULT_SIZE, 136, Short.MAX_VALUE)
                            .addComponent(jButtonReInit, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 136, Short.MAX_VALUE)
                            .addGroup(jPanelControlsLayout.createSequentialGroup()
                                .addComponent(jLabel7)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jSpinnerGeneration, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addContainerGap())
        );

        jPanelControlsLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jButtonRun, jButtonStep, jButtonStop});

        jPanelControlsLayout.setVerticalGroup(
            jPanelControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelControlsLayout.createSequentialGroup()
                .addGroup(jPanelControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jSliderSpeed, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(8, 8, 8)
                .addGroup(jPanelControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jSliderMutations, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(30, 30, 30)
                .addGroup(jPanelControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonRun)
                    .addComponent(jButtonStop)
                    .addComponent(jButtonStep))
                .addGap(27, 27, 27)
                .addGroup(jPanelControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jComboBoxRender, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(35, 35, 35)
                .addGroup(jPanelControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonReInit)
                    .addComponent(jButtonBigView))
                .addGap(24, 24, 24)
                .addGroup(jPanelControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonSave, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonLoad))
                .addGap(31, 31, 31)
                .addGroup(jPanelControlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonRestart)
                    .addComponent(jLabel7)
                    .addComponent(jSpinnerGeneration, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanelCM1.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Parent", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 0, 12)));
        jPanelSelect1.setLayout(new java.awt.GridBagLayout());

        CMCanvas1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                CMCanvasMousePressed(evt);
            }
        });

        javax.swing.GroupLayout CMCanvas1Layout = new javax.swing.GroupLayout(CMCanvas1);
        CMCanvas1.setLayout(CMCanvas1Layout);
        CMCanvas1Layout.setHorizontalGroup(
            CMCanvas1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 217, Short.MAX_VALUE)
        );
        CMCanvas1Layout.setVerticalGroup(
            CMCanvas1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 182, Short.MAX_VALUE)
        );
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
        jPanelSelect1.add(CMCanvas1, gridBagConstraints);

        javax.swing.GroupLayout jPanelCM1Layout = new javax.swing.GroupLayout(jPanelCM1);
        jPanelCM1.setLayout(jPanelCM1Layout);
        jPanelCM1Layout.setHorizontalGroup(
            jPanelCM1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCM1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanelSelect1, javax.swing.GroupLayout.DEFAULT_SIZE, 223, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanelCM1Layout.setVerticalGroup(
            jPanelCM1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelCM1Layout.createSequentialGroup()
                .addComponent(jPanelSelect1, javax.swing.GroupLayout.DEFAULT_SIZE, 188, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanelCM2.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Child 1", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 0, 12)));
        jPanelSelect2.setLayout(new java.awt.GridBagLayout());

        CMCanvas2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                CMCanvasMousePressed(evt);
            }
        });

        javax.swing.GroupLayout CMCanvas2Layout = new javax.swing.GroupLayout(CMCanvas2);
        CMCanvas2.setLayout(CMCanvas2Layout);
        CMCanvas2Layout.setHorizontalGroup(
            CMCanvas2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 217, Short.MAX_VALUE)
        );
        CMCanvas2Layout.setVerticalGroup(
            CMCanvas2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 182, Short.MAX_VALUE)
        );
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
        jPanelSelect2.add(CMCanvas2, gridBagConstraints);

        javax.swing.GroupLayout jPanelCM2Layout = new javax.swing.GroupLayout(jPanelCM2);
        jPanelCM2.setLayout(jPanelCM2Layout);
        jPanelCM2Layout.setHorizontalGroup(
            jPanelCM2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCM2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanelSelect2, javax.swing.GroupLayout.DEFAULT_SIZE, 223, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanelCM2Layout.setVerticalGroup(
            jPanelCM2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCM2Layout.createSequentialGroup()
                .addComponent(jPanelSelect2, javax.swing.GroupLayout.DEFAULT_SIZE, 188, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanelCM3.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Child 2", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 0, 12)));
        jPanelSelect3.setLayout(new java.awt.GridBagLayout());

        CMCanvas3.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                CMCanvasMousePressed(evt);
            }
        });

        javax.swing.GroupLayout CMCanvas3Layout = new javax.swing.GroupLayout(CMCanvas3);
        CMCanvas3.setLayout(CMCanvas3Layout);
        CMCanvas3Layout.setHorizontalGroup(
            CMCanvas3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 217, Short.MAX_VALUE)
        );
        CMCanvas3Layout.setVerticalGroup(
            CMCanvas3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 182, Short.MAX_VALUE)
        );
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
        jPanelSelect3.add(CMCanvas3, gridBagConstraints);

        javax.swing.GroupLayout jPanelCM3Layout = new javax.swing.GroupLayout(jPanelCM3);
        jPanelCM3.setLayout(jPanelCM3Layout);
        jPanelCM3Layout.setHorizontalGroup(
            jPanelCM3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCM3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanelSelect3, javax.swing.GroupLayout.DEFAULT_SIZE, 223, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanelCM3Layout.setVerticalGroup(
            jPanelCM3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCM3Layout.createSequentialGroup()
                .addComponent(jPanelSelect3, javax.swing.GroupLayout.DEFAULT_SIZE, 188, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanelCM4.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Child 3", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 0, 12)));
        jPanelSelect4.setLayout(new java.awt.GridBagLayout());

        CMCanvas4.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                CMCanvasMousePressed(evt);
            }
        });

        javax.swing.GroupLayout CMCanvas4Layout = new javax.swing.GroupLayout(CMCanvas4);
        CMCanvas4.setLayout(CMCanvas4Layout);
        CMCanvas4Layout.setHorizontalGroup(
            CMCanvas4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 217, Short.MAX_VALUE)
        );
        CMCanvas4Layout.setVerticalGroup(
            CMCanvas4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 182, Short.MAX_VALUE)
        );
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
        jPanelSelect4.add(CMCanvas4, gridBagConstraints);

        javax.swing.GroupLayout jPanelCM4Layout = new javax.swing.GroupLayout(jPanelCM4);
        jPanelCM4.setLayout(jPanelCM4Layout);
        jPanelCM4Layout.setHorizontalGroup(
            jPanelCM4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelCM4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanelSelect4, javax.swing.GroupLayout.DEFAULT_SIZE, 223, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanelCM4Layout.setVerticalGroup(
            jPanelCM4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCM4Layout.createSequentialGroup()
                .addComponent(jPanelSelect4, javax.swing.GroupLayout.DEFAULT_SIZE, 188, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanelCM5.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Child 4", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 0, 12)));
        jPanelSelect5.setLayout(new java.awt.GridBagLayout());

        CMCanvas5.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                CMCanvasMousePressed(evt);
            }
        });

        javax.swing.GroupLayout CMCanvas5Layout = new javax.swing.GroupLayout(CMCanvas5);
        CMCanvas5.setLayout(CMCanvas5Layout);
        CMCanvas5Layout.setHorizontalGroup(
            CMCanvas5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 217, Short.MAX_VALUE)
        );
        CMCanvas5Layout.setVerticalGroup(
            CMCanvas5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 181, Short.MAX_VALUE)
        );
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
        jPanelSelect5.add(CMCanvas5, gridBagConstraints);

        javax.swing.GroupLayout jPanelCM5Layout = new javax.swing.GroupLayout(jPanelCM5);
        jPanelCM5.setLayout(jPanelCM5Layout);
        jPanelCM5Layout.setHorizontalGroup(
            jPanelCM5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCM5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanelSelect5, javax.swing.GroupLayout.DEFAULT_SIZE, 223, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanelCM5Layout.setVerticalGroup(
            jPanelCM5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCM5Layout.createSequentialGroup()
                .addComponent(jPanelSelect5, javax.swing.GroupLayout.DEFAULT_SIZE, 187, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanelCM6.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Child 5", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 0, 12)));
        jPanelSelect6.setLayout(new java.awt.GridBagLayout());

        CMCanvas6.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                CMCanvasMousePressed(evt);
            }
        });

        javax.swing.GroupLayout CMCanvas6Layout = new javax.swing.GroupLayout(CMCanvas6);
        CMCanvas6.setLayout(CMCanvas6Layout);
        CMCanvas6Layout.setHorizontalGroup(
            CMCanvas6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 217, Short.MAX_VALUE)
        );
        CMCanvas6Layout.setVerticalGroup(
            CMCanvas6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 181, Short.MAX_VALUE)
        );
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
        jPanelSelect6.add(CMCanvas6, gridBagConstraints);

        javax.swing.GroupLayout jPanelCM6Layout = new javax.swing.GroupLayout(jPanelCM6);
        jPanelCM6.setLayout(jPanelCM6Layout);
        jPanelCM6Layout.setHorizontalGroup(
            jPanelCM6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCM6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanelSelect6, javax.swing.GroupLayout.DEFAULT_SIZE, 223, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanelCM6Layout.setVerticalGroup(
            jPanelCM6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCM6Layout.createSequentialGroup()
                .addComponent(jPanelSelect6, javax.swing.GroupLayout.DEFAULT_SIZE, 187, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanelCellMorphsLayout = new javax.swing.GroupLayout(jPanelCellMorphs);
        jPanelCellMorphs.setLayout(jPanelCellMorphsLayout);
        jPanelCellMorphsLayout.setHorizontalGroup(
            jPanelCellMorphsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCellMorphsLayout.createSequentialGroup()
                .addGroup(jPanelCellMorphsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanelCM5, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelCM3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelCM1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelCellMorphsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanelCM6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelCM4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanelCellMorphsLayout.createSequentialGroup()
                        .addComponent(jPanelCM2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                .addContainerGap())
        );
        jPanelCellMorphsLayout.setVerticalGroup(
            jPanelCellMorphsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCellMorphsLayout.createSequentialGroup()
                .addGroup(jPanelCellMorphsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanelCM2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelCM1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelCellMorphsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanelCM4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelCM3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelCellMorphsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanelCM6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelCM5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );

        jMenu.setText("Menu");
        jMenu.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                jMenuMousePressed(evt);
            }
        });

        jMenuItemHelp.setText("Help");
        jMenuItemHelp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemHelpActionPerformed(evt);
            }
        });

        jMenu.add(jMenuItemHelp);

        jMenuItemRestart.setText("Restart");
        jMenuItemRestart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemRestartActionPerformed(evt);
            }
        });

        jMenu.add(jMenuItemRestart);

        jMenu.add(jSeparator1);

        jMenuItemSave.setText("Save Selected");
        jMenuItemSave.setEnabled(false);
        jMenuItemSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSaveActionPerformed(evt);
            }
        });

        jMenu.add(jMenuItemSave);

        jMenuItemLoad.setText("Load");
        jMenuItemLoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemLoadActionPerformed(evt);
            }
        });

        jMenu.add(jMenuItemLoad);

        jMenu.add(jSeparator2);

        jMenuItemExit.setText("Exit");
        jMenuItemExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemExitActionPerformed(evt);
            }
        });

        jMenu.add(jMenuItemExit);

        jMenuBar.add(jMenu);

        setJMenuBar(jMenuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanelCellMorphs, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanelInfo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelControls, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanelCellMorphs, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanelInfo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanelControls, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        //pack();
    }// </editor-fold>                        
    
    private void CMCanvasMousePressed(java.awt.event.MouseEvent evt) {                                      
        for (int i = 0; i < CellMorphCount; i++)
            if (Canvases[i] == evt.getComponent())
                Select(i, false);
    }                                     
    
    private void jSpinnerGenerationStateChanged(javax.swing.event.ChangeEvent evt) {                                                
        SpinnerNumberModel m = (SpinnerNumberModel)jSpinnerGeneration.getModel();
        int val = m.getNumber().intValue();
        if (DisplayedGeneration != val) {
            DisplayedGeneration = val;
            SetNewParent(new CCellMorph(CMCanvas1.getWidth(), CMCanvas1.getHeight(), (CGenome)Generations.get(DisplayedGeneration - 1)), false);
            Select(CellMorphParent, true);
            Erase();
            Draw();
            UpdateControls();
            UpdateInfo();
        }
        
    }                                               
    
    private void jButtonRestartActionPerformed(java.awt.event.ActionEvent evt) {                                               
        jButtonStopActionPerformed(null);
        if (JOptionPane.showConfirmDialog(this,"You are about to completely restart CellMorphs. You will lose your all your cellular automata. Do you want to restart?", "Restart?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            Restart();
        }
        jButtonRunActionPerformed(null);
    }                                              
    
    private void jMenuItemHelpActionPerformed(java.awt.event.ActionEvent evt) {                                              
        try {
            //BrowserLauncher.openURL("D:\\Projects\\Java NetBeans\\CellMorphs\\dist\\CellMorphs.html");
            // BrowserLauncher.openURL("file:///D:/Projects/Java%20NetBeans/CellMorphs/dist/CellMorphs.html");
            
            // CellMorphs.html is in the same directory as CellMorphs.jar. Incredibly enough, there seems to
            // be no way of determining the running jar's directory. System.getProperty("user.dir") usually
            // corresponds to it, but can be different if the jar is run from the commandline with a path specified.
            // BrowserLauncher.openURL("CellMorphs.html");
            
             BrowserLauncher.openURL("http://www.aimfeld.ch/CellMorphs/CellMorphs.html");
        } catch(Exception e) {
            JOptionPane.showMessageDialog(this, "Default browser could not be launched. Please go to www.aimfeld.ch/CellMorphs/CellMorphs.html for help.");
        }
    }                                             
    
    private void jMenuItemSaveActionPerformed(java.awt.event.ActionEvent evt) {                                              
        jButtonSaveActionPerformed(null);
    }                                             
    
    private void jMenuItemLoadActionPerformed(java.awt.event.ActionEvent evt) {                                              
        jButtonLoadActionPerformed(null);
    }                                             
    
    private void jMenuItemExitActionPerformed(java.awt.event.ActionEvent evt) {                                              
        System.exit(0);
    }                                             
    
    private void jMenuItemRestartActionPerformed(java.awt.event.ActionEvent evt) {                                                 
        jButtonRestartActionPerformed(null);
    }                                                
    
    private void jMenuMousePressed(java.awt.event.MouseEvent evt) {                                   
        jButtonStopActionPerformed(null); // Prevent menu from being overdrawn by the canvas.
    }                                  
    
    private void cACanvasMousePressed(java.awt.event.MouseEvent evt) {                                      
        
    }                                     
    
    private void jComboBoxRenderItemStateChanged(java.awt.event.ItemEvent evt) {                                                 
        if (jComboBoxRender.getSelectedIndex() != -1)
            RenderType = jComboBoxRender.getSelectedIndex();
        UpdateControls();
        Erase();
        Draw();
    }                                                
    
    private void jButtonLoadActionPerformed(java.awt.event.ActionEvent evt) {                                            
        jButtonStopActionPerformed(null);
        Load();
        //Draw();
        jButtonRunActionPerformed(null);
    }                                           
    
    private void jButtonSaveActionPerformed(java.awt.event.ActionEvent evt) {                                            
        jButtonStopActionPerformed(null);
        Save();
    }                                           
    
    private void formComponentResized(java.awt.event.ComponentEvent evt) {                                      
        /* Annoying resizing problem: A canvas that is anchored to all sides of
         * a jpanel will prevent the panel from getting smaller. This seems to be
         * a bug, since a jbutton resize fine. Here's a workaround: */
        
//        for (int i = 0; i < CellMorphCount; i++)
//            Canvases[i].setSize(1, 1);
//
//        validate(); // Will resize the canvases again because of anchoring
        
        for (int i = 0; i < CellMorphCount; i++)
            CellMorphs[i].Resize(Canvases[i].getSize().width, Canvases[i].getSize().height);
        
        Erase();
        Draw();
    }                                     
    
    private void jButtonReInitActionPerformed(java.awt.event.ActionEvent evt) {                                              
        for (int i = 0; i < CellMorphCount; i++) {
            CellMorphs[i].Init();
            //CellMorphs[i].Resize(Canvases[i].getWidth(), Canvases[i].getHeight());
        }
        
        Erase();
        Draw();
    }                                             
    
    private void jButtonBigViewActionPerformed(java.awt.event.ActionEvent evt) {                                               
        jButtonStopActionPerformed(null);
        new BigViewFrame(CellMorphs[Selected].CopyGenes(), RenderType).setVisible(true);
    }                                              
    
    private void jButtonStopActionPerformed(java.awt.event.ActionEvent evt) {                                            
        timer.stop();
        UpdateControls();
    }                                           
    
    private void jSliderSpeedStateChanged(javax.swing.event.ChangeEvent evt) {                                          
        timer.setDelay((jSliderSpeed.getMaximum() - jSliderSpeed.getValue() + 1) * 10);
    }                                         
    
    private void jButtonRunActionPerformed(java.awt.event.ActionEvent evt) {                                           
        timer.setDelay((jSliderSpeed.getMaximum() - jSliderSpeed.getValue() + 1) * 10);
        timer.start();
        UpdateControls();
    }                                          
    
    private void jButtonStepActionPerformed(java.awt.event.ActionEvent evt) {                                            
        Step();
        Draw();
    }                                           
    
    /**
     * @param args the command line arguments
     */
//    public static void main(String args[]) {
//        java.awt.EventQueue.invokeLater(new Runnable() {
//            public void run() {
//                new CellMorphsUI().setVisible(true);
//            }
//        });
//    }
    
    // Variables declaration - do not modify                     
    private my.CellMorphs.CCellMorphCanvas CMCanvas1;
    private my.CellMorphs.CCellMorphCanvas CMCanvas2;
    private my.CellMorphs.CCellMorphCanvas CMCanvas3;
    private my.CellMorphs.CCellMorphCanvas CMCanvas4;
    private my.CellMorphs.CCellMorphCanvas CMCanvas5;
    private my.CellMorphs.CCellMorphCanvas CMCanvas6;
    private javax.swing.JButton jButtonBigView;
    private javax.swing.JButton jButtonLoad;
    private javax.swing.JButton jButtonReInit;
    private javax.swing.JButton jButtonRestart;
    private javax.swing.JButton jButtonRun;
    private javax.swing.JButton jButtonSave;
    private javax.swing.JButton jButtonStep;
    private javax.swing.JButton jButtonStop;
    private javax.swing.JComboBox jComboBoxRender;
    private javax.swing.JFileChooser jFileChooser;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabelAlpha;
    private javax.swing.JLabel jLabelBirthMax;
    private javax.swing.JLabel jLabelBirthMin;
    private javax.swing.JLabel jLabelInitMode;
    private javax.swing.JLabel jLabelIsolation;
    private javax.swing.JLabel jLabelNHRadius;
    private javax.swing.JLabel jLabelNHSize;
    private javax.swing.JLabel jLabelNoise;
    private javax.swing.JLabel jLabelOvercrowding;
    private javax.swing.JLabel jLabelStep;
    private javax.swing.JLabel jLabelStepCnt;
    private javax.swing.JMenu jMenu;
    private javax.swing.JMenuBar jMenuBar;
    private javax.swing.JMenuItem jMenuItemExit;
    private javax.swing.JMenuItem jMenuItemHelp;
    private javax.swing.JMenuItem jMenuItemLoad;
    private javax.swing.JMenuItem jMenuItemRestart;
    private javax.swing.JMenuItem jMenuItemSave;
    private javax.swing.JPanel jPanelCM1;
    private javax.swing.JPanel jPanelCM2;
    private javax.swing.JPanel jPanelCM3;
    private javax.swing.JPanel jPanelCM4;
    private javax.swing.JPanel jPanelCM5;
    private javax.swing.JPanel jPanelCM6;
    private javax.swing.JPanel jPanelCellMorphs;
    private javax.swing.JPanel jPanelControls;
    private javax.swing.JPanel jPanelInfo;
    private javax.swing.JPanel jPanelSelect1;
    private javax.swing.JPanel jPanelSelect2;
    private javax.swing.JPanel jPanelSelect3;
    private javax.swing.JPanel jPanelSelect4;
    private javax.swing.JPanel jPanelSelect5;
    private javax.swing.JPanel jPanelSelect6;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSlider jSliderMutations;
    private javax.swing.JSlider jSliderSpeed;
    private javax.swing.JSpinner jSpinnerGeneration;
    // End of variables declaration                   
    
}
