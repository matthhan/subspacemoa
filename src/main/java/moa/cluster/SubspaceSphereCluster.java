/**
 * [SubspaceSphereCluster.java] for Subspace MOA
 * 
 * Subspace version of [SphereCluster] class.
 * 
 * @author Yunsu Kim
 * Data Management and Data Exploration Group, RWTH Aachen University
 */

package moa.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import weka.core.DenseInstance;
import weka.core.Instance;

public class SubspaceSphereCluster extends Cluster {

	private static final long serialVersionUID = 1L;
	
	private boolean debug = false;

	private double[] center;
	private boolean[] subspace;
	private int subspaceSize;
	private double radius;
	private double weight;
	
	// Extra: adjusted by subspace events
	private boolean[] adjustedSubspace;
	private int adjustedSubspaceSize;
	
	/* Constructors */ 
	
	public SubspaceSphereCluster() {
	
	}
	
	public SubspaceSphereCluster(double[] center, double radius, boolean[] subspace) {
		this(center, radius, subspace, 1.0);
		setAdjustedSubspace(subspace);
	}
	
	public SubspaceSphereCluster(double[] center, double radius, double weight) {
		this();
		this.center = center;
		this.radius = radius;
		this.weight = weight;
		boolean[] subspace = new boolean[center.length];
		
		// Full-space
		for (int i = 0; i < center.length; i++)
			subspace[i] = true;
		this.subspace = subspace;
		this.subspaceSize = center.length;
		setAdjustedSubspace(subspace);
	}

	public SubspaceSphereCluster(double[] center, double radius, boolean[] subspace, double weightedSize) {
		this();
		this.center = center;
		this.subspace = subspace;
		for (int j = 0; j < subspace.length; j++) {
			if (subspace[j])
				this.subspaceSize++;
		}
		this.radius = radius;
		this.weight = weightedSize;
		
		setAdjustedSubspace(subspace);
	}

	public SubspaceSphereCluster(List<?extends Instance> instances, boolean[] subspace) {
		this();
		if (instances == null || instances.size() <= 0)
			return;

		int subSpaceSize = 0;
		for (int i = 0; i < subspace.length; i++) {
			if (subspace[i])
				subSpaceSize++;
		}
		Miniball mb = new Miniball(subSpaceSize);
		mb.clear();
		
		double[] sumCenter = new double[instances.get(0).numAttributes()];
		for (int i = 0; i < sumCenter.length; i++)
			sumCenter[i] = 0;

		for (Instance instance : instances) {
			// Iterate each attribute value in 'instance'
			List<Double> relevantValues = new ArrayList<Double>();
			for (int j = 0; j < subspace.length; j++) {
				// Collect relevant values
				if (subspace[j])
					relevantValues.add(instance.value(j));
				// Sum up for calculating mean center
				sumCenter[j] += instance.value(j);
			}
			
			// Check-in the miniball
			double[] relevantArray = new double[relevantValues.size()];
			for (int j = 0; j < relevantValues.size(); j++) {
				relevantArray[j] = relevantValues.get(j);
			}
			mb.check_in(relevantArray);
		}

		// Miniball build
		mb.build();
		
		// Radius
		this.radius = mb.radius();
		
		// Center
		double[] mbCenter = mb.center();
		int m = 0;
		double[] newCenter = new double[subspace.length];
		for (int j = 0; j < newCenter.length; j++) {
			if (subspace[j]) {
				newCenter[j] = mbCenter[m];
				m++;
			} else
				newCenter[j] = sumCenter[j] / (double)instances.size();
		}
		this.center = newCenter;
		
		// Miniball clear
		mb.clear();
		
		// Weight & Subspace
		this.weight = instances.size();
		this.subspace = subspace;
		this.subspaceSize = subSpaceSize;
		
		setAdjustedSubspace(subspace);
	}
	
	public SphereCluster toSphereCluster() {
		SphereCluster sc = new SphereCluster(center, radius, weight);
		sc.setGroundTruth(this.getGroundTruth());
		return sc;
	}
	

	public boolean hasSameSubspaceWith(SubspaceSphereCluster cluster) {
		if (subspace.length != cluster.subspace.length)
			return false;
		for (int i = 0; i < subspace.length; i++)
			if (subspace[i] != cluster.subspace[i])
				return false;
		return true;
	}

	
	/* Cluster overlap */
	
	/**
	 * Return the degree of overlapping between clusters.
	 * It is the distance between cluster centers, normalized w.r.t the diameter of the smaller cluster.
	 * 
	 * @param other
	 * @return overlapping degree [0, 1]
	 */
	public double overlapRadiusDegree(SubspaceSphereCluster other) {
		
		if (!this.hasSameSubspaceWith(other))	// Until now, clusters having a same subspace can be overlapped together
			return 0;
		
		double[] center0 = getCenter();
		double radius0 = getRadius();

		double[] center1 = other.getCenter();
		double radius1 = other.getRadius();

		double radiusBig;
		double radiusSmall;
		if (radius0 < radius1) {
			radiusBig = radius1;
			radiusSmall = radius0;
		} else {
			radiusBig = radius0;
			radiusSmall = radius1;
		}
		
		// Distance between centers
		double dist = 0;
		for (int i = 0; i < center0.length; i++) {
			double delta = center0[i] - center1[i];
			dist += delta * delta;
		}
		dist = Math.sqrt(dist);

		// Results
		if (dist > radiusSmall + radiusBig)				// Don't overlap
			return 0;
		else if (dist + radiusSmall <= radiusBig) { 	// One lies within the other
			return 1;
		} else {
			return (radiusSmall + radiusBig - dist) / (2 * radiusSmall);	// Normalized
		}
	}

	/**
	 * Combine this cluster with another cluster.
	 * 
	 * @param cluster
	 */
	public boolean combine(SubspaceSphereCluster cluster) {
		
		if (!this.hasSameSubspaceWith(cluster))		// Until now, clusters having a same subspace can be combined together
			return false;
		
		double[] center = getCenter();
		double weight = getWeight();
		double radius = getRadius();
		double[] other_center = cluster.getCenter();
		double other_weight = cluster.getWeight();
		double other_radius = cluster.getRadius();

		// Center: Normalized weighted sum
		double[] newcenter = new double[center.length];
		for (int i = 0; i < center.length; i++) {
			newcenter[i] = (center[i] * weight + other_center[i] * other_weight) / (weight + other_weight);
		}
		setCenter(newcenter);
		
		// Radius: Maximum of "newcenter to each end"
		double r_0 = radius + Math.abs(distance(center, newcenter));
		double r_1 = other_radius + Math.abs(distance(other_center, newcenter));
		setRadius(Math.max(r_0, r_1));
		
		// Weight: Sum
		setWeight(weight + other_weight);
		
		return true;
	}

	/**
	 * Merge this cluster and a given cluster.
	 * 
	 * @param cluster
	 */
	public boolean merge(SubspaceSphereCluster cluster) {
		
		// Until now, only clusters having a same subspace can be merged together
		if (!this.hasSameSubspaceWith(cluster))
			return false;
		
		double[] c0 = getCenter();
		double w0 = getWeight();
		double r0 = getRadius();

		double[] c1 = cluster.getCenter();
		double w1 = cluster.getWeight();
		double r1 = cluster.getRadius();
		boolean[] s1 = cluster.getSubspace();

		double[] v = new double[c0.length];		// Center difference
		double d = 0;

		for (int i = 0; i < c0.length; i++) {
			if (s1[i]) {
				v[i] = c0[i] - c1[i];
				d += v[i] * v[i];
			}
		}
		d = Math.sqrt(d);


		double r = 0;
		double[] c = new double[c0.length];

		if (d + r0 <= r1 || d + r1 <= r0) {		// One lies within the other
			if (d + r0 <= r1) {
				r = r1;
				c = c1;
			} else {
				r = r0;
				c = c0;
			}
		} else {
			r = (r0 + r1 + d) / 2.0;
			for (int i = 0; i < c.length; i++) {
				if (s1[i])
					c[i] = c1[i] - v[i]/d * (r1 - r);
					//c[i] = (c0[i] + c1[i]) / 2.0;
				else
					c[i] = c0[i];
			}
		}
		
		System.out.println("merged radius = " + radius);

		setCenter(c);
		setRadius(r);
		setWeight(w0 + w1);
		
		return true;
	}

	
	/* Instance in this cluster */
	
	/**
	 * Test whether the instance is included or not.
	 * Inside = 1.0 / Outside = 0.0
	 * 
	 */
	@Override
	public double getInclusionProbability(Instance instance) {
		if (subspaceSize == 0) {		// Dead cluster
			return 0.0;
		} else if (getCenterDistance(instance) <= getRadius()) {
			return 1.0;
		} else {
			return 0.0;
		}
	}
	
	/**
	 * Samples this cluster by returning a point from inside it.
	 * 
	 * @param random - a random number source
	 * @return a point that lies inside this cluster
	 */
	public Instance sample(Random random) {
		// Create sample in hypersphere coordinates
		double[] center = getCenter();

		final double sin[] = new double[this.subspaceSize - 1];
		final double cos[] = new double[this.subspaceSize - 1];
		final double length = random.nextDouble() * getRadius();

		double lastValue = 1.0;
		for (int i = 0; i < this.subspaceSize - 1; i++) {
			double angle = random.nextDouble() * 2 * Math.PI;
			sin[i] = lastValue * Math.sin( angle ); 	// Store cumulative values
			cos[i] = Math.cos( angle );
			lastValue = sin[i];
		}

		// Calculate cartesian coordinates
		final int fullspaceSize = this.center.length;
		double res[] = new double[fullspaceSize + 1];	// +1 for generator label

		int j = 0;
		for (int i = 0; i < this.center.length; i++) {
			if (subspace[i]) {	// Relevant dimensions
				
				if (j == 0) {
					res[i] = center[i] + length * cos[0];				// First value uses only cosines
				} else if (j >= 1 && j < this.subspaceSize - 1) {
					res[i] = center[i] + length * sin[j - 1] * cos[j];	// Loop through 'middle' coordinates which use cosines and sines
				} else if (j == this.subspaceSize - 1) {
					res[i] = center[i] + length * sin[j - 1];			// Last value uses only sines
				}
				
				j++;
				
			} else {			// Irrelevant dimensions: just random number
				res[i] = random.nextDouble();
			}
		}
		
		// Generator label (representing class label)
		res[fullspaceSize] = getId();
		
		return new DenseInstance(1.0, res);
	}
	
	
	/* Distance functions */

	/**
	 * Euclidean distance from the center defined in subspace.
	 * 
	 * @param instance
	 * @return distance from the center
	 */
	public double getCenterDistance(Instance instance) {
		double distance = 0.0;
		double[] center = getCenter();
		
		for (int i = 0; i < subspace.length; i++) {
			if (subspace[i]) {
				double d = center[i] - instance.value(i);
				distance += d * d;
			}
		}
		return Math.sqrt(distance);
	}

	/**
	 * Euclidean distance from the center defined in subspace.
	 * -1 if subspaces do not match.
	 * 
	 * @param other - cluster
	 * @return distance from the center
	 */
	public double getCenterDistance(SubspaceSphereCluster other) {
		if (this.hasSameSubspaceWith(other))
			return -1;
		else
			return distance(getCenter(), other.getCenter());
	}

	/**
	 * The minimal distance between the surface of two clusters defined in subspace.
	 * NEGATIVE if the two clusters overlap.
	 * NaN if subspaces do not match.
	 * 
	 * @param other
	 * @return
	 */
	public double getHullDistance(SubspaceSphereCluster other) {
		if (!this.hasSameSubspaceWith(other))
			return Double.NaN;
		else {
			double[] center0 = getCenter();
			double[] center1 = other.getCenter();
			return distance(center0, center1) - getRadius() - other.getRadius();
		}
	}

	
	/* Auxiliary distance functions */
	
	/**
	 * Euclidean distance between two vectors, defined in subspace.
	 * WARNING: Vector lengths are not checked
	 * 
	 * @param v1
	 * @param v2
	 * @return distance between two vectors
	 */
	private double distance(double[] v1, double[] v2){
		double distance = 0.0;
		
		for (int i = 0; i < subspace.length; i++) {
			if (subspace[i]) {
				double d = v1[i] - v2[i];
				distance += d * d;
			}
		}
		return Math.sqrt(distance);
	}

	/**
	 * Difference between the cluster center and a given instance.
	 * 
	 * @param instance
	 * @return
	 */
	public double[] getDistanceVector(Instance instance){
		return distanceVector(getCenter(), instance.toDoubleArray());
	}

	/**
	 * Difference between the cluster center and the center of a given cluster.
	 * NULL if the subspaces do not match.
	 *  
	 * @param other
	 * @return
	 */
	public double[] getDistanceVector(SubspaceSphereCluster other){
		if (this.hasSameSubspaceWith(other))
			return null;
		return distanceVector(getCenter(), other.getCenter());
	}

	/**
	 * Difference between v1 and v2.
	 * 
	 * @param v1
	 * @param v2
	 * @return (v2 - v1)
	 */
	private double[] distanceVector(double[] v1, double[] v2){
		double[] v = new double[v1.length];
		for (int i = 0; i < v1.length; i++) {
			v[i] = v2[i] - v1[i];
		}
		return v;
	}

 
	

	
	/**
	 * Getters and setters.
	 *  
	 */
	
	@Override
	public double[] getCenter() {
		double[] copy = new double[center.length];
		System.arraycopy(center, 0, copy, 0, center.length);
		return copy;
	}

	public void setCenter(double[] center) {
		this.center = center;
	}
	
	public int getFullspaceSize() {
		return this.subspace.length;
	}
	
	public boolean[] getSubspace() {
		boolean[] copy = new boolean[subspace.length];
		System.arraycopy(subspace, 0, copy, 0, subspace.length);
		return copy;
	}
	
	public int getSubspaceSize() {
		return subspaceSize;
	}
	
	public List<Integer> getRelevantDims() {
		List<Integer> relevantDims = new ArrayList<Integer>();
		int fullspaceSize = this.subspace.length;
		for (int j = 0; j < fullspaceSize && relevantDims.size() <= subspaceSize; j++) {
			if (subspace[j])
				relevantDims.add(j);
		}
		return relevantDims;
	}
	
	public boolean isRelevant(int dim) {
		return this.subspace[dim];
	}
	
	public List<Integer> getIrrelevantDims() {
		List<Integer> irrelevantDims = new ArrayList<Integer>();
		int fullspaceSize = this.subspace.length;
		for (int j = 0; j < fullspaceSize && irrelevantDims.size() <= fullspaceSize - subspaceSize; j++) {
			if (!subspace[j])
				irrelevantDims.add(j);
		}
		return irrelevantDims;
	}
	
	public void setSubspace(boolean[] subspace) {
		this.subspace = subspace;
		this.subspaceSize = 0;
		for (int j = 0; j < subspace.length; j++) {
			if (subspace[j])
				this.subspaceSize++;
		}
		setAdjustedSubspace(subspace);
	}
	
	public void setRelevantDim(int dim, boolean relevant) {
		if (subspace[dim] != relevant) {
			subspace[dim] = relevant;
			if (relevant) subspaceSize++;
			else subspaceSize--;
		}
		setAdjustedSubspace(subspace);
	}

	public double getRadius() {
		return radius;
	}

	public void setRadius(double radius) {
		this.radius = radius;
	}
	
	public void adjustRadius(double ratio) {
		this.radius = this.radius * ratio;
	}

	@Override
	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	
	@Override
	protected void getClusterSpecificInfo(ArrayList<String> infoTitle, ArrayList<String> infoValue) {
		super.getClusterSpecificInfo(infoTitle, infoValue);
		infoTitle.add("Radius");
		infoValue.add(Double.toString(getRadius()));
	}
	
	
	/** Extra **/
	
	public boolean[] getAdjustedSubspace() {
		boolean[] copy = new boolean[adjustedSubspace.length];
		System.arraycopy(adjustedSubspace, 0, copy, 0, adjustedSubspace.length);
		return copy;
	}
	
	public int getAdjustedSubspaceSize() {
		return adjustedSubspaceSize;
	}
	
	public List<Integer> getAdjustedRelevantDims() {
		if (debug) System.out.println("Full space size = " + adjustedSubspace.length);
		List<Integer> relevantDims = new ArrayList<Integer>();
		int fullspaceSize = this.adjustedSubspace.length;
		if (debug) System.out.println("Adjusted subspace indices = ");
		for (int j = 0; j < fullspaceSize && relevantDims.size() <= adjustedSubspaceSize; j++) {
			if (adjustedSubspace[j]) {
				if (debug) System.out.print(j + " ");
				relevantDims.add(j);
			}
		}
		return relevantDims;
	}
	
	public boolean isAdjustedRelevant(int dim) {
		return this.adjustedSubspace[dim];
	}
	
	public List<Integer> getAdjustedIrrelevantDims() {
		List<Integer> irrelevantDims = new ArrayList<Integer>();
		int fullspaceSize = this.adjustedSubspace.length;
		for (int j = 0; j < fullspaceSize && irrelevantDims.size() <= fullspaceSize - adjustedSubspaceSize; j++) {
			if (!adjustedSubspace[j])
				irrelevantDims.add(j);
		}
		return irrelevantDims;
	}
	
	public void setAdjustedSubspace(boolean[] adjustedSubspace) {
		this.adjustedSubspace = adjustedSubspace;
		this.adjustedSubspaceSize = 0;
		for (int j = 0; j < adjustedSubspace.length; j++) {
			if (adjustedSubspace[j])
				this.adjustedSubspaceSize++;
		}
	}
	
	public void setAdjustedRelevantDim(int dim, boolean relevant) {
		if (adjustedSubspace[dim] != relevant) {
			adjustedSubspace[dim] = relevant;
			if (relevant) adjustedSubspaceSize++;
			else adjustedSubspaceSize--;
		}
	}
	
	public double getLeftBoundary(int dim) {
		return center[dim] - radius; 
	}
	
	public double getRightBoundary(int dim) {
		return center[dim] + radius; 
	}
	
	public boolean boundaryCheck(double value, int dim) {
		return isRelevant(dim) && value > getLeftBoundary(dim) && value < getRightBoundary(dim); 
	}
}
