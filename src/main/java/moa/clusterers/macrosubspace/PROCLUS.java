/**
 * [PROCLUS.java] for Subspace MOA
 * 
 * Linked to PROCLUS implementation of OpenSubspace (http://dme.rwth-aachen.de/en/OpenSubspace). 
 * 
 * @editor Yunsu Kim
 * Data Management and Data Exploration Group, RWTH Aachen University
 */
package moa.clusterers.macrosubspace;

import i9.subspace.proclus.Proclus;

import java.util.List;

import moa.cluster.Clustering;
import moa.cluster.SubspaceClustering;
import moa.options.IntOption;
import weka.core.Instances;

public class PROCLUS extends MacroSubspaceClusterer {
	
	private static final long serialVersionUID = 1L;
	
	private boolean debug = false;
	
	public IntOption numOfClustersOption = new IntOption("numOfClusters", 'c', "Desired number of clusters for the result.", 5);
	public IntOption avgDimensionsOption = new IntOption("avgDimensions", 'd', "Average dimensional size of the subspace clusters.", 4);

	@Override
	public SubspaceClustering getClusteringResult(Clustering microClustering) {
		if (microClustering == null || microClustering.size() == 0) {
			System.out.println("PROCLUS cannot do macroclustering since given microclustering is null or empty");
			return null;
		}
		
		// Prepare Proclus
		Instances data = moaClusteringToWEKAInstances(microClustering);
		Proclus proclus = new Proclus(data, numOfClustersOption.getValue(), avgDimensionsOption.getValue());
		
		// Run!
		List<i9.subspace.base.Cluster> clusters = null;
		try { proclus.runClustering(); } catch(Exception e) { e.printStackTrace(); };
		clusters = proclus.getClustering();
		if (debug) {
			System.out.println();
			System.out.println("-- PROCLUS Results --");
			System.out.println(openSubspaceClustersToString(clusters));
			System.out.println();
		}		

		// Return the result
		return openSubspaceClustersToMOASubspaceClustering(clusters);
	}
}
