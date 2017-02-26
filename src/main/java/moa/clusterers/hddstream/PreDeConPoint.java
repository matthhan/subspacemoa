/**
 * [PreDeConPoint.java] for Subspace MOA
 * 
 * HDDStream: A point for PreDeCon
 * 			  - based on an instance/microcluster
 * 
 * @author Yunsu Kim (yunsu.kim@cs.rwth-aachen.de)
 * 		   based on the implementation by Pascal Spaus
 * Data Management and Data Exploration Group, RWTH Aachen University
 */

package moa.clusterers.hddstream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import moa.cluster.CFCluster;
import weka.core.Instance;

public class PreDeConPoint {

	private boolean debug = false;
	
	/** Status **/
	protected int status;
	protected final int STATUS_UNCLASSIFIED = 0,
						STATUS_CLASSIFIED = 1,
						STATUS_NOISE = 2;
	
	/** Base (either one of them) **/
	protected CFCluster mCluster;
	protected Instance mInstance;
	
	/** Subspace **/
	protected final int DIM_RELEVANT = 1;
	protected final int DIM_IRRELEVANT = 0;
	protected int numDim;
	private int numRelDim;
		
	private double[] dimVariance;
	protected int[] subspacePrefVector;

	/** Neighborhoods **/
	protected List<PreDeConPoint> neighborhood;
	protected List<PreDeConPoint> weightedNeighborhood;
	private double weightSumOfWeightedNeighborhood;
	
	/** Input parameters inherited from PreDeCon main class **/
	protected double epsilon;
	protected int mu;
	protected double delta;
	protected int lambda;
	protected int kappa;


	public PreDeConPoint(CFCluster mc, double epsilon, int mu,
						 int lambda, double delta, int kappa) {
		mCluster = mc;
		mInstance = null;
		status = STATUS_UNCLASSIFIED;
		
		numDim = mc.LS.length;
		subspacePrefVector = new int[numDim];
		dimVariance = new double[numDim];
		neighborhood = new ArrayList<PreDeConPoint>();
		weightedNeighborhood = new ArrayList<PreDeConPoint>();
		
		this.mu = mu;
		this.epsilon = epsilon;
		this.delta = delta;
		this.kappa = kappa;
		this.lambda = lambda;
	}
	
	public PreDeConPoint(Instance inst, double epsilon, int mu,
						 int lambda, double delta, int kappa) {
		mCluster = null;
		mInstance = inst;
		status = STATUS_UNCLASSIFIED;
		
		numDim = inst.numAttributes();
		subspacePrefVector = new int[numDim];
		dimVariance = new double[numDim];
		neighborhood = new ArrayList<PreDeConPoint>();
		weightedNeighborhood = new ArrayList<PreDeConPoint>();
		
		this.mu = mu;
		this.epsilon = epsilon;
		this.delta = delta;
		this.kappa = kappa;
		this.lambda = lambda;
	}

	
	/* Neighborhood and subspace preference */

	/**
	 * Calculate epsilon-neighborhood and decide for every dimension if it's relevant or not.
	 * 
	 * @param otherPoints
	 */
	public void preprocess(List<PreDeConPoint> otherPoints) {
		neighborhood = findNeighborhood(otherPoints);
		dimVariance = dimVarianceInsideNeighborhood(neighborhood);
		subspacePrefVector = preferenceWeights(dimVariance);
		weightedNeighborhood = findWeightedNeighborhood(otherPoints);
		weightSumOfWeightedNeighborhood = weightSum(weightedNeighborhood);
	}


	/**
	 * Calculate the neighborhood of this point, among the 'candidates'.
	 *  
	 * @param candidates - neighborhood candidates (PreDeConPoints)
	 * @return
	 */
	private List<PreDeConPoint> findNeighborhood(List<PreDeConPoint> candidates) {
		List<PreDeConPoint> inRange = new ArrayList<PreDeConPoint>();
		for (PreDeConPoint mc : candidates) {
			if (this.distance(mc) <= epsilon) {
				inRange.add(mc);
			}
		}
		return inRange;
	}

	/**
	 * Calculates the variance for every dimension... of neighborhood.
	 */
	private double[] dimVarianceInsideNeighborhood(List<PreDeConPoint> neighbors) {
		double dist;

		double[] center = getCenter();
		double[] variances = new double[numDim];
		
		// For every dimension...
		for (int j = 0; j < numDim; j++) {
			dist = 0;
			double diff = 0;
			for (int k = 0; k < neighbors.size(); k++) {
				PreDeConPoint neighbor = neighbors.get(k);
				diff = center[j] - neighbor.getCenter()[j];
				dist += Math.pow(diff, 2);
			}
			variances[j] = dist / neighbors.size();
		}
		return variances;
	}

	/**
	 * Construct the subspace preference vector. 
	 * 
	 * @param neighborhoodVariance
	 * @return
	 */
	private int[] preferenceWeights(double[] neighborhoodVariance) {
		numRelDim = 0;
		int[] weights = new int[numDim];
		
		for (int i = 0; i < neighborhoodVariance.length; i++) {
			if (neighborhoodVariance[i] > delta) {
				weights[i] = 1;
			} else {
				weights[i] = kappa;
				numRelDim++;
			}
		}
		
		if (debug) {
			System.out.println("PreDeConMicroCluster: NumRelDim = " + numRelDim);
		}
		
		return weights;
	}
	
	/**
	 * Creates the preference weighted epsilon-neighborhood.
	 * 
	 * @param candidates
	 */
	private List<PreDeConPoint> findWeightedNeighborhood(List<PreDeConPoint> candidates) {
		List<PreDeConPoint> inWeightedRange = new ArrayList<PreDeConPoint>();

		for (PreDeConPoint p : candidates) {
			double weightedDist = prefWeightedDist(p);
			if (weightedDist <= epsilon) {
				inWeightedRange.add(p);
			}
		}
		
		if (debug) {
			System.out.println("PreDeConMicroCluster, #epsilonWeightedNeighborhood = " + inWeightedRange.size());
		}
		
		return inWeightedRange;
	}
	
	private double weightSum(List<PreDeConPoint> points) {
		double sum = 0;
		
		for (PreDeConPoint p : points) {
			sum += p.getWeight();
		}
		
		return sum;
	}

	
	
	/* Point properties */
	
	public boolean isCore() {
		// Either Pot/Core MC can be a core point 
		if (weightSumOfWeightedNeighborhood < mu) {
			return false;
		} else if (numRelDim > lambda) {
			return false;
		} else {
			return true;
		}
		
		// Base microcluster should be a core-microcluster
		/*if (((MicroCluster) mCluster).isCore()) {
			if (weightSumOfWeightedNeighborhood < mu) {
				return false;
			} else if (numRelDim > lambda) {
				return false;
			} else {
				return true;
			}
		} else {
			return false;
		}*/
	}
	
	
	/* Getters & Setters */

	public boolean isUnclassified() {
		return (status == STATUS_UNCLASSIFIED);
	}

	public boolean isNoise() {
		return (status == STATUS_NOISE);
	}

	public void setClassified() {
		status = STATUS_CLASSIFIED;
	}

	public void markAsNoise() {
		status = STATUS_NOISE;
	}

	public CFCluster getCFCluster() {
		return mCluster;
	}
	
	public Instance getInstance() {
		return mInstance;
	}
	
	public double[] getCenter() {
		if (mCluster != null) {
			return mCluster.getCenter();
		} else if (mInstance != null) {
			return mInstance.toDoubleArray();
		} else {
			return null;
		}
	}

	public double getRadius() {
		if (mCluster != null) {
			return mCluster.getRadius();
		} else {
			return Double.NaN;
		}
	}
	
	public double getWeight() {
		if (mCluster != null) {
			return mCluster.getWeight();
		} else if (mInstance != null) {
			return mInstance.weight();
		} else {
			return Double.NaN;
		}
	}

	public List<PreDeConPoint> getWeightedNeighborhood() {
		List<PreDeConPoint> copy = new ArrayList<PreDeConPoint>();
		copy.addAll(weightedNeighborhood);
		return copy;
	}
	
	public int getNumRelDim() {
		return numRelDim;
	}

	
	
	/* Auxiliaries: Distance functions */
	
	/**
	 * Distance to other 'object', weighted by subspace preference vector. 
	 * 
	 * @param object
	 * @return
	 */
	public double prefWeightedDist(PreDeConPoint object) {
		double first, second;
		first = asymmetricDistance(this, object);
		second = asymmetricDistance(object, this);
		return Math.max(first, second);
	}
	
	protected double asymmetricDistance(PreDeConPoint first, PreDeConPoint second) {
		double distance = 0d;
		double[] center1 = first.getCenter();
		double[] center2 = second.getCenter();

		for (int i = 0; i < first.numDim; i++) {
			double diff = center1[i] - center2[i];
			distance += first.subspacePrefVector[i] * diff * diff;		// Weighting
		}
		
		return Math.sqrt(distance);
	}
	
	private double distance(PreDeConPoint o) {
		return distance(this.getCenter(), o.getCenter());
	}

	private double distance(double[] center, double[] center2) {
		double d = 0D;
		for (int i = 0; i < center.length; i++) {
			d += Math.pow((center[i] - center2[i]), 2);
		}
		return Math.sqrt(d);
	}
	
	
	/* Miscellaneous */
	
	public String toString() {
		return super.toString() + ", [ " + Arrays.toString(subspacePrefVector) + " ]";
	}
}
