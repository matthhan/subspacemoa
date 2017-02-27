/**
 * [MicroCluster.java] for Subspace MOA
 * 
 * PreDeConStream: microcluster class for online processing
 * - weight: sum of weights of points in the microcluster
 * - LS, SS are also weighted
 * 
 * @author Yunsu Kim
 * 		   based on the implementation by Stephan Wels
 * Data Management and Data Exploration Group, RWTH Aachen University
 */

package moa.clusterers.predeconstream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import moa.cluster.CFCluster;
import weka.core.Instance;

public class MicroCluster extends CFCluster {

	private static final long serialVersionUID = 1L;
	
	private boolean debug = false;
	
	protected long creationTimestamp = -1;
	protected long lastEditTimestamp = -1;

	/* Online parameters */
	protected double epsilon;
    protected double muN;
    protected double lambda;	// Decaying factor
    
    /* Offline parameters */
    protected double offlineFactor;
    protected double muF;
    protected double delta;
	protected double kappa;
	protected int tau;			// Max. number of relevant dimensions
	
	/* Weight (redefined) */
	protected double weight;
    
    /** Status **/
	protected int status;
	protected final int STATUS_UNCLASSIFIED = 0,
						STATUS_CLASSIFIED = 1,
						STATUS_NOISE = 2;
	
	/** Subspace **/
	protected final int DIM_RELEVANT = 1;
	protected final int DIM_IRRELEVANT = 0;
	protected int numDim;
	private int numRelDim;
	private double[] dimVariance;
	protected double[] subspacePrefVector;
	
	/** Neighborhoods **/
	protected List<MicroCluster> neighborhood;
	protected List<MicroCluster> weightedNeighborhood;
	private double weightSumOfWeightedNeighborhood;
	
		
	/* Constructor */
    public MicroCluster(double[] center,
    					double epsilon, double muN, double lambda,
    					double offlineFactor, double muF, double delta, double kappa, int tau,
    					long creationTimestamp, long currentTimestamp) {
        super(center, center.length);		// Setting CF1 and CF2
        this.creationTimestamp = creationTimestamp;
        this.lastEditTimestamp = currentTimestamp;
        
        this.epsilon = epsilon;
        this.muN = muN;
        this.lambda = lambda;
        
        this.offlineFactor = offlineFactor;
        this.muF = muF;
		this.delta = delta;
		this.kappa = kappa;
		this.tau = tau;
		
        this.weight = Math.pow(2, -lambda * (currentTimestamp - creationTimestamp));
        
		status = STATUS_UNCLASSIFIED;
		
		numDim = LS.length;
		subspacePrefVector = new double[numDim];
		dimVariance = new double[numDim];
		
		neighborhood = new ArrayList<MicroCluster>();
		weightedNeighborhood = new ArrayList<MicroCluster>();
		
    }
	public MicroCluster(CFCluster clus,double weight) {
		super(clus);
		this.setWeight(weight);
	}

    
    /** ------------ MICROCLUSTER MAINTENANCE ------------ **/
    
    public void updateForNoHitsUntil(long currentTimestamp) {	/* If it hasn't been updated for a while */
    	if (lastEditTimestamp < currentTimestamp) {
    		long dt = currentTimestamp - lastEditTimestamp;
    		double decayingFactor = Math.pow(2, -lambda * dt);
    		
    		weight *= decayingFactor;
    		for (int j = 0; j < LS.length; j++) {
        		LS[j] *= decayingFactor;
        		SS[j] *= decayingFactor;
        	}
    		
        	lastEditTimestamp = currentTimestamp;
        } else if (lastEditTimestamp > currentTimestamp) {
        	System.out.println("PreDeConStream: MicroCluster.getWeight() => "
					 + "ERROR: current timestamp is smaller than the last edited timestamp");
        	return;
        } else {
        	// lastEditTimestamp == currentTimestamp
        	// (don't need any updates)
        }
    }
    
    public void insert(Instance instance, long currentTimestamp) {
    	updateForNoHitsUntil(currentTimestamp);
        
    	/* Update for a new instance */
    	weight++;
    	
        for (int j = 0; j < instance.numValues(); j++) {
            LS[j] += instance.value(j);
            SS[j] += instance.value(j) * instance.value(j);
        }
    }    
    
    
    /** Later calculations (based on LS, SS) **/
    
    public double[] getCenter() {
    	double[] center = new double[LS.length];
        for (int j = 0; j < LS.length; j++) {
            center[j] = LS[j] / weight;
        }
        
        return center;
    }

    public double getRadius() {
    	double dimBound = Double.MIN_VALUE;
        
        for (int j = 0; j < SS.length; j++) {
            double temp = SS[j] / weight - Math.pow(LS[j] / weight, 2);
            
            if (temp >= 0) {
            	double sqrted = Math.sqrt(temp);
            	if (sqrted > dimBound) {
            		dimBound = sqrted;
            	}
            }
        }
    	
        if (dimBound > 0) {
        	return dimBound;	// Factor needed?
        } else {
	    	double LSnorm = 0, SSnorm = 0;
	        
	        for (int j = 0; j < LS.length; j++) {
	            LSnorm += LS[j] * LS[j];
	            SSnorm += SS[j] * SS[j];
	        }
	        
	        LSnorm = Math.sqrt(LSnorm);
	        SSnorm = Math.sqrt(SSnorm);
	        
	        double radiusSq = Math.abs(SSnorm / weight - Math.pow(LSnorm / weight, 2));
	        double radius = Math.sqrt(radiusSq);
	        if (debug) {
	        	System.out.println("MicroCluster: radius = " + radius);
	        	System.out.println("MicroCluster: radiusSq = " + radiusSq);
	        }
	        
	        return radius;
        }
    }

    
    
    /** ------------ FOR OFFLINE CLUSTERING ------------ **/
        
	/**
	 * Calculate epsilon-neighborhood and decide for every dimension if it's relevant or not.
	 * 
	 * @param otherPoints
	 */
	public void preprocess(List<MicroCluster> otherPoints) {
		neighborhood = findNeighborhood(otherPoints);
		dimVariance = dimVarianceInsideNeighborhood(neighborhood);
		subspacePrefVector = preferenceWeights(dimVariance);
		weightedNeighborhood = findWeightedNeighborhood(otherPoints);
		weightSumOfWeightedNeighborhood = weightSum(weightedNeighborhood);
	}


	/**
	 * Calculate the neighborhood of this point, among the 'candidates'.
	 *  
	 * @param candidates - neighborhood candidates (MicroClusters)
	 * @return
	 */
	private List<MicroCluster> findNeighborhood(List<MicroCluster> candidates) {
		List<MicroCluster> inRange = new ArrayList<MicroCluster>();
		for (MicroCluster mc : candidates) {
			if (this.distance(mc) <= epsilon * offlineFactor) {
				inRange.add(mc);
			}
		}
		return inRange;
	}

	/**
	 * Calculates the variance for every dimension... of neighborhood.
	 */
	private double[] dimVarianceInsideNeighborhood(List<MicroCluster> neighbors) {
		double dist;

		double[] center = getCenter();
		double[] variances = new double[numDim];
		
		// For every dimension...
		for (int j = 0; j < numDim; j++) {
			dist = 0;
			double diff = 0;
			for (int k = 0; k < neighbors.size(); k++) {
				MicroCluster neighbor = neighbors.get(k);
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
	private double[] preferenceWeights(double[] neighborhoodVariance) {
		numRelDim = 0;
		double[] weights = new double[numDim];
		
		for (int i = 0; i < neighborhoodVariance.length; i++) {
			if (neighborhoodVariance[i] > delta) {
				weights[i] = 1;
			} else {
				weights[i] = kappa;
				numRelDim++;
			}
		}
		
		if (debug) {
			System.out.println("MicroCluster: NumRelDim = " + numRelDim);
		}
		
		return weights;
	}
	
	/**
	 * Creates the preference weighted epsilon-neighborhood.
	 * 
	 * @param candidates
	 */
	private List<MicroCluster> findWeightedNeighborhood(List<MicroCluster> candidates) {
		List<MicroCluster> inWeightedRange = new ArrayList<MicroCluster>();

		for (MicroCluster p : candidates) {
			double weightedDist = prefWeightedDist(p);
			if (weightedDist <= epsilon * offlineFactor) {
				inWeightedRange.add(p);
			}
		}
		
		if (debug) {
			System.out.println("MicroCluster, #epsilonWeightedNeighborhood = " + inWeightedRange.size());
		}
		
		return inWeightedRange;
	}
	
	private double weightSum(List<MicroCluster> points) {
		double sum = 0;
		
		for (MicroCluster p : points) {
			sum += p.getWeight();
		}
		
		return sum;
	}

	/**
	 * Distance to other 'object', weighted by subspace preference vector. 
	 * 
	 * @param object
	 * @return
	 */
	public double prefWeightedDist(MicroCluster object) {
		double first, second;
		first = asymmetricDistance(this, object);
		second = asymmetricDistance(object, this);
		return Math.max(first, second);
	}
	
	protected double asymmetricDistance(MicroCluster first, MicroCluster second) {
		double distance = 0d;
		double[] center1 = first.getCenter();
		double[] center2 = second.getCenter();

		for (int i = 0; i < first.numDim; i++) {
			double diff = center1[i] - center2[i];
			distance += first.subspacePrefVector[i] * diff * diff;		// Weighting
		}
		
		return Math.sqrt(distance);
	}
	
	private double distance(MicroCluster o) {
		return distance(this.getCenter(), o.getCenter());
	}

	private double distance(double[] center, double[] center2) {
		double d = 0D;
		for (int i = 0; i < center.length; i++) {
			d += Math.pow((center[i] - center2[i]), 2);
		}
		return Math.sqrt(d);
	}
	
	
	
	
	
	
	
	/** Properties **/
    
    public boolean isCoreMicroCluster() {
    	return (weight >= muN);
    }
    
    public boolean isPrefCorePoint() {
		// Either Pot/Core MC can be a core point 
		if (weightSumOfWeightedNeighborhood < muN) {
			return false;
		} else if (numRelDim > tau) {
			return false;
		} else {
			return true;
		}
		
		// Base microcluster should be a core-microcluster
		/*if (((MicroCluster) mCluster).isCore()) {
			if (weightSumOfWeightedNeighborhood < mu) {
				return false;
			} else if (numRelDim > tau) {
				return false;
			} else {
				return true;
			}
		} else {
			return false;
		}*/
	}
    
	public boolean isUnclassified() {
		return (status == STATUS_UNCLASSIFIED);
	}

	public boolean isNoise() {
		return (status == STATUS_NOISE);
	}

	public void setClassified() {
		status = STATUS_CLASSIFIED;
	}
	
	public void setUnclassified() {
		status = STATUS_UNCLASSIFIED;
	}

	public void markAsNoise() {
		status = STATUS_NOISE;
	}

	
	

	
	
	
	
	
	
	/** Getters & Setters **/
    
    public long getCreationTime() {
        return creationTimestamp;
    }
    
    public long getLastEditTimestamp() {
        return lastEditTimestamp;
    }
    
    public double getWeight() {
        return weight;
    }
    
    @Override
    public void setWeight(double newWeight) {
    	weight = newWeight;
    }
    
    public int getNumRelDim() {
		return numRelDim;
	}
    
	public List<MicroCluster> getWeightedNeighborhood() {
		List<MicroCluster> copy = new ArrayList<MicroCluster>();
		copy.addAll(weightedNeighborhood);
		return copy;
	}

    public double[] getSubspacePrefVector() {
    	return subspacePrefVector.clone();
    }
    
    
    
	/* Auxiliaries */
    
    public MicroCluster copy() {
        MicroCluster copy = new MicroCluster(this.LS.clone(),
        									 this.epsilon, this.muN, this.lambda,
        									 this.offlineFactor, this.muF, this.delta, this.kappa, this.tau,
        									 this.getCreationTime(), this.getLastEditTimestamp());
        copy.weight = this.weight;
        return copy;
    }

    @Override
    public double getInclusionProbability(Instance instance) {
        if (getCenterDistance(instance) <= getRadius()) {
            return 1.0;
        } else {
        	return 0.0;
        }
    }
		
	public String toString() {
		return super.toString() + ", [ " + Arrays.toString(subspacePrefVector) + " ]";
	}
    
	
	
    /** Deprecated **/
    
	@Deprecated
	public CFCluster getCF() {
		return null;
	}
}
