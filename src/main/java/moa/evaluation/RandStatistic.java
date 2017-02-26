/**
 * [RandStatistic.java] for Subspace MOA
 * 
 * Evaluation measure: Rand statistic
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

public class RandStatistic extends SubspaceMeasureCollection {

	private static final long serialVersionUID = 1L;
	private boolean debug = false;

	@Override
	protected String[] getNames() {
		return new String[] {"Rand statistic"};
	}
	
	@Override
    protected boolean[] getDefaultEnabled() {
        boolean [] defaults = {true};
        return defaults;
    }

	@Override
	protected void subEvaluateSubspaceClustering(SubspaceClustering foundClustering,
			SubspaceClustering gtClustering,
			List<SubspaceDataPoint> points) throws Exception {
		
		List<Cluster> foundClusters = foundClustering.getClustering();
		List<Cluster> gtClusters = gtClustering.getClustering();
		
        int n = points.size();
        int numFC = foundClusters.size();
        int numGC = gtClusters.size();
		double inclusionProbabilityThreshold = 0.5;
    	
		/* Cluster assignments */
		
		List<List<Integer>> foundClusterAssignments = new ArrayList<List<Integer>>();
		List<List<Integer>> trueClusterAssignments = new ArrayList<List<Integer>>();
    	
    	for (int j = 0; j < n; j++) {
    		SubspaceDataPoint p = points.get(j);
    		foundClusterAssignments.add(new ArrayList<Integer>());
    		trueClusterAssignments.add(new ArrayList<Integer>());
    		
    		int numAssignedFC = 0;
    		for (int i = 0; i < numFC; i++) {
    			Cluster fc = foundClusters.get(i);
    			if (fc.getInclusionProbability(p) >= inclusionProbabilityThreshold) {
    				foundClusterAssignments.get(j).add(i);
    				numAssignedFC++;
    			}
    		}
    		if (numAssignedFC == 0)
    			foundClusterAssignments.get(j).add(-1);
    		
    		int numAssignedGC = 0;
    		for (int i = 0; i < numGC; i++) {
    			Cluster gc = gtClusters.get(i);
    			if (gc.getInclusionProbability(p) >= inclusionProbabilityThreshold) {
    				trueClusterAssignments.get(j).add(i);
    				numAssignedGC++;
    			}
    		}
    		if (numAssignedGC == 0)
    			trueClusterAssignments.get(j).add(-1);
    	}
    	
		
		/* Rand statistic */
    	
    	double N11 = 0, N00 = 0;

    	for (int j = 0; j < n; j++) {
    		for (int k = j + 1; k < n; k++) {
    			//if (j == k) continue;
    			
    			boolean sameFCfound = false;
    			for (int c1 : foundClusterAssignments.get(j)) {
    				for (int c2 : foundClusterAssignments.get(k)) {
    					if (c1 == c2) {
    						sameFCfound = true;
    						break;
    					}
    				}
    				if (sameFCfound) break;
    			}
    			
    			boolean sameGCfound = false;
    			for (int c1 : trueClusterAssignments.get(j)) {
    				for (int c2 : trueClusterAssignments.get(k)) {
    					if (c1 == c2) {
    						sameGCfound = true;
    						break;
    					}
    				}
    				if (sameGCfound) break;
    			}
    			
    			if (sameFCfound && sameGCfound) N11++;
    			if (!sameFCfound && !sameGCfound) N00++;
    		}
    	}
    	
    	
    	double N = n * (n - 1) / 2.0;
    	double rand = (N11 + N00) / N;
    	
    	if (debug) {
    		System.out.println("Rand statistic: N = " + N
        					 + ", N11 = " + N11
        					 + ", N00 = " + N00
        					 + "/ rand = " + rand);
    	}
    	
        addSubValue("Rand statistic", rand);
	}
}
