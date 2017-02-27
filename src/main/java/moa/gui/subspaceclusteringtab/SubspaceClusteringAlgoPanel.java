/**
 * [SubspaceClusteringAlgoPanel.java] for Subspace MOA
 * 
 * Algorithm selection panel of "SubspaceClustering" tab - "Setup" subtab.
 * 
 * @author Yunsu Kim
 * 		   based on the implementation of Timm Jansen
 * Data Management and Data Exploration Group, RWTH Aachen University
 */

package moa.gui.subspaceclusteringtab;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;

import moa.clusterers.AbstractClusterer;
import moa.clusterers.AbstractSubspaceClusterer;
import moa.clusterers.Clusterer;
import moa.clusterers.SubspaceClusterer;
import moa.clusterers.macrosubspace.MacroSubspaceClusterer;
import moa.gui.GUIUtils;
import moa.gui.OptionEditComponent;
import moa.options.AbstractClassOption;
import moa.options.ClassOption;
import moa.options.ClassOptionWithNames;
import moa.streams.clustering.SubspaceClusteringStream;

public class SubspaceClusteringAlgoPanel extends javax.swing.JPanel implements ActionListener {

	private static final long serialVersionUID = 1L;

	protected List<OptionEditComponent> editComponents = new LinkedList<OptionEditComponent>();

    private ClassOption streamOption = new ClassOption("Stream", 's',
            "Subspace-clustered stream generator.", SubspaceClusteringStream.class, "SubspaceARFFStream"); 

    private ClassOptionWithNames microAlgorithmOption1 = new ClassOptionWithNames("microAlgorithm1", 'a',
            "Stream clustering algorithm for micro-clustering.", Clusterer.class, "clustream.Clustream",
            new String[] {"Clustream", "DenStream"});

    private ClassOption macroAlgorithmOption1 = new ClassOption("macroAlgorithm1", 'A',
        	"Subspace clustering algorithm for macro-clustering.", MacroSubspaceClusterer.class, "CLIQUE");
    
    private ClassOption oneStopAlgorithmOption1 = new ClassOption("oneStopAlgorithm1", 'o',
            "Premade one-stop algorithm which can care both micro- and macro- clustering.", SubspaceClusterer.class, "predeconstream.PreDeConStream");
    
    private ClassOptionWithNames microAlgorithmOption2 = new ClassOptionWithNames("microAlgorithm2", 'c',
        	"Stream clustering algorithm for micro-clustering.", Clusterer.class, "clustream.Clustream",
        	new String[] {"Clustream", "DenStream", "ClusterGenerator"});
    
    private ClassOption macroAlgorithmOption2 = new ClassOption("macroAlgorithm2", 'C',
    		"Subspace clustering algorithm for macro-clustering.", MacroSubspaceClusterer.class, "P3C");
    
    private ClassOption oneStopAlgorithmOption2 = new ClassOption("oneStopAlgorithm2", 'O',
            "Premade one-stop algorithm which can care both micro- and macro- clustering.", SubspaceClusterer.class, "hddstream.HDDStream");
    
    
    /**
     * Setting selections
     * 
     */
    public static final boolean SETTING_COMBINATION = true;
    public static final boolean SETTING_ONESTOP = false;
    private boolean settingType1;
    private boolean settingType2;
    private JCheckBox checkBox1;
    private JCheckBox checkBox2;
    

    public SubspaceClusteringAlgoPanel() {
        initComponents();
    }

    public void renderAlgoPanel(){
   	    setLayout(new BorderLayout());

	    ArrayList<AbstractClassOption> localArrayList = new ArrayList<AbstractClassOption>();
	    localArrayList.add(this.streamOption);
	    localArrayList.add(this.microAlgorithmOption1);
	    localArrayList.add(this.macroAlgorithmOption1);
	    localArrayList.add(this.oneStopAlgorithmOption1);
	    localArrayList.add(this.microAlgorithmOption2);
	    localArrayList.add(this.macroAlgorithmOption2);
	    localArrayList.add(this.oneStopAlgorithmOption2);

	    /* Main panel setting */
	    JPanel localJPanel = new JPanel();
	    GridBagLayout localGridBagLayout = new GridBagLayout();
	    localJPanel.setLayout(localGridBagLayout);

	    /** GridBagConstraints **/
	    GridBagConstraints labelGBC = new GridBagConstraints();
	    labelGBC.gridy = 0;
	    
	    GridBagConstraints checkBoxGBC = new GridBagConstraints();
	    checkBoxGBC.gridy = 1;
	    
	    GridBagConstraints labelAndCheckBoxGBC = new GridBagConstraints();
	    labelAndCheckBoxGBC.gridx = 0;
	    // Note: You need to specify 'gridy'
	    labelAndCheckBoxGBC.gridheight = 4;	    
	    
	    GridBagConstraints optionLabelGBC = new GridBagConstraints();
	    optionLabelGBC.gridx = 4;
	    optionLabelGBC.anchor = GridBagConstraints.EAST;
	    optionLabelGBC.insets = new Insets(3, 3, 3, 3);

	    GridBagConstraints optionGBC = new GridBagConstraints();
	    optionGBC.gridx = 5;
	    optionGBC.fill = GridBagConstraints.HORIZONTAL;
	    optionGBC.anchor = GridBagConstraints.CENTER;
	    optionGBC.weightx = 1;
	    optionGBC.insets = new Insets(3, 3, 3, 0);
	    
	    GridBagConstraints longHorizontalSeparatorGBC = new GridBagConstraints();
	    longHorizontalSeparatorGBC.gridx = 0;
	    longHorizontalSeparatorGBC.gridwidth = 6;
	    longHorizontalSeparatorGBC.fill = GridBagConstraints.BOTH;
	    
	    GridBagConstraints shortHorizontalSeparatorGBC = new GridBagConstraints();
	    shortHorizontalSeparatorGBC.gridx = 2;
	    shortHorizontalSeparatorGBC.gridwidth = 4;
	    shortHorizontalSeparatorGBC.fill = GridBagConstraints.HORIZONTAL;
	    
	    GridBagConstraints longVerticalSeparatorGBC = new GridBagConstraints();
	    // Note: You need to specify 'gridy'
	    longVerticalSeparatorGBC.gridheight = 4;
	    longVerticalSeparatorGBC.fill = GridBagConstraints.BOTH;
	    
	    GridBagConstraints shortVerticalSeparatorGBC = new GridBagConstraints();
	    // Note: You need to specify 'gridy'
	    shortVerticalSeparatorGBC.gridheight = 2;
	    shortVerticalSeparatorGBC.fill = GridBagConstraints.BOTH;
	    
	    GridBagConstraints extraShortVerticalSeparatorGBC = new GridBagConstraints();
	    // Note: You need to specify 'gridy'
	    extraShortVerticalSeparatorGBC.gridheight = 1;
	    extraShortVerticalSeparatorGBC.fill = GridBagConstraints.BOTH;
	    
	    GridBagConstraints tallRadioGBC = new GridBagConstraints();
	    // Note: You need to specify 'gridy'
	    tallRadioGBC.gridheight = 2;
	    tallRadioGBC.fill = GridBagConstraints.BOTH;
	    
	    GridBagConstraints shortRadioGBC = new GridBagConstraints();
	    // Note: You need to specify 'gridy'
	    shortRadioGBC.gridheight = 1;
	    shortRadioGBC.fill = GridBagConstraints.BOTH;
	    
	    /** Size controls **/
	    Dimension optionSize = new Dimension(100, 23);
	    

	    /** Components **/
	    
	    // "Stream"
	    JLabel labelStream = new JLabel("Stream");
	    labelStream.setToolTipText("Data stream to be clustered.");
	    localJPanel.add(labelStream, optionLabelGBC);
	    JComponent optionStream = this.streamOption.getEditComponent();
	    optionStream.setPreferredSize(optionSize);
	    labelStream.setLabelFor(optionStream);
	    this.editComponents.add((OptionEditComponent)optionStream);
	    localJPanel.add(optionStream, optionGBC);
	    
	    localJPanel.add(new JSeparator(), longHorizontalSeparatorGBC);
	    //-------------------------------------------------------------------------------------------//
	    
	    // "Setting 1"
	    JPanel labelAndCheckBox1 = new JPanel();
	    GridBagLayout labelAndCheckBox1GridBagLayout = new GridBagLayout();
	    labelAndCheckBox1.setLayout(labelAndCheckBox1GridBagLayout);
	    
		    JLabel labelAlgo1 = new JLabel("Setting 1");
		    labelAlgo1.setForeground(Color.RED);
		    labelAlgo1.setToolTipText("Algorithm setting 1. Corresponds to the 'left' part of the [Visualization] panel.");
		    labelAndCheckBox1.add(labelAlgo1, labelGBC);
		    	    
		    checkBox1 = new JCheckBox();
		    checkBox1.setSelected(true);
		    checkBox1.addActionListener(new ActionListener() {
		    	public void actionPerformed(ActionEvent evt) {
		    		checkBox1ActionPerformed(evt);
		    	}
		    });
		    labelAndCheckBox1.add(checkBox1, checkBoxGBC);
		    
		labelAndCheckBoxGBC.gridy = 2;
		localJPanel.add(labelAndCheckBox1, labelAndCheckBoxGBC);
	    
	    longVerticalSeparatorGBC.gridy = 2;
	    localJPanel.add(new JSeparator(JSeparator.VERTICAL), longVerticalSeparatorGBC);
	    
	    // Radio button for Combi 1
	    JRadioButton radio1Combi = new JRadioButton();
	    radio1Combi.setSelected(true);
	    radio1Combi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                radio1CombiActionPerformed(evt);
            }
        });
	    tallRadioGBC.gridy = 2;
	    localJPanel.add(radio1Combi, tallRadioGBC);
	    
	    shortVerticalSeparatorGBC.gridy = 2;
	    localJPanel.add(new JSeparator(JSeparator.VERTICAL), shortVerticalSeparatorGBC);
	    
	    
	    // "Micro"
	    JLabel labelMicro1 = new JLabel("Micro");
	    labelMicro1.setToolTipText("Stream clustering algorithm to produce microclusterings.");
	    localJPanel.add(labelMicro1, optionLabelGBC);
	    JComponent optionMicro1 = this.microAlgorithmOption1.getEditComponent();
	    optionMicro1.setPreferredSize(optionSize);
	    labelMicro1.setLabelFor(optionMicro1);
	    this.editComponents.add((OptionEditComponent) optionMicro1);
	    localJPanel.add(optionMicro1, optionGBC);

	    // "Macro"
	    JLabel labelMacro1 = new JLabel("Macro");
	    labelMacro1.setToolTipText("Subspace clustering algorithm to produce macroclusterings.");
	    localJPanel.add(labelMacro1, optionLabelGBC);
	    JComponent optionMacro1 = this.macroAlgorithmOption1.getEditComponent();
	    optionMacro1.setPreferredSize(optionSize);
	    labelMacro1.setLabelFor(optionMacro1);
	    this.editComponents.add((OptionEditComponent)optionMacro1);
	    localJPanel.add(optionMacro1, optionGBC);
	    
	    localJPanel.add(new JSeparator(), shortHorizontalSeparatorGBC);
	    
	    // Radio button for One-stop 1
	    JRadioButton radio1OneStop = new JRadioButton();
	    radio1OneStop.setSelected(false);
	    radio1OneStop.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                radio1OneStopActionPerformed(evt);
            }
        });
	    shortRadioGBC.gridy = 5;
	    localJPanel.add(radio1OneStop, shortRadioGBC);
	    
	    extraShortVerticalSeparatorGBC.gridy = 5;
	    localJPanel.add(new JSeparator(JSeparator.VERTICAL), extraShortVerticalSeparatorGBC);
	    
	    // "One-stop"
	    JLabel labelOneStop1 = new JLabel("One-stop");
	    labelOneStop1.setToolTipText("One-stop algorithm to handle both micro- and macro- clustering processes.");
	    localJPanel.add(labelOneStop1, optionLabelGBC);
	    JComponent optionOneStop1 = this.oneStopAlgorithmOption1.getEditComponent();
	    optionOneStop1.setPreferredSize(optionSize);
	    labelOneStop1.setLabelFor(optionOneStop1);
	    this.editComponents.add((OptionEditComponent) optionOneStop1);
	    localJPanel.add(optionOneStop1, optionGBC);
	    
	    localJPanel.add(new JSeparator(), longHorizontalSeparatorGBC);
	    //-------------------------------------------------------------------------------------------//
	    
	    // "Setting 2"
	    JPanel labelAndCheckBox2 = new JPanel();
	    GridBagLayout labelAndCheckBox2GridBagLayout = new GridBagLayout();
	    labelAndCheckBox2.setLayout(labelAndCheckBox2GridBagLayout);
	    
		    JLabel labelAlgo2 = new JLabel("Setting 2");
		    labelAlgo2.setForeground(Color.BLUE);
		    labelAlgo2.setToolTipText("Algorithm setting 2. Corresponds to the 'right' part of the [Visualization] panel.");
		    labelAndCheckBox2.add(labelAlgo2, labelGBC);
		    	    
		    checkBox2 = new JCheckBox();
		    checkBox2.setSelected(false);
		    checkBox2.addActionListener(new ActionListener() {
		    	public void actionPerformed(ActionEvent evt) {
		    		checkBox2ActionPerformed(evt);
		    	}
		    });
		    labelAndCheckBox2.add(checkBox2, checkBoxGBC);
		    
		labelAndCheckBoxGBC.gridy = 7;
		localJPanel.add(labelAndCheckBox2, labelAndCheckBoxGBC);
	    
	    
	    longVerticalSeparatorGBC.gridy = 7;
	    localJPanel.add(new JSeparator(JSeparator.VERTICAL), longVerticalSeparatorGBC);
	    
	    // Radio button for Combi 2
	    JRadioButton radio2Combi = new JRadioButton();
	    radio2Combi.setSelected(true);
	    radio2Combi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                radio2CombiActionPerformed(evt);
            }
        });
	    tallRadioGBC.gridy = 7;
	    localJPanel.add(radio2Combi, tallRadioGBC);
	    
	    shortVerticalSeparatorGBC.gridy = 7;
	    localJPanel.add(new JSeparator(JSeparator.VERTICAL), shortVerticalSeparatorGBC);
	    
	    // "Micro"
	    JLabel labelMicro2 = new JLabel("Micro");
	    labelMicro2.setToolTipText("Stream clustering algorithm to produce microclusterings.");
	    localJPanel.add(labelMicro2, optionLabelGBC);
	    JComponent optionMicro2 = this.microAlgorithmOption2.getEditComponent();
	    optionMicro2.setPreferredSize(optionSize);
	    labelMicro2.setLabelFor(optionMicro2);
	    this.editComponents.add((OptionEditComponent) optionMicro2);
	    localJPanel.add(optionMicro2, optionGBC);

	    // "Macro"
	    JLabel labelMacro2 = new JLabel("Macro");
	    labelMacro2.setToolTipText("Subspace clustering algorithm to produce macroclusterings.");
	    localJPanel.add(labelMacro2, optionLabelGBC);
	    JComponent optionMacro2 = this.macroAlgorithmOption2.getEditComponent();
	    optionMacro2.setPreferredSize(optionSize);
	    labelMacro1.setLabelFor(optionMacro2);
	    this.editComponents.add((OptionEditComponent) optionMacro2);
	    localJPanel.add(optionMacro2, optionGBC);
	    
	    localJPanel.add(new JSeparator(), shortHorizontalSeparatorGBC);
	    
	    // Radio button for One-stop 2
	    JRadioButton radio2OneStop = new JRadioButton();
	    radio2OneStop.setSelected(false);
	    radio2OneStop.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                radio2OneStopActionPerformed(evt);
            }
        });
	    shortRadioGBC.gridy = 10;
	    localJPanel.add(radio2OneStop, shortRadioGBC);
	    
	    extraShortVerticalSeparatorGBC.gridy = 10;
	    localJPanel.add(new JSeparator(JSeparator.VERTICAL), extraShortVerticalSeparatorGBC);
	    
	    // "One-stop"
	    JLabel labelOneStop2 = new JLabel("One-stop");
	    labelOneStop2.setToolTipText("One-stop algorithm to handle both micro- and macro- clustering processes.");
	    localJPanel.add(labelOneStop2, optionLabelGBC);
	    JComponent optionOneStop2 = this.oneStopAlgorithmOption2.getEditComponent();
	    optionOneStop2.setPreferredSize(optionSize);
	    labelMacro1.setLabelFor(optionOneStop2);
	    this.editComponents.add((OptionEditComponent)optionOneStop2);
	    localJPanel.add(optionOneStop2, optionGBC);

	    /* Option clear */
	    /*GridBagConstraints localGridBagConstraints3 = new GridBagConstraints();
	    localGridBagConstraints3.gridx = 2;
	    localGridBagConstraints3.gridy = 2;
	    localGridBagConstraints3.fill = 0;
	    localGridBagConstraints3.anchor = 10;
	    localGridBagConstraints3.insets = new Insets(5, 0, 5, 5);

	    JButton localJButton = new JButton("Clear");
	    localJButton.addActionListener(this);
	    localJButton.setActionCommand("clear");
	    localJPanel.add(localJButton, localGridBagConstraints3);*/

	    add(localJPanel);
	    
	    
	    /* Radio button control */
	    ButtonGroup radioGroup1 = new ButtonGroup();
	    radioGroup1.add(radio1Combi);
	    radioGroup1.add(radio1OneStop);
	    ButtonGroup radioGroup2 = new ButtonGroup();
	    radioGroup2.add(radio2Combi);
	    radioGroup2.add(radio2OneStop);
	    
	    /* Initial settings */
	    settingType1 = radio1Combi.isSelected() ? SETTING_COMBINATION : SETTING_ONESTOP;
	    settingType2 = radio2Combi.isSelected() ? SETTING_COMBINATION : SETTING_ONESTOP;
	}

    public void actionPerformed(ActionEvent e) {
        /*if (e.getActionCommand().equals("clear")) {
            microAlgorithmOption1.setValueViaCLIString("None");
            macroAlgorithmOption1.setValueViaCLIString("None");
            editComponents.get(3).setEditState("None");
            editComponents.get(4).setEditState("None");
        }*/
    }    
    
    
    /** 
     * Clusterer-getters
     * 
     **/
    
    public AbstractClusterer getMicroClusterer1() {
        AbstractClusterer c = null;
        applyChanges();
        try {
            c = (AbstractClusterer) ClassOption.cliStringToObject(microAlgorithmOption1.getValueAsCLIString(), Clusterer.class, null);
        } catch (Exception ex) {
            Logger.getLogger(SubspaceClusteringAlgoPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
        return c;
    }
    
    public MacroSubspaceClusterer getMacroClusterer1() {
        MacroSubspaceClusterer c = null;
        applyChanges();
        if (!macroAlgorithmOption1.getValueAsCLIString().equals("None")) {
            try {
                c = (MacroSubspaceClusterer) ClassOption.cliStringToObject(macroAlgorithmOption1.getValueAsCLIString(), MacroSubspaceClusterer.class, null);
            } catch (Exception ex) {
                Logger.getLogger(SubspaceClusteringAlgoPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return c;
    }
    
    public AbstractSubspaceClusterer getOneStopClusterer1() {
    	AbstractSubspaceClusterer c = null;
        applyChanges();
        try {
            c = (AbstractSubspaceClusterer) ClassOption.cliStringToObject(oneStopAlgorithmOption1.getValueAsCLIString(), SubspaceClusterer.class, null);
        } catch (Exception ex) {
            Logger.getLogger(SubspaceClusteringAlgoPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
        return c;
    }
    
    public AbstractClusterer getMicroClusterer2() {
        AbstractClusterer c = null;
        applyChanges();
        try {
            c = (AbstractClusterer) ClassOption.cliStringToObject(microAlgorithmOption2.getValueAsCLIString(), Clusterer.class, null);
        } catch (Exception ex) {
            Logger.getLogger(SubspaceClusteringAlgoPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
        return c;
    }
    
    public MacroSubspaceClusterer getMacroClusterer2() {
        MacroSubspaceClusterer c = null;
        applyChanges();
        if(!macroAlgorithmOption2.getValueAsCLIString().equals("None")){
            try {
                c = (MacroSubspaceClusterer) ClassOption.cliStringToObject(macroAlgorithmOption2.getValueAsCLIString(), MacroSubspaceClusterer.class, null);
            } catch (Exception ex) {
                Logger.getLogger(SubspaceClusteringAlgoPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return c;
    }
    
    public AbstractSubspaceClusterer getOneStopClusterer2() {
    	AbstractSubspaceClusterer c = null;
        applyChanges();
        try {
            c = (AbstractSubspaceClusterer) ClassOption.cliStringToObject(oneStopAlgorithmOption2.getValueAsCLIString(), SubspaceClusterer.class, null);
        } catch (Exception ex) {
            Logger.getLogger(SubspaceClusteringAlgoPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
        return c;
    }
    
    public SubspaceClusteringStream getStream() {
        SubspaceClusteringStream s = null;
        applyChanges();
        try {
            s = (SubspaceClusteringStream) ClassOption.cliStringToObject(streamOption.getValueAsCLIString(), SubspaceClusteringStream.class, null);
        } catch (Exception ex) {
            Logger.getLogger(SubspaceClusteringAlgoPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
        return s;
    }

    public String getStreamValueAsCLIString() {
        applyChanges();
        return streamOption.getValueAsCLIString();
    }

    public String getMicroAlgorithm1ValueAsCLIString() {
        applyChanges();
        return microAlgorithmOption1.getValueAsCLIString();
    }
    
    public String getMacroAlgorithm1ValueAsCLIString() {
        applyChanges();
        return macroAlgorithmOption1.getValueAsCLIString();
    }
    
    public String getMicroAlgorithm2ValueAsCLIString() {
        applyChanges();
        return microAlgorithmOption2.getValueAsCLIString();
    }
    
    public String getMacroAlgorithm2ValueAsCLIString() {
        applyChanges();
        return macroAlgorithmOption2.getValueAsCLIString();
    }
    
    /**
     * Setting selections
     * 
     */
    
    public boolean getSettingType1() {
    	return settingType1;
    }
    
    public boolean getSettingType2() {
    	return settingType2;
    }
    
    public boolean getSettingChecked1() {
    	return checkBox1.isSelected();
    }
    
    public boolean getSettingChecked2() {
    	return checkBox2.isSelected();
    }
    
    /* We need to fetch the right item from editComponents list, index needs to match GUI order */
    public void setStreamValueAsCLIString(String s) {
        streamOption.setValueViaCLIString(s);
        editComponents.get(0).setEditState(streamOption.getValueAsCLIString());
    }

    public void setMicroAlgorithm0ValueAsCLIString(String s) {
        microAlgorithmOption1.setValueViaCLIString(s);
        editComponents.get(1).setEditState(microAlgorithmOption1.getValueAsCLIString());
    }
    
    public void setMacroAlgorithm0ValueAsCLIString(String s) {
        macroAlgorithmOption1.setValueViaCLIString(s);
        editComponents.get(2).setEditState(macroAlgorithmOption1.getValueAsCLIString());
    }

    public void setMicroAlgorithm1ValueAsCLIString(String s) {
        microAlgorithmOption2.setValueViaCLIString(s);
        editComponents.get(3).setEditState(microAlgorithmOption2.getValueAsCLIString());
    }
    
    public void setMacroAlgorithm1ValueAsCLIString(String s) {
        macroAlgorithmOption2.setValueViaCLIString(s);
        editComponents.get(4).setEditState(macroAlgorithmOption2.getValueAsCLIString());
    }

    public void applyChanges() {
        for (OptionEditComponent editor : this.editComponents) {
            try {
                    editor.applyState();
            } catch (Exception ex) {
                    GUIUtils.showExceptionDialog(this, "Problem with option "
                                    + editor.getEditedOption().getName(), ex);
            }
        }
    }

    public void setPanelTitle(String title) {
        setBorder(javax.swing.BorderFactory.createTitledBorder(null, title, javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
        													   javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 1, 11)));
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Cluster Algorithm Setup", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 1, 11))); // NOI18N
        setLayout(new java.awt.GridBagLayout());
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
    
    
    /**
     * Button controls
     * 
     */
    
    private void radio1CombiActionPerformed(ActionEvent evt) {
    	settingType1 = SETTING_COMBINATION;
    }
    
    private void radio1OneStopActionPerformed(ActionEvent evt) {
    	settingType1 = SETTING_ONESTOP;
    }
    
    private void radio2CombiActionPerformed(ActionEvent evt) {
    	settingType2 = SETTING_COMBINATION;
    }
    
    private void radio2OneStopActionPerformed(ActionEvent evt) {
    	settingType2 = SETTING_ONESTOP;
    }
    
    private void checkBox1ActionPerformed(ActionEvent evt) {
    	if (!checkBox1.isSelected() && !checkBox2.isSelected()) {
    		checkBox1.setSelected(true);
    	}
    }
    
    private void checkBox2ActionPerformed(ActionEvent evt) {
    	if (!checkBox2.isSelected() && !checkBox1.isSelected()) {
    		checkBox2.setSelected(true);
    	}
    }
}
