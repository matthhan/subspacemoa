/**
 * [SUBCLU.java] for Subspace MOA
 * 
 * Linked to SUBCLU implementation of OpenSubspace (http://dme.rwth-aachen.de/en/OpenSubspace). 
 * 
 * @editor Yunsu Kim
 * Data Management and Data Exploration Group, RWTH Aachen University
 */

package moa.clusterers.macrosubspace;

import i9.subspace.base.ArffStorage;

import java.util.List;

import moa.cluster.Clustering;
import moa.cluster.SubspaceClustering;
import moa.options.FloatOption;
import moa.options.IntOption;
import weka.core.Instances;

public class SUBCLU extends MacroSubspaceClusterer {
	
	private static final long serialVersionUID = 1L;
	
	public FloatOption epsilonOption = new FloatOption("epsilon", 'e', "epsilon", 0.1D);
	public IntOption minSupportOption = new IntOption("minSupport", 's', "minSupport", 4);
	public IntOption minOutputDimOption = new IntOption("minOutputDim", 'd', "minOutputDim", 1);

	@Override
	public SubspaceClustering getClusteringResult(Clustering microClustering) {
		if (microClustering == null || microClustering.size() == 0) {
			System.out.println("SUBCLU cannot do macroclustering since given microclustering is null or empty");
			return null;
		}
		
		// Prepare SUBCLU
		Instances data = moaClusteringToWEKAInstances(microClustering);
		ArffStorage arffstorage = new ArffStorage(data);
		int dimensions = data.numAttributes();
		i9.subspace.subclu.SUBCLU subclu = new i9.subspace.subclu.SUBCLU(dimensions, epsilonOption.getValue(), minSupportOption.getValue(), arffstorage, minOutputDimOption.getValue());

		// Run!
		List<i9.subspace.base.Cluster> clusters = null;
		clusters = subclu.runClustering();

		// Return the result
		return openSubspaceClustersToMOASubspaceClustering(clusters);
	}
}