/**
 * [RNIA.java] for Subspace MOA
 * 
 * Evaluation measure: Relative Non-Intersecting Area (RNIA)
 * 
 * Reference: Patrikainen et al., "Comparing Subspace Clusterings", IEEE TKDE, 2006
 * 
 * @author Yunsu Kim
 * Data Management and Data Exploration Group, RWTH Aachen University
 */

package moa.evaluation;

import java.util.List;

import moa.cluster.Cluster;
import moa.cluster.SubspaceClustering;
import moa.cluster.SubspaceSphereCluster;
import moa.gui.subspacevisualization.SubspaceDataPoint;

public class RNIA extends SubspaceMeasureCollection {

	private static final long serialVersionUID = 1L;
	
	private boolean debug = false;

	@Override
	protected String[] getNames() {
		String[] names = {"1.0-RNIA"};
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
		
		double inclusionProbabilityThreshold = 0.5;
		int numDims = points.get(0).getClassLabels().length;
				
		int union = 0;
		int intersection = 0;
		
		// Full-space (just in case)
		boolean[] fullSpace = new boolean[numDims];
		for (int j = 0; j < numDims; j++) {		// Full-space
			fullSpace[j] = true;
		}

    	for (SubspaceDataPoint p : points) {
    		int[] dimCoveredByFCs = new int[numDims],
      			  dimCoveredByGCs = new int[numDims];
      		
      		for (int j = 0; j < numDims; j++) {
      			dimCoveredByFCs[j] = 0;
      			dimCoveredByGCs[j] = 0;
      		}
    		
    		for (int i = 0; i < foundClusters.size(); i++) {
    			Cluster fc = foundClusters.get(i);
    			if (fc.getInclusionProbability(p) >= inclusionProbabilityThreshold) {
    				if (fc instanceof SubspaceSphereCluster) {
	    				for (int j : ((SubspaceSphereCluster) fc).getAdjustedRelevantDims()) {
	    					dimCoveredByFCs[j]++;
	    				}
    				} else {
    					for (int j = 0; j < numDims; j++) {		// Full-space
    						dimCoveredByFCs[j]++;
    					}
    				}
    			}
    		}
    		
    		for (int i = 0; i < gtClusters.size(); i++) {
    			Cluster gc = gtClusters.get(i);
    			if (gc.getInclusionProbability(p) >= inclusionProbabilityThreshold) {
    				if (gc instanceof SubspaceSphereCluster) {
	    				for (int j : ((SubspaceSphereCluster) gc).getAdjustedRelevantDims()) {
	    					dimCoveredByGCs[j]++;
	    				}
    				} else {
    					for (int j = 0; j < numDims; j++) {		// Full-space
    						dimCoveredByGCs[j]++;
    					}
    				}
    			}
    		}
    		
    		for (int j = 0; j < numDims; j++) {
    			union += Math.max(dimCoveredByFCs[j], dimCoveredByGCs[j]);
    			intersection += Math.min(dimCoveredByFCs[j], dimCoveredByGCs[j]);
    		}
    	}
    	
    	double RNIA = 1 - (double)(union - intersection) / (double)union;
    	if (debug) System.out.println("RNIA: union = " + union + " / intersection = " + intersection);
    	
    	addSubValue("1.0-RNIA", RNIA);
	}
}
