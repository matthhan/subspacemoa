/**
 * [F1Subspace.java] for Subspace MOA
 * 
 * Evaluation measure: F1
 * 
 * @author Yunsu Kim
 * Data Management and Data Exploration Group, RWTH Aachen University
 */

package moa.evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import moa.cluster.Cluster;
import moa.cluster.SubspaceClustering;
import moa.gui.subspacevisualization.SubspaceDataPoint;

public class F1Subspace extends SubspaceMeasureCollection {

	private static final long serialVersionUID = 1L;
	
	@Override
	protected String[] getNames() {
		String[] names = {"F1"};
		return names;
	}
	
	@Override
    protected boolean[] getDefaultEnabled() {
        boolean [] defaults = {true};
        return defaults;
    }

	@Override
	protected void subEvaluateSubspaceClustering(SubspaceClustering foundClustering, SubspaceClustering gtClustering, List<SubspaceDataPoint> points) throws Exception {
		
		HashMap<Double, Integer> labelMap = gtClustering.getLabelMap();
		int numClasses = labelMap.size();
		if (labelMap.containsKey(-1.0)) {
			numClasses--; // w/o noise
		}
		
		// Assign points to found clusters
		List<Cluster> foundClusters = foundClustering.getClustering();
		List<List<SubspaceDataPoint>> pointsInFC = new ArrayList<List<SubspaceDataPoint>>();
		
		for (Cluster fc : foundClusters) {
    		pointsInFC.add(new ArrayList<SubspaceDataPoint>());
    	}
		
		double inclusionProbabilityThreshold = 0.5;
    	for (SubspaceDataPoint p : points) {
    		for (int i = 0; i < foundClusters.size(); i++) {
    			Cluster fc = foundClusters.get(i);
    			if (fc.getInclusionProbability(p) >= inclusionProbabilityThreshold) {
    				pointsInFC.get(i).add(p);
    			}
    		}
    	}
    	
    	
    	// Count the objects for each label (GROUND TRUTH)
    	List<List<SubspaceDataPoint>> pointsForLabel = new ArrayList<List<SubspaceDataPoint>>();
    	
    	for (int m = 0; m < numClasses; m++) {
    		pointsForLabel.add(new ArrayList<SubspaceDataPoint>());
    	}
    	for (int i = 0; i < points.size(); i++) {
    		SubspaceDataPoint p = points.get(i);
			for (double label : p.getClassLabelSet()) {
				if (label == p.getNoiseLabel())		// Don't count noise
					continue;
				else
					pointsForLabel.get(labelMap.get(label)).add(p);
			}
    	}
    	
    			
		/** Class-clusters mapping **/
    	List<Set<SubspaceDataPoint>> mappedPointsForLabel = new ArrayList<Set<SubspaceDataPoint>>();
    	
    	for (int m = 0; m < numClasses; m++) {
    		mappedPointsForLabel.add(new HashSet<SubspaceDataPoint>());
    	}
    	
    	for (int i = 0; i < pointsInFC.size(); i++) {
    		int[] classDistributionFC = new int[numClasses];
    		
    		for (SubspaceDataPoint p : pointsInFC.get(i)) {
    			for (double label : p.getClassLabelSet()) {
    				if (label == -1.0)		// Don't count noise
    					continue;
    				else {
    					classDistributionFC[labelMap.get(label)]++;
    				}
    			}
    		}
    		
    		double maxCovered = 0.0;
    		int maxCoveredIndex = -1;
    		for (int m = 0; m < numClasses; m++) {
    			double covered = (double)classDistributionFC[m] / (double)pointsForLabel.get(m).size();
    			if (covered > maxCovered) {
    				maxCovered = covered;
    				maxCoveredIndex = m;
    			}
    		}

    		if (maxCoveredIndex != -1)
    			mappedPointsForLabel.get(maxCoveredIndex).addAll(pointsInFC.get(i));
    	}

    	

		
		//F-value berechnen pro AusgabeCluster
		double[] m_F1_values = new double[numClasses];
		double[] m_precision = new double[numClasses];
		double[] m_recall = new double[numClasses];
		double m_F1 = 0.0;
		
		for (int m = 0; m < numClasses; m++){
			int intersect = 0;
			
			for (SubspaceDataPoint p : pointsForLabel.get(m)) {
				if (mappedPointsForLabel.get(m).contains(p))
					intersect++;
			}
			
			m_precision[m] = 0;
			m_recall[m] = 0;
			
			if (mappedPointsForLabel.get(m).size() == 0 && pointsForLabel.get(m).size() == 0) {
				m_F1_values[m] = 0;
			} else {
				m_F1_values[m] = 2 * (double)intersect / (mappedPointsForLabel.get(m).size() + pointsForLabel.get(m).size());
				if (mappedPointsForLabel.get(m).size() != 0) m_precision[m] = (double)intersect / mappedPointsForLabel.get(m).size();
				if (pointsForLabel.get(m).size() != 0) m_recall[m] = (double)intersect / pointsForLabel.get(m).size();
			}

			m_F1 += m_F1_values[m];
		}
		m_F1 = m_F1 / numClasses;
		
		addSubValue("F1", m_F1);
	}
}
