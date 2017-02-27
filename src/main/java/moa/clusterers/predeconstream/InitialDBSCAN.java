/**
 * [InitialDBSCAN.java] for Subspace MOA
 * 
 * PreDeConStream: Initial clustering algorithm
 * 
 * @author Yunsu Kim
 * 		   based on the implementation by Pascal Spaus
 * Data Management and Data Exploration Group, RWTH Aachen University
 */
package moa.clusterers.predeconstream;

import java.util.ArrayList;

public class InitialDBSCAN {

	private boolean debug = false;
	
	private ArrayList<DenPoint> initBuffer;
	private ArrayList<MicroCluster> foundMCs = new ArrayList<MicroCluster>();
	private long initTimestamp;
	
	private double epsilonN, muN, beta, lambda,
					offlineFactor, muF, delta, kappa;
	private int tau;
	private final double EPSILON_FACTOR = 0.9;
	
	public InitialDBSCAN(ArrayList<DenPoint> initBuffer,
						 double epsilonN, double muN, double beta, double lambda,
						 double offlineFactor, double muF, double delta, double kappa,
						 int tau, long initTimestamp) {
		this.epsilonN = epsilonN;
		this.muN = muN;
		this.beta = beta;
		this.lambda = lambda;
		
		this.offlineFactor = offlineFactor;
		this.muF = muF;
		this.delta = delta;
		this.kappa = kappa;
		
		this.tau = tau;
		this.initTimestamp = initTimestamp;
		
		setBuffer(initBuffer);
	}
	
	public void setBuffer(ArrayList<DenPoint> initBuffer) {
		this.initBuffer = initBuffer;
		foundMCs = new ArrayList<MicroCluster>();
		runDBScan();
	}
	
	protected void runDBScan() {
		for (int i = 0; i < initBuffer.size(); i++) {
			DenPoint p = initBuffer.get(i);
			if (!p.isCovered()) {
				ArrayList<Integer> neighbourhoodIDs = getNeighbourhoodIDs(p, initBuffer, epsilonN * EPSILON_FACTOR);
				
				if (sumWeights(initBuffer, neighbourhoodIDs) >= beta * muN) { 
					MicroCluster mc = new MicroCluster(p.toDoubleArray(),
													   epsilonN, muN, lambda,
													   offlineFactor, muF, delta, kappa, tau, 
													   initTimestamp, initTimestamp);
					p.setCovered();
					
					expandCluster(mc, initBuffer, neighbourhoodIDs);
					foundMCs.add(mc);
				}
			}
		}
		
		// Pruning 'too big' MCs
		if (debug) {
			System.out.println("Initial (potential) microclusters have radius: ");
		}
		
		ArrayList<MicroCluster> toBePruned = new ArrayList<MicroCluster>(); 
		for (MicroCluster c : foundMCs) {
			double radius = c.getRadius();
			if (debug) System.out.print(radius);
			
			if (radius > epsilonN) {
				toBePruned.add(c);
				if (debug) System.out.print(" - pruned");
			}
			
			if (debug) System.out.println();
		}
		
		foundMCs.removeAll(toBePruned);
		if (debug) System.out.println("= " + foundMCs.size() + " microclusters");
	}
	
	/**
	 * Return the 'eps'-neighbor indices of given 'point'.
	 * 
	 * @param point
	 * @param points
	 * @param eps
	 * @return
	 */
	protected ArrayList<Integer> getNeighbourhoodIDs(DenPoint point, ArrayList<DenPoint> points, double eps) {
		ArrayList<Integer> neighbourIDs = new ArrayList<Integer>();
		for (int p = 0; p < points.size(); p++) {
			DenPoint npoint = points.get(p);
			if (!npoint.covered) {
				double dist = distance(point.toDoubleArray(), points.get(p).toDoubleArray());
				if (dist < eps) {
					neighbourIDs.add(p);
				}
			}
		}
		return neighbourIDs;
	}
	
	/**
	 * Sum of weights of the given points whose indices are from 'IDs'.
	 * 
	 * @param points
	 * @param IDs
	 * @return
	 */
	protected double sumWeights(ArrayList<DenPoint> points, ArrayList<Integer> IDs) {
		double sum = 0;
		
		for (int i : IDs) {
			DenPoint p = points.get(i);
			sum += Math.pow(2, -lambda * (initTimestamp - p.getCreationTimestamp()));
		}
		
		return sum;
	}

	/**
	 * Expand microcluster 'mc' with the points of 'points' whose indices are from 'neighbourhood'. 
	 * 
	 * @param mc
	 * @param points
	 * @param neighbourhoodIDs
	 */
	protected void expandCluster(MicroCluster mc, ArrayList<DenPoint> points, ArrayList<Integer> neighbourhoodIDs) {
		for (int i : neighbourhoodIDs) {
			DenPoint neighbour = points.get(i);
			if (!neighbour.isCovered()) {
				mc.insert(neighbour, initTimestamp);
				neighbour.setCovered();
				
				ArrayList<Integer> neighbourhoodIDs2 = getNeighbourhoodIDs(neighbour, initBuffer, epsilonN * EPSILON_FACTOR);
				if (sumWeights(initBuffer, neighbourhoodIDs2) > beta * muN) {
					expandCluster(mc, points, neighbourhoodIDs2);
				}
			}
		}
	}
	
	
	
	/* Auxiliary */
	protected double distance(double[] pointA, double[] pointB) {
		double distance = 0.0;
		for (int i = 0; i < pointA.length; i++) {
			double d = pointA[i] - pointB[i];
			distance += d * d;
		}
		return Math.sqrt(distance);
	}
	
	public ArrayList<MicroCluster> getFoundMicroClusters() {
		return foundMCs;
	}
}
