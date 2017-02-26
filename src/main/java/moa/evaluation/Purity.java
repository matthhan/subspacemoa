/**
 * [Purity.java] for Subspace MOA
 * 
 * Evaluation measure: Purity
 * 
 * @author Yunsu Kim
 * Data Management and Data Exploration Group, RWTH Aachen University
 */

package moa.evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import moa.cluster.Cluster;
import moa.cluster.SubspaceClustering;
import moa.gui.subspacevisualization.SubspaceDataPoint;

public class Purity extends SubspaceMeasureCollection {

	private static final long serialVersionUID = 1L;
	
	private boolean debug = false;

	@Override
	protected String[] getNames() {
		return new String[] {"Purity"};
	}

	@Override
    protected boolean[] getDefaultEnabled() {
        boolean [] defaults = {true};
        return defaults;
    }
	
	@Override
	protected void subEvaluateSubspaceClustering(SubspaceClustering foundClustering,
											  SubspaceClustering trueClustering,
											  List<SubspaceDataPoint> points) throws Exception {
		HashMap<Double, Integer> labelMap = trueClustering.getLabelMap();
		int numClasses = labelMap.size();
		if (trueClustering.hasNoise()) {
			numClasses--; // w/o noise
		}
		
		if (debug) {
    		System.out.println("Purity: # of true class labels = " + numClasses);
    	}
		
		/** Assign points to found clusters
		  + Calculate purity for each found cluster **/
		
		List<Cluster> foundClusters = foundClustering.getClustering();
		int numFound = foundClusters.size();
		
		List<List<SubspaceDataPoint>> pointsInFC = new ArrayList<List<SubspaceDataPoint>>();
		int[][] classDistFC = new int[numFound][numClasses];
		
		// Initialize
		for (int i = 0; i < numFound; i++) {
    		pointsInFC.add(new ArrayList<SubspaceDataPoint>());
    		for (int c = 0; c < numClasses; c++) {
    			classDistFC[i][c] = 0;
    		}
    	}
		
		double inclusionProbabilityThreshold = 0.5;
    	for (SubspaceDataPoint p : points) {
    		double label = p.classValue();
    		if (label != p.getNoiseLabel()) {
	    		for (int i = 0; i < foundClusters.size(); i++) {
	    			Cluster fc = foundClusters.get(i);
	    			if (fc.getInclusionProbability(p) >= inclusionProbabilityThreshold) {
	    				pointsInFC.get(i).add(p);
	    				classDistFC[i][labelMap.get(label)]++;
	    			}
	    		}
    		}
    	}
    	
    	int numFoundNonEmpty = 0;
    	int[] dominatingIndex = new int[numFound];
    	double[] purityEachFC = new double[numFound];
		double puritySum = 0;
    	for (int i = 0; i < numFound; i++) {
    		if (pointsInFC.get(i).size() > 0) {
    			numFoundNonEmpty++;
    			
	    		int dominating = 0;
	    		for (int c = 1; c < numClasses; c++) {
	    			if (classDistFC[i][c] > classDistFC[i][dominating]) {
	    				dominating = c;
	    			}
	    		}
	    		dominatingIndex[i] = dominating;
	    		purityEachFC[i] = (double) classDistFC[i][dominating] / (double) pointsInFC.get(i).size();
	    		puritySum += purityEachFC[i];
    		}
    	}
    	
    	if (debug) {
    		System.out.print("Purity: found clusters contain = ");
    		for (int i = 0; i < pointsInFC.size(); i++) {
    			System.out.print(pointsInFC.get(i).size() + " ");
    		}
    		System.out.println();
    		
    		System.out.print("Purity: found cluster's dominating label = ");
    		for (int i = 0; i < pointsInFC.size(); i++) {
    			System.out.print(dominatingIndex[i] + " ");
    		}
    		System.out.println();
    		
    		System.out.print("Purity: found cluster's purity = ");
    		for (int i = 0; i < pointsInFC.size(); i++) {
    			System.out.print(purityEachFC[i] + " ");
    		}
    		System.out.println();
    	}
    	
    	double purityFinal = puritySum / (double) numFoundNonEmpty;
    	if (Double.isNaN(purityFinal)) purityFinal = 0.0;
    	if (debug) System.out.println("Purity: total purity = " + purityFinal);
    	
    	addSubValue("Purity", purityFinal);
	}
}
