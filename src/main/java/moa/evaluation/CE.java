/**
 * [CE.java] for Subspace MOA
 * 
 * Evaluation measure: Clustering Error (CE)
 * 
 * Reference: Patrikainen et al., "Comparing Subspace Clusterings", IEEE TKDE, 2006
 * 
 * @author Yunsu Kim
 * Data Management and Data Exploration Group, RWTH Aachen University
 */

package moa.evaluation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import moa.cluster.Cluster;
import moa.cluster.SubspaceClustering;
import moa.cluster.SubspaceSphereCluster;
import moa.gui.subspacevisualization.SubspaceDataPoint;

public class CE extends SubspaceMeasureCollection {

	private static final long serialVersionUID = 1L;
	
	private boolean debug = false;
	
	@Override
	protected String[] getNames() {
		String[] names = {"1.0-CE"};
		return names;
	}
	
	@Override
    protected boolean[] getDefaultEnabled() {
        boolean [] defaults = {true};
        return defaults;
    }

	@Override
	protected void subEvaluateSubspaceClustering(SubspaceClustering foundClustering, SubspaceClustering gtClustering, List<SubspaceDataPoint> points) throws Exception {
		List<Cluster> foundClusters = foundClustering.getClustering();
		List<Cluster> gtClusters = gtClustering.getClustering();
		List<List<SubspaceDataPoint>> pointsInFC = new ArrayList<List<SubspaceDataPoint>>();
		List<List<SubspaceDataPoint>> pointsInGC = new ArrayList<List<SubspaceDataPoint>>();
		
		// Initialize per-cluster point containers
		for (int i = 0; i < foundClusters.size(); i++) {
    		pointsInFC.add(new ArrayList<SubspaceDataPoint>());
    	}

    	for (int i = 0; i < gtClusters.size(); i++) {
    		pointsInGC.add(new ArrayList<SubspaceDataPoint>());
    	}		
		
		double inclusionProbabilityThreshold = 0.5;
		int numDims = points.get(0).getClassLabels().length;
				
		int union = 0;
		
		// Full-space (just in case)
		boolean[] fullSpace = new boolean[numDims];
		for (int j = 0; j < numDims; j++) {		// Full-space
			fullSpace[j] = true;
		}
		
		// Calculate union
    	for (SubspaceDataPoint p : points) {
    		int[] dimCoveredByFCs = new int[numDims],
    			  dimCoveredByGCs = new int[numDims];
    		
    		for (int j = 0; j < numDims; j++) {
    			dimCoveredByFCs[j] = 0;
    			dimCoveredByGCs[j] = 0;
    		}
    		
    		// Dimensions covered by found clustering
    		for (int i = 0; i < foundClusters.size(); i++) {
    			Cluster fc = foundClusters.get(i);
    			if (fc.getInclusionProbability(p) >= inclusionProbabilityThreshold) {
    				if (fc instanceof SubspaceSphereCluster) {
	    				for (int j : ((SubspaceSphereCluster) fc).getAdjustedRelevantDims()) {
	    					dimCoveredByFCs[j]++;
	    				}
    				} else {
    					for (int j = 0; j < numDims; j++) {		// Full-space
    						dimCoveredByFCs[j]++;
    					}
    				}
    				pointsInFC.get(i).add(p);	// To be used to construct confusion matrix
    			}
    		}
    		
    		for (int i = 0; i < gtClusters.size(); i++) {
    			Cluster gc = gtClusters.get(i);
    			if (gc.getInclusionProbability(p) >= inclusionProbabilityThreshold) {
    				if (gc instanceof SubspaceSphereCluster) {
	    				for (int j : ((SubspaceSphereCluster) gc).getAdjustedRelevantDims()) {
	    					dimCoveredByGCs[j]++;
	    				}
    				} else {
    					for (int j = 0; j < numDims; j++) {		// Full-space
    						dimCoveredByGCs[j]++;
    					}
    				}
    				pointsInGC.get(i).add(p);	// To be used to construct confusion matrix
    			}
    		}
    		
    		for (int j = 0; j < numDims; j++) {
    			union += Math.max(dimCoveredByFCs[j], dimCoveredByGCs[j]);
    		}
    	}
    	
    	// Confusion matrix
		int maxDimOfMtx = Math.max(foundClusters.size(), gtClusters.size());
		double[][] cost = new double[maxDimOfMtx][maxDimOfMtx];
		
		for (int i = 0; i < maxDimOfMtx; i++)
			for (int j = 0; j < maxDimOfMtx; j++)
				cost[i][j] = 0;
		
		int count1 = 0;
		for (Cluster c1 : gtClusters) {
			int count2 = 0;
			boolean[] c1Subspace = new boolean[numDims];
			if (c1 instanceof SubspaceSphereCluster) {
				c1Subspace = ((SubspaceSphereCluster) c1).getAdjustedSubspace();
			} else {
				c1Subspace = fullSpace;
			}
			
			for (Cluster c2 : foundClusters) {
				boolean[] c2Subspace = new boolean[numDims];
				if (c2 instanceof SubspaceSphereCluster) {
					c2Subspace = ((SubspaceSphereCluster) c2).getAdjustedSubspace();
				} else {
					c2Subspace = fullSpace;
				}
				
				
				// Common dimensions
				int sharedDims = 0;
				for (int d = 0; d < numDims; d++) {
					if (c1Subspace[d] && c2Subspace[d])
						sharedDims++;
				}
				
				// Common objects
				int sharedObj = 0;
				for (SubspaceDataPoint p1 : pointsInGC.get(count1)) {
					if (pointsInFC.get(count2).contains(p1))
						sharedObj++;
				}
				
				cost[count1][count2] = sharedDims * sharedObj;
				count2++;
			}
			count1++;
		}
		
		// We need negative of the costs, since the algorithm minimizes the cost
		double[][] negCost = cost.clone();
		for (int i = 0; i < negCost.length; i++) {
			for (int j = 0; j < negCost[0].length; j++) {
				negCost[i][j] *= -1;
			}
		}
		
		HungarianAlgorithm H = new HungarianAlgorithm(negCost);
		int[] matching = H.execute();
		double dmax = 0;
		for (int i = 0; i < matching.length; i++) {
			dmax += cost[i][matching[i]];
		}
		dmax *= -1;		// Now we get the maximum
		
	    if (debug) System.out.println("CE: union = " + union + " / dmax = " + dmax);
    	double CE = 1 - (double)(union - dmax) / (double)union;
    	addSubValue("1.0-CE", CE);
	}
	
	/* Copyright (c) 2012 Kevin L. Stern
	 * 
	 * Permission is hereby granted, free of charge, to any person obtaining a copy
	 * of this software and associated documentation files (the "Software"), to deal
	 * in the Software without restriction, including without limitation the rights
	 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	 * copies of the Software, and to permit persons to whom the Software is
	 * furnished to do so, subject to the following conditions:
	 * 
	 * The above copyright notice and this permission notice shall be included in
	 * all copies or substantial portions of the Software.
	 * 
	 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
	 * SOFTWARE.
	 */

	/**
	 * An implementation of the Hungarian algorithm for solving the assignment
	 * problem. An instance of the assignment problem consists of a number of
	 * workers along with a number of jobs and a cost matrix which gives the cost of
	 * assigning the i'th worker to the j'th job at position (i, j). The goal is to
	 * find an assignment of workers to jobs so that no job is assigned more than
	 * one worker and so that no worker is assigned to more than one job in such a
	 * manner so as to minimize the total cost of completing the jobs.
	 * <p>
	 * 
	 * An assignment for a cost matrix that has more workers than jobs will
	 * necessarily include unassigned workers, indicated by an assignment value of
	 * -1; in no other circumstance will there be unassigned workers. Similarly, an
	 * assignment for a cost matrix that has more jobs than workers will necessarily
	 * include unassigned jobs; in no other circumstance will there be unassigned
	 * jobs. For completeness, an assignment for a square cost matrix will give
	 * exactly one unique worker to each job.
	 * <p>
	 * 
	 * This version of the Hungarian algorithm runs in time O(n^3), where n is the
	 * maximum among the number of workers and the number of jobs.
	 * 
	 * @author Kevin L. Stern
	 */
	public class HungarianAlgorithm {
		private final double[][] costMatrix;
		private final int rows, cols, dim;
		private final double[] labelByWorker, labelByJob;
		private final int[] minSlackWorkerByJob;
		private final double[] minSlackValueByJob;
		private final int[] matchJobByWorker, matchWorkerByJob;
		private final int[] parentWorkerByCommittedJob;
		private final boolean[] committedWorkers;

		/**
		 * Construct an instance of the algorithm.
		 * 
		 * @param costMatrix
		 *            the cost matrix, where matrix[i][j] holds the cost of
		 *            assigning worker i to job j, for all i, j. The cost matrix
		 *            must not be irregular in the sense that all rows must be the
		 *            same length.
		 */
		public HungarianAlgorithm(double[][] costMatrix) {
			this.dim = Math.max(costMatrix.length, costMatrix[0].length);
			this.rows = costMatrix.length;
			this.cols = costMatrix[0].length;
			this.costMatrix = new double[this.dim][this.dim];
			for (int w = 0; w < this.dim; w++) {
				if (w < costMatrix.length) {
					if (costMatrix[w].length != this.cols) {
						throw new IllegalArgumentException("Irregular cost matrix");
					}
					this.costMatrix[w] = Arrays.copyOf(costMatrix[w], this.dim);
				} else {
					this.costMatrix[w] = new double[this.dim];
				}
			}
			labelByWorker = new double[this.dim];
			labelByJob = new double[this.dim];
			minSlackWorkerByJob = new int[this.dim];
			minSlackValueByJob = new double[this.dim];
			committedWorkers = new boolean[this.dim];
			parentWorkerByCommittedJob = new int[this.dim];
			matchJobByWorker = new int[this.dim];
			Arrays.fill(matchJobByWorker, -1);
			matchWorkerByJob = new int[this.dim];
			Arrays.fill(matchWorkerByJob, -1);
		}

		/**
		 * Compute an initial feasible solution by assigning zero labels to the
		 * workers and by assigning to each job a label equal to the minimum cost
		 * among its incident edges.
		 */
		protected void computeInitialFeasibleSolution() {
			for (int j = 0; j < dim; j++) {
				labelByJob[j] = Double.POSITIVE_INFINITY;
			}
			for (int w = 0; w < dim; w++) {
				for (int j = 0; j < dim; j++) {
					if (costMatrix[w][j] < labelByJob[j]) {
						labelByJob[j] = costMatrix[w][j];
					}
				}
			}
		}

		/**
		 * Execute the algorithm.
		 * 
		 * @return the minimum cost matching of workers to jobs based upon the
		 *         provided cost matrix. A matching value of -1 indicates that the
		 *         corresponding worker is unassigned.
		 */
		public int[] execute() {
			/*
			 * Heuristics to improve performance: Reduce rows and columns by their
			 * smallest element, compute an initial non-zero dual feasible solution
			 * and create a greedy matching from workers to jobs of the cost matrix.
			 */
			reduce();
			computeInitialFeasibleSolution();
			greedyMatch();

			int w = fetchUnmatchedWorker();
			while (w < dim) {
				initializePhase(w);
				executePhase();
				w = fetchUnmatchedWorker();
			}
			int[] result = Arrays.copyOf(matchJobByWorker, rows);
			for (w = 0; w < result.length; w++) {
				if (result[w] >= cols) {
					result[w] = -1;
				}
			}
			return result;
		}

		/**
		 * Execute a single phase of the algorithm. A phase of the Hungarian
		 * algorithm consists of building a set of committed workers and a set of
		 * committed jobs from a root unmatched worker by following alternating
		 * unmatched/matched zero-slack edges. If an unmatched job is encountered,
		 * then an augmenting path has been found and the matching is grown. If the
		 * connected zero-slack edges have been exhausted, the labels of committed
		 * workers are increased by the minimum slack among committed workers and
		 * non-committed jobs to create more zero-slack edges (the labels of
		 * committed jobs are simultaneously decreased by the same amount in order
		 * to maintain a feasible labeling).
		 * <p>
		 * 
		 * The runtime of a single phase of the algorithm is O(n^2), where n is the
		 * dimension of the internal square cost matrix, since each edge is visited
		 * at most once and since increasing the labeling is accomplished in time
		 * O(n) by maintaining the minimum slack values among non-committed jobs.
		 * When a phase completes, the matching will have increased in size.
		 */
		protected void executePhase() {
			while (true) {
				int minSlackWorker = -1, minSlackJob = -1;
				double minSlackValue = Double.POSITIVE_INFINITY;
				for (int j = 0; j < dim; j++) {
					if (parentWorkerByCommittedJob[j] == -1) {
						if (minSlackValueByJob[j] < minSlackValue) {
							minSlackValue = minSlackValueByJob[j];
							minSlackWorker = minSlackWorkerByJob[j];
							minSlackJob = j;
						}
					}
				}
				if (minSlackValue > 0) {
					updateLabeling(minSlackValue);
				}
				parentWorkerByCommittedJob[minSlackJob] = minSlackWorker;
				if (matchWorkerByJob[minSlackJob] == -1) {
					/*
					 * An augmenting path has been found.
					 */
					int committedJob = minSlackJob;
					int parentWorker = parentWorkerByCommittedJob[committedJob];
					while (true) {
						int temp = matchJobByWorker[parentWorker];
						match(parentWorker, committedJob);
						committedJob = temp;
						if (committedJob == -1) {
							break;
						}
						parentWorker = parentWorkerByCommittedJob[committedJob];
					}
					return;
				} else {
					/*
					 * Update slack values since we increased the size of the
					 * committed workers set.
					 */
					int worker = matchWorkerByJob[minSlackJob];
					committedWorkers[worker] = true;
					for (int j = 0; j < dim; j++) {
						if (parentWorkerByCommittedJob[j] == -1) {
							double slack = costMatrix[worker][j]
									- labelByWorker[worker] - labelByJob[j];
							if (minSlackValueByJob[j] > slack) {
								minSlackValueByJob[j] = slack;
								minSlackWorkerByJob[j] = worker;
							}
						}
					}
				}
			}
		}

		/**
		 * 
		 * @return the first unmatched worker or {@link #dim} if none.
		 */
		protected int fetchUnmatchedWorker() {
			int w;
			for (w = 0; w < dim; w++) {
				if (matchJobByWorker[w] == -1) {
					break;
				}
			}
			return w;
		}

		/**
		 * Find a valid matching by greedily selecting among zero-cost matchings.
		 * This is a heuristic to jump-start the augmentation algorithm.
		 */
		protected void greedyMatch() {
			for (int w = 0; w < dim; w++) {
				for (int j = 0; j < dim; j++) {
					if (matchJobByWorker[w] == -1
							&& matchWorkerByJob[j] == -1
							&& costMatrix[w][j] - labelByWorker[w] - labelByJob[j] == 0) {
						match(w, j);
					}
				}
			}
		}

		/**
		 * Initialize the next phase of the algorithm by clearing the committed
		 * workers and jobs sets and by initializing the slack arrays to the values
		 * corresponding to the specified root worker.
		 * 
		 * @param w
		 *            the worker at which to root the next phase.
		 */
		protected void initializePhase(int w) {
			Arrays.fill(committedWorkers, false);
			Arrays.fill(parentWorkerByCommittedJob, -1);
			committedWorkers[w] = true;
			for (int j = 0; j < dim; j++) {
				minSlackValueByJob[j] = costMatrix[w][j] - labelByWorker[w]
						- labelByJob[j];
				minSlackWorkerByJob[j] = w;
			}
		}

		/**
		 * Helper method to record a matching between worker w and job j.
		 */
		protected void match(int w, int j) {
			matchJobByWorker[w] = j;
			matchWorkerByJob[j] = w;
		}

		/**
		 * Reduce the cost matrix by subtracting the smallest element of each row
		 * from all elements of the row as well as the smallest element of each
		 * column from all elements of the column. Note that an optimal assignment
		 * for a reduced cost matrix is optimal for the original cost matrix.
		 */
		protected void reduce() {
			for (int w = 0; w < dim; w++) {
				double min = Double.POSITIVE_INFINITY;
				for (int j = 0; j < dim; j++) {
					if (costMatrix[w][j] < min) {
						min = costMatrix[w][j];
					}
				}
				for (int j = 0; j < dim; j++) {
					costMatrix[w][j] -= min;
				}
			}
			double[] min = new double[dim];
			for (int j = 0; j < dim; j++) {
				min[j] = Double.POSITIVE_INFINITY;
			}
			for (int w = 0; w < dim; w++) {
				for (int j = 0; j < dim; j++) {
					if (costMatrix[w][j] < min[j]) {
						min[j] = costMatrix[w][j];
					}
				}
			}
			for (int w = 0; w < dim; w++) {
				for (int j = 0; j < dim; j++) {
					costMatrix[w][j] -= min[j];
				}
			}
		}

		/**
		 * Update labels with the specified slack by adding the slack value for
		 * committed workers and by subtracting the slack value for committed jobs.
		 * In addition, update the minimum slack values appropriately.
		 */
		protected void updateLabeling(double slack) {
			for (int w = 0; w < dim; w++) {
				if (committedWorkers[w]) {
					labelByWorker[w] += slack;
				}
			}
			for (int j = 0; j < dim; j++) {
				if (parentWorkerByCommittedJob[j] != -1) {
					labelByJob[j] -= slack;
				} else {
					minSlackValueByJob[j] -= slack;
				}
			}
		}
	}
}