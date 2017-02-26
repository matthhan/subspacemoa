/**
 * [EntropySubspace.java] for Subspace MOA
 * 
 * Evaluation measure: Entropy
 * 
 * @author Yunsu Kim
 * Data Management and Data Exploration Group, RWTH Aachen University
 */

package moa.evaluation;

import java.util.ArrayList;
import java.util.List;

import moa.cluster.Cluster;
import moa.cluster.SubspaceClustering;
import moa.gui.subspacevisualization.SubspaceDataPoint;

public class EntropySubspace extends SubspaceMeasureCollection {

	private static final long serialVersionUID = 1L;

	private boolean debug = false;
	
	List<List<SubspaceDataPoint>> pointsInFC;
	List<List<SubspaceDataPoint>> pointsInGC;
	
	@Override
	protected String[] getNames() {
		String[] names = {"Entropy"};
		return names;
	}
	
	@Override
    protected boolean[] getDefaultEnabled() {
        boolean [] defaults = {true};
        return defaults;
    }

	@Override
	protected void subEvaluateSubspaceClustering(SubspaceClustering foundClustering, SubspaceClustering gtClustering, List<SubspaceDataPoint> points) throws Exception {
		List<Cluster> foundClusters = foundClustering.getClustering();
		List<Cluster> gtClusters = gtClustering.getClustering();
		pointsInFC = new ArrayList<List<SubspaceDataPoint>>();
		pointsInGC = new ArrayList<List<SubspaceDataPoint>>();
		
		/** Assign points to clusters **/
		
		double inclusionProbabilityThreshold = 0.5;
		
		for (int i = 0; i < foundClusters.size(); i++) {
    		pointsInFC.add(new ArrayList<SubspaceDataPoint>());
    	}

    	for (int i = 0; i < gtClusters.size(); i++) {
    		pointsInGC.add(new ArrayList<SubspaceDataPoint>());
    	}
    	
    	
    	for (SubspaceDataPoint p : points) {
    		for (int i = 0; i < foundClusters.size(); i++) {
    			Cluster fc = foundClusters.get(i);
    			if (fc.getInclusionProbability(p) >= inclusionProbabilityThreshold) {
    				pointsInFC.get(i).add(p);
    			}
    		}
    		
    		for (int i = 0; i < gtClusters.size(); i++) {
    			Cluster gc = gtClusters.get(i);
    			if (gc.getInclusionProbability(p) >= inclusionProbabilityThreshold) {
    				pointsInGC.get(i).add(p);
    			}
    		}
    	}
    	
		
		int max_Objects_in_Clusters = 0;
		for (List<SubspaceDataPoint> pointsList : pointsInFC) {
			max_Objects_in_Clusters += pointsList.size();
		}

		double sumEntropie = 0;
		for (int i = 0; i < pointsInFC.size(); i++) {
			sumEntropie += (entropy(i) * pointsInFC.get(i).size());
		}
		
		double entropy = 0.0;
		if (max_Objects_in_Clusters != 0)
			entropy = 1 - (sumEntropie / (double)max_Objects_in_Clusters);
		
		addSubValue("Entropy", entropy);
	}
	
	private double entropy(int fcIndex) {
		double entropy = 0.0;
		List<SubspaceDataPoint> O = pointsInFC.get(fcIndex);
		if (O.size() == 0) {
			return 0.0;
		}
		
		for (int m = 0; m < pointsInGC.size(); m++) {
			int intersect = 0;
			for (SubspaceDataPoint p : pointsInGC.get(m)) {
				if (O.contains(p))
					intersect++;
			}
			if (debug) System.out.println("intersect = " + intersect);
			
			double relativeNum = (double)intersect / (double)O.size();
			if (relativeNum != 0)
				entropy += relativeNum * Math.log(relativeNum);
			if (debug) System.out.println("entropy = " + entropy);
		}
		
		// Normalize
		double toReturn = 0.0;
		if (pointsInGC.size() > 0)
			toReturn = -entropy / Math.log(pointsInGC.size());
		if (debug) System.out.println("entropy (normalized) = " + toReturn);		
		return toReturn;
	}
}