/**
 * [weightedCFCluster.java] for Subspace MOA
 * 
 * HDDStream: Class that represents the individual Microclusters in an intermediate stage of the processing.
 * Was added in debugging to correct an error in the calculation of Macrocluster Centers
 * 
 * @author Matthias Hansen
 * Data Management and Data Exploration Group, RWTH Aachen University
 */
package moa.clusterers.hddstream;

import weka.core.Instance;
import moa.cluster.CFCluster;

public class weightedCFCluster extends CFCluster {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private double weight;
	public double getWeight() {
		return weight;
	}
	public void setWeight(double weight) {
		this.weight = weight;
	}
	public weightedCFCluster(CFCluster clus,double weight) {
		super(clus);
		this.setWeight(weight);
	}
	@Override
	public CFCluster getCF() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getInclusionProbability(Instance instance) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getRadius() {
		// TODO Auto-generated method stub
		return 0;
	}

}
