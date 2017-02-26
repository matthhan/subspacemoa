/**
 * [P3C.java] for Subspace MOA
 * 
 * Linked to P3C implementation of OpenSubspace (http://dme.rwth-aachen.de/en/OpenSubspace). 
 * 
 * @editor Yunsu Kim
 * Data Management and Data Exploration Group, RWTH Aachen University
 */
package moa.clusterers.macrosubspace;

import java.util.List;

import moa.cluster.Clustering;
import moa.cluster.SubspaceClustering;
import moa.options.FloatOption;
import moa.options.IntOption;
import weka.core.Instances;

public class P3C extends MacroSubspaceClusterer {

	private boolean debug = false;
	private static final long serialVersionUID = 1L;
	
	public IntOption poissonThresholdOption = new IntOption("poissonThreshold", 'p', "poissonThreshold (10^-p scale)", 10);
	public FloatOption chiSquareAlphaOption = new FloatOption("chiSquareAlpha", 'c', "chiSquareAlpha", 0.001D);
		
	@Override
	public SubspaceClustering getClusteringResult(Clustering microClustering) {
		if (microClustering == null || microClustering.size() == 0) {
			System.out.println("P3C cannot do macroclustering since given microclustering is null or empty");
			return null;
		}
		
		// Prepare P3C
		Instances data = moaClusteringToWEKAInstances(microClustering);
		i9.subspace.p3c.P3C p3c = new i9.subspace.p3c.P3C(data, poissonThresholdOption.getValue(), chiSquareAlphaOption.getValue());

		// Run!
		List<i9.subspace.base.Cluster> clusters = null;
		clusters = p3c.runClustering();
		if (debug) {
			System.out.println("P3C found: " + clusters.size() + " clusters");
			System.out.print("=> Each contains (#points):");
			for (int i = 0; i < clusters.size(); i++)
				System.out.print(" " + clusters.get(i).m_objects.size());
			System.out.println();
		}

		// Return the result
		SubspaceClustering result = openSubspaceClustersToMOASubspaceClustering(clusters);
		if (debug) {
			System.out.println("SubspaceClustering result contains: " + result.getClustering().size() + " SubspaceSphereCluster objects");
		}
		return result;
	}
}
