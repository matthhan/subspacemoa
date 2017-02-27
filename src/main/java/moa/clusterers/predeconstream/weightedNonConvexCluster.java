package moa.clusterers.predeconstream;

/**
 * [weightedNonConvexCluster.java] for Subspace MOA
 * 
 * PreDeConStream: Class that represents the individual Macroclusters.
 * Was added in debugging to correct an error in the calculation of Macrocluster Centers
 * 
 * @author Matthias Hansen
 * Data Management and Data Exploration Group, RWTH Aachen University
 */
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
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	@SuppressWarnings("unchecked")
	public weightedNonConvexCluster(MicroCluster clus,List<MicroCluster> lis) {
		super((CFCluster)clus,(List<CFCluster>)(List<?>)lis);
		this.setWeight(clus.getWeight());
		for(MicroCluster clus1 : lis) {
			this.setWeight(this.getWeight()+clus1.getWeight());
		}
	}
	public void add(CFCluster cluster) {
		super.add(cluster);
		this.setWeight(this.getWeight()+cluster.getWeight());
	}
	 public double[] getCenter() {
		 double res[] = new double[this.LS.length];
		 for ( int i = 0; i < res.length; i++ ) {
			 res[i] = this.LS[i] / this.getWeight();
		 }
		 return res;
	 }
}
