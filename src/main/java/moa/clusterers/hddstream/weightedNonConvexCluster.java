/**
 * [weightedNonConvexCluster.java] for Subspace MOA
 * 
 * HDDStream: Class that represents the individual Macroclusters.
 * Was added in debugging to correct an error in the calculation of Macrocluster Centers
 * 
 * @author Matthias Hansen
 * Data Management and Data Exploration Group, RWTH Aachen University
 */
//Class was added by mth in 2015 to correctly handle weights of macroclusters.
//Weights were previously thrown away for Macroclusters. This was an issue because the
//Centers of CFClusters can only be computed correctly by using their weight and linear
//sum. However, the old code used the linear sum and N, where N was the number of microclusters that they were composed of
package moa.clusterers.hddstream;

import java.util.List;

import moa.cluster.CFCluster;
import moa.clusterers.macro.NonConvexCluster;

public class weightedNonConvexCluster extends NonConvexCluster {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5678743813133140978L;
	private double weight;
	
	public double getWeight() {
		return this.weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	@SuppressWarnings("unchecked")
	public weightedNonConvexCluster(weightedCFCluster clus,List<weightedCFCluster> lis) {
		super((CFCluster)clus,(List<CFCluster>)(List<?>)lis);
		this.setWeight(clus.getWeight());
		for(weightedCFCluster clus1 : lis) {
			this.setWeight(this.getWeight()+clus1.getWeight());
		}
	}
	public void add(CFCluster cluster) {
		super.add(cluster);
		this.setWeight(this.getWeight()+cluster.getWeight());
	}
	public void remove(CFCluster cluster) {
		super.remove(cluster);
		this.setWeight(this.getWeight()-cluster.getWeight());
	}
	 public double[] getCenter() {
		 double res[] = new double[this.LS.length];
		 for ( int i = 0; i < res.length; i++ ) {
			 res[i] = this.LS[i] / this.getWeight();
		 }
		 return res;
	 }
}
