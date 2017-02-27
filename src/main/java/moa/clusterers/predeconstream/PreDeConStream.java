/**
 * [Predeconstream.java] for Subspace MOA
 * 
 * PreDeConStream: Main class
 * 
 * @author Yunsu Kim
 * 		   based on the implementation by Pascal Spaus
 * @editor Matthias Hansen
 * Data Management and Data Exploration Group, RWTH Aachen University
 */

package moa.clusterers.predeconstream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import moa.cluster.CFCluster;
import moa.cluster.Cluster;
import moa.cluster.Clustering;
import moa.cluster.SubspaceClustering;
import moa.clusterers.AbstractSubspaceClusterer;
import moa.clusterers.macro.NonConvexCluster;
import moa.core.Measurement;
import moa.options.FloatOption;
import moa.options.IntOption;
import weka.core.Instance;

public class PreDeConStream extends AbstractSubspaceClusterer {

	private static final long serialVersionUID = 1L;
	
	private boolean debug = false;
	
	
	/* Options */
	
	public FloatOption epsilonNOption = new FloatOption("epsilonN", 'e',
			"Radius of each neighborhood.", 6, 0, 50);
	
	public FloatOption betaOption = new FloatOption("beta", 'b',
			"Control the effect of mu.", 0.16, 0, 1);
	
	public IntOption muNOption = new IntOption("muN", 'n',
			"Minimum number of points desired to be in a microcluster.", 4, 1, Integer.MAX_VALUE);
	
	public IntOption muFOption = new IntOption("muF", 'f',
			"Minimum number of points desired to be in a final cluster.", 3, 1, Integer.MAX_VALUE);
	
	public FloatOption lambdaOption = new FloatOption("lambda", 'l',
			"Decaying parameter.", 0.25, 0, 1);
	
	public IntOption initPointsOption = new IntOption("initPoints", 'i',
			"Number of points to use for initialization.", 1000);

	public IntOption tauOption = new IntOption("tau", 't',
			"Number of maximal subspace dimensionality.", 2);

	public FloatOption kappaOption = new FloatOption("kappa", 'k',
			"Parameter to define preference weighted vector.", 10, 1, 100);

	public FloatOption deltaOption = new FloatOption("delta", 'd',
			"Defines the threshold for the variance.", 0.01, 0.001, 2);

	public FloatOption offlineOption = new FloatOption("offline", 'o',
			"Offline multiplier for epsilion.", 2, 1, 20);
	
	public IntOption speedOption = new IntOption("processingSpeed", 's',
			"Number of incoming points per time unit.", 100);

	
	/* Parameters for online processing */
	private double lambda;
	private double epsilonN;
	private int muN;
	private double beta;
	
	/* Parameters for offline processing (PreDeCon) */
	private double offlineFactor;
	private int muF;
	private int tau;
	private double kappa;
	private double delta;

	/* Microcluster containers */
	protected ArrayList<MicroCluster> potential_microclusters;
	protected ArrayList<MicroCluster> outlier_microclusters;

	/* Initialization */
	protected boolean initialized;
	protected ArrayList<DenPoint> initBuffer;
	
	/* Time variables */
	protected final long TIMESTAMP_START = 0;
	protected long currentTimestamp;
	private long Tp, Td, Tv;
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
	
	/* Weight boundaries */
	protected double Wmax, Wmin, Wd = 1.0;
	
	/* Offline clustering */
	private Clustering offlineClustering;
	private ArrayList<MicroCluster> Inserted_PMC, Deleted_PMC;
	private int nextClusterID = 0;
	
	
	
	
	/** Online processing **/

	@Override
	public void resetLearningImpl() {
		lambda = lambdaOption.getValue();
		epsilonN = epsilonNOption.getValue();
		muN = muNOption.getValue();
		muF = muFOption.getValue();
		beta = betaOption.getValue();
		
		offlineFactor = offlineOption.getValue();
		tau = tauOption.getValue();
		kappa = kappaOption.getValue();
		delta = deltaOption.getValue();

		potential_microclusters = new ArrayList<MicroCluster>();
		outlier_microclusters = new ArrayList<MicroCluster>();
		
		initialized = false;
		initBuffer = new ArrayList<DenPoint>();
		
		currentTimestamp = TIMESTAMP_START;
		Tp = (long) Math.ceil((1 / lambda) * (Math.log(1 / (1 - beta * muN * (1 - Math.pow(2, -lambda)))) / Math.log(2)) - 1);
		Td = (long) Math.ceil((1 / lambda) * (Math.log(beta * muN) / Math.log(2)));
		Tv = Math.min(Tp, Td);
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
		
		Wmax = 1 / (1 - Math.pow(2, -lambda));
		Wmin = beta * muN;
		
		offlineClustering = null;
		Inserted_PMC = new ArrayList<MicroCluster>();
		Deleted_PMC = new ArrayList<MicroCluster>();
		
		if (debug) {
			System.out.println("----------------------------------------\n"
							 + "PreDeConStream: initialization phase...\n"
							 + "Wmax = " + Wmax + " / Wmin = " + Wmin + " / Wd = " + Wd + "\n" 
							 + "Tp = " + Tp + " / Td = " + Td + " ==> Tv = min(Tp, Td) = " + Tv + "\n"
							 + "beta * mu = " + (beta * muN) + "\n");
		}
	}
	
	@Override
	public void trainOnInstanceImpl(Instance inst) {
		DenPoint point = new DenPoint(inst, currentTimestamp);
		numProcessedPerUnit++;
		
		/* Controlling the stream speed */
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
			
			Inserted_PMC = new ArrayList<MicroCluster>();
			Deleted_PMC = new ArrayList<MicroCluster>();
		}
		
		// ////////////// //
		// Initialization //
		// ////////////// //
		if (!initialized) {
			initBuffer.add(point);
			if (initBuffer.size() >= numInitPoints) {
				InitialDBSCAN initializer = new InitialDBSCAN(initBuffer, 
															  epsilonN, muN, beta, lambda,
															  offlineFactor, muF, delta, kappa,
															  tau, currentTimestamp);
				potential_microclusters = initializer.getFoundMicroClusters();
				
				for (MicroCluster pmc : potential_microclusters) {
					Inserted_PMC.add(pmc);
				}
				
				initialized = true;
			}
		}
		
		if (initialized) {
			
			// ////////// //
			// Merging(p) //
			// ////////// //
			boolean merged = false;
			
			// Merge into the nearest potential microcluster
			if (potential_microclusters.size() > 0) {
				MicroCluster x = getNearestMC(point, potential_microclusters);
				MicroCluster xCopy = x.copy();
				
				xCopy.insert(point, currentTimestamp);		// Tentatively insert
				double radiusAfterInsertion = xCopy.getRadius();
				if (radiusAfterInsertion <= epsilonN) {
					x.insert(point, currentTimestamp);
					merged = true;
					numIncludedInPMC++;
				}
			}
			
			// Merge into the nearest potential microcluster
			if (!merged && outlier_microclusters.size() > 0) {
				MicroCluster x = getNearestMC(point, outlier_microclusters);
				MicroCluster xCopy = x.copy();
				
				xCopy.insert(point, currentTimestamp);		// Tentatively insert
				double radiusAfterInsertion = xCopy.getRadius();
				if (radiusAfterInsertion <= epsilonN) {
					x.insert(point, currentTimestamp);
					merged = true;
					numIncludedInOMC++;
					
					if (x.getWeight() >= beta * muN) {
						outlier_microclusters.remove(x);
						potential_microclusters.add(x);
						Inserted_PMC.add(x);	// For later offline processing
						numPromotedToPMC++;
					}
				}
			}
			
			// Create new outlier microcluster
			if (!merged) {
				outlier_microclusters.add(new MicroCluster(point.toDoubleArray(),
														   epsilonN, muN, lambda,
														   offlineFactor, muF, delta, kappa, tau, 
														   currentTimestamp, currentTimestamp));
				numNewMCs++;
			}
			
			// ///////////////////////// //
			// Microclusters maintenance //
			// ///////////////////////// //
			
			for (MicroCluster c : potential_microclusters) {
				c.updateForNoHitsUntil(currentTimestamp);
			}
			
			for (MicroCluster c : outlier_microclusters) {
				c.updateForNoHitsUntil(currentTimestamp);
			}
			
			
			// //////////////////////// //
			// Periodic cluster removal //
			// //////////////////////// //
			
			if (currentTimestamp % Tv == 0 && !removalProcessed) {
				ArrayList<MicroCluster> outlierRemovalList = new ArrayList<MicroCluster>();
				ArrayList<MicroCluster> potentialRemovalList = new ArrayList<MicroCluster>();
				
				/* Remove outlier microclusters */
				for (MicroCluster c : outlier_microclusters) {
					if (c.getWeight() < 1) {
						outlierRemovalList.add(c);
					}
				}
				
				for (MicroCluster c : outlierRemovalList) {
					outlier_microclusters.remove(c);
				}
				
				numDeletedMCs += outlierRemovalList.size();
				
				/* Remove potential microclusters */
				for (MicroCluster c : potential_microclusters) {
					if (c.getWeight() < beta * muN) {
						potentialRemovalList.add(c);
					}
				}
				
				for (MicroCluster c : potentialRemovalList) {
					potential_microclusters.remove(c);
					outlier_microclusters.add(c);
					Deleted_PMC.add((MicroCluster) c);
				}
				
				numDegradedPMCs += potentialRemovalList.size();
				
				updateClustering();
				
				removalProcessed = true;
			}			
		}
	}


	/**
	 * Find the nearest microcluster of p.
	 * 
	 * @param p
	 * @param MCs
	 * @return
	 */
	protected MicroCluster getNearestMC(DenPoint p, ArrayList<MicroCluster> MCs) {
		MicroCluster minA = null, minB = null;
		double minDistToCenter = Double.MAX_VALUE,
			   minDistToContour = Double.MAX_VALUE;
		
		for (int c = 0; c < MCs.size(); c++) {
			MicroCluster x = (MicroCluster) MCs.get(c);
			if (minA == null) {
				minA = x;
				continue;
			}
			
			double[] point = p.toDoubleArray();
			double[] clusterCenter = x.getCenter();
			double clusterRadius = x.getRadius();
			
			double distToCenter = distance(point, clusterCenter);
			if (distToCenter < minDistToCenter) {
				minDistToCenter = distToCenter;
				minB = x;
			}
			
			double distToContour = distToCenter - clusterRadius;
			
			if (distToContour > 0 && distToContour < minDistToContour) {
				minDistToContour = distToContour;
				minA = x;
			}
		}
		
		return minA != null ? minA : minB;
	}

	protected double distance(double[] pointA, double[] pointB) {
		double distance = 0.0;
		for (int i = 0; i < pointA.length; i++) {
			double d = pointA[i] - pointB[i];
			distance += d * d;
		}
		return Math.sqrt(distance);
	}

	
	
	
	
	
	
	/** Offline clustering **/
	
	private void updateClustering() {
		
		if (offlineClustering == null) {
			offlineClustering = new Clustering();
		}
		
		/* For Inserted_PMC */
		
		ArrayList<MicroCluster> AFFECTED_CORESi = new ArrayList<MicroCluster>();
		for (MicroCluster cp : Inserted_PMC) {
			cp.preprocess(potential_microclusters);
			
			for (MicroCluster cq : cp.getWeightedNeighborhood()) {
				boolean wasPrefCorePoint = cq.isPrefCorePoint();
				double[] beforeSubspacePrefVector = cq.getSubspacePrefVector();
				
				cq.preprocess(potential_microclusters);
				
				boolean isPrefCorePoint = cq.isPrefCorePoint();
				double[] afterSubspacePrefVector = cq.getSubspacePrefVector();
				
				if ((wasPrefCorePoint ^ isPrefCorePoint) ||
					 Arrays.equals(beforeSubspacePrefVector, afterSubspacePrefVector)) {
					AFFECTED_CORESi.add(cq);
				}
			}
		}
		
		Set<NonConvexCluster> AFFECTED_CLUSTERSi = new HashSet<NonConvexCluster>();
		for (MicroCluster mc : AFFECTED_CORESi) {
			List<Cluster> offlineClusters = offlineClustering.getClustering();
			for (int i = 0; i < offlineClusters.size(); i++) {
				NonConvexCluster ncc = (NonConvexCluster) offlineClusters.get(i);
				for (CFCluster cf : ncc.getMicroClusters()) {
					if (cf.equals(mc)) {
						AFFECTED_CLUSTERSi.add(ncc);
						break;
					}
				}
			}
		}
		
		ArrayList<MicroCluster> UPDSEEDi = new ArrayList<MicroCluster>();
		for (MicroCluster mc : AFFECTED_CORESi) {
			List<MicroCluster> affectedNeighborhood = mc.getWeightedNeighborhood();
			UPDSEEDi.removeAll(affectedNeighborhood);
			UPDSEEDi.addAll(affectedNeighborhood);
		}
		for (NonConvexCluster ncc : AFFECTED_CLUSTERSi) {
			List<MicroCluster> affectedClassifiedMCs = new ArrayList<MicroCluster>();
			
			for (CFCluster cf : ncc.getMicroClusters()) {
				affectedClassifiedMCs.add((MicroCluster) cf);
			}
			
			UPDSEEDi.removeAll(affectedClassifiedMCs);
			UPDSEEDi.addAll(affectedClassifiedMCs);
		}
		
		
		/* For Deleted_PMC */
		
		ArrayList<MicroCluster> AFFECTED_CORESd = new ArrayList<MicroCluster>();
		for (MicroCluster cd : Deleted_PMC) {
			cd.preprocess(potential_microclusters);
			
			for (MicroCluster cq : cd.getWeightedNeighborhood()) {
				boolean wasPrefCorePoint = cq.isPrefCorePoint();
				double[] beforeSubspacePrefVector = cq.getSubspacePrefVector();
				
				cq.preprocess(potential_microclusters);
				
				boolean isPrefCorePoint = cq.isPrefCorePoint();
				double[] afterSubspacePrefVector = cq.getSubspacePrefVector();
				
				if ((wasPrefCorePoint ^ isPrefCorePoint) ||
					 Arrays.equals(beforeSubspacePrefVector, afterSubspacePrefVector)) {
					AFFECTED_CORESd.add(cq);
				}
			}
		}
		
		Set<NonConvexCluster> AFFECTED_CLUSTERSd = new HashSet<NonConvexCluster>();
		for (MicroCluster mc : AFFECTED_CORESd) {
			List<Cluster> offlineClusters = offlineClustering.getClustering();
			for (int i = 0; i < offlineClusters.size(); i++) {
				NonConvexCluster ncc = (NonConvexCluster) offlineClusters.get(i);
				for (CFCluster cf : ncc.getMicroClusters()) {
					if (cf.equals(mc)) {
						AFFECTED_CLUSTERSd.add(ncc);
						break;
					}
				}
			}
		}
		
		ArrayList<MicroCluster> UPDSEEDd = new ArrayList<MicroCluster>();
		for (MicroCluster c : AFFECTED_CORESd) {
			List<MicroCluster> affectedNeighborhood = c.getWeightedNeighborhood();
			UPDSEEDd.removeAll(affectedNeighborhood);
			UPDSEEDd.addAll(affectedNeighborhood);
		}
		for (NonConvexCluster ncc : AFFECTED_CLUSTERSd) {
			List<MicroCluster> affectedClassifiedMCs = new ArrayList<MicroCluster>();
			
			for (CFCluster cf : ncc.getMicroClusters()) {
				affectedClassifiedMCs.add((MicroCluster) cf);
			}
			
			UPDSEEDd.removeAll(affectedClassifiedMCs);
			UPDSEEDd.addAll(affectedClassifiedMCs);
		}
		
		
		/* Preparing the update seeds */
		
		ArrayList<MicroCluster> UPDSEED = UPDSEEDi;
		UPDSEED.removeAll(UPDSEEDd);
		UPDSEED.addAll(UPDSEEDd);
		
		for (MicroCluster cp : UPDSEED) {
			cp.setUnclassified();
		}
		
		
		/* Clean up the deprecated clusters */
		
		List<NonConvexCluster> AFFECTED_CLUSTERS = new ArrayList<NonConvexCluster>();
		AFFECTED_CLUSTERS.addAll(AFFECTED_CLUSTERSi);
		AFFECTED_CLUSTERS.removeAll(AFFECTED_CLUSTERSd);
		AFFECTED_CLUSTERS.addAll(AFFECTED_CLUSTERSd);
		offlineClustering.getClustering().removeAll(AFFECTED_CLUSTERS);
		if (debug) {
			System.out.print("Removed offline clusters (ID):");
			for (NonConvexCluster ncc : AFFECTED_CLUSTERS) {
				System.out.print(" " + (int) ncc.getId());
			}
			System.out.println();
		}
		
		
		/* Re-clustering */
		
		expandCluster(UPDSEED);
	}
	
	private void expandCluster(ArrayList<MicroCluster> UPDSEED) {
		ArrayList<PreDeConCluster> clusters = new ArrayList<PreDeConCluster>();
		for (MicroCluster cp : UPDSEED) {
			if (cp.isUnclassified()) {
				if (cp.isPrefCorePoint()) {	// Expand a new cluster from o
					PreDeConCluster cluster = new PreDeConCluster();
					List<MicroCluster> queue = cp.getWeightedNeighborhood();
					
					while (!queue.isEmpty()) {
						MicroCluster cq = queue.get(0);
						if (cq.isUnclassified()) {
							cluster.add(cq);
							cq.setClassified();
						}
						
						if (cq.isPrefCorePoint()) {
							List<MicroCluster> M = cq.getWeightedNeighborhood();
							M.retainAll(UPDSEED);
							
							for (MicroCluster c : M) {
								if (c.getNumRelDim() <= tau) {	// DirReach(cq,c)
									if (c.isUnclassified()) {
										queue.add(c);
									}
									if (c.isUnclassified() || c.isNoise()) {
										cluster.add(c);
										c.setClassified();
									}
								}
							}
						}
						
						queue.remove(0);		
					}
					
					clusters.add(cluster);
				} else {
					cp.markAsNoise();
				}
			}
		}
		
		/** --- List of points => Nonconvex connected microclusters --- */
		//Changed by mth in 2015 to use weightedNonConvexClusters
		if (debug) System.out.print("Newly added offline clusters (ID):");
		for (PreDeConCluster cluster : clusters) {
			if (cluster.size() > 0) {
				List<MicroCluster> connectedMCs = cluster.toMicroClusterList();
				CFCluster converted = new weightedNonConvexCluster(connectedMCs.get(0),connectedMCs);
				for (int i = 1; i < cluster.size(); i++) {
					converted.add(cluster.get(i));
				}
				
				converted.setId(nextClusterID);
				offlineClustering.add(converted);
				
				if (debug) System.out.print(" " + nextClusterID);
				nextClusterID++;
			}
		}
		if (debug) System.out.println();
	}
	
	@Override
	public SubspaceClustering getClusteringResult() {
		if (offlineClustering != null) {
			return new SubspaceClustering(offlineClustering);
		} else {
			return null;	// Not yet constructed
		}
	}

	@Override
	public boolean implementsMicroClusterer() {
		return true;
	}

	@Override
	public Clustering getMicroClusteringResult() {
		Clustering all_microclusters = new Clustering();
		
		for (Cluster mc: potential_microclusters) {
			all_microclusters.add(mc);
		}
		
		for (Cluster mc: outlier_microclusters) {
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
		PreDeConStream clusterer = new PreDeConStream();
		moa.streams.clustering.RandomRBFSubspaceGeneratorEvents stream = new moa.streams.clustering.RandomRBFSubspaceGeneratorEvents();
		stream.numAttsOption.setValue(5);
		stream.prepareForUse();
		clusterer.prepareForUse();
		for(int i = 0; i < 5000;i++) {
			weka.core.Instance inst = stream.nextInstance();
			clusterer.trainOnInstance(inst);
		}
		SubspaceClustering clus = clusterer.getClusteringResult();
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
			double cur = (clusvec.get(i)).getWeight();
			System.out.println(cur + ", ");
		}
		return;
	}
}
