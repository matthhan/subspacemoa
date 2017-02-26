/**
 * [ProjectedMicroCluster.java] for Subspace MOA
 * 
 * HDDStream: projected microcluster class for online processing
 * - weight: sum of weights of points in the microcluster
 * - LS, SS are also weighted
 * 
 * @author Yunsu Kim (yunsu.kim@rwth-aachen.de)
 * 		   based on the implementation by Stephan Wels
 * Data Management and Data Exploration Group, RWTH Aachen University
 */

package moa.clusterers.hddstream;

import weka.core.Instance;

public class ProjectedMicroCluster extends MicroCluster {

	private static final long serialVersionUID = 1L;
	
	private boolean debug = false;
	
	protected double delta;
    protected double kappa;
    protected int numDim, numRelDim;
    protected int pi;
    
    protected double[] dimVariance;
    protected double[] dimPrefVector;
    
    protected double epsilon;
    protected double beta;

    public ProjectedMicroCluster(double[] center, int dimensions, long creationTimestamp, double lambda, long currentTimestamp, double coreThreshold,
    							 double epsilon, double delta, double kappa, int pi) {
        super(center, dimensions, creationTimestamp, lambda, currentTimestamp, coreThreshold);
        
        this.numDim = center.length;
        this.epsilon = epsilon;
        this.delta = delta;
        this.kappa = kappa;
        this.pi = pi;
    }

    public ProjectedMicroCluster(Instance instance, int dimensions, long creationTimestamp, double lambda, long currentTimestamp, double coreThreshold,
    							 double epsilon, double delta, double kappa, int pi) {
        this(instance.toDoubleArray(), dimensions, creationTimestamp, lambda, currentTimestamp, coreThreshold,
        	 epsilon, delta, kappa, pi);
    }

    
       
    /** Projected properties **/
    
    private void computeDimPrefVector() {
    	dimVariance = new double[numDim];
    	dimPrefVector = new double[numDim];
    	numRelDim = 0;
    	
    	for (int j = 0; j < numDim; j++) {
    		double temp = SS[j] / weight - Math.pow((LS[j] / weight), 2);
    		if (temp >= 0) {
    			dimVariance[j] = Math.sqrt(temp);
    			if (dimVariance[j] <= delta) {
    				dimPrefVector[j] = kappa;
    				numRelDim++;
    			} else {
    				dimPrefVector[j] = 1;
    			}
    		}
    	}
    }
    
    public double getProjectedRadius() {
    	computeDimPrefVector();
    	
    	double sum = 0;
    	double sumOfPositives = 0;
    	
    	for (int j = 0; j < numDim; j++) {
    		double temp = (SS[j] / weight - Math.pow((LS[j] / weight), 2)) / dimPrefVector[j];
    		sum += temp;
    		if (temp > 0) {
    			sumOfPositives += temp;
    		}
    	}
    	
    	if (sum > 0) {
    		return Math.sqrt(sum);
    	} else {
    		return Math.sqrt(sumOfPositives);
    	}
    }
    
    public int getNumRelDim() {
    	return numRelDim;
    }
    
    public double projectedDistanceTo(Instance inst) {
    	computeDimPrefVector();
    	
    	double[] center = getCenter();
    	double[] p = inst.toDoubleArray();
    	
    	if (center.length != p.length) {
    		System.out.println("hddstream.ProjectedMicroCluster.projectedDistanceTo(inst):" 
    						+ "given inst has different #dimensions");
    	}
    	
    	double sum = 0;
    	for (int j = 0; j < center.length; j++) {
    		sum += Math.pow(2, p[j] - center[j]) / dimPrefVector[j];
    	}
    	
    	return Math.sqrt(sum);
    }
    
    
    /** Microcluster types **/

    @Override
    public boolean isCore() {
    	if (getProjectedRadius() > epsilon) {
    		return false;
    	} else if (weight < mu) {
    		return false;
    	} else if (numRelDim > pi) {
    		return false;
    	} else {
    		return true;
    	}
    }
    
    public boolean isPCore() {
    	if (getProjectedRadius() > epsilon) {
    		return false;
    	} else if (weight < beta * mu) {
    		return false;
    	} else if (numRelDim > pi) {
    		return false;
    	} else {
    		return true;
    	}
    }
    
    public boolean isOutlier() {
    	if (getProjectedRadius() > epsilon) {
    		return false;
    	} else if (weight < mu * beta || numRelDim > pi) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
    public boolean isToBeDeleted(long t, long Tspan) {
    	double w_exp = (Math.pow(2, -lambda * (t - creationTimestamp + Tspan)) - 1)
    					/ (Math.pow(2, -lambda * Tspan) - 1);
    	if (weight < w_exp) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
    
    /** Auxiliaries **/
    
    @Override
    public ProjectedMicroCluster copy() {
    	computeDimPrefVector();
    	
        ProjectedMicroCluster copy = new ProjectedMicroCluster(this.LS.clone(), this.numDim,
        													   this.creationTimestamp, this.lambda, this.lastEditTimestamp, this.mu,
        													   this.epsilon, this.delta, this.kappa, this.pi);
        
        copy.weight = this.weight;
        copy.numRelDim = this.numRelDim;
        copy.dimPrefVector = this.dimPrefVector.clone();
        
        return copy;
    }
}
