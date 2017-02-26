/**
 * [EvaluateSubspaceClustering.java] for Subspace MOA
 * 
 * Task for evaluating a subspace clustering on a stream.
 * 
 * @author Yunsu Kim
 * Data Management and Data Exploration Group, RWTH Aachen University
 */

package moa.tasks;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import moa.cluster.Clustering;
import moa.cluster.SubspaceClustering;
import moa.clusterers.AbstractClusterer;
import moa.clusterers.AbstractSubspaceClusterer;
import moa.clusterers.Clusterer;
import moa.clusterers.SubspaceClusterer;
import moa.clusterers.macrosubspace.MacroSubspaceClusterer;
import moa.core.ObjectRepository;
import moa.core.SubspaceInstance;
import moa.evaluation.CE;
import moa.evaluation.CMM_S;
import moa.evaluation.EntropySubspace;
import moa.evaluation.F1Subspace;
import moa.evaluation.LearningCurve;
import moa.evaluation.Purity;
import moa.evaluation.RNIA;
import moa.evaluation.RandStatistic;
import moa.evaluation.SubCMM;
import moa.evaluation.SubspaceMeasureCollection;
import moa.gui.subspacevisualization.SubspaceDataPoint;
import moa.gui.subspacevisualization.SubspaceRunVisualizer;
import moa.options.ClassOption;
import moa.options.ClassOptionWithNames;
import moa.options.FileOption;
import moa.options.IntOption;
import moa.options.RequiredOptionNotSpecifiedException;
import moa.streams.clustering.ClusterEvent;
import moa.streams.clustering.ClusterEventListener;
import moa.streams.clustering.RandomRBFSubspaceGeneratorEvents;
import moa.streams.clustering.SubspaceClusteringStream;


public class EvaluateSubspaceClustering extends MainTask implements ClusterEventListener {

    @Override
    public String getPurposeString() {
        return "Evaluates a subspace clustering on a stream.";
    }

    private static final long serialVersionUID = 1L;

    /* Clusterer */
    public ClassOptionWithNames microAlgorithmOption = new ClassOptionWithNames("microAlgorithm", 'a',
            "Stream clustering algorithm for micro-clustering.", Clusterer.class, null, "",
            new String[] {"Clustream", "DenStream"});

    public ClassOption macroAlgorithmOption = new ClassOption("macroAlgorithm", 'A',
        	"Subspace clustering algorithm for macro-clustering.", MacroSubspaceClusterer.class, null, "");
    
    public ClassOption oneStopAlgorithmOption = new ClassOption("oneStopAlgorithm", 'o',
            "Premade one-stop algorithm which can care both micro- and macro- clustering.", SubspaceClusterer.class, null, "");
    
    /* Stream */
    public ClassOption streamOption = new ClassOption("stream", 's',
            "Stream to learn from.", SubspaceClusteringStream.class,
            "RandomRBFSubspaceGeneratorEvents", "");

    /* Misc */
    public IntOption instanceLimitOption = new IntOption("instanceLimit", 'i',
            "Maximum number of instances to test/train on  (-1 = no limit).",
            100000, -1, Integer.MAX_VALUE);

    public FileOption dumpFileOption = new FileOption("dumpFile", 'd',
            "File to append intermediate csv reslts to.", "dumpSubspaceClustering.csv", "csv", true);
    
    // TODO: Measure option
    
    
    /* Local settings */
    private AbstractClusterer microClusterer;
    private MacroSubspaceClusterer macroClusterer;
    private AbstractSubspaceClusterer oneStopClusterer;
    private boolean combinationSet;		// Algorithm setting type
    
	private SubspaceClusteringStream stream;
	private SubspaceMeasureCollection[] measures;
	
	private int totalInstances;
	private String dumpFilename;
    
	private ArrayList<ClusterEvent> clusterEvents;
	

    @Override
    public Class<?> getTaskResultType() {
        return LearningCurve.class;
    }

    @Override
    protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
        
    	/* Initial settings */
    	if (streamOption.getValueAsCLIString() != "") {
    		stream = (SubspaceClusteringStream) getPreparedClassOption(streamOption);    		
    	} else {
    		throw new RuntimeException("EvaluateSubspaceClustering: stream is not specified",
    									new RequiredOptionNotSpecifiedException());
    	}
    	
    	if (microAlgorithmOption.getValueAsCLIString() != "") {
    		microClusterer = (AbstractClusterer) getPreparedClassOption(microAlgorithmOption);
    		if (macroAlgorithmOption.getValueAsCLIString() != "") {
    			macroClusterer = (MacroSubspaceClusterer) getPreparedClassOption(macroAlgorithmOption);
    		} else {
    			throw new RuntimeException("EvaluateSubspaceClustering: macro clusterer is not specified,"
    										+ "while micro clusterer is given", new RequiredOptionNotSpecifiedException());
    		}
    		combinationSet = true;
    	} else {
    		if (oneStopAlgorithmOption.getValueAsCLIString() != "") {
        		oneStopClusterer = (AbstractSubspaceClusterer) getPreparedClassOption(oneStopAlgorithmOption);
        		combinationSet = false;
    		} else {
    			if (macroAlgorithmOption.getValueAsCLIString() != "") {
    				throw new RuntimeException("EvaluateSubspaceClustering: micro clusterer is not specified,"
							+ "while macro clusterer is given", new RequiredOptionNotSpecifiedException());
    			} else {
    				throw new RuntimeException("EvaluateSubspaceClustering: clusterer is not specified",
    											new RequiredOptionNotSpecifiedException());
    			}
    		}
    	}
    	
        totalInstances = instanceLimitOption.getValue();
        if (totalInstances == -1) {
        	System.out.println("EvaluateSubspaceClustering: (WARNING) stream limit is not specified");
        }
        
        dumpFilename = dumpFileOption.getValue();
        measures = getMeasureInstances();
        
        if (stream instanceof RandomRBFSubspaceGeneratorEvents){
			((RandomRBFSubspaceGeneratorEvents) stream).addClusterChangeListener(this);
			clusterEvents = new ArrayList<ClusterEvent>();
		} else {
			clusterEvents = null;
		}
        
        
        /* Prepare for use */
		stream.prepareForUse();
		if (microClusterer != null) {
			microClusterer.prepareForUse();
		}
		if (macroClusterer != null) {
			macroClusterer.prepareForUse();
		}
		if (oneStopClusterer != null) {
			oneStopClusterer.prepareForUse();
		}
		
		
		/** Simulate & Evaluate **/
		run();
		
        /* Result */
        LearningCurve learningCurve = new LearningCurve("EvaluateSubspaceClustering does not support custom output file (> [filename]).\n" +
				"Check out the dump file to see the results (if you haven't specified, dumpSubspaceClustering.csv by default).");

        return learningCurve;
    }
    
    

	@Override
	public void changeCluster(ClusterEvent e) {
		if (clusterEvents != null) clusterEvents.add(e);
	}
    
    protected List<Class> getMeasureClasses() {
    	List<Class> classes = new ArrayList<Class>();
    	classes.add(Purity.class);
        classes.add(EntropySubspace.class);
        classes.add(F1Subspace.class);
        classes.add(RandStatistic.class);
        classes.add(RNIA.class);
        classes.add(CE.class);
        classes.add(CMM_S.class);
        classes.add(SubCMM.class);
    	return classes;
    }
    
    protected SubspaceMeasureCollection[] getMeasureInstances() {
    	List<Class> measureClasses = getMeasureClasses();
        int numMeasureClasses = measureClasses.size();
        SubspaceMeasureCollection[] instances = new SubspaceMeasureCollection[numMeasureClasses];
        for (int i=0; i < numMeasureClasses; i++) {
        	try {
        		instances[i] = (SubspaceMeasureCollection) measureClasses.get(i).newInstance();
        	} catch (Exception e) {
        		Logger.getLogger("Couldn't create Instance for " + measureClasses.get(i).getName());
    			e.printStackTrace();
        	}
        }	
        
        return instances;
    }

	protected void run() {
		
		// Basic stream settings
		int subEvaluationFrequency = stream.getSubEvaluationFrequency();
		int evaluationFrequency = stream.getEvaluationFrequency();
        if (subEvaluationFrequency > evaluationFrequency) {
        	throw new RuntimeException("EvaluateSubspaceClustering: subEvaluationFrequency cannot exceed evaluationFrequency");
        }
        if (subEvaluationFrequency <= 0) {
        	subEvaluationFrequency = evaluationFrequency;
        }
		
		int decayHorizon = stream.getDecayHorizon();
		double decayThreshold = stream.getDecayThreshold();
		double decayRate = (-1 * Math.log(decayThreshold) / decayHorizon);

		// Progress monitors
		int timestamp = 0;
		int evaluationPointCounter = 0;
		LinkedList<SubspaceDataPoint> pointBuffer = new LinkedList<SubspaceDataPoint>();
		
		// Intermediate results
		Clustering microResult;
		SubspaceClustering macroResult;

		while (timestamp < totalInstances && stream.hasMoreInstances()) {
			timestamp++;
			evaluationPointCounter++;
			
			// New instance coming
			SubspaceInstance next = stream.nextInstance();
			SubspaceDataPoint point = new SubspaceDataPoint(next, timestamp);
			pointBuffer.add(point);
            while (pointBuffer.size() > decayHorizon) {
                pointBuffer.removeFirst();
            }
			
			// Train clusterers
            SubspaceInstance trainInst = new SubspaceInstance(point);
			if (combinationSet) {
				if (microClusterer.keepClassLabel()) {
            		trainInst.setDataset(point.dataset());
            	} else {
            		trainInst.deleteAttributeAt(point.classIndex());
            	}
				microClusterer.trainOnInstanceImpl(trainInst);
			} else {
				if (oneStopClusterer.keepClassLabel()) {
            		trainInst.setDataset(point.dataset());
            	} else {
            		trainInst.deleteAttributeAt(point.classIndex());
            	}
				oneStopClusterer.trainOnInstanceImpl(trainInst);
			}

			
			// Evaluation point!
			if (evaluationPointCounter >= subEvaluationFrequency) {
				
				// Update weights
				for (SubspaceDataPoint p : pointBuffer) {
					p.updateWeight(timestamp, decayRate);
				}
				
				// Prepare an array of points
				List<SubspaceDataPoint> pointArray = new ArrayList<SubspaceDataPoint>(pointBuffer);
				
				// Get clustering results
				if (combinationSet) {
					if (microClusterer.implementsMicroClusterer()) {
		        		microResult = microClusterer.getMicroClusteringResult();
		            } else {
		            	throw new RuntimeException("EvaluateSubspaceClustering: given microClusterer does not provide microclustering");
		            }
		        	macroResult = macroClusterer.getClusteringResult(microResult);
		        	
		        } else {
		        	microResult = oneStopClusterer.getMicroClusteringResult();
		        	macroResult = oneStopClusterer.getClusteringResult();
				}
				
				SubspaceClustering gtClustering = new SubspaceClustering(pointBuffer);
				
				
				// (Sub)Evaluation
				for (int i = 0; i < measures.length; i++) {
					if (macroResult != null) {
		        		try {
		                    double msec = measures[i].subEvaluateClusteringPerformance(macroResult, gtClustering, pointArray);
		                } catch (Exception ex) { ex.printStackTrace(); }
		            } else {
		                for (int j = 0; j < measures[i].getNumMeasures(); j++) {
		                    measures[i].addEmptySubValue(j);
		                }
		            }
				}
				
				// Averaging subevaluations
				if (timestamp % evaluationFrequency == 0) {
					for (int i = 0; i < measures.length; i++) {
	    	        	measures[i].averageSubEvaluations();
	    	        }
				}
				
				// Prepare for the next evaluation point
				evaluationPointCounter = 0;
			}
		}
		
		/** Write a dump file **/
		exportCSV(dumpFilename, clusterEvents, measures, evaluationFrequency);
	}
	
	protected void exportCSV(String filepath, ArrayList<ClusterEvent> clusterEvents, SubspaceMeasureCollection[] measures, int horizon) {
		PrintWriter out = null;
		try {
			// Prepare an output file			
			if (!filepath.endsWith(".csv")) {
				filepath += ".csv";
			}
			out = new PrintWriter(new BufferedWriter(new FileWriter(filepath)));
			
			
			String delimiter = ";";

			// Header
			int numValues = 0;
			out.write("Nr" + delimiter);
			out.write("Event" + delimiter);
			for (int i = 0; i < measures.length; i++) {
				for (int j = 0; j < measures[i].getNumMeasures(); j++) {
					if (measures[i].isEnabled(j)) {
						out.write(measures[i].getName(j) + delimiter);
						numValues = measures[i].getNumberOfValues(j);
					}
				}
			}
			
			out.write("\n");

			// Rows
			Iterator<ClusterEvent> eventIt = null;
			ClusterEvent event = null;
			if (clusterEvents != null) {
				if (clusterEvents.size() > 0) {
					eventIt = clusterEvents.iterator();
					event = eventIt.next();
				}
			}
			
			for (int v = 0; v < numValues; v++) {
				// Nr
				out.write(v + delimiter);

				// Events
				if (event != null && event.getTimestamp() <= horizon) {
					out.write(event.getType() + delimiter);
					if (eventIt != null && eventIt.hasNext()) {
						event = eventIt.next();
					} else {
						event = null;
					}
				} else {
					out.write(delimiter);
				}

				// Values
				for (int i = 0; i < measures.length; i++) {
					for (int j = 0; j < measures[i].getNumMeasures(); j++) {
						if (measures[i].isEnabled(j)) {
							out.write(measures[i].getValue(j, v) + delimiter);
						}
					}
				}
				
				out.write("\n");
			}
			
			// Mean values
			out.write("mean;;");
			for (int i = 0; i < measures.length; i++) {
				for (int j = 0; j < measures[i].getNumMeasures(); j++) {
					if (measures[i].isEnabled(j)) {
						out.write(measures[i].getMean(j) + delimiter);
					}
				}
			}
			
			out.close();
			
		} catch (IOException ex) {
			Logger.getLogger(SubspaceRunVisualizer.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			out.close();
		}
	}
}