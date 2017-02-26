/**
 * [MacroSubspaceClusterer.java] for Subspace MOA
 * 
 * Subspace clustering algorithm format for macro-clustering.
 * 
 * @editor Yunsu Kim
 * Data Management and Data Exploration Group, RWTH Aachen University
 */

package moa.clusterers.macrosubspace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import moa.cluster.CFCluster;
import moa.cluster.Cluster;
import moa.cluster.Clustering;
import moa.cluster.SubspaceClustering;
import moa.cluster.SubspaceSphereCluster;
import moa.core.AutoExpandVector;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.tasks.TaskMonitor;

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public abstract class MacroSubspaceClusterer extends AbstractOptionHandler {

	private static final long serialVersionUID = 1L;
	
	protected Instances convertedWekaInstances;
	/**
	 * Convert MOA micro-clusters to WEKA instances.
	 * These instances can be used as input to OpenSubspace clusterers.
	 * 
	 * @param microClustering
	 * @return WEKA Instances
	 */
	//Changed by MTH in 2016 to use covariances instead of pooled covariance.
	//Previously, the regeneration was done using the average of the covariances of all
	//Microclusters. This was done so that the covariance Matrices do not have a zero
	//entry on the diagonal, which cause problems with the Normal distribution.
	//After the change, these problem has been resolved and the actual covariances 
	//can be used as they should.
	protected Instances moaClusteringToWEKAInstances(Clustering microClustering) {
		
		// Parameters for initializing a "Instances"
		String name = "instances";
		ArrayList<Attribute> attInfo = new ArrayList<Attribute>();
		for (int i = 0; i < microClustering.dimension(); i++) {
			attInfo.add(new Attribute("dim" + i));
		}		
		int capacity = microClustering.size();

		// Initialize "Instances"
		Instances instances = new Instances(name, attInfo, capacity);
		
		// Copy info's from "Clustering" to "Instances"
		AutoExpandVector<Cluster> microClusters = microClustering.getClustering();
		
		for (int i = 0; i < microClusters.size(); i++) {
			CFCluster microCluster = (CFCluster) microClusters.get(i);
			double[] stds = standardDeviationsOfMicroCluster(microCluster);

			double[] mean = microCluster.getCenter();
			MultipleNormalDistributions normDist = new MultipleNormalDistributions(mean, stds);
			
			// Reconstruct samples
			for (int a = 0; a < microCluster.getWeight(); a++) {
				double attValues[] = normDist.sample();
				Instance inst = new DenseInstance(1.0D,attValues);
				instances.add(inst);
			}
			Cluster cluster = microClusters.get(i);
			instances.add(new DenseInstance(cluster.getWeight(), cluster.getCenter()));
		}
		CFCluster justOneCluster = (CFCluster) microClusters.get(0);
		double sumDistances = 0;
		for(Instance inst : instances){
			sumDistances += justOneCluster.getCenterDistance(inst);
		}
		convertedWekaInstances = instances;
		return instances;
	}


	
	/**
	 * Convert a list of OpenSubspace clusters to MOA-compatible SubspaceClustering object.
	 * 
	 * @param clusters - list of OpenSubspace clusters
	 * @return MOA-compatible SubspaceClustering object
	 */
	protected SubspaceClustering openSubspaceClustersToMOASubspaceClustering(List<i9.subspace.base.Cluster> clusters) {
		SubspaceSphereCluster[] converted = new SubspaceSphereCluster[clusters.size()];
		for (int i = 0; i < clusters.size(); i++) {
			i9.subspace.base.Cluster c = clusters.get(i);
			List<Instance> clusteredObjects = new ArrayList<Instance>();
			for (Integer obj : c.m_objects) {
				clusteredObjects.add(convertedWekaInstances.get(obj));
			}
			converted[i] = new SubspaceSphereCluster(clusteredObjects, c.m_subspace);
		}
		return new SubspaceClustering(converted);
	}
	
	
	/**
	 * Returns string representation of subspace clusters.
	 * Each line contains subspace, number of objects, and objects of each subspace cluster.
	 * 
	 * @param clusters - list of subspace clusters
	 * @return String representation of subspace clusters
	 */
	protected String openSubspaceClustersToString(List<i9.subspace.base.Cluster> clusters) {
		StringBuffer text = new StringBuffer();

		if ((clusters == null) || (clusters.size() == 0))
			text.append("no clusters");
		else {
			for (int i = 0; i < clusters.size(); i++) {
				text.append("SC_"
						+ i
						+ ": "
						+ ((i9.subspace.base.Cluster) clusters.get(i))
								.toStringWeka());
			}
		}
		return text.toString();
	}
	
	/**
	 * Run the subspace macro-clustering with a given micro-clustering.
	 * 
	 * @param microClustering
	 * @return
	 */
	public abstract SubspaceClustering getClusteringResult(Clustering microClustering);

	
	public void getDescription(StringBuilder sb, int indent) {
			
	}

	@Override
	protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
		
	}
	
	/**
	 * Print details of the given microclustering.
	 */
	protected void printMicroClustering(Clustering microClustering) {
		System.out.println("--- MacroSubspaceClusterer.printMicroClustering() ---");
		int numClusters = microClustering.size();
		System.out.println("#clusters = " + numClusters);
		System.out.println("---//");
	}
	
	/**
	 * Independent covariance of a microcluster.
	 * 
	 * @param c
	 * @return
	 */
	protected double[][] covOfMicroCluster(CFCluster c) {
		int D = c.LS.length;
		double N = c.getN();
		double[][] cov = new double[D][D];
		
		for (int d = 0; d < D; d++) {
			cov[d][d] = c.SS[d] / N - Math.pow(c.LS[d] / N, 2.0D);
		}
		
		return cov;
	}
	protected double[] variancesOfMicroCluster(CFCluster c) {
		int D = c.LS.length;
		double N = c.getN();
		double[] cov = new double[D];
		
		for (int d = 0; d < D; d++) {
			cov[d] = c.SS[d] / N - Math.pow(c.LS[d] / N, 2.0D);
		}
		return cov;
	}
	protected double[] standardDeviationsOfMicroCluster(CFCluster c) {
		double[] res = variancesOfMicroCluster(c);
		for(int i = 0; i < res.length;i++) {
			res[i] = Math.sqrt(res[i]);
		}
		return res;
	}
	
	/**
	 * Pooled covariance of microclusters.
	 * 
	 * @param microClusters
	 * @return
	 */
	protected double[][] pooledCovOfMicroClusters(AutoExpandVector<Cluster> microClusters) {
		int D = ((CFCluster) microClusters.get(0)).LS.length;
		double[] SSsum = new double[D];
		double[] LSsum = new double[D];
		double N = 0.0;
		
		for (int i = 0; i < microClusters.size(); i++) {
			CFCluster microCluster = (CFCluster) microClusters.get(i);
			
			for (int d = 0; d < D; d++) {
				SSsum[d] += microCluster.SS[d];
				LSsum[d] += microCluster.LS[d];
			}
			
			N += microCluster.getN();
		}
		
		double[][] cov = new double[D][D];
		for (int d = 0; d < D; d++) {
			cov[d][d] = SSsum[d] / N - Math.pow(LSsum[d] / N, 2.0D); 
		}
		
		return cov;
	}
}
