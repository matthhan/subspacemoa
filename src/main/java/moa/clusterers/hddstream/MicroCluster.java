/**
 * [MicroCluster.java] for Subspace MOA
 * 
 * HDDStream: microcluster class for online processing
 * - weight: sum of weights of points in the microcluster
 * - LS, SS are also weighted
 * 
 * @author Yunsu Kim
 * 		   based on the implementation by Stephan Wels
 * Data Management and Data Exploration Group, RWTH Aachen University
 */

package moa.clusterers.hddstream;

import moa.cluster.CFCluster;
import weka.core.Instance;

public class MicroCluster extends CFCluster {

	private static final long serialVersionUID = 1L;
	
	private boolean debug = false;
	
	protected long creationTimestamp = -1;
	protected long lastEditTimestamp = -1;
       
    protected double lambda;
    protected double weight;
    protected double mu;

    public MicroCluster(double[] center, int dimensions, long creationTimestamp, double lambda, long currentTimestamp, double mu) {
        super(center, dimensions);		// Setting CF1 and CF2
        this.creationTimestamp = creationTimestamp;
        this.lastEditTimestamp = currentTimestamp;
        this.lambda = lambda;
        this.weight = Math.pow(2, -lambda * (currentTimestamp - creationTimestamp));
        this.mu = mu;
    }

    public MicroCluster(Instance instance, int dimensions, long creationTimestamp, double lambda, long currentTimestamp, double mu) {
        this(instance.toDoubleArray(), dimensions, creationTimestamp, lambda, currentTimestamp, mu);
    }

    
    /** Updates **/
    
    /* If it hasn't been updated for a while */
    public void updateForNoHitsUntil(long currentTimestamp) {
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
    
    public void insert(Instance instance, long currentTimestamp){
    	updateForNoHitsUntil(currentTimestamp);
        
    	/* Update for a new instance */
    	weight++;
    	
        for (int j = 0; j < instance.numValues(); j++) {
            LS[j] += instance.value(j);
            SS[j] += instance.value(j) * instance.value(j);
        }
    }    
    

    /** Timestamps **/
    
    public long getCreationTime() {
        return creationTimestamp;
    }
    
    public long getLastEditTimestamp() {
        return lastEditTimestamp;
    }
    
    
    
    /** Weight **/
    
    public double getWeight() {
        return weight;
    }
    
    @Override
    public void setWeight(double newWeight) {
    	weight = newWeight;
    }
    
    
    
    /** Later calculations **/
        
    public double[] getCenter() {
    	double[] center = new double[LS.length];
        for (int j = 0; j < LS.length; j++) {
            center[j] = LS[j] / this.weight;
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
        	return dimBound * 2;
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

    
    /** Auxiliaries **/
    
    public boolean isCore() {
    	return (weight >= mu);
    }
    
    public MicroCluster copy() {
        MicroCluster copy = new MicroCluster(this.LS.clone(), this.LS.length, this.getCreationTime(), this.lambda, this.getLastEditTimestamp(), this.mu);
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

    
    
    /** Deprecated **/
    
	@Deprecated
	public CFCluster getCF() {
		return null;
	}
}
