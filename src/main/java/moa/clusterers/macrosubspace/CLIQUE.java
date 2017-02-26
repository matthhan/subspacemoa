/**
 * [CLIQUE.java] for Subspace MOA
 * 
 * Linked to CLIQUE implementation of OpenSubspace (http://dme.rwth-aachen.de/en/OpenSubspace). 
 * 
 * @editor Yunsu Kim
 * Data Management and Data Exploration Group, RWTH Aachen University
 */
package moa.clusterers.macrosubspace;

import i9.subspace.base.ArffStorage;
import i9.subspace.clique.Cover;

import java.util.ArrayList;
import java.util.List;

import moa.cluster.Clustering;
import moa.cluster.SubspaceClustering;
import moa.cluster.SubspaceSphereCluster;
import moa.options.FloatOption;
import moa.options.IntOption;
import weka.core.Instance;
import weka.core.Instances;

public class CLIQUE extends MacroSubspaceClusterer {

	private static final long serialVersionUID = 1L;

	public IntOption xiOption = new IntOption("xi", 'x', "The number of intervals for each dimension.", 10);
	public FloatOption tauOption = new FloatOption("tau", 't', "Density threshold to determine clusters.", 0.01, 0, 1);
	
	private boolean debug = false;
	
	@Override
	public SubspaceClustering getClusteringResult(Clustering microClustering) {
		if (microClustering == null || microClustering.size() == 0) {
			System.out.println("CLIQUE cannot do macroclustering since given microclustering is null or empty");
			return null;
		}
		
		// Prepare CLIQUE
		Instances data = moaClusteringToWEKAInstances(microClustering);
		ArffStorage arffstorage = new ArffStorage(data);
		int dimensions = data.numAttributes();
		i9.subspace.clique.CLIQUE clique = new i9.subspace.clique.CLIQUE(dimensions, arffstorage, xiOption.getValue(), tauOption.getValue());
		
		// Run!
		List<Cover> covers = null;
		covers = clique.runClustering();

		// Return the result
		return openSubspaceCliqueCoversToMOASubspaceClustering(covers);
	}

	private SubspaceClustering openSubspaceCliqueCoversToMOASubspaceClustering(List<Cover> covers) {
		SubspaceSphereCluster[] converted = new SubspaceSphereCluster[covers.size()];
		for (int i = 0; i < covers.size(); i++) {
			Cover c = covers.get(i);
			List<Instance> clusteredObjects = new ArrayList<Instance>();
			for (Integer obj : c.m_objects) {
				clusteredObjects.add(convertedWekaInstances.get(obj));
			}
			converted[i] = new SubspaceSphereCluster(clusteredObjects, c.m_subspace);
		}
		
		if (debug) {
			System.out.println("CLIQUE: " + converted.length + " subspace clusters");
		}
		
		return new SubspaceClustering(converted);
	}
}
