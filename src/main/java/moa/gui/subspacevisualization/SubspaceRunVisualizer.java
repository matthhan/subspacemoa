/**
 * [SubspaceRunVisualizer.java] for Subspace MOA
 * 
 * Main class which deals with subspace clustering process & its visualization.
 * 
 * @author Yunsu Kim
 * 		   based on the implementation of Timm Jansen
 * Data Management and Data Exploration Group, RWTH Aachen University
 */

package moa.gui.subspacevisualization;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import moa.cluster.Cluster;
import moa.cluster.Clustering;
import moa.cluster.SubspaceClustering;
import moa.clusterers.AbstractClusterer;
import moa.clusterers.AbstractSubspaceClusterer;
import moa.clusterers.macrosubspace.MacroSubspaceClusterer;
import moa.core.SubspaceInstance;
import moa.evaluation.SubspaceMeasureCollection;
import moa.gui.TextViewerPanel;
import moa.gui.subspaceclusteringtab.SubspaceClusteringAlgoPanel;
import moa.gui.subspaceclusteringtab.SubspaceClusteringSetupTab;
import moa.gui.subspaceclusteringtab.SubspaceClusteringVisualEvalPanel;
import moa.gui.subspaceclusteringtab.SubspaceClusteringVisualTab;
import moa.gui.visualization.WekaExplorer;
import moa.streams.clustering.ClusterEvent;
import moa.streams.clustering.ClusterEventListener;
import moa.streams.clustering.RandomRBFSubspaceGeneratorEvents;
import moa.streams.clustering.SubspaceClusteringStream;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

public class SubspaceRunVisualizer implements Runnable, ActionListener, ClusterEventListener {

	private boolean debug = false; 
	
    /** the pause interval, being read from the gui at startup */
    public static final int initialPauseInterval = 5000;
    
    /** factor to control the speed */
    private int m_wait_frequency = 1000;
    
    /** after how many instances do we repaint the streampanel?
     *  the GUI becomes very slow with small values 
     * */
    private int m_redrawInterval = 100;

    
    /* flags to control the run behavior */
    private static boolean work;
    private boolean stop = false;
    
    /* total amount of processed instances */
    private static int timestamp;
    private static int lastPauseTimestamp;
            
    /* amount of instances to process in one step*/
    private int subEvaluationFrequency = -1;
    private int evaluationFrequency = -1;
    
    /* the stream that delivers the instances */
    private final SubspaceClusteringStream m_stream;
    
    /* amount of relevant instances; older instances will be dropped;
       creates the 'sliding window' over the stream; 
       is strongly connected to the decay rate and decay threshold*/
    private int m_stream0_decayHorizon;

    /* the decay threshold defines the minimum weight of an instance to be relevant */
    private double m_stream0_decay_threshold;
    
    /* the decay rate of the stream, often reffered to as lambda;
       is being calculated from the horizion and the threshold 
       as these are more intuitive to define */
    private double m_stream0_decay_rate;
    
    
    /** Settings **/
    private boolean m_settingChecked1;
    private boolean m_settingChecked2;
    
    /* the clusterer */    
    private AbstractClusterer m_microClusterer1;
    private MacroSubspaceClusterer m_macroClusterer1;
    private AbstractSubspaceClusterer m_onestopClusterer1;
    private boolean m_settingType1;
    
    private AbstractClusterer m_microClusterer2;
    private MacroSubspaceClusterer m_macroClusterer2;
    private AbstractSubspaceClusterer m_onestopClusterer2;
    private boolean m_settingType2;

    /* the measure collections contain all the measures */
    private SubspaceMeasureCollection[] m_measures1 = null;
    private SubspaceMeasureCollection[] m_measures2 = null;

    /* left and right stream panel that datapoints and clusterings will be drawn to */
    private SubspaceStreamPanel m_streampanel1;
    private SubspaceStreamPanel m_streampanel2;

    /* panel that shows the evaluation results */
    private SubspaceClusteringVisualEvalPanel m_evalPanel;
    
    /* panel to hold the graph */
    private SubspaceGraphCanvas m_graphCanvas;
    
    /* reference to the visual panel */
    private SubspaceClusteringVisualTab m_visualPanel;

    /* all possible clusterings */
    //not pretty to have all the clusterings, but otherwise we can't just redraw clusterings
    private SubspaceClustering gtSubspaceClustering1 = null;
    private SubspaceClustering gtSubspaceClustering2 = null;
    private Clustering microResult1 = null;
    private SubspaceClustering macroResult1 = null;
    private Clustering microResult2 = null;
    private SubspaceClustering macroResult2 = null;
    
    /** points **/
    private ArrayList<SubspaceDataPoint> pointsInLastWindow;

    /* holds all the events that have happend, if the stream supports events */
    private ArrayList<ClusterEvent> clusterEvents;
    
    /* reference to the log panel */
    private final TextViewerPanel m_logPanel;
    
    public SubspaceRunVisualizer(SubspaceClusteringVisualTab subspaceVisualPanel, SubspaceClusteringSetupTab subspaceClusteringSetupTab) {
        m_visualPanel = subspaceVisualPanel;
        m_streampanel1 = subspaceVisualPanel.getLeftStreamPanel();
        m_streampanel2 = subspaceVisualPanel.getRightStreamPanel();
        m_graphCanvas = subspaceVisualPanel.getGraphCanvas();
        m_evalPanel = subspaceVisualPanel.getEvalPanel();
        m_logPanel = subspaceClusteringSetupTab.getLogPanel();

        m_stream = subspaceClusteringSetupTab.getStream();
        m_stream0_decayHorizon = m_stream.getDecayHorizon();
        m_stream0_decay_threshold = m_stream.getDecayThreshold();
        m_stream0_decay_rate = (Math.log(1.0/m_stream0_decay_threshold)/Math.log(2)/m_stream0_decayHorizon);
        
        timestamp = 0;
        lastPauseTimestamp = 0;
        work = true;


        if (m_stream instanceof RandomRBFSubspaceGeneratorEvents) {
            ((RandomRBFSubspaceGeneratorEvents)m_stream).addClusterChangeListener(this);
            clusterEvents = new ArrayList<ClusterEvent>();
            m_graphCanvas.setClusterEventsList(clusterEvents);
        }
        
        m_stream.prepareForUse();
        
        m_settingChecked1 = subspaceClusteringSetupTab.getSettingChecked1();
        m_settingChecked2 = subspaceClusteringSetupTab.getSettingChecked2();
        
        if (m_settingChecked1) {
        	m_settingType1 = subspaceClusteringSetupTab.getSettingType1();
        	if (m_settingType1 == SubspaceClusteringAlgoPanel.SETTING_COMBINATION) {
		        m_microClusterer1 = subspaceClusteringSetupTab.getMicroClusterer1();
		        m_microClusterer1.prepareForUse();
		        m_macroClusterer1 = subspaceClusteringSetupTab.getMacroClusterer1();
		        m_macroClusterer1.prepareForUse();
        	} else if (m_settingType1 == SubspaceClusteringAlgoPanel.SETTING_ONESTOP) {
		        m_onestopClusterer1 = subspaceClusteringSetupTab.getOneStopClusterer1();
		        m_onestopClusterer1.prepareForUse();
        	}
        }
        
        if (m_settingChecked2) {
	        m_settingType2 = subspaceClusteringSetupTab.getSettingType2();
        	if (m_settingType2 == SubspaceClusteringAlgoPanel.SETTING_COMBINATION) {
	        	m_microClusterer2 = subspaceClusteringSetupTab.getMicroClusterer2();
		        m_microClusterer2.prepareForUse();
		        m_macroClusterer2 = subspaceClusteringSetupTab.getMacroClusterer2();
		        m_macroClusterer2.prepareForUse();
        	} else if (m_settingType2 == SubspaceClusteringAlgoPanel.SETTING_ONESTOP) {
		        m_onestopClusterer2 = subspaceClusteringSetupTab.getOneStopClusterer2();
		        m_onestopClusterer2.prepareForUse();
        	}
        }
        
        m_measures1 = subspaceClusteringSetupTab.getMeasures();
        m_measures2 = subspaceClusteringSetupTab.getMeasures();


        /* TODO this option needs to move from the stream panel to the setup panel */
        subEvaluationFrequency = m_stream.getSubEvaluationFrequency();
        evaluationFrequency = m_stream.getEvaluationFrequency();
        if (subEvaluationFrequency > evaluationFrequency) {
        	System.out.println("[ERROR] SubspaceRunVisualizer terminated.\n" +
        					   "=> subEvaluationFrequency > evaluationFrequency");
        	return;
        }
        if (subEvaluationFrequency <= 0) {
        	subEvaluationFrequency = evaluationFrequency;
        }

        // Visual panel settings (from the stream)
        int dims = m_stream.numAttsOption.getValue();
        subspaceVisualPanel.setDimensionComboBoxes(dims);

        m_evalPanel.setMeasures(m_measures1, m_measures2, this);
        m_graphCanvas.setGraph(m_measures1[0], m_measures2[0], 0, evaluationFrequency);
    }


    public void run() {
    	m_logPanel.addText("\n");
    	
    	// Experiment settings
    	m_logPanel.addText("Stream: " + m_stream.getClass().getSimpleName());
    	if (m_settingChecked1) {
    		if (m_settingType1 == SubspaceClusteringAlgoPanel.SETTING_COMBINATION) {
    			m_logPanel.addText("Setting 1: " + m_microClusterer1.getClass().getSimpleName() + " + " + m_macroClusterer1.getClass().getSimpleName());
    		} else if (m_settingType1 == SubspaceClusteringAlgoPanel.SETTING_ONESTOP) {
    			m_logPanel.addText("Setting 1: " + m_onestopClusterer1.getClass().getSimpleName());
    		}
    	}
    	if (m_settingChecked2) {
    		if (m_settingType2 == SubspaceClusteringAlgoPanel.SETTING_COMBINATION) {
    			m_logPanel.addText("Setting 2: " + m_microClusterer2.getClass().getSimpleName() + " + " + m_macroClusterer2.getClass().getSimpleName());
    		} else if (m_settingType2 == SubspaceClusteringAlgoPanel.SETTING_ONESTOP) {
    			m_logPanel.addText("Setting 2: " + m_onestopClusterer2.getClass().getSimpleName());
    		}
    	}
    	m_logPanel.addText("\n");
    	
    	// Draw measure value table
    	StringBuilder sbCombs = new StringBuilder();
    	if (m_settingChecked1) {
    		sbCombs.append("(1)");
    		for (int j = 0; j < m_measures1.length; j++) sbCombs.append("\t");
    		if (m_settingChecked2) sbCombs.append("\t" + "(2)");
    	} else if (m_settingChecked2) {
    		sbCombs.append("(2)");
    		for (int j = 0; j < m_measures2.length; j++) sbCombs.append("\t");
    	}
    	m_logPanel.addText(sbCombs.toString());
    	
    	StringBuilder sbMeasures = new StringBuilder();
    	if (m_settingChecked1) {
	    	for (int j = 0; j < m_measures1.length; j++) {
	    		for (int k = 0; k < m_measures1[j].getNumMeasures(); k++) {
	    			if (m_measures1[j].isEnabled(k)) {
			    		String measureName = m_measures1[j].getName(k);
			    		if (measureName.length() >= 8) measureName = measureName.substring(0, 7);
			    		sbMeasures.append(measureName);
			    		if (k < m_measures1[j].getNumMeasures() - 1) sbMeasures.append("\t");
	    			}
	    		}

		    	if (j < m_measures1.length - 1) sbMeasures.append("\t");
	    	}
			sbMeasures.append("\t");
    	}
    	if (m_settingChecked2) {
    		for (int j = 0; j < m_measures2.length; j++) {
    			for (int k = 0; k < m_measures2[j].getNumMeasures(); k++) {
	    			if (m_measures2[j].isEnabled(k)) {
	    				String measureName = m_measures2[j].getName(k);
			    		if (measureName.length() >= 8) measureName = measureName.substring(0, 7);
			    		sbMeasures.append(measureName);
			    		if (k < m_measures2[j].getNumMeasures() - 1) sbMeasures.append("\t");
					}
				}
	    		
	    		if (j < m_measures2.length - 1) sbMeasures.append("\t");
	    	}
			sbMeasures.append("\t");
    	}
    	m_logPanel.addText(sbMeasures.toString() + "\n");
    	
    	// Start streaming
    	runVisual();
    }

    public void runVisual() {
    	int processCounter = 0;
        int speedCounter = 0;
        LinkedList<SubspaceDataPoint> pointBuffer1 = new LinkedList<SubspaceDataPoint>();
        LinkedList<SubspaceDataPoint> pointBuffer2 = new LinkedList<SubspaceDataPoint>();
        ArrayList<SubspaceDataPoint> pointarray1 = null;
        ArrayList<SubspaceDataPoint> pointarray2 = null;

        while (work || processCounter != 0) {
            if (m_stream.hasMoreInstances()) {
                timestamp++;
                speedCounter++;
                processCounter++;
                if (timestamp % 100 == 0) {
                    m_visualPanel.setProcessedPointsCounter(timestamp);
                }
              
                SubspaceInstance nextInstance = m_stream.nextInstance();
                
                // DEBUG: print each instance
                if (debug) {
                /*System.out.println("Instance " + processCounter + ": "
                		 			+ next0.value(0) + " " + next0.value(1) + " " + next0.value(2) + " " + next0.value(3) + " " + next0.value(4) + " "
                		 			+ "[" + next0.getClassLabel(0) + " " + next0.getClassLabel(1) + " " + next0.getClassLabel(2) + " " + next0.getClassLabel(3) + " "
                		 			+ next0.getClassLabel(4) + "]");*/
                }
                
                // Generate a data point & Remove a decayed point
                SubspaceDataPoint point1 = new SubspaceDataPoint(nextInstance, timestamp);
                SubspaceDataPoint point2 = new SubspaceDataPoint(nextInstance, timestamp);
                
                if (m_settingChecked1) {
	                pointBuffer1.add(point1);
	                while (pointBuffer1.size() > m_stream0_decayHorizon) {
	                    pointBuffer1.removeFirst();
	                }
                }

                if (m_settingChecked2) {
	                pointBuffer2.add(point2);
	                while (pointBuffer2.size() > m_stream0_decayHorizon) {
	                    pointBuffer2.removeFirst();
	                }
                }
                
                
                // Draw the points
                if (m_visualPanel.isEnabledDrawPoints()) {
                	if (m_settingChecked1) m_streampanel1.drawPoint(point1, m_stream0_decay_rate, m_stream0_decay_threshold);
                	if (m_settingChecked2) m_streampanel2.drawPoint(point2, m_stream0_decay_rate, m_stream0_decay_threshold);
                    if (processCounter % m_redrawInterval == 0) {
                    	if (m_settingChecked1) m_streampanel1.applyDrawDecay(m_stream0_decayHorizon / (float)(m_redrawInterval));
                    	if (m_settingChecked2) m_streampanel2.applyDrawDecay(m_stream0_decayHorizon / (float)(m_redrawInterval));
                    }
                }

                                
                // Train micro-clusterers
                if (m_settingChecked1) {
                	SubspaceInstance trainInst1 = new SubspaceInstance(point1);
	                if (m_settingType1 == SubspaceClusteringAlgoPanel.SETTING_COMBINATION) {
	                	if (m_microClusterer1.keepClassLabel()) {
	                		trainInst1.setDataset(point1.dataset());
	                	} else {
	                		trainInst1.deleteAttributeAt(point1.classIndex());
	                	}
	                	m_microClusterer1.trainOnInstanceImpl(trainInst1);
	                } else if (m_settingType1 == SubspaceClusteringAlgoPanel.SETTING_ONESTOP) {
	                	if (m_onestopClusterer1.keepClassLabel()) {
	                		trainInst1.setDataset(point1.dataset());
	                	} else {
	                		trainInst1.deleteAttributeAt(point1.classIndex());
	                	}
	                	m_onestopClusterer1.trainOnInstanceImpl(trainInst1);
	                }
                }
                
                if (m_settingChecked2) {
                	SubspaceInstance trainInst2 = new SubspaceInstance(point2);
	                if (m_settingType2 == SubspaceClusteringAlgoPanel.SETTING_COMBINATION) {
	                	if (m_microClusterer2.keepClassLabel()) {
	                		trainInst2.setDataset(point2.dataset());
	                	} else {
	                		trainInst2.deleteAttributeAt(point2.classIndex());
	                	}
	                	m_microClusterer2.trainOnInstanceImpl(nextInstance);
	                } else if (m_settingType2 == SubspaceClusteringAlgoPanel.SETTING_ONESTOP) {
	                	if (m_onestopClusterer2.keepClassLabel()) {
	                		trainInst2.setDataset(point2.dataset());
	                	} else {
	                		trainInst2.deleteAttributeAt(point2.classIndex());
	                	}
	                	m_onestopClusterer2.trainOnInstanceImpl(nextInstance);
	                }
                }
                
                // Evaluation point

                if (processCounter >= subEvaluationFrequency) {
                    processCounter = 0;
                    
                    // Update weights
                    if (m_settingChecked1) {
	                    for (SubspaceDataPoint p : pointBuffer1) {
	                        p.updateWeight(timestamp, m_stream0_decay_rate);
	                    }
	                    pointarray1 = new ArrayList<SubspaceDataPoint>(pointBuffer1);
	                    pointsInLastWindow = pointarray1;	// Store the last set of points
	                }
                    
                    if (m_settingChecked2) {
	                    for (SubspaceDataPoint p : pointBuffer2) {
	                        p.updateWeight(timestamp, m_stream0_decay_rate);
	                    }
	            		pointarray2 = new ArrayList<SubspaceDataPoint>(pointBuffer2);
	            		pointsInLastWindow = pointarray2;	// Store the last set of points
                    }
            		
            		
                    
                    // Macro-clustering
                    processSubspaceClusterings(pointarray1, pointarray2);
                    
                    // Pause
                    int pauseInterval = m_visualPanel.getPauseInterval();
                    if (pauseInterval != 0 && lastPauseTimestamp + pauseInterval <= timestamp) {
                        m_visualPanel.toggleVisualizer(true);
                        printMeans();
                    }
                }
            } else {
            	m_visualPanel.stopVisualizer();
            	printMeans();
            	
                System.out.println("DONE");
                return;
            }
            
            if (speedCounter > m_wait_frequency * 30 && m_wait_frequency < 15) {
                try {
                    synchronized (this) {
                        if (m_wait_frequency == 0)
                            wait(50);
                        else
                            wait(1);
                    }
                } catch (InterruptedException ex) {
                    
                }
                speedCounter = 0;
            }
        }
        
        // "Pause"
        if (!stop) {
        	if (m_settingChecked1) m_streampanel1.drawPointPanels(pointarray1, m_stream0_decay_rate, m_stream0_decay_threshold);
        	if (m_settingChecked2) m_streampanel2.drawPointPanels(pointarray2, m_stream0_decay_rate, m_stream0_decay_threshold);
            work_pause();
        }
    }

    private void processSubspaceClusterings(ArrayList<SubspaceDataPoint> points1, ArrayList<SubspaceDataPoint> points2) {
    	if (debug) {
    	   	System.out.print("points1 = " + points1.size() + " '" + points1.get(0).getClass().getSimpleName() + "'s / ");
    	   	System.out.println("points2 = " + points2.size() + " '" + points2.get(0).getClass().getSimpleName() + "'s");
    	   	System.out.print("m_microClusterer1 = " + m_microClusterer1.getClass().getSimpleName() + " / ");
    	   	System.out.println("m_macroClusterer1 = " + m_macroClusterer1.getClass().getSimpleName() + " / ");
    	   	System.out.print("m_microClusterer2 = " + m_microClusterer2.getClass().getSimpleName() + " / ");
    	   	System.out.println("m_macroClusterer2 = " + m_macroClusterer2.getClass().getSimpleName());
    	}
        
    	// Ground truth clustering
    	if (m_settingChecked1) gtSubspaceClustering1 = new SubspaceClustering(points1);
    	if (m_settingChecked2) gtSubspaceClustering2 = new SubspaceClustering(points2);

        SubspaceClustering evalSubspaceClustering1 = null;
        SubspaceClustering evalSubspaceClustering2 = null;

        // TODO Special Case for SubspaceClusterGenerator

        
        // Get clusterings
        if (m_settingChecked1) {
	        if (m_settingType1 == SubspaceClusteringAlgoPanel.SETTING_COMBINATION) {
	        	if (m_microClusterer1.implementsMicroClusterer()) {
	        		microResult1 = m_microClusterer1.getMicroClusteringResult();
	            } else {
	            	System.out.println("ERROR: Micro 1 algorithm should provide micro-clustering");
	            	stop(); return;
	            }
	        	macroResult1 = m_macroClusterer1.getClusteringResult(microResult1);
	        	
	        } else if (m_settingType1 == SubspaceClusteringAlgoPanel.SETTING_ONESTOP) {
	        	microResult1 = m_onestopClusterer1.getMicroClusteringResult();
	        	macroResult1 = m_onestopClusterer1.getClusteringResult();
	        }
    	}
        
        if (m_settingChecked2) {
	        if (m_settingType2 == SubspaceClusteringAlgoPanel.SETTING_COMBINATION) {
		        if (m_microClusterer2.implementsMicroClusterer()) {
		    		microResult2 = m_microClusterer2.getMicroClusteringResult();
		        } else {
		        	System.out.println("ERROR: Micro 2 algorithm should provide micro-clustering");
		        	stop(); return;
		        }
		        macroResult2 = m_macroClusterer2.getClusteringResult(microResult2);
		        
	        } else if (m_settingType2 == SubspaceClusteringAlgoPanel.SETTING_ONESTOP) {
	        	microResult2 = m_onestopClusterer2.getMicroClusteringResult();
	        	macroResult2 = m_onestopClusterer2.getClusteringResult();
	        }
        }
        
        // Set evaluation targets
        evalSubspaceClustering1 = macroResult1;		// Only subspace macro clusterings now!
        evalSubspaceClustering2 = macroResult2;
        
        // Evaluate
        evaluateSubspaceClusterings(evalSubspaceClustering1, gtSubspaceClustering1, points1,
        							evalSubspaceClustering2, gtSubspaceClustering2, points2);
        
        if (debug) System.out.println("Evaluation done.");
        
        // Draw the clusterings
        drawClusterings(points1, points2);
    }

    private void evaluateSubspaceClusterings(SubspaceClustering foundClustering0, SubspaceClustering trueClustering0, ArrayList<SubspaceDataPoint> points0,
    										 SubspaceClustering foundClustering1, SubspaceClustering trueClustering1, ArrayList<SubspaceDataPoint> points1) {
    	
    	
    	StringBuilder sb = new StringBuilder();
    	
    	if (m_settingChecked1) {
	        for (int i = 0; i < m_measures1.length; i++) {
	        	if (foundClustering0 != null) {
	        		try {
	                    double msec = m_measures1[i].subEvaluateClusteringPerformance(foundClustering0, trueClustering0, points0);
	                    //sb.append(m_measures0[i].getClass().getSimpleName() + " took " + msec + "ms (Mean:" + m_measures0[i].getMeanRunningTime() + ")");
	                } catch (Exception ex) { ex.printStackTrace(); }
	            } else {
	            	if (debug) System.out.println("ERROR: found clustering 0 is null");
	                for (int j = 0; j < m_measures1[i].getNumMeasures(); j++) {
	                    m_measures1[i].addEmptySubValue(j);
	                }
	            }
	        }
    	}
        
    	if (m_settingChecked2) {
	        for (int i = 0; i < m_measures2.length; i++) {
	        	if (foundClustering1 != null) {
	                try {
	                    double msec = m_measures2[i].subEvaluateClusteringPerformance(foundClustering1, trueClustering1, points1);
	                    //sb.append(m_measures1[i].getClass().getSimpleName() + " took " + msec + "ms (Mean:" + m_measures1[i].getMeanRunningTime() + ")");
	                } catch (Exception ex) { ex.printStackTrace(); }
	            } else {
	            	if (debug) System.out.println("ERROR: found clustering 1 is null");
	                for (int j = 0; j < m_measures2[i].getNumMeasures(); j++) {
	                    m_measures2[i].addEmptySubValue(j);
	                }
	            }
	        }
    	}
    	
        
        if (timestamp % evaluationFrequency == 0) {
        	if (m_settingChecked1) {
    	        for (int i = 0; i < m_measures1.length; i++) {
    	        	m_measures1[i].averageSubEvaluations();
    	        	
    	        	double rounding = 100000.0;
                    int measureIdx = 0;
                    for (int j = 0; j < m_measures1[i].getNumMeasures(); j++) {
                    	if (m_measures1[i].isEnabled(j)) {
                    		measureIdx = j;
                    		sb.append(Math.round(m_measures1[i].getLastValue(measureIdx) * rounding) / rounding);
                    		if (j < m_measures1[i].getNumMeasures() - 1) sb.append("\t");
                    	}
                    }
                    
                    if (i < m_measures1.length - 1) sb.append("\t");
    	        }
    	        sb.append("\t");
        	}
            
        	if (m_settingChecked2) {
    	        for (int i = 0; i < m_measures2.length; i++) {
    	        	m_measures2[i].averageSubEvaluations();
    	        	
                    double rounding = 100000.0;
                    int measureIdx = 0;
                    for (int j = 0; j < m_measures2[i].getNumMeasures(); j++) {
                    	if (m_measures2[i].isEnabled(j)) {
                    		measureIdx = j;
                    		sb.append(Math.round(m_measures2[i].getLastValue(measureIdx) * rounding) / rounding);
                    		if (j < m_measures2[i].getNumMeasures() - 1) sb.append("\t");
                    	}
                    }
                    
                    if (i < m_measures2.length - 1) sb.append("\t");
    	        }
        	}
        	
            m_logPanel.addText(sb.toString());

            m_evalPanel.update();
            m_graphCanvas.updateCanvas();
    	}
    }

    public void drawClusterings(ArrayList<SubspaceDataPoint> points1, ArrayList<SubspaceDataPoint> points2) {
    	if (m_settingChecked1) {
	    	if (gtSubspaceClustering1 != null && gtSubspaceClustering1.size() > 0)
	            m_streampanel1.drawGTClustering(gtSubspaceClustering1, new Color(34, 139, 34));
	    	if (macroResult1 != null && macroResult1.size() > 0)
	        	m_streampanel1.drawMacroClustering(macroResult1, points1, Color.RED);
	        /*if (microResult0!= null && microResult0.size() > 0)
	            m_streampanel0.drawMicroClustering(microResult1, Color.GREEN);*/
    	}

    	if (m_settingChecked2) {
	        if (gtSubspaceClustering2 != null && gtSubspaceClustering2.size() > 0)
	            m_streampanel2.drawGTClustering(gtSubspaceClustering2, new Color(34, 139, 34));
	        if (macroResult2 != null && macroResult2.size() > 0)
	            m_streampanel2.drawMacroClustering(macroResult2, points2, Color.BLUE);
	        /*if (microResult1!= null && microResult1.size() > 0)
	            m_streampanel1.drawMicroClustering(microResult2, Color.GREEN);*/
    	}
    }

    public void redraw() {
    	if (m_settingChecked1) m_streampanel1.repaint();
    	if (m_settingChecked2) m_streampanel2.repaint();
    }


    public static int getCurrentTimestamp() {
        return timestamp;
    }

    private void work_pause() {
        while (!work && !stop) {
            try {
                synchronized (this) {
                    wait(1000);
                }
            } catch (InterruptedException ex) { }
        }
        runVisual();
    }

    public static void pause() {
        work = false;
        lastPauseTimestamp = timestamp;
    }

    public static void resume() {
        work = true;
    }

    public void stop() {
        work = false;
        stop = true;
        // TODO Print the total result
        // TODO Why does it continue again?
    }

    public void setSpeed(int speed) {
        m_wait_frequency = speed;
    }

    public void actionPerformed(ActionEvent e) {
        // React on graph selection and find out which measure was selected
        int selected = Integer.parseInt(e.getActionCommand());
        int counter = selected;
        int m_select = 0;
        int m_select_offset = 0;
        boolean found = false;
        for (int i = 0; i < m_measures1.length; i++) {
            for (int j = 0; j < m_measures1[i].getNumMeasures(); j++) {
                if(m_measures1[i].isEnabled(j)){
                	counter--;
                    if(counter<0){
                        m_select = i;
                        m_select_offset = j;
                        found = true;
                        break;
                    }
                }
            }
            if(found) break;
        }
        m_graphCanvas.setGraph(m_measures1[m_select], m_measures2[m_select],m_select_offset,subEvaluationFrequency);
    }

    public void setPointLayerVisibility(boolean selected) {
    	if (m_settingChecked1) m_streampanel1.setPointVisibility(selected);
    	if (m_settingChecked2) m_streampanel2.setPointVisibility(selected);
    }
    public void setMicroLayerVisibility(boolean selected) {
    	if (m_settingChecked1) m_streampanel1.setMicroLayerVisibility(selected);
    	if (m_settingChecked2) m_streampanel2.setMicroLayerVisibility(selected);
    }
    public void setMacroVisibility(boolean selected) {
    	if (m_settingChecked1) m_streampanel1.setMacroLayerVisibility(selected);
    	if (m_settingChecked2) m_streampanel2.setMacroLayerVisibility(selected);
    }
    public void setGroundTruthVisibility(boolean selected) {
    	if (m_settingChecked1) m_streampanel1.setGroundTruthLayerVisibility(selected);
    	if (m_settingChecked2) m_streampanel2.setGroundTruthLayerVisibility(selected);
    }

    public void changeCluster(ClusterEvent e) {
        if (clusterEvents != null) clusterEvents.add(e);
        System.out.println(e.getType() + ": " + e.getMessage());
    }



    public void exportCSV(String filepath) {
        PrintWriter out = null;
        try {
            if(!filepath.endsWith(".csv"))
                filepath+=".csv";
            out = new PrintWriter(new BufferedWriter(new FileWriter(filepath)));
            String del = ";";

            Iterator<ClusterEvent> eventIt = null;
            ClusterEvent event = null;
            if(clusterEvents!=null && clusterEvents.size() > 0){
                eventIt = clusterEvents.iterator();
                event = eventIt.next();
            }
                 
            //raw data
            SubspaceMeasureCollection measurecol[][] = new SubspaceMeasureCollection[2][];
            measurecol[0] = m_measures1;
            measurecol[1] = m_measures2;
            int numValues = 0;
            //header
            out.write("Nr"+del);
            out.write("Event"+del);
            for (int m = 0; m < 2; m++) {
                for (int i = 0; i < measurecol[m].length; i++) {
                    for (int j = 0; j < measurecol[m][i].getNumMeasures(); j++) {
                        if(measurecol[m][i].isEnabled(j)){
                            out.write(m+"-"+measurecol[m][i].getName(j)+del);
                            numValues = measurecol[m][i].getNumberOfValues(j);
                        }
                    }
                }
            }
            out.write("\n");


            //rows
            for (int v = 0; v < numValues; v++){
                //Nr
                out.write(v+del);

                //events
                if(event!=null && event.getTimestamp()<=m_stream0_decayHorizon*v){
                    out.write(event.getType()+del);
                    if(eventIt!= null && eventIt.hasNext()){
                        event=eventIt.next();
                    }
                    else
                        event = null;
                }
                else
                    out.write(del);

                //values
                for (int m = 0; m < 2; m++) {
                    for (int i = 0; i < measurecol[m].length; i++) {
                        for (int j = 0; j < measurecol[m][i].getNumMeasures(); j++) {
                            if(measurecol[m][i].isEnabled(j)){
                            		double value = measurecol[m][i].getValue(j, v);
                            		if(Double.isNaN(value))
                            			out.write(del);
                            		else
                            			out.write(value+del);
                            }
                        }
                    }
                }
                out.write("\n");
            }
            out.close();
        } catch (IOException ex) {
            Logger.getLogger(SubspaceRunVisualizer.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            out.close();
        }
    }

    public void weka() {
    	try {
    		Class.forName("weka.gui.Logger");
    	} catch (Exception e) {
    		m_logPanel.addText("Please add weka.jar to the classpath to use the Weka explorer.");
    		return;
    	}    	
    	
        SubspaceClustering wekaClustering = macroResult1;
        // TODO choose clustering

        if (wekaClustering == null || wekaClustering.size() == 0) {
            m_logPanel.addText("Empty Clustering");
            return;
        }

        int dims = wekaClustering.get(0).getCenter().length;
        FastVector attributes = new FastVector();
        for(int i = 0; i < dims; i++)
                attributes.addElement( new Attribute("att" + i) );

        Instances instances = new Instances("trainset",attributes,0);

        for(int c = 0; c < wekaClustering.size(); c++){
            Cluster cluster = wekaClustering.get(c);
            Instance inst = new DenseInstance(cluster.getWeight(), cluster.getCenter());
            inst.setDataset(instances);
            instances.add(inst);
        }

        WekaExplorer explorer = new WekaExplorer(instances);
    }


    /** Helper results **/
    
    // Printing the means
    protected void printMeans() {
    	
        StringBuilder sb = new StringBuilder();
        sb.append("mean values\n");
        double rounding = 100000.0;
        if (m_settingChecked1) {
            for (int i = 0; i < m_measures1.length; i++) {
            	int measureIdx = 0;
                for (int j = 0; j < m_measures1[i].getNumMeasures(); j++) {
                	if (m_measures1[i].isEnabled(j))
                		measureIdx = j;
                }
            	double mean = m_measures1[i].getMean(measureIdx);
            	if (Double.isNaN(mean)) {
            		sb.append("NaN");
            	} else {
            		sb.append(Math.round(mean * rounding) / rounding);
            	}
            	sb.append("\t");
            } sb.append("\t");
        }
        if (m_settingChecked2) {
            for (int i = 0; i < m_measures2.length; i++) {
            	int measureIdx = 0;
                for (int j = 0; j < m_measures2[i].getNumMeasures(); j++) {
                	if (m_measures2[i].isEnabled(j))
                		measureIdx = j;
                }
            	double mean = m_measures2[i].getMean(measureIdx);
            	if (Double.isNaN(mean)) {
            		sb.append("NaN");
            	} else {
            		sb.append(Math.round(mean * rounding) / rounding);
            	}
            	sb.append("\t");
            }
        }
        m_logPanel.addText(sb.toString());
    }
    
    
    
    /** Helper getters **/
    
    public ArrayList<SubspaceDataPoint> getPointsInLastWindow() {
    	return pointsInLastWindow;
    }
    
    public SubspaceClustering getLastFoundClustering0() {
    	return macroResult1;
    }
    
    public SubspaceClustering getLastFoundClustering1() {
    	return macroResult2;
    }
}

