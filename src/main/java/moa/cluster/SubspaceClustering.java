/**
 * [SubspaceClustering.java] for Subspace MOA
 * 
 * Subspace version of [Clustering] class.
 * 
 * @author Yunsu Kim
 * Data Management and Data Exploration Group, RWTH Aachen University
 */

package moa.cluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import moa.AbstractMOAObject;
import moa.core.AutoExpandVector;
import moa.gui.subspacevisualization.SubspaceDataPoint;
import moa.gui.visualization.DataPoint;
import weka.core.Instance;

public class SubspaceClustering extends AbstractMOAObject {

	private static final long serialVersionUID = 1L;

	private boolean debug = false;
	
    private AutoExpandVector<Cluster> clusters;
    private boolean hasNoise;
    
    // Helpers
    private HashMap<Double, Integer> labelMap = new HashMap<Double, Integer>();
    private HashMap<Integer, boolean[]> classSubspaces = new HashMap<Integer, boolean[]>();
    private List<Double> classLabelList = new ArrayList<Double>();

    public SubspaceClustering() {
        this.clusters = new AutoExpandVector<Cluster>();
    }

    // "SubspaceSphereCluster"s
    public SubspaceClustering(Cluster[] clusters) {
        this.clusters = new AutoExpandVector<Cluster>();
        for (int i = 0; i < clusters.length; i++) {
        	this.clusters.add((SubspaceSphereCluster) clusters[i]);
        }
    }
    
    // This constructor converts full-space clustering to subspace clustering format **
    public SubspaceClustering(Clustering clustering) {
    	this.clusters = clustering.getClustering();
    }

    /**
     * Make a ground truth subspace clustering.
     * 
     * @param points
     */
    public SubspaceClustering(List<? extends DataPoint> points) {	// Should be a list of "SubspaceDataPoint"s
    	labelMap = constructClassValueMap(points);		// Also set 'classSubspaces'

        int numClasses = labelMap.size();
        double noiseLabel = points.get(0).getNoiseLabel();

        // Prepare bins
        ArrayList<SubspaceDataPoint>[] sorted_points = (ArrayList<SubspaceDataPoint>[]) new ArrayList[numClasses];
        for (int i = 0; i < numClasses; i++) {
            sorted_points[i] = new ArrayList<SubspaceDataPoint>();
        }
        
        // Binning the points
        for (Instance point : points) {
        	if (point instanceof SubspaceDataPoint) {
        		double representingLabel = ((SubspaceDataPoint) point).classValue();
		        if (representingLabel == noiseLabel) {			// No need to make noise cluster
		          	continue;
		        } else {
		         	sorted_points[labelMap.get(representingLabel)].add((SubspaceDataPoint) point);
		        }
        	} else {
        		System.out.println("SubspaceClustering cannot be initiated: "
						 		 + "given parameters should be SubspaceDataPoint but this is " + point.getClass().getSimpleName());
        		break;
        	}
        }

        if (debug) {
        	System.out.print("sorted_points(size): ");
        	for (int i = 0; i < sorted_points.length; i++)
        		System.out.print(sorted_points[i].size() + " ");
        	System.out.println();
        }
        
        // Wrap each bin as a (ground truth) cluster
        this.clusters = new AutoExpandVector<Cluster>();
        for (int i = 0; i < numClasses; i++) {
            if (sorted_points[i].size() > 0) {
                SubspaceSphereCluster s = new SubspaceSphereCluster(sorted_points[i], classSubspaces.get(i));
                s.setId(classLabelList.get(i));
                s.setGroundTruth(classLabelList.get(i));
                clusters.add(s);
            }
        }
    }
    
    
    /** Roll-back to [Clustering] object. **/
    public Clustering toClustering() {
    	ArrayList<Cluster> convertedClusters = new ArrayList<Cluster>();
    	for (Cluster sc : clusters) {
    		if (sc instanceof SubspaceSphereCluster) {
    			convertedClusters.add(((SubspaceSphereCluster) sc).toSphereCluster());
    		} else {
    			convertedClusters.add(sc);
    		}
    	}
    	
    	Cluster[] scarray = new Cluster[convertedClusters.size()];
    	for (int i = 0; i < scarray.length; i++)
    		scarray[i] = convertedClusters.get(i);
    	return new Clustering(scarray);
    }

    
    /**
     * Return the mapping from class values to consecutive integer indices (0, 1, ...).
     * Also construct these for the future usage:
     * - classSubspaces
     * - classLabelList
     * 
     * @param points - "SubspaceDataPoint"s
     * @return mapping
     */
    public HashMap<Double, Integer> constructClassValueMap(List<? extends DataPoint> points) {
        HashMap<Double, Integer> classValueMap = new HashMap<Double, Integer>();
        int workcluster = 0;
        double noiseLabel = ((DataPoint)points.get(0)).getNoiseLabel();
        
        hasNoise = false;
        for (int i = 0; i < points.size(); i++) {
        	if (points.get(i) instanceof SubspaceDataPoint) {
        		SubspaceDataPoint sdp = (SubspaceDataPoint) points.get(i);
	            double[] classLabels = sdp.getClassLabels();
	            double representingLabel = sdp.classValue();
	            for (int j = 0; j < classLabels.length; j++) {
	            	if (classLabels[j] == noiseLabel) {
	            		hasNoise = true;
	            	} else if (!classValueMap.containsKey(classLabels[j]) && classLabels[j] == representingLabel) {
		                classValueMap.put(classLabels[j], workcluster);
		                classSubspaces.put(workcluster, ((SubspaceDataPoint) points.get(i)).getSubspace(classLabels[j]));
		                classLabelList.add(classLabels[j]);
		                workcluster++;
		            }
	            }
        	} else {
        		System.out.println("SubspaceClustering cannot be initiated: "
				 		 		 + "given parameters should be subspace-specific (SubspaceDataPoint)");
        		break;
        	}
        }
        
        if (hasNoise) {
        	classValueMap.put(noiseLabel, workcluster);
        	boolean[] noiseSpace = new boolean[points.get(0).numAttributes() - 1];
        	for (int j = 0; j < noiseSpace.length; j++)
        		noiseSpace[j] = true;        	
        	classSubspaces.put(workcluster, noiseSpace);
        	classLabelList.add((double) noiseLabel);
        }
        return classValueMap;
    }
    
    public HashMap<Double, Integer> getLabelMap() {
    	return labelMap;
    }
    
   
    /**
     * add a cluster to the clustering
     */
    public void add(Cluster cluster) {
        clusters.add(cluster);
    }

    /**
     * remove a cluster from the clustering
     */
    public void remove(int index) {
        if (index < clusters.size()) {
            clusters.remove(index);
        }
    }

    /**
     * remove a cluster from the clustering
     */
    public Cluster get(int index) {
        if (index < clusters.size()) {
            return clusters.get(index);
        }
        return null;
    }

    /**
     * @return the <code>Clustering</code> as an AutoExpandVector
     */
    public AutoExpandVector<Cluster> getClustering() {
        return clusters;
    }

    /**
     * @return A deepcopy of the <code>Clustering</code> as an AutoExpandVector
     */
    public AutoExpandVector<Cluster> getClusteringCopy() {
        return (AutoExpandVector<Cluster>) clusters.copy();
    }


    /**
     * @return the number of clusters
     */
    public int size() {
    	return clusters.size();
    }

    /**
     * @return the number of dimensions of this clustering
     */
    public int dimension() {
    	assert (clusters.size() != 0);
    	return clusters.get(0).getCenter().length;
    }

	public void getDescription(StringBuilder sb, int indent) {
		sb.append("Subspace clustering object");
	}
	
	public void printClusterings() {
        for (int i = 0; i < clusters.size(); i++) {
        	System.out.println("Cluster " + i);
        	double[] center = clusters.get(i).getCenter();
        	System.out.println("center: [" + center[0] + " " + center[1] + " " + center[2] + " " + center[3] + " " + center[4] + "]");
        	//System.out.println("radius: " + clusters.get(i).getRadius());
        	//boolean[] subspace = clusters.get(i).getSubspace();
        	//System.out.println("subspace: [" + subspace[0] + " " + subspace[1] + " " + subspace[2] + " " + subspace[3] + " " + subspace[4] + "]");
        }
	}
	
	public boolean hasNoise() {
		return hasNoise;
	}
}
