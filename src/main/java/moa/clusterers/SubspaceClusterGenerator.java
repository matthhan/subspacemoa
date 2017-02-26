package moa.clusterers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import moa.cluster.Clustering;
import moa.cluster.SubspaceClustering;
import moa.cluster.SubspaceSphereCluster;
import moa.core.Measurement;
import moa.core.SubspaceInstance;
import moa.gui.subspacevisualization.SubspaceDataPoint;
import moa.options.FloatOption;
import moa.options.IntOption;
import weka.core.Instance;

public class SubspaceClusterGenerator extends AbstractSubspaceClusterer {

	private static final long serialVersionUID = 1L;

	public IntOption timeWindowOption = new IntOption("timeWindow",
			't', "Range of the window.", 1000);

    public FloatOption radiusDecreaseOption = new FloatOption("radiusDecrease", 'r',
                "Decrease the average radii of the centroids in the model.", 0, 0, 1);

    public FloatOption radiusIncreaseOption = new FloatOption("radiusIncrease", 'R',
                "Increase the average radii of the centroids in the model.", 0, 0, 1);

    public FloatOption positionOffsetOption = new FloatOption("positionOffset", 'p',
                "Drift the centroids in the model.", 0, 0, 1);

    public FloatOption clusterRemoveOption = new FloatOption("clusterRemove", 'D',
                "Deletes complete clusters from the clustering.", 0, 0, 1);

    public FloatOption joinClustersOption = new FloatOption("joinClusters", 'j',
            	"Join two clusters if their hull distance is less minRadius times this factor.", 0, 0, 1);

    public FloatOption clusterAddOption = new FloatOption("clusterAdd", 'A',
                "Adds additional clusters.", 0, 0, 1);
    
    public FloatOption dimensionErrorProbOption = new FloatOption("dimensionErrorProb",
			'd', "Probability to add/remove dimensions to/from the subspace.", 0, 0, 1);
    
    public IntOption dimensionErrorOffsetOption = new IntOption("dimensionErrorOffset",
			's', "How many dimensions can be added/removed.", 0);

    private static double err_intervall_width = 0.0;
    private ArrayList<SubspaceDataPoint> points;
    private int instanceCounter;
    private Random random;
    private SubspaceClustering sourceClustering = null;

    @Override
    public void resetLearningImpl() {
        points = new ArrayList<SubspaceDataPoint>();
        instanceCounter = 0;
        random = new Random(227);
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
    	if (inst instanceof SubspaceInstance) {
	        if (instanceCounter >= timeWindowOption.getValue()) {
	            points.remove(0);
	        }
	        instanceCounter++;
	        points.add(new SubspaceDataPoint((SubspaceInstance) inst, instanceCounter));
    	} else {
    		System.out.println("SubspaceClusterGenerator.trainOnInstanceImpl(): takes only SubspaceInstance");
    	}
    }

    @Override
    public boolean implementsMicroClusterer() {
        return false;	// Not supported yet
    }

    public void setSourceClustering(SubspaceClustering source) {
        sourceClustering = source;
    }
    
    @Override
    public Clustering getMicroClusteringResult() {
        return null;	// Not supported yet
    }

    
    public SubspaceClustering getClusteringResult() {
    	sourceClustering = new SubspaceClustering(points);
        return alterClustering(sourceClustering);
    }


    private SubspaceClustering alterClustering(SubspaceClustering scclustering){
    	
    	//percentage of the radius that will be cut off
        //0: no changes to radius
        //1: radius of 0
        double errLevelRadiusDecrease = radiusDecreaseOption.getValue();

        //0: no changes to radius
        //1: radius 100% bigger
        double errLevelRadiusIncrease = radiusIncreaseOption.getValue();

        //0: no changes
        //1: distance between centers is 2 * original radius
        double errLevelPosition = positionOffsetOption.getValue();

        // Cluster remove
        int numRemoveCluster = (int)(clusterRemoveOption.getValue() * scclustering.size());
        for (int c = 0; c < numRemoveCluster; c++) {
            int delId = random.nextInt(scclustering.size());
            scclustering.remove(delId);
        }
        
        // Cluster add
        int numAddCluster = (int)(clusterAddOption.getValue() * scclustering.size());

        // Recalculate number of clusters
        int numCluster = scclustering.size();
        
        // Error seeds for clusters
        double[] err_seeds = new double[numCluster];
        double err_seed_sum = 0.0;
        double tmp_seed;
        for (int i = 0; i < numCluster; i++) {
            tmp_seed = random.nextDouble();
            err_seeds[i] = err_seed_sum + tmp_seed;
            err_seed_sum += tmp_seed;
        }

        // Sum of weights
        double sumWeight = 0;
        for (int i = 0; i < numCluster; i++) {
            sumWeight += scclustering.get(i).getWeight();
        }

        
        
        
        // Altering...
        SubspaceClustering clustering = new SubspaceClustering();

        for (int i = 0; i < numCluster; i++) {
            if(!(scclustering.get(i) instanceof SubspaceSphereCluster)){
                System.out.println("Not a SubspaceSphereCluster");
                continue;
            }
            SubspaceSphereCluster sourceCluster = (SubspaceSphereCluster)scclustering.get(i);
            double[] center = Arrays.copyOf(sourceCluster.getCenter(),sourceCluster.getCenter().length);
            double weight = sourceCluster.getWeight();
            double radius = sourceCluster.getRadius();
            boolean[] subspace = sourceCluster.getSubspace();
            boolean[] adjustedSubspace = sourceCluster.getAdjustedSubspace();
            double id = sourceCluster.getId();
            double gtLabel = sourceCluster.getGroundTruth();

            // Move cluster center
            if (errLevelPosition > 0) {
                double errOffset = random.nextDouble() * err_intervall_width / 2.0;
                double errOffsetDirection = ((random.nextBoolean())? 1 : -1);
                double level = errLevelPosition + errOffsetDirection * errOffset;
                double[] vector = new double[center.length];
                double vectorLength = 0;
                for (int d = 0; d < center.length; d++) {
                	if (subspace[d]) {
                		vector[d] = (random.nextBoolean() ? 1 : -1) * random.nextDouble();
                		vectorLength += Math.pow(vector[d], 2);
                	} else {
                		vector[d] = 0;
                	}
                }
                vectorLength = Math.sqrt(vectorLength);

                
                // max is when clusters are next to each other
                double length = 2 * radius * level;

                for (int d = 0; d < center.length; d++) {
                    // normalize length and then stretch to reach error position
                    vector[d]=vector[d]/vectorLength*length;
                }
//                System.out.println("Center "+Arrays.toString(center));
//                System.out.println("Vector "+Arrays.toString(vector));
                
                // check if error position is within bounds
                double [] newCenter = new double[center.length];
                for (int d = 0; d < center.length; d++) {
                	if (subspace[d]) {
	                    //check bounds, otherwise flip vector
	                    if (center[d] + vector[d] >= 0 && center[d] + vector[d] <= 1){
	                        newCenter[d] = center[d] + vector[d];
	                    } else {
	                        newCenter[d] = center[d] + (-1)*vector[d];
	                    }
                	} else {
                		newCenter[d] = center[d];
                	}
                }
                
                center = newCenter;
                for (int d = 0; d < center.length; d++) {
                    if(newCenter[d] >= 0 && newCenter[d] <= 1){
                    }
                    else{
                        System.out.println("This shouldnt have happend, Cluster center out of bounds:"+Arrays.toString(newCenter));
                    }
                }
                //System.out.println("new Center "+Arrays.toString(newCenter));

            }
            
            // alter radius
            if (errLevelRadiusDecrease > 0 || errLevelRadiusIncrease > 0) {
                double errOffset = random.nextDouble()*err_intervall_width/2.0;
                int errOffsetDirection = ((random.nextBoolean())? 1 : -1);

                if(errLevelRadiusDecrease > 0 && (errLevelRadiusIncrease == 0 || random.nextBoolean())){
                    double level = (errLevelRadiusDecrease + errOffsetDirection * errOffset);//*sourceCluster.getWeight()/sumWeight;
                    level = (level<0)?0:level;
                    level = (level>1)?1:level;
                    radius*=(1-level);
                }
                else{
                    double level = errLevelRadiusIncrease + errOffsetDirection * errOffset;
                    level = (level<0)?0:level;
                    level = (level>1)?1:level;
                    radius+=radius*level;
                }
            }
            
            // Alter subspace
            if (random.nextFloat() < dimensionErrorProbOption.getValue()) {
	            int numDimAlter = dimensionErrorOffsetOption.getValue();
	            int numDimActuallyAltered = 0;
	            if (numDimAlter > 0) {		// Add dimensions
	            	List<Integer> irrelevantDims = sourceCluster.getAdjustedIrrelevantDims();
	            	numDimAlter = Math.min(numDimAlter, irrelevantDims.size());
            		while (numDimAlter > numDimActuallyAltered) {
            			int dimToBeAdded = irrelevantDims.get(random.nextInt(irrelevantDims.size()));
            			if (adjustedSubspace[dimToBeAdded] == false) {
            				adjustedSubspace[dimToBeAdded] = true;
            				numDimActuallyAltered++;
            			}
            		}
            	
	            } else if (numDimAlter < 0) {		// Remove dimensions
	            	System.out.println("numDimAlter = " + numDimAlter);
        			List<Integer> relevantDims = sourceCluster.getAdjustedRelevantDims();
        			numDimAlter = Math.min(-numDimAlter, relevantDims.size());
        			while (numDimAlter > numDimActuallyAltered) {
        				int dimToBeRemoved = relevantDims.get(random.nextInt(relevantDims.size()));
        				if (adjustedSubspace[dimToBeRemoved] == true) {
        					adjustedSubspace[dimToBeRemoved] = false;
        					numDimActuallyAltered++;
        				}	
            		}
        			System.out.println("numDimAlter = " + numDimAlter);
        			System.out.println("numDimActuallyAltered = " + numDimActuallyAltered);
	            }
            }
            SubspaceSphereCluster newCluster = new SubspaceSphereCluster(center, radius, subspace, weight);
            newCluster.setAdjustedSubspace(adjustedSubspace);
            newCluster.setMeasureValue("Source Cluster", "C"+sourceCluster.getId());
            newCluster.setId(id);
            newCluster.setGroundTruth(gtLabel);

            clustering.add(newCluster);
        }

        if (joinClustersOption.getValue() > 0) {
            clustering = joinClusters(clustering);
        }

        //add new clusters by copying clusters and set a random center (same subspace)
        for (int c = 0; c < numAddCluster; c++) {
            int copyId = random.nextInt(clustering.size());
            SubspaceSphereCluster scorg = (SubspaceSphereCluster)clustering.get(copyId);
            int dim = scorg.getCenter().length;
            double[] center = new double [dim];
            double radius = scorg.getRadius();
            boolean[] subspace = scorg.getSubspace();

            boolean outofbounds = true;
            int tryCounter = 0;
            while (outofbounds && tryCounter < 20) {
                tryCounter++;
                outofbounds = false;
                for (int j = 0; j < center.length; j++) {
                     center[j] = random.nextDouble();
                     if (subspace[j]) {
	                     if(center[j]- radius < 0 || center[j] + radius > 1){
	                        outofbounds = true;
	                        break;
	                     }
                     }
                }
            }
            if(outofbounds){
                System.out.println("Couldn't place additional cluster");
            }
            else{
                SubspaceSphereCluster scnew = new SubspaceSphereCluster(center, radius, subspace, scorg.getWeight()/2);
                scorg.setWeight(scorg.getWeight()-scnew.getWeight());
                clustering.add(scnew);
            }
        }
        
        /*System.out.println("----- [ ARTIFICIALLY ALTERED CLUSTERS ] -----");
        for (int i = 0; i < clustering.size(); i++) {
        	System.out.println("Cluster " + i);
        	double[] center = clustering.get(i).getCenter();
        	System.out.println("center: [" + center[0] + " " + center[1] + " " + center[2] + " " + center[3] + " " + center[4] + "]");
        	System.out.println("radius: " + ((SubspaceSphereCluster) clustering.get(i)).getRadius());
        	boolean[] subspace = ((SubspaceSphereCluster) clustering.get(i)).getSubspace();
        	System.out.println("subspace: [" + subspace[0] + " " + subspace[1] + " " + subspace[2] + " " + subspace[3] + " " + subspace[4] + "]");
        }
        System.out.println("----- [ ARTIFICIALLY ALTERED CLUSTERS ] -----/////");*/

        return clustering;
    }



    private SubspaceClustering joinClusters(SubspaceClustering clustering){

        double radiusFactor = joinClustersOption.getValue();
        boolean[] merged = new boolean[clustering.size()];
        for (int i = 0; i < merged.length; i++)		// Mark the clusters to be merged
        	merged[i] = false;

        // Result clustering
        SubspaceClustering mclustering = new SubspaceClustering();

        if (radiusFactor > 0) {
System.out.println("------------- radiusFactor = " + radiusFactor + " --------------");
            for (int c1 = 0; c1 < clustering.size(); c1++) {
                SubspaceSphereCluster sc1 = (SubspaceSphereCluster) clustering.get(c1);
                double minDist = Double.MAX_VALUE;
                int minIndex = -1;
                for (int c2 = 0; c2 < clustering.size(); c2++) {
                	if (c2 == c1) continue;
                	
                    SubspaceSphereCluster sc2 = (SubspaceSphereCluster) clustering.get(c2);
                    double dist = sc1.getHullDistance(sc2);
//System.out.print(dist);
                    double threshold = Math.min(sc1.getRadius(), sc2.getRadius()) * radiusFactor;
                    if (dist != Double.NaN && dist < minDist && dist < threshold) {
                        minDist = dist;
                        minIndex = c2;
                    }
                }
                
                // Merge and add it
                if (minIndex != -1) {
                	if (!merged[c1] && !merged[minIndex]) {
	                    merged[c1] = true;
	                    merged[minIndex] = true;
System.out.println();
System.out.println("Cluster " + c1 + " and " + minIndex + "are merged: minDist = " + minDist);
                    	SubspaceSphereCluster scnew = new SubspaceSphereCluster(sc1.getCenter(), sc1.getRadius(), sc1.getSubspace(), sc1.getWeight());
System.out.println("c1's radius = " + sc1.getRadius());
                    	SubspaceSphereCluster sc2 = (SubspaceSphereCluster) clustering.get(minIndex);
System.out.println("c2's radius = " + sc2.getRadius());
						scnew.merge(sc2);
System.out.println("Merged radius = " + scnew.getRadius());
                    	mclustering.add(scnew);
                	}
                }
System.out.println();
            }
        }

        // Add unmerged clusters
        for (int i = 0; i < merged.length; i++) {
            if (!merged[i])
                 mclustering.add((SubspaceSphereCluster) clustering.get(i));
        }

        return mclustering;
    }



    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void getModelDescription(StringBuilder out, int indent) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isRandomizable() {
        return false;
    }

    @Override
    public boolean keepClassLabel() {
        return true;
    }

    public double[] getVotesForInstance(Instance inst) {
        return null;
    }

}
