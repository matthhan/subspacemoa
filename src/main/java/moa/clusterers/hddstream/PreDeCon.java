/**
 * [PreDeCon.java] for Subspace MOA
 * 
 * HDDStream: used in initialization (clustering stream instances)
 * 					  offline clustering (clustering microclusters)
 * 
 * @author Yunsu Kim
 * 		   based on the implementation by Pascal Spaus
 * Data Management and Data Exploration Group, RWTH Aachen University
 */

package moa.clusterers.hddstream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import moa.cluster.CFCluster;
import moa.cluster.Cluster;
import moa.cluster.Clustering;
import moa.clusterers.macro.AbstractMacroClusterer;
import moa.clusterers.macro.NonConvexCluster;
import weka.core.Instance;

public class PreDeCon extends AbstractMacroClusterer {
	
	/** Input points (= microclusters) **/
	ArrayList<PreDeConPoint> inputPoints;
	
	/** Input parameters **/
	private double epsilon;
	private int mu;
	private int lambda;
	private double delta;
	private int kappa;
	
	private long currentTimestamp;
	private double decayingFactor;

	/**
	 * Initial setup. Doesn't do the actual clustering.
	 * 
	 * @param microClustering - microclusters to be an input set
	 * @param epsilon - neighborhood radius
	 * @param mu - minimum number of points to be in a neighborhood
	 * @param lambda - maximum number of relevant dimensions of an output cluster
	 * @param delta - maximum dimension-wise variance to be a relevant dimension
	 * @param kappa - weight of distances in relevant dimensions
	 */
	public PreDeCon(Clustering microClustering, double epsilon, int mu,
												int lambda, double delta, int kappa) {
		this.inputPoints = new ArrayList<PreDeConPoint>();
		this.epsilon = epsilon;
		this.mu = mu;
		this.lambda = lambda;
		this.delta = delta;
		this.kappa = kappa;
		
		this.currentTimestamp = -1;
		this.decayingFactor = -1;
		
		/* Each MicroCluster object => a point in PreDeCon clustering */
		for (Cluster c : microClustering.getClustering()) {
			CFCluster cf = (CFCluster) c;
			inputPoints.add(new PreDeConPoint(cf, epsilon, mu, lambda, delta, kappa));
		}
	}
	
	public PreDeCon(List<DenPoint> instances, double epsilon, int mu,
					int lambda, double delta, int kappa,
					long timestamp, double decayingFactor) {
		this.inputPoints = new ArrayList<PreDeConPoint>();
		this.epsilon = epsilon;
		this.mu = mu;
		this.lambda = lambda;
		this.delta = delta;
		this.kappa = kappa;
		
		this.currentTimestamp = timestamp;
		this.decayingFactor = decayingFactor;
		
		/* Each instance => a point in PreDeCon clustering */
		for (Instance inst : instances) {
			inputPoints.add(new PreDeConPoint(inst, epsilon, mu, lambda, delta, kappa));
		}
	}
	
	public Clustering getClustering(Clustering microClusters) {
		return getClustering(false);
	}

	public Clustering getClustering(boolean print) {
		if (currentTimestamp == -1) {
			return getOfflineClustering(print);
		} else {
			return getInitialClustering(print);
		}
	}
	
	private Clustering getInitialClustering(boolean print) {
		if (print) {
			System.out.println("-----------------------------------\n" +
							   "HDDStream: Initial PreDeCon");
			System.out.println("# of points considered = " + inputPoints.size());
		}
		
		/** --- Preprocess: Subspace preference calculation --- **/
		
		for (PreDeConPoint p : inputPoints) {
			p.preprocess(inputPoints);
		}
		

		/** --- Clustering --- **/
		
		ArrayList<PreDeConCluster> clusters = new ArrayList<PreDeConCluster>();
		for (PreDeConPoint o : inputPoints) {
			if (o.isUnclassified()) {
				if (o.isCore()) {	// Expand a new cluster from o
					PreDeConCluster cluster = new PreDeConCluster();
					List<PreDeConPoint> queue = o.getWeightedNeighborhood();
					
					while (!queue.isEmpty()) {
						PreDeConPoint q = queue.get(0);
						if (q.isUnclassified()) {
							cluster.add(q);
							q.setClassified();
						}
						
						if (q.isCore()) {
							List<PreDeConPoint> neighborQ = q.getWeightedNeighborhood();
							for (PreDeConPoint x : neighborQ) {
								if (x.getNumRelDim() <= lambda) {	// DirReach(q,x)
									if (x.isUnclassified()) {
										queue.add(x);
									}
									if (x.isUnclassified() || x.isNoise()) {
										cluster.add(x);
										x.setClassified();
									}
								}
							}
						}
						
						queue.remove(0);		
					}
					
					clusters.add(cluster);
				} else {
					o.markAsNoise();
				}
			}
		}

		if (print) {
			System.out.println("# of initial microclusters = " + clusters.size());
		}
		
		
		/** --- List of points => Projected microclusters --- **/ 
		ProjectedMicroCluster[] converted = new ProjectedMicroCluster[clusters.size()];
		int clusterPos = 0;
		for (PreDeConCluster cluster : clusters) {
			if (cluster.size() > 0) {
				PreDeConPoint p = cluster.get(0);
				converted[clusterPos] = new ProjectedMicroCluster(p.getCenter(), p.numDim,
																  currentTimestamp, decayingFactor, currentTimestamp,
																  mu, epsilon, delta, kappa, lambda);
				for (int i = 1; i < cluster.size(); i++) {
					converted[clusterPos].insert(p.getInstance(), currentTimestamp);
				}
				clusterPos++;
			}
		}

		return new Clustering(converted);
	}
	
	private Clustering getOfflineClustering(boolean print) {
		if (print) {
			System.out.println("-----------------------------------\n" +
							   "HDDStream: Offline clustering");
			System.out.println("# of microclusters considered = " + inputPoints.size());
		}
		
		/** --- Preprocess: Subspace preference calculation --- **/
		
		for (PreDeConPoint p : inputPoints) {
			p.preprocess(inputPoints);
		}
	
		if (print) {
			System.out.println("Creation time of microclusters: ");
			HashMap<Long, Integer> creationTimes = new HashMap<Long, Integer>();
			HashMap<Long, Integer> numCores = new HashMap<Long, Integer>();
			
			for (PreDeConPoint p : inputPoints) {
				long t = ((ProjectedMicroCluster) p.getCFCluster()).getCreationTime();
				if (creationTimes.containsKey(t)) {
					creationTimes.put(t, creationTimes.get(t) + 1);
				} else {
					creationTimes.put(t, 1);
					numCores.put(t, 0);
				}
				
				if (p.isCore()) {
					numCores.put(t, numCores.get(t) + 1);
				}
			}
			
			for (long t : creationTimes.keySet()) {
				System.out.print("- t = " + t + " : " + creationTimes.get(t) + " microclusters");
				if (numCores.get(t) > 0) {
					System.out.print(" (" + numCores.get(t) + " cores)");
				}
				System.out.println();
			}
		}
		

		/** --- Clustering --- **/
		
		ArrayList<PreDeConCluster> clusters = new ArrayList<PreDeConCluster>();
		for (PreDeConPoint o : inputPoints) {
			if (o.isUnclassified()) {
				if (o.isCore()) {	// Expand a new cluster from o
					PreDeConCluster cluster = new PreDeConCluster();
					List<PreDeConPoint> queue = o.getWeightedNeighborhood();
					
					while (!queue.isEmpty()) {
						PreDeConPoint q = queue.get(0);
						if (q.isUnclassified()) {
							cluster.add(q);
							q.setClassified();
						}
						
						if (q.isCore()) {
							List<PreDeConPoint> neighborQ = q.getWeightedNeighborhood();
							for (PreDeConPoint x : neighborQ) {
								if (x.getNumRelDim() <= lambda) {	// DirReach(q,x)
									if (x.isUnclassified()) {
										queue.add(x);
									}
									if (x.isUnclassified() || x.isNoise()) {
										cluster.add(x);
										x.setClassified();
									}
								}
							}
						}
						
						queue.remove(0);		
					}
					
					clusters.add(cluster);
				} else {
					o.markAsNoise();
				}
			}
		}

		if (print) {
			System.out.println("# of offline clusters = " + clusters.size());
		}
		
		
		/** --- List of points => Nonconvex connected microclusters --- *
		CFCluster[] converted = new CFCluster[clusters.size()];
		int clusterPos = 0;
		for (PreDeConCluster cluster : clusters) {
			if (cluster.size() > 0) {
				List<CFCluster> mcList = cluster.toCFClusterList();
				converted[clusterPos] = new NonConvexCluster(mcList.get(0), mcList);
				for (int i = 1; i < cluster.size(); i++) {
					converted[clusterPos].add(cluster.get(i).getCFCluster());
				}
				clusterPos++;
			}
		}*/
		/*Replaced by mth in 2015 to give out correct weights so that centers of output can be computed correctly*/
		CFCluster[] converted = new CFCluster[clusters.size()];
		int clusterPos = 0;
		for(int i = 0;i < clusters.size();i++) {
			PreDeConCluster cluster = clusters.get(i);
			if(cluster.size() > 0) {
				List<weightedCFCluster> mcList = cluster.toWeightedCFClusterList();
				converted[clusterPos] = new weightedNonConvexCluster(mcList.get(0),mcList);
				for(int j = 1; j < cluster.size();j++) {
					converted[clusterPos].add(cluster.get(j).getCFCluster());
				}
			}
			clusterPos++;
		}

		// Count Noise
		int numNoise = 0;
		for (PreDeConPoint c : inputPoints) {
			if (c.isNoise()) {
				numNoise++;
			}
		}
		
		if (print) {
			System.out.println("# noise microclusters = " + numNoise);
		}
		
		Clustering result = new Clustering(converted);
		setClusterIDs(result);
		
		return result;
	}
}
