/**
 * [RandomRBFSubspaceGeneratorEvents.java] for Subspace MOA
 * 
 * Subspace version of RandomRBFGeneratorEvents class
 * 
 * @author Yunsu Kim
 * Data Management and Data Exploration Group, RWTH Aachen University
 */

package moa.streams.clustering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import moa.cluster.Clustering;
import moa.cluster.SubspaceSphereCluster;
import moa.core.AutoExpandVector;
import moa.core.InstancesHeader;
import moa.core.ObjectRepository;
import moa.core.SubspaceInstance;
import moa.gui.subspacevisualization.SubspaceDataPoint;
import moa.gui.visualization.DataPoint;
import moa.options.FlagOption;
import moa.options.FloatOption;
import moa.options.IntOption;
import moa.streams.InstanceStream;
import moa.tasks.TaskMonitor;
import weka.core.Attribute;
import weka.core.Instances;

public class RandomRBFSubspaceGeneratorEvents extends SubspaceClusteringStream {
	
	private static final long serialVersionUID = 1L;

	
	/* Cluster/instance generation options */
    public IntOption modelRandomSeedOption = new IntOption("modelRandomSeed",
                    'm', "Seed for random generation of model.", 1);

    public IntOption instanceRandomSeedOption = new IntOption(
                    "instanceRandomSeed", 'i',
                    "Seed for random generation of instances.", 5);

    public IntOption numClusterOption = new IntOption("numCluster", 'K',
                    "The average number of centroids in the model.", 5, 1, Integer.MAX_VALUE);

    public IntOption numClusterRangeOption = new IntOption("numClusterRange", 'k',
                    "Deviation of the number of centroids in the model.", 0, 0, Integer.MAX_VALUE);
    
    public IntOption avgSubspaceSizeOption = new IntOption("avgSubspaceSize", 'S',
            		"The average number of dimensions in the subspace of a cluster.", 4, 1, Integer.MAX_VALUE);

    public IntOption avgSubspaceSizeRangeOption = new IntOption("avgSubspaceSizeRange", 's',
            		"Deviation of the number of dimensions in the subspace of a cluster.", 0, 0, Integer.MAX_VALUE);

    public FloatOption kernelRadiiOption = new FloatOption("kernelRadius", 'R',
                    "The average radii of the centroids in the model." +
                    "This value will be adjusted according to the kernel subspace size.", 0.07, 0, 1);

    public FloatOption kernelRadiiRangeOption = new FloatOption("kernelRadiusRange", 'r',
                    "Deviation of average radii of the centroids in the model.", 0, 0, 1);
    
    public IntOption numOverlappedClusterOption = new IntOption("numOverlappedCluster", 'o',
            		"The number of overlapped clusters in the initial state.", 0, 0, Integer.MAX_VALUE);
    
    public FloatOption overlappingDegreeOption = new FloatOption("overlappingDegree", 'O',
    				"How close the initially overlapped clusters are.", 0.0, 0, 1);

    public FloatOption densityRangeOption = new FloatOption("densityRange", 'd',
                    "Offset of the average weight a cluster has. Value of 0 means all cluster " +
                    "contain the same amount of points.", 0, 0, 1);
    
    public FloatOption noiseLevelOption = new FloatOption("noiseLevel", 'N',
            		"Noise level.", 0.1, 0, 1);

    public FlagOption noiseInClusterOption = new FlagOption("noiseInCluster", 'n',
            		"Allow noise to be placed within a cluster.");

    /* Cluster movement options */
    public IntOption speedOption = new IntOption("speed", 'V',
                    "Kernels move a predefined distance of 0.01 every X points", 200, 1, Integer.MAX_VALUE);

    public IntOption speedRangeOption = new IntOption("speedRange", 'v',
                    "Speed/Velocity point offset", 0, 0, Integer.MAX_VALUE);

    /* Object event options */
    public IntOption eventFrequencyOption = new IntOption("eventFrequency", 'E',
                    "Event frequency. Enable at least one of the events below and set numClusterRange!", 30000, 0, Integer.MAX_VALUE);

    public FlagOption eventMergeSplitOption = new FlagOption("eventMergeSplitOption", 'M',
                    "Enable merging and splitting of clusters. Set eventFrequency and numClusterRange!");

    public FlagOption eventDeleteCreateOption = new FlagOption("eventDeleteCreate", 'C',
    				"Enable emering and disapperaing of clusters. Set eventFrequency and numClusterRange!");
    
    /* Subspace event options */
    public IntOption subspaceEventFrequencyOption = new IntOption("subspaceEventFrequency", 'F',
					"Subspace event frequency by each kernel movement destination.", 0, 0, Integer.MAX_VALUE);
    
    /* Stream pausing options */
    //public IntOption streamPauseIntervalOption = new IntOption("streamPauseInterval", 'p',
	//				"The pause interval of incoming stream.", 30000, 0, Integer.MAX_VALUE);
    

    private boolean debug = false;

    /* Variables: Generator clusters */
    protected Random modelRandom;
    private AutoExpandVector<GeneratorSubspaceCluster> kernels;
    private int maxTryGenerate = 50;
    private int numActiveKernels;
    private int clusterIdCounter;
    private int noiseId;
    
    /* Variables: Instances */
    protected Random instanceRandom;
    private int numGeneratedInstances;
    private AutoExpandVector<Double>[] inClusterRangeSize;
    private AutoExpandVector<Double>[] inClusterRangeCoord;
    private double inClusterRangeSizeSum;
    
    /* Variables: Cluster moves */
    private int kernelMovePointFrequency = 10;		// Step size
    private double maxDistanceMoveThresholdByStep = 0.01;
    
    /* Variables: Cluster events */
    private transient Vector<ClusterEventListener> listeners;
    private double eventFrequencyRange = 0;
    private int nextEventCounter;
    private int nextEventChoice = -1;
    
    private double merge_threshold = 0.7;
    private GeneratorSubspaceCluster mergeClusterA;
    private GeneratorSubspaceCluster mergeClusterB;
    private boolean mergeKernelsOverlapping = false;
	
/*-------------------- CONSTRUCTORS --------------------*/
    
    public RandomRBFSubspaceGeneratorEvents() {
    	
    }
	

	
/*-------------------- STREAM BASICS --------------------*/
	
	@Override
	protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
		if (debug) System.out.println("----- RandomRBFSubspaceGeneratorEvents.prepareForUseImpl() -----");
		monitor.setCurrentActivity("Preparing random subspace RBF...", -1.0);
		if (debug) System.out.println("current activity set");
        generateHeader();
        if (debug) System.out.println("header generated");
        restart();
        if (debug) System.out.println("restarted");
	}

	public boolean isRestartable() {
		return true;
	}
	
	public void restart() {
		if (debug) System.out.println("----- RandomRBFSubspaceGeneratorEvents.restart() -----");
		modelRandom = new Random(modelRandomSeedOption.getValue());
		instanceRandom = new Random(instanceRandomSeedOption.getValue());
        nextEventCounter = eventFrequencyOption.getValue();
        nextEventChoice = getNextEvent();
        numGeneratedInstances = 0;
        clusterIdCounter = 0;
        noiseId = numClusterOption.getValue();		// The last index is for noise
        mergeClusterA = mergeClusterB = null;
        if (debug) System.out.println("parameters set");
        
        numActiveKernels = 0;
        kernels = new AutoExpandVector<GeneratorSubspaceCluster>();
        initKernels();
        if (debug) System.out.println("----- RandomRBFSubspaceGeneratorEvents.restart() /////");
	}
	
	protected void generateHeader() {
        ArrayList<Attribute> attributes = new ArrayList<Attribute>();
        for (int i = 0; i < this.numAttsOption.getValue(); i++) {
            attributes.add(new Attribute("att" + (i + 1)));
        }
        
        ArrayList<String> classLabels = new ArrayList<String>();
        for (int i = 0; i < this.numClusterOption.getValue(); i++) {
            classLabels.add("class" + (i + 1));
        }
        if (noiseLevelOption.getValue() > 0) classLabels.add("noise");	// The last label = "noise"
        
        attributes.add(new Attribute("class", classLabels));
        streamHeader = new InstancesHeader(new Instances(getCLICreationString(InstanceStream.class), attributes, 0));
        streamHeader.setClassIndex(streamHeader.numAttributes() - 1);
    }
	
	public void getDescription(StringBuilder sb, int indent) {

	}
	
    @Override
    public String getPurposeString() {
            return "Generates a random radial basis function stream (subspace-based).";
    }

    public String getParameterString(){
        return "";
    }
	
	
	
/*-------------------- INSTANCES --------------------*/
	
    public long estimatedRemainingInstances() {
		return -1;		// Infinite
	}

	public boolean hasMoreInstances() {
		return true;	// Always coming
	}
	
	@Override
    public SubspaceInstance nextInstance() {
    	numGeneratedInstances++;	// Timeline is extended
        eventScheduler();			// Event processing

        // Initializations
        double[] values = null;
        double[] classLabels = null;
        int clusterChoice = -1;
        double chosenLabel = -1;
        
        // Fill in the attribute values / class labels
        if (instanceRandom.nextDouble() > noiseLevelOption.getValue()) {	// From generator cluster
			clusterChoice = chooseWeightedElement();	// TODO: From one or more kernels
            SubspaceSphereCluster generator = kernels.get(clusterChoice).generator;
            values = generator.sample(instanceRandom).toDoubleArray();
            boolean[] curSubspace = generator.getSubspace();
		            
            chosenLabel = generator.getId();
        	classLabels = new double[numAttsOption.getValue()];
        	for (int j = 0; j < classLabels.length; j++) {
            	if (curSubspace[j]) {
            		classLabels[j] = chosenLabel;
            	} else {
            		classLabels[j] = noiseId;	// Noise label by default
            		for (int i = 0; i < kernels.size(); i++) {
            			SubspaceSphereCluster otherGenerator = kernels.get(i).generator;
            			if (otherGenerator.boundaryCheck(values[j], j)) {
            				classLabels[j] = otherGenerator.getId();	// If it's in another cluster, label of that cluster
            				break;
	            		}
            		}
            	}
            }            
        } else {	// Pure noise
            values = getNoisePoint();
            classLabels = new double[numAttsOption.getValue()];
            for (int j = 0; j < classLabels.length; j++) {
            	classLabels[j] = noiseId;
            }
        }

        // Abnormal value detection
        if (Double.isNaN(values[0])) {
            System.out.println("Instance corrupted: " + numGeneratedInstances);
        }
        
        // Construct an "Instance"
        SubspaceInstance inst = new SubspaceInstance(1.0, values, classLabels);
        inst.setDataset(getHeader());
        if (clusterChoice != -1)
        	kernels.get(clusterChoice).addInstance(inst);	// TODO: Add to one or more kernels
        
//        System.out.println(numGeneratedInstances+": Overlap is"+updateOverlaps());
        
        return inst;
    }
    
    /**
     * Randomly choose a kernel index.
     * 
     * @return kernel index
     */
    private int chooseWeightedElement() {
        double r = instanceRandom.nextDouble();

        // Determine index of chosen element
        int i = 0;
        while (r > 0.0) {
            r -= kernels.get(i).generator.getWeight();
            i++;
        }
        i--;	// Overcounted once
        //System.out.println(i);
        return i;
    }
    
    /**
     * Randomly generate a noise point.
     * 
     * @return noise point
     */
    private double[] getNoisePoint() {
        double [] sample = new double [numAttsOption.getValue() + 1];	// +1 for class label
        
        if (noiseInClusterOption.isSet()) {
           	// Generate a random point
            for (int j = 0; j < numAttsOption.getValue(); j++) {
                sample[j] = instanceRandom.nextDouble();
            }
        } else {
        	for (int j = 0; j < numAttsOption.getValue(); j++) {
        		sample[j] = instanceRandom.nextDouble() * (1 - inClusterRangeSizeSum);
        		
        		double blockSum = 0.0;		// Shifting amount
        		for (int k = 0; k < inClusterRangeCoord[j].size(); k++) {
        			double coord = inClusterRangeCoord[j].get(k);
        			if (coord < sample[j])
        				blockSum += inClusterRangeSize[j].get(k);
        			else if (coord == sample[j])	// Should not be on the boundary coord. Adjust a little bit.
        				sample[j] -= Double.MIN_VALUE;
        		}
        		sample[j] += blockSum;
        	}
        }
        
        // Noise label
        sample[numAttsOption.getValue()] = numClusterOption.getValue();
        
        return sample;
    }
    

	
    
/*-------------------- EVENT SCHEDULING --------------------*/
    
    private void eventScheduler() {
        
    	// Update kernel every time
    	for (int i = 0; i < kernels.size(); i++) {
            kernels.get(i).updateKernel();
        }
        
        nextEventCounter--;
        
        // Kernel moves
        if (nextEventCounter % kernelMovePointFrequency == 0) {
            for (int i = 0; i < kernels.size(); i++) {
                kernels.get(i).move();
            }
            updateNoiseInterval();
        }

        // No other events
        if (eventFrequencyOption.getValue() == 0) {
            return;
        }

        String type = "";
        String message = "";
        boolean eventFinished = false;
        switch (nextEventChoice) {
            case 0:		// Merge when kernels are too many
                if (numActiveKernels > 1 && numActiveKernels > numClusterOption.getValue() - numClusterRangeOption.getValue()) {
                    message = mergeKernels(nextEventCounter);
                    type = "Merge";
                }
                if (mergeClusterA == null && mergeClusterB == null && message.startsWith("Clusters merging")) {
                	eventFinished = true;
                }                
                break;
            case 1:		// Split when kernels are too few
                if(nextEventCounter<=0){
                    if (numActiveKernels < numClusterOption.getValue() + numClusterRangeOption.getValue()) {
                        message = splitKernel();
                        type = "Split";
                    }
                    eventFinished = true;
                }
                break;
            case 2:		// Delete when kernels are too many 
                if (nextEventCounter <= 0) {
                	if (numActiveKernels > 1 && numActiveKernels > numClusterOption.getValue() - numClusterRangeOption.getValue()){
                        message = fadeOut();
                        type = "Delete";
                    }
                	eventFinished = true;
                }
                break;
            case 3:
                if (nextEventCounter <= 0) {
                	if (numActiveKernels < numClusterOption.getValue() + numClusterRangeOption.getValue()) {
	                    message = fadeIn();
	                    type = "Create";
                	}
                	eventFinished = true;          	
                }
                break;
        }
        
        if (eventFinished) {
        	nextEventCounter = (int) (eventFrequencyOption.getValue()
        							  + (instanceRandom.nextBoolean() ? -1 : 1) * eventFrequencyRange * instanceRandom.nextDouble());
        	nextEventChoice = getNextEvent();
        }
        
        if (!message.isEmpty()) {
        	message += " (numKernels = " + numActiveKernels + " at " + numGeneratedInstances+ ")";
        	if (!type.equals("Merge") || message.startsWith("Clusters merging"))
        		fireClusterChange(numGeneratedInstances, type, message);
        }
    }
    
    private int getNextEvent() {
    	int choice = -1;
    	boolean lowerLimit = numActiveKernels <= numClusterOption.getValue() - numClusterRangeOption.getValue();
    	boolean upperLimit = numActiveKernels >= numClusterOption.getValue() + numClusterRangeOption.getValue();

    	if (!lowerLimit || !upperLimit) {
	    	int mode = -1;
	    	if (eventDeleteCreateOption.isSet() && eventMergeSplitOption.isSet()) {
	    		mode = instanceRandom.nextInt(2);
	    	}
	    	
			if (mode == 0 || (mode == -1 && eventMergeSplitOption.isSet())) {
				//have we reached a limit? if not free choice
				if (!lowerLimit && !upperLimit) 
					choice = instanceRandom.nextInt(2);
				else
					//we have a limit. if lower limit, choose split
					if (lowerLimit)
						choice = 1;
					//otherwise we reached upper level, choose merge
					else
						choice = 0;
			}
			
			if (mode == 1 || (mode == -1 && eventDeleteCreateOption.isSet())) {
				//have we reached a limit? if not free choice
				if(!lowerLimit && !upperLimit) 
					choice = instanceRandom.nextInt(2)+2;
				else
					//we have a limit. if lower limit, choose create
					if (lowerLimit)
						choice = 3;
					//otherwise we reached upper level, choose delete
					else
						choice = 2;
			}
    	}
    	
    	return choice;
    }

    


/*-------------------- EVENTS --------------------*/

    /**
     * Choice 0: Merge
     *     
     * @param steps
     * @return
     */
    private String mergeKernels(int steps) {
        if (numActiveKernels > 1 && (mergeClusterA == null && mergeClusterB == null)) {

        	//choose clusters to merge
        	double diseredDist = steps / speedOption.getValue() * maxDistanceMoveThresholdByStep;
        	double minDist = Double.MAX_VALUE;
//        	System.out.println("DisredDist:"+(2*diseredDist));
        	for (int i = 0; i < kernels.size(); i++) {
        		for (int j = 0; j < i; j++) {
            		if (kernels.get(i).killTimer != -1 || kernels.get(j).killTimer != -1) {
            			continue;
            		}
            		else {
            			double kernelDist = kernels.get(i).generator.getCenterDistance(kernels.get(j).generator);
            			double d = kernelDist - 2 * diseredDist;
//            			System.out.println("Dist:"+i+" / "+j+" "+d);
            			if (Math.abs(d) < minDist && (minDist != Double.MAX_VALUE || d > 0 || Math.abs(d) < 0.001)) {
            				minDist = Math.abs(d);
            				mergeClusterA = kernels.get(i);
            				mergeClusterB = kernels.get(j);
            			}
            		}
        		}
        	}
        	
        	if (mergeClusterA != null && mergeClusterB != null) {
	        	double[] merge_point = mergeClusterA.generator.getCenter();
	        	double[] v = mergeClusterA.generator.getDistanceVector(mergeClusterB.generator);
	        	for (int i = 0; i < v.length; i++) {
	        		merge_point[i] = merge_point[i] + v[i] * 0.5;
				}
	
	            mergeClusterA.merging = true;
	            mergeClusterB.merging = true;
	            mergeClusterA.setDestination(merge_point);
	            mergeClusterB.setDestination(merge_point);
	            
	            if (debug) {
	            	System.out.println("Center1"+Arrays.toString(mergeClusterA.generator.getCenter()));
		        	System.out.println("Center2"+Arrays.toString(mergeClusterB.generator.getCenter()));
		            System.out.println("Vector"+Arrays.toString(v));        	
	            	
	                System.out.println("Try to merge cluster "+mergeClusterA.generator.getId()+
	                        " into "+mergeClusterB.generator.getId()+
	                        " at "+Arrays.toString(merge_point)+
	                        " time "+numGeneratedInstances);
	            }
	            
	            return "Init merge";
        	}
        }

        if (mergeClusterA != null && mergeClusterB != null) {

            //movekernels will move the kernels close to each other,
            //we just need to check and merge here if they are close enough
            return mergeClusterA.tryMerging(mergeClusterB);
        }

        return "";
    }
    
    /**
     * Choice 1: Split
     * 
     * @return
     */
    private String splitKernel() {
        int id = instanceRandom.nextInt(kernels.size());
        while (kernels.get(id).killTimer != -1)
            id = instanceRandom.nextInt(kernels.size());

        String message = kernels.get(id).splitKernel();

        return message;
    }
    
    /**
     * Choice 2: Delete
     * 
     * @return
     */
	private String fadeOut() {
	    int id = instanceRandom.nextInt(kernels.size());
	    while(kernels.get(id).killTimer != -1)
	        id = instanceRandom.nextInt(kernels.size());
	
	    String message = kernels.get(id).fadeOut();
	    return message;
    }
    
	/**
	 * Choice 3: Create
	 * 
	 * @return
	 */
    private String fadeIn() {
    	GeneratorSubspaceCluster gc = new GeneratorSubspaceCluster(clusterIdCounter++);
    	kernels.add(gc);
    	numActiveKernels++;
    	normalizeWeights();
    	
    	// Update header to add a new class label
    	streamHeader.attribute("class").addStringValue("class" + (clusterIdCounter + 1));
    	
    	return "Creating new cluster";
    }
    
    

/*-------------------- EVENT LISTENER --------------------*/
    
    synchronized public void addClusterChangeListener(ClusterEventListener l) {
    	if (listeners == null)
    		listeners = new Vector();
    	listeners.addElement(l);
    }

    synchronized public void removeClusterChangeListener(ClusterEventListener l) {
    	if (listeners == null)
    		listeners = new Vector();
    	listeners.removeElement(l);
    }

    /** 
     * Fire a ClusterChangeEvent to all registered listeners
     * 
     **/
    protected void fireClusterChange(long timestamp, String type, String message) {
    	// if we have no listeners, do nothing...
    	if (listeners != null && !listeners.isEmpty()) {
    		// create the event object to send
    		ClusterEvent event = new ClusterEvent(this, timestamp, type , message);

	        // make a copy of the listener list in case
	        //   anyone adds/removes listeners
	        Vector targets;
	        synchronized (this) {
	        	targets = (Vector) listeners.clone();
	        }

	        // walk through the listener list and
	        //   call the sunMoved method in each
	        Enumeration e = targets.elements();
	        while (e.hasMoreElements()) {
	        	ClusterEventListener l = (ClusterEventListener) e.nextElement();
	        	l.changeCluster(event);
	
	        }
    	}
    }
    
    
    
/*-------------------- KERNELS (GENERATOR CLUSTERS) --------------------*/
	
    // Initially overlapped clusters
    int numCluster;
    int numOverlappedCluster;
    boolean[] overlappingSubspace;
    double[] overlappingReferencePoint;
    
    protected void initKernels() {
    	setInitialOverlappingConditions();
    	    	
        for (int i = 0; i < numCluster; i++) {
            kernels.add(new GeneratorSubspaceCluster(clusterIdCounter));
            numActiveKernels++;
            clusterIdCounter++;
        }
        normalizeWeights();
        
        // InCluster-range setting
        int numDims = numAttsOption.getValue();
        inClusterRangeCoord = (AutoExpandVector<Double>[]) new AutoExpandVector[numDims];
        inClusterRangeSize = (AutoExpandVector<Double>[]) new AutoExpandVector[numDims];
        updateNoiseInterval();
        
        // Noise ID = the last index
        clusterIdCounter++;
    }
    
    protected void setInitialOverlappingConditions() {
    	
    	// How many?
    	numCluster = numClusterOption.getValue();
    	numOverlappedCluster = numOverlappedClusterOption.getValue();
    	if (numOverlappedCluster > numCluster)	// It should not be!
    		return;
    	
    	// Construct a common subspace
    	int fullSpaceDims = numAttsOption.getValue();
    	overlappingSubspace = new boolean[fullSpaceDims];
        for (int j = 0; j < fullSpaceDims; j++)
        	overlappingSubspace[j] = false;
        
    	int numRelevantDims = (int) (avgSubspaceSizeOption.getValue() + (modelRandom.nextBoolean() ? -1 : 1) * avgSubspaceSizeRangeOption.getValue() * modelRandom.nextDouble());
        if (numRelevantDims < 2)
        	numRelevantDims = 2;		// At least 2
     
        for (int j = 0; j < numRelevantDims; j++) {
        	int relevantDim;
        	do {
        		relevantDim = (int) (modelRandom.nextDouble() * fullSpaceDims);
        	} while (overlappingSubspace[relevantDim] == true);	// No duplicates
        	
        	overlappingSubspace[relevantDim] = true;
        }
        
        // Set a reference point: TODO randomize?
        overlappingReferencePoint = new double[fullSpaceDims];
        for (int j = 0; j < fullSpaceDims; j++)
        	overlappingReferencePoint[j] = 0.5;
    }
    
    public Clustering getGeneratingClusters() {
        Clustering clustering = new Clustering();
        for (int c = 0; c < kernels.size(); c++) {
            clustering.add(kernels.get(c).generator);
        }
        return clustering;
    }
    
    /**
     * Update ranges where pure noise points can reside.
     * 
     */
    protected void updateNoiseInterval() {
    	
    	/* Cluster range boundary. Pair of two doubles. */
        class Boundary {
            public double l;
            public double r;
            
            public Boundary(double l, double r) {
                this.l = l;
                this.r = r;
            }
        }
        
        /* For sorting a list of Boundary's. */
        class BoundaryComparator implements Comparator<Boundary> {
			public int compare(Boundary b1, Boundary b2) {
				if (b1.l < b2.l) return -1;
				else if (b1.l > b2.l) return 1;
				else return 0;
			}
    	}
    	
    	for (int j = 0; j < numAttsOption.getValue(); j++) {
    		
    		// Construct (non-overlapping) cluster boundaries for dim 'j'
        	ArrayList<Boundary> inClusterRangeBoundary = new ArrayList<Boundary>();
	    	for (int i = 0; i < kernels.size(); i++) {
	    		SubspaceSphereCluster kernel = kernels.get(i).generator;
	    		if (kernel.isRelevant(j)) {
	    			double leftBoundary = kernels.get(i).generator.getLeftBoundary(j);
	    			double rightBoundary = kernels.get(i).generator.getRightBoundary(j);
	    			Boundary newB = new Boundary(leftBoundary, rightBoundary);
	    			ArrayList<Boundary> markedToRemove = new ArrayList<Boundary>(); 
	    			for (int k = 0; k < inClusterRangeBoundary.size(); k++) {
	    				Boundary oldB = inClusterRangeBoundary.get(k);
	    				if (newB.l > oldB.r || newB.r < oldB.l) {				// 1) No overlaps
	    					continue;
	    				} else if (newB.l >= oldB.l && newB.r <= oldB.r) {		// 2) Completely included
	    					newB = null;
	    					break;
	    				} else {
	    					markedToRemove.add(oldB);							// 3) Completely include
	    					
	    					if (newB.l >= oldB.l && newB.l <= oldB.r) {			// 4) Left-extend
		    					newB.l = oldB.l;
		    				} else if (newB.r <= oldB.r && newB.r >= oldB.l) {	// 5) Right-extend
		    					newB.r = oldB.r;
		    				}
	    				}
	    			}
	    			
	    			// Rearrange the boundaries
	    			inClusterRangeBoundary.removeAll(markedToRemove);
	    			if (newB != null) {
	    				inClusterRangeBoundary.add(newB);
	    			}
	    		}
	    	}
	    	
	    	Collections.sort(inClusterRangeBoundary, new BoundaryComparator());
	    	
	    	// Calculate range sizes/coords
	    	inClusterRangeSize[j] = new AutoExpandVector<Double>();
	    	inClusterRangeCoord[j] = new AutoExpandVector<Double>();
        	
	    	double rangeSizeSum = 0.0;
        	for (Boundary b : inClusterRangeBoundary) {
        		double rangeSize = b.r - b.l;
        		inClusterRangeSize[j].add(rangeSize);
        		inClusterRangeCoord[j].add(b.l - rangeSizeSum);
        		rangeSizeSum += rangeSize;
        	}
        	
        	inClusterRangeSizeSum = rangeSizeSum;
    	}
    }
    
    
	
    /**
     * Subspace cluster generating data streams.
     * 
     */
    private class GeneratorSubspaceCluster {
    	
    	/* Generator cluster & generated instances */
    	SubspaceSphereCluster generator;
    	LinkedList<DataPoint> points = new LinkedList<DataPoint>();
    	
        /* Cluster removal variables */
    	int killTimer = -1;
        
        /* Cluster movement variables */
        double[] moveVector;
        int totalMovementSteps;
        int currentMovementSteps;
        int numDestinationsMoved = 0;
        
        /* Cluster merge/split variables */
        boolean merging = false;
        boolean isSplitting = false;
        

        
        /**
         * Constructors
         * 
         */
        public GeneratorSubspaceCluster(int label) {
        	
        	boolean isOverlapped = overlappingDegreeOption.getValue() > 0 && numOverlappedCluster > 0;
        	double overlappingDegree = 1.0 - overlappingDegreeOption.getValue();
        	
        	// Prepare subspace vector
        	int fullSpaceDims = numAttsOption.getValue();
        	boolean[] subspace = new boolean[fullSpaceDims];
            
        	if (isOverlapped) {
        		for (int j = 0; j < fullSpaceDims; j++) {
                	subspace[j] = overlappingSubspace[j];
                }
        	} else {
        		int numRelevantDims = (int) (avgSubspaceSizeOption.getValue() + (modelRandom.nextBoolean() ? -1 : 1) * avgSubspaceSizeRangeOption.getValue() * modelRandom.nextDouble());
                if (numRelevantDims < 2)
                	numRelevantDims = 2;		// At least 2
                
                for (int j = 0; j < numRelevantDims; j++) {
	            	int relevantDim;
	            	do {
	            		relevantDim = (int) (modelRandom.nextDouble() * fullSpaceDims);
	            	} while (subspace[relevantDim] == true);	// No duplicates
	            	
	            	subspace[relevantDim] = true;
	            }
        	}
        	
        	
        	// Set radius & center            
            double radius = 0;
            double[] center = new double[fullSpaceDims];
            
        	int tryCounter = 0;
        	boolean outOfBounds = true;
            while (outOfBounds && tryCounter < maxTryGenerate) {
                tryCounter++;
                outOfBounds = false;
                
                // Generate a nonnegative radius value
                radius = kernelRadiiOption.getValue() + (modelRandom.nextBoolean() ? -1 : 1) * kernelRadiiRangeOption.getValue() * modelRandom.nextDouble();
                while (radius <= 0) {
                    radius = kernelRadiiOption.getValue() + (modelRandom.nextBoolean() ? -1 : 1) * kernelRadiiRangeOption.getValue() * modelRandom.nextDouble();
                }
                          
                // Generate a center point
                double overlappingRange = (1 - 2 * radius) * overlappingDegree;
                for (int j = 0; j < fullSpaceDims; j++) {
                	if (subspace[j]) {
                		if (isOverlapped)
                    		center[j] = overlappingReferencePoint[j] - overlappingRange / 2.0 + modelRandom.nextDouble() * overlappingRange;
                    	else
                    		center[j] = modelRandom.nextDouble() * (1 - radius * 2) + radius;
                	
                		// Test out-of-bounds
                		if (center[j] - radius < 0 || center[j] + radius > 1) {	// Cluster boundary exceeds [0,1]
	                    	outOfBounds = true;
	                    	break;
	                    }
                	} else {	// Irrelevant dimensions: pure random value
                		center[j] = modelRandom.nextDouble();
                	}
                }
            }
            
            if (tryCounter < maxTryGenerate) {		// Success: Ready to generate
            	generator = new SubspaceSphereCluster(center, radius, subspace);
                generator.setId(label);
                
                double avgWeight = 1.0 / numClusterOption.getValue();
                double weight = avgWeight + (modelRandom.nextBoolean() ? -1 : 1) * avgWeight * densityRangeOption.getValue() * modelRandom.nextDouble();
                generator.setWeight(weight);
                
                setDestination(null);
                
                if (isOverlapped) numOverlappedCluster--;
            } else {								// Failed: Can't find an appropriate radius/center
                generator = null;
                killTimer = 0;	// "to be removed"
                System.out.println("Tried " + maxTryGenerate + " times to create kernel. Reduce average radii.");
            }
        }

        public GeneratorSubspaceCluster(int label, SubspaceSphereCluster cluster) {
            this.generator = cluster;
            cluster.setId(label);
            setDestination(null);
        }

        
        /*----- Instance/kernel creation/deletion -----*/

        /**
         * A newly generated instance (from nextInstance()) is handled by its mother kernel.
         * 
         * @param instance
         */
        private void addInstance(SubspaceInstance instance){
            SubspaceDataPoint point = new SubspaceDataPoint(instance, numGeneratedInstances);
            points.add(point);
        }
        
        /**
         * Remove a point or kill this cluster.
         * 
         */
        private void updateKernel(){
            
        	// Remove this cluster
        	if (killTimer == 0) {
                kernels.remove(this);
            } else if (killTimer > 0) {
                killTimer--;	// Getting old
            }

            // Remove a point in this cluster
            if (!points.isEmpty() &&
            	numGeneratedInstances - points.getFirst().getTimestamp() >= decayHorizonOption.getValue()) {
                points.removeFirst();
            }
        }
        
        /**
         * Mark this cluster to be removed slowly.
         * 
         * @return a string of fadeout statement
         */
        private String fadeOut() {
        	killTimer = decayHorizonOption.getValue();		// Will be removed in a horizon
        	generator.setWeight(0.0);						// No more new instances
        	numActiveKernels--;
        	normalizeWeights();
        	return "Fading out C"+generator.getId();
        }

        
        /*----- Cluster movements -----*/
        
        /**
         * Cluster center movement.
         * 
         */
        private void move() {
            if (currentMovementSteps < totalMovementSteps) {
                currentMovementSteps++;
                if (moveVector == null) {
                    return;
                } else {
                    double[] center = generator.getCenter();
                    double radius = generator.getRadius();
                    
                    int d;
                    for (d = 0; d < center.length; d++) {
                    	center[d] += moveVector[d];
                    	if (center[d] - radius < 0 || center[d] + radius > 1)
                    		break;		// Bounced by the boundary [0,1]
                    }
                    
                    if (d == center.length) 
                    	generator.setCenter(center);
                    else	// Bounced: moves to random destination
                    	setDestination(null);
                }
            } else {	// Current movement ends
            	numDestinationsMoved++;
            	
            	// Subspace change event
            	if (!merging && !isSplitting) {
	            	int subspaceEventFrequency = subspaceEventFrequencyOption.getValue();
	            	if (subspaceEventFrequency > 0 && numDestinationsMoved % subspaceEventFrequency == 0) {
	            		boolean[] subspace = generator.getSubspace();
	            		int fullspaceSize = generator.getFullspaceSize();
	            		int curSubspaceSize = generator.getSubspaceSize();
	            		boolean addDim = instanceRandom.nextBoolean();
	            		
	            		if (addDim) {		// Add a dimension to subspace
	            			if (curSubspaceSize < fullspaceSize) {
	            				List<Integer> relevantDims = generator.getRelevantDims();
	            				int dimToBeAdded = relevantDims.get(instanceRandom.nextInt(relevantDims.size() - 1));
	            				generator.setRelevantDim(dimToBeAdded, true);
	            			}           			
	            		} else {			// Remove a dimensions from subspace
	            			if (curSubspaceSize > 2) {
	            				List<Integer> irrelevantDims = generator.getRelevantDims();
	            				int dimToBeRemoved = irrelevantDims.get(instanceRandom.nextInt(irrelevantDims.size() - 1));
	            				generator.setRelevantDim(dimToBeRemoved, false);
	            			}
	            		}
	            		
	            		// Adjust radius (relatively to the subspace size)
	            		int newSubspaceSize = generator.getSubspaceSize();
	            		generator.adjustRadius(newSubspaceSize / curSubspaceSize);
	            		
	            		// Adjust radius (boundary test)
	            		double[] center = generator.getCenter();
	            		double radius = generator.getRadius();
	            		int d;
	                    for (d = 0; d < center.length; d++) {
	                    	if (center[d] - radius < 0)
	                    		radius -= radius - center[d];
	                    	else if (center[d] + radius > 1)
	                    		radius -= center[d] + radius - 1;
	                    }
	            	}
            	}
            	
            	// Next destination
                if (!merging) {
                    setDestination(null);
                    isSplitting = false;
                }
            }
        }

        /**
         * Set the destination of this cluster movement.
         * Irrelevant-dimension values do not change from the center.
         * 
         * @param destination - point where the movement ends (null: random)
         */
        void setDestination(double[] destination){

        	int fullspaceDims = numAttsOption.getValue();
        	double[] center = generator.getCenter();
        	
            if (destination == null) {		// Random destination
                destination = new double[fullspaceDims];
                boolean[] subspace = generator.getSubspace();
                
                for (int j = 0; j < fullspaceDims; j++) {
                	if (subspace[j])
                		destination[j] = instanceRandom.nextDouble();
                	else
                		destination[j] = center[j];
                }
            }

            // Set move vector (actually used in the movement)
            double[] moveVector = new double[fullspaceDims];
            for (int d = 0; d < fullspaceDims; d++) {
                moveVector[d] = destination[d] - center[d];
            }
            setMoveVector(moveVector);
        }
        

        /**
         * Set the amount of future change in the attribute values for each step.
         * 
         * @param moveVector - future change in each attribute
         */
        void setMoveVector(double[] moveVector) {
            this.moveVector = moveVector;
            
            // Cluster will move every 'speedInPoints'
            int speedInPoints = speedOption.getValue();
            int speedRangeInPoints = speedRangeOption.getValue();
            if (speedRangeInPoints > 0)
            	speedInPoints += (instanceRandom.nextBoolean() ? -1 : 1) * instanceRandom.nextInt(speedRangeInPoints);
            if (speedInPoints < 1) speedInPoints  = speedOption.getValue();		// Can't move every 0.x points

            // Euclidean distance of the movement
            double length = 0;
            for (int d = 0; d < this.moveVector.length; d++) {
                length += Math.pow(moveVector[d], 2);
            }
            length = Math.sqrt(length);

            // Calculate the movement for each step
            totalMovementSteps = (int) (length / (maxDistanceMoveThresholdByStep * kernelMovePointFrequency) * speedInPoints);
            for (int d = 0; d < this.moveVector.length; d++) {
                this.moveVector[d] /= (double) totalMovementSteps;
            }
            
            /* 	(Details)
               	Kernel moves 0.01 every speedInPoints
			   	Kernel will move (length) eventually
				Kernel will move during ((length/0.01) * speedInPoints) input points
				Kernel will move during (((length/0.01) * speedInPoints) / 10) steps = totalMovementSteps
             */

            // Starting point of the movement
            currentMovementSteps = 0;
        }

        
        /*----- Merge & split -----*/
        
        /**
         * Try merging between this cluster and a given cluster.
         * 
         * @param merge - a cluster to be merged
         * @return
         */
        private String tryMerging(GeneratorSubspaceCluster merge){
        	String message = "";
        	
        	double overlapDegree = generator.overlapRadiusDegree(merge.generator);
        	if (overlapDegree > merge_threshold) {
                SubspaceSphereCluster mcluster = merge.generator;
                
                // Combine & new radius setting
                double radius = Math.max(generator.getRadius(), mcluster.getRadius());
                generator.combine(mcluster);
                generator.setRadius(radius);
                message = "Clusters merging: " + mergeClusterB.generator.getId() + " into " + mergeClusterA.generator.getId();

                // Update the merged kernel
                merge.killTimer = decayHorizonOption.getValue();	// Will be killed in a horizon
                mcluster.setWeight(0.0);							// No more new instances
                normalizeWeights();
                numActiveKernels--;

                // Reset merge variables
                mergeClusterB = mergeClusterA = null;
                merging = false;
                mergeKernelsOverlapping = false;
            } else {
        		if (overlapDegree > 0 && !mergeKernelsOverlapping){		// Overlapping started, but not be merged yet
        			mergeKernelsOverlapping = true;
        			message = "Merge overlapping started";
        		}
            }
        	
        	return message;
        }

        /**
         * Make a new cluster with the same center and subspace, splitting away.
         * 
         */
        private String splitKernel(){
            isSplitting = true;
            
            // New cluster: new radius, weight
            double radius = kernelRadiiOption.getValue();
            double avgWeight = 1.0 / numClusterOption.getValue();
            double weight = avgWeight + avgWeight * densityRangeOption.getValue() * instanceRandom.nextDouble();
            SubspaceSphereCluster spcluster = null;

            // New cluster: same center, subspace
            double[] center = generator.getCenter();
            spcluster = new SubspaceSphereCluster(center, radius, generator.getSubspace(), weight); 

            if (spcluster != null) {
                GeneratorSubspaceCluster gc = new GeneratorSubspaceCluster(clusterIdCounter++, spcluster);
                gc.isSplitting = true;
                
                kernels.add(gc);
                normalizeWeights();
                numActiveKernels++;
                
                return "Split from Kernel "+generator.getId();
            } else {	// Failed to create a new cluster
                System.out.println("Tried to split new kernel from C" + generator.getId() + 
                        		   ". Not enough room for new cluster, decrease average radii, number of clusters or enable overlap.");
                return "";
            }
        }
    }
    
    
/*-------------------- MISCELLANEOUS --------------------*/
	
	/**
	 * Normalize the weights of kernels.
	 * Should be called after creation/deletion of kernels.
	 * 
	 */
    private void normalizeWeights() {
        double sumWeights = 0.0;
        for (int i = 0; i < kernels.size(); i++) {
            sumWeights += kernels.get(i).generator.getWeight();
        }
        for (int i = 0; i < kernels.size(); i++) {
            kernels.get(i).generator.setWeight(kernels.get(i).generator.getWeight() / sumWeights);
        }
    }
    
    public Clustering getMicroClustering(){
    	return null;
    }
	
}
