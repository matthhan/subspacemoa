/**
 * [HDDStream.java] for Subspace MOA
 * 
 * HDDStream: Main class
 * 
 * @author Yunsu Kim
 * 		   based on the implementation by Pascal Spaus
 * Data Management and Data Exploration Group, RWTH Aachen University
 */

package moa.clusterers.hddstream;

import java.util.ArrayList;

import moa.cluster.Cluster;
import moa.cluster.Clustering;
import moa.cluster.SubspaceClustering;
import moa.clusterers.AbstractSubspaceClusterer;
import moa.core.Measurement;
import moa.options.FloatOption;
import moa.options.IntOption;
import weka.core.Instance;

public class HDDStream extends AbstractSubspaceClusterer {

	private static final long serialVersionUID = 1L;
	
	private boolean debug = false;
	
	
	/* Options */
	
	public FloatOption epsilonNOption = new FloatOption("epsilonN", 'e',
			"Radius of each neighborhood.", 16, 0, 50);
	
	public FloatOption betaOption = new FloatOption("beta", 'b',
			"Control the effect of mu.", 0.5, 0, 1);
	
	public IntOption muOption = new IntOption("mu", 'm',
			"Minimum number of points desired to be in a microcluster.", 10, 1, Integer.MAX_VALUE);
	
	public FloatOption lambdaOption = new FloatOption("lambda", 'l',
			"Decaying parameter.", 0.5, 0, 1);
	
	public IntOption initPointsOption = new IntOption("initPoints", 'i',
			"Number of points to use for initialization.", 2000);

	public IntOption piOption = new IntOption("pi", 'p',
			"Number of maximal subspace dimensionality.", 30);

	public IntOption kappaOption = new IntOption("kappa", 'k',
			"Parameter to define preference weighted vector.", 10);

	public FloatOption deltaOption = new FloatOption("delta", 'd',
			"Defines the threshold for the variance.", 0.001, 0.001, 2);
	
	public FloatOption offlineOption = new FloatOption("offline", 'o',
			"Offline multiplier for epsilion.", 2, 1, 20);
	
	public IntOption speedOption = new IntOption("processingSpeed", 's',
			"Number of incoming points per time unit.", 100);

	
	/* Parameters for online processing */
	private double lambda;
	private double epsilon;
	private int mu;
	private double beta;
	
	/* Parameters for offline processing (PreDeCon) */
	private int pi;
	private int kappa;
	private double delta;

	/* Microcluster containers */
	protected Clustering potential_microclusters;
	protected Clustering outlier_microclusters;

	/* Initialization */
	protected boolean initialized;
	protected ArrayList<DenPoint> initBuffer;
	
	/* Time variables */
	protected final long TIMESTAMP_START = 0;
	protected long currentTimestamp;
	private long Tspan;
	private boolean removalProcessed;
	
	/* #point variables */
	protected int numInitPoints;
	protected int numProcessedPerUnit;
	protected int processingSpeed;
	// TODO Some variables to prevent duplicated processes
	
	/* For monitoring online part (per time unit) */
	protected int numIncludedInPMC, numIncludedInOMC;
	protected int numNewMCs, numDeletedMCs;
	protected int numPromotedToPMC, numDegradedPMCs;
	
	
	
	/** Online processing **/

	@Override
	public void resetLearningImpl() {
		lambda = lambdaOption.getValue();
		epsilon = epsilonNOption.getValue();
		mu = muOption.getValue();
		beta = betaOption.getValue();
		
		pi = piOption.getValue();
		kappa = kappaOption.getValue();
		delta = deltaOption.getValue();

		potential_microclusters = new Clustering();
		outlier_microclusters = new Clustering();
		
		initialized = false;
		initBuffer = new ArrayList<DenPoint>();
		
		currentTimestamp = TIMESTAMP_START;
		Tspan = (long) Math.ceil((1 / lambda) * Math.log(beta * mu / beta * mu - 1) / Math.log(2));
		removalProcessed = false;
		
		numInitPoints = initPointsOption.getValue();
		numProcessedPerUnit = 0;
		processingSpeed = speedOption.getValue();
		
		numIncludedInPMC = 0;
		numIncludedInOMC = 0;
		numNewMCs = 0;
		numDeletedMCs = 0;
		numPromotedToPMC = 0;
		numDegradedPMCs = 0;
		
		if (debug) {
			System.out.println("----------------------------------------\n"
							 + "HDDStream: initialization phase...\n"
							 + "beta * mu = " + (beta * mu) + "\n");
		}
	}
	
	@Override
	public void trainOnInstanceImpl(Instance inst) {
		DenPoint point = new DenPoint(inst, currentTimestamp);
		numProcessedPerUnit++;
		
		/* Controlling the stream speed */
		if (numProcessedPerUnit % processingSpeed == 0) {
			currentTimestamp++;
			removalProcessed = false;
			
			if (debug && initialized) {
				// Summary of the previous timestamp
				System.out.println("-\n# of points included in potential microclusters = " + numIncludedInPMC
								 + "\n# of points included in outlier microclusters = " + numIncludedInOMC
								 + "\n# of deleted microclusters = " + numDeletedMCs
								 + "\n# of created microclusters = " + numNewMCs
								 + "\n# of microclusters promoted from OMC to PMC = " + numPromotedToPMC
								 + "\n# of microclusters degraded from PMC to OMC = " + numDegradedPMCs
								 + "\n# of current potential microclusters = " + potential_microclusters.size()
								 + "\n# of current outlier microclusters = " + outlier_microclusters.size());
				
				// Announce of the current timestamp
				System.out.println("-----------------------------------------\n"
								 + ">> Time unit: " + currentTimestamp);
			}
			numIncludedInPMC = 0;
			numIncludedInOMC = 0;
			numDeletedMCs = 0;
			numNewMCs = 0;
			numPromotedToPMC = 0;
			numDegradedPMCs = 0;
		}
		
		// ////////////// //
		// Initialization //
		// ////////////// //
		if (!initialized) {
			initBuffer.add(point);
			if (initBuffer.size() >= numInitPoints) {
				initialPreDeCon();
				initialized = true;
			}
		} else {
			
			// ////////// //
			// Merging(p) //
			// ////////// //
			boolean merged = false;
			
			// Merge into the nearest potential microcluster
			if (potential_microclusters.size() > 0) {
				ProjectedMicroCluster x = nearestCluster(point, potential_microclusters);
				
				if (x != null) {
					x.insert(point, currentTimestamp);
					merged = true;
					numIncludedInPMC++;
				}
			}
			
			// Merge into the nearest outlier microcluster
			if (!merged && outlier_microclusters.size() > 0) {
				ProjectedMicroCluster x = nearestCluster(point, outlier_microclusters);
				
				if (x != null) {
					x.insert(point, currentTimestamp);
					merged = true;
					numIncludedInOMC++;
					
					if (x.isPCore()) {
						outlier_microclusters.getClustering().remove(x);
						potential_microclusters.add(x);
						numPromotedToPMC++;
					}
				}
			}
			
			// Create new outlier microcluster
			if (!merged) {
				outlier_microclusters.add(new ProjectedMicroCluster(point.toDoubleArray(), point.toDoubleArray().length,
									 	  currentTimestamp, lambda, currentTimestamp, mu,
									 	  epsilon, delta, kappa, pi));
				numNewMCs++;
			}
			
			// ////////////// //
			// No-hit updates //
			// ////////////// //
			
			for (Cluster c : potential_microclusters.getClustering()) {
				((ProjectedMicroCluster) c).updateForNoHitsUntil(currentTimestamp);
			}
			
			for (Cluster c : outlier_microclusters.getClustering()) {
				((ProjectedMicroCluster) c).updateForNoHitsUntil(currentTimestamp);
			}
			
			
			// //////////////////////// //
			// Periodic cluster removal //
			// //////////////////////// //
			
			if (currentTimestamp % Tspan == 0 && !removalProcessed) {
				ArrayList<ProjectedMicroCluster> outlierRemovalList = new ArrayList<ProjectedMicroCluster>();
				ArrayList<ProjectedMicroCluster> potentialRemovalList = new ArrayList<ProjectedMicroCluster>();
				
				/* Remove outlier microclusters */
				for (Cluster c : outlier_microclusters.getClustering()) {
					if (((ProjectedMicroCluster) c).isToBeDeleted(currentTimestamp, Tspan)) {
						outlierRemovalList.add((ProjectedMicroCluster) c);
					}
				}
				
				for (Cluster c : outlierRemovalList) {
					outlier_microclusters.getClustering().remove(c);
				}
				
				numDeletedMCs += outlierRemovalList.size();
				
				/* Degrade potential microclusters */
				for (Cluster c : potential_microclusters.getClustering()) {
					if (!((ProjectedMicroCluster) c).isPCore()) {
						potentialRemovalList.add((ProjectedMicroCluster) c);
					}
				}
				
				for (Cluster c : potentialRemovalList) {
					potential_microclusters.getClustering().remove(c);
					outlier_microclusters.add(c);
				}
				
				numDegradedPMCs += potentialRemovalList.size();
				
				removalProcessed = true;
			}
		}
	}

	
	
	/** Initialization phase **/
	
	protected void initialPreDeCon() {
		PreDeCon predecon = new PreDeCon(initBuffer,
										 epsilon, mu, 
										 pi, delta, kappa,
										 currentTimestamp, lambda);
		potential_microclusters = predecon.getClustering(debug);
	}

	/**
	 * Find the nearest microcluster of p.
	 * 
	 * @param p
	 * @param cl
	 * @return
	 */
	protected ProjectedMicroCluster nearestCluster(DenPoint p, Clustering cl) {
		ProjectedMicroCluster min = null;
		double minProjectedDist = Double.MAX_VALUE;
		
		for (int c = 0; c < cl.size(); c++) {
			ProjectedMicroCluster x = (ProjectedMicroCluster) cl.get(c);
			ProjectedMicroCluster xCopy = x.copy();
			
			// Tentatively insert and check the conditions
			xCopy.insert(p, currentTimestamp);
			double radiusAfterInsertion = xCopy.getProjectedRadius();
			if (radiusAfterInsertion <= epsilon && xCopy.getNumRelDim() <= pi) {
				if (min == null) {
					//ADDED BY MTH: On the first attempt already set minProjectedDist.
					//Otherwise first cluster will NEVER be chosen if any other clusters exist.
					minProjectedDist = x.projectedDistanceTo(p);
					//END ADDED BY MTH
					min = x;
					continue;
				}
				
				double projectedDist = x.projectedDistanceTo(p);
				if (projectedDist < minProjectedDist) {
					minProjectedDist = projectedDist;
					min = x;
				}
			}
		}
		
		return min;
	}

	/*protected double distance(double[] pointA, double[] pointB) {
		double distance = 0.0;
		for (int i = 0; i < pointA.length; i++) {
			double d = pointA[i] - pointB[i];
			distance += d * d;
		}
		return Math.sqrt(distance);
	}*/

	
	/** Offline processing (results) **/
	
	public SubspaceClustering getClusteringResult() {
		PreDeCon predecon = new PreDeCon(potential_microclusters,
										 offlineOption.getValue() * epsilon, mu, 
										 pi, delta, kappa);
		return new SubspaceClustering(predecon.getClustering(debug));
	}

	@Override
	public boolean implementsMicroClusterer() {
		return true;
	}

	@Override
	public Clustering getMicroClusteringResult() {
		Clustering all_microclusters = new Clustering();
		
		for (Cluster mc: potential_microclusters.getClustering()) {
			all_microclusters.add(mc);
		}
		
		for (Cluster mc: outlier_microclusters.getClustering()) {
			all_microclusters.add(mc);
		}
		
		return all_microclusters;
	}

	
	
	/** Auxiliaries **/
	
	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void getModelDescription(StringBuilder out, int indent) {
	
	}

	public boolean isRandomizable() {
		return true;
	}

	public double[] getVotesForInstance(Instance inst) {
		return null;
	}
	public static void main(String[] args) {
		HDDStream clusterer = new HDDStream();
		clusterer.epsilonNOption.setValue(0.5);
		moa.streams.clustering.RandomRBFSubspaceGeneratorEvents stream = new moa.streams.clustering.RandomRBFSubspaceGeneratorEvents();
		stream.numAttsOption.setValue(5);
		stream.prepareForUse();
		clusterer.prepareForUse();
		for(int i = 0; i < 5000;i++) {
			weka.core.Instance inst = stream.nextInstance();
			clusterer.trainOnInstance(inst);
		}
		Clustering clus = clusterer.getMicroClusteringResult();
		moa.core.AutoExpandVector<Cluster> clusvec = clus.getClustering();
		for(int i = 0; i < clusvec.size();i++) {
			double[] cur = clusvec.get(i).getCenter();
			for(int j = 0; j < cur.length; j++) {
				System.out.print(cur[j] + ", ");
			}
			System.out.println("");
		}
		System.out.println("weights:");
		for(int i = 0 ; i < clusvec.size();i++) {
			double cur = clusvec.get(i).getWeight();
			System.out.println(cur + ", ");
		}
		return;
	}
}
