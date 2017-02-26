/**
 * [SubCMM.java] for Subspace MOA
 * 
 * Evaluation measure: SubCMM
 * 
 * Reference: Hassani et al., "Effective Evaluation Measures for Subspace Clustering of Data Streams", QIMIE (PAKDD), 2013
 * 
 * @author Yunsu Kim
 * Data Management and Data Exploration Group, RWTH Aachen University
 */

package moa.evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import moa.cluster.Cluster;
import moa.cluster.SubspaceClustering;
import moa.cluster.SubspaceSphereCluster;
import moa.core.AutoExpandVector;
import moa.gui.subspacevisualization.SubspaceDataPoint;

public class SubCMM extends SubspaceMeasureCollection {
	
	private static final long serialVersionUID = 1L;

	private boolean debug = false;
	
	private List<SubspaceDataPoint> points;
	private AutoExpandVector<Cluster> foundClusters;
	private AutoExpandVector<Cluster> gtClusters;
	private List<List<SubspaceDataPoint>> pointsInFC;
	private List<SubspaceDataPoint> pointsUnassigned;
	private List<List<SubspaceDataPoint>> pointsInGC;
	private List<SubspaceDataPoint> pointsCLnoise;
	private SubspaceSphereCluster CLnoise;
	private HashMap<Double, Integer> labelMap;
	
	/* Full-space cases */
	private List<Integer> fullDims;
	private int numDims;
	private boolean[] fullSpace;
		
	/* Cluster mapping */
	private int[] map;
	
	/* Penalty calculation */
	private double[][] conToOrgGT;
	private double[] knhDimDist_C, knhObjDist_C;
	private double[][] knhDimDist_jC, knhObjDist_pC;
	private double[][] maxPen;
	private final int k = 2;
	
	
	@Override
    protected String[] getNames() {
        String[] names = {"SubCMM"};
        return names;
    }

    @Override
    protected boolean[] getDefaultEnabled() {
        boolean [] defaults = {true};
        return defaults;
    }

    @Override
    protected void subEvaluateSubspaceClustering(SubspaceClustering foundClustering, SubspaceClustering gtClustering, List<SubspaceDataPoint> points) throws Exception {
		this.points = points;
		foundClusters = foundClustering.getClustering();
		gtClusters = gtClustering.getClustering();
		pointsInFC = new ArrayList<List<SubspaceDataPoint>>();
		pointsUnassigned = new ArrayList<SubspaceDataPoint>();
		pointsInGC = new ArrayList<List<SubspaceDataPoint>>();
		pointsCLnoise = new ArrayList<SubspaceDataPoint>();
		
    	double inclusionProbabilityThreshold = 0.5;
    	numDims = points.get(0).numAttributes() - 1;
    	fullSpace = new boolean[numDims];
    	for (int j = 0; j < numDims; j++) {
    		fullSpace[j] = true;
    	}
    	fullDims = new ArrayList<Integer>();
    	
    	// Preprocess: assign points to clusters
    	for (int i = 0; i < foundClusters.size(); i++) {
    		pointsInFC.add(new ArrayList<SubspaceDataPoint>());
    	}

    	for (int i = 0; i < gtClusters.size(); i++) {
    		pointsInGC.add(new ArrayList<SubspaceDataPoint>());
    	}

    	for (SubspaceDataPoint p : points) {
    		int numAssignedFC = 0;
    		for (int i = 0; i < foundClusters.size(); i++) {
    			Cluster fc = foundClusters.get(i);
    			if (fc.getInclusionProbability(p) >= inclusionProbabilityThreshold) {
    				pointsInFC.get(i).add(p);
    				numAssignedFC++;
    			}
    		}
    		if (numAssignedFC == 0)
    			pointsUnassigned.add(p);
    		
    		int numAssignedGC = 0;
    		for (int i = 0; i < gtClusters.size(); i++) {
    			Cluster gc = gtClusters.get(i);
    			if (gc.getInclusionProbability(p) >= inclusionProbabilityThreshold) {
    				pointsInGC.get(i).add(p);
    				numAssignedGC++;
    			}
    		}
    		if (numAssignedGC == 0)
    			pointsCLnoise.add(p);
    	}
    	
    	// DEBUG: print the ground truth
    	if (debug) {
    		System.out.println("-- SubCMM: Ground truth --");
    		System.out.print("pointsInGC(size): ");
    		for (int i = 0; i < pointsInGC.size(); i++)
    			System.out.print(pointsInGC.get(i).size() + " ");
    		System.out.println();
    		System.out.println("pointsCLnoise: " + pointsCLnoise.size());
    		System.out.println();
    	}
    	
    	// DEBUG: print the found clustering
    	if (debug) {
    		System.out.println("-- SubCMM: Found clustering --");
    		System.out.print("pointsInFC(size): ");
    		for (int i = 0; i < pointsInFC.size(); i++)
    			System.out.print(pointsInFC.get(i).size() + " ");
    		System.out.println();
    		System.out.println("pointsUnassigned: " + pointsUnassigned.size());
    		System.out.println();
    	}
    	
    	// Ground truth noise cluster
		CLnoise = new SubspaceSphereCluster();
		CLnoise.setGroundTruth(points.get(0).getNoiseLabel());
		boolean[] subspaceCLnoise = new boolean[numDims];
		for (int j = 0; j < subspaceCLnoise.length; j++)
			subspaceCLnoise[j] = true;
		CLnoise.setSubspace(subspaceCLnoise);

    	// Map: class label -> index integer
    	labelMap = gtClustering.getLabelMap();

    	map = new int[foundClustering.size()];	// Mapping: found cluster -> GT cluster
    	
    	// For later use
    	for (int j = 0; j < numDims; j++) {
    		fullDims.add(j);
    	}
    	
    	clusterMappingPhase();
    	
    	
    	// Initializing the storages of "connectivity"
    	
    	conToOrgGT = new double[points.size()][points.get(0).getClassLabels().length];
    	for (int i = 0; i < conToOrgGT.length; i++) {
    		for (int j = 0; j < conToOrgGT[i].length; j++)
    			conToOrgGT[i][j] = -1;
    	}
    	knhDimDist_C = new double[labelMap.size()];
    	for (int c = 0; c < knhDimDist_C.length; c++) {
    		knhDimDist_C[c] = -1;
    	}
    	knhDimDist_jC = new double[points.get(0).getClassLabels().length][labelMap.size()];
    	for (int j = 0; j < points.get(0).getClassLabels().length; j++) {
    		for (int c = 0; c < labelMap.size(); c++)
    			knhDimDist_jC[j][c] = -1;
    	}
    	knhObjDist_C = new double[labelMap.size()];
    	for (int c = 0; c < knhObjDist_C.length; c++) {
    		knhObjDist_C[c] = -1;
    	}
    	knhObjDist_pC = new double[points.size()][labelMap.size()];
    	for (int i = 0; i < points.size(); i++) {
    		for (int c = 0; c < labelMap.size(); c++)
    			knhObjDist_pC[i][c] = -1;
    	}
    	maxPen = new double[points.size()][points.get(0).getClassLabels().length];
    	for (int i = 0; i < maxPen.length; i++) {
    		for (int j = 0; j < maxPen[i].length; j++)
    			maxPen[i][j] = 0.0;
    	}
    	
    	penaltyCalculationPhase();
    }

    
    
    /**---------------------------- Cluster mapping phase ----------------------------**/
    
    
    
    /**
     * Cluster mapping.
     * 
     * @param foundClusters
     * @param gtClusters
     * @param pointsInFC
     * @param pointsInGC
     * @param labelMap
     */
    private void clusterMappingPhase() {
    	
    	/* Try one of these! */
    	mappingWithClassDistribution();
    	//mappingWithRNIA(foundClusters, gtClusters, pointsInFC, pointsInGC);
    	if (debug) {
    		System.out.println("-- SubCMM: Cluster mappings --");
    		for (int i = 0; i < map.length; i++)
    			System.out.println("found " + i + " => true " + map[i]);
    		System.out.println();
    	}
    }
    
    private void mappingWithClassDistribution() {

    	// Initializing
    	int classDistributionFC[][] = new int[foundClusters.size()][gtClusters.size()];
    	int classDistributionGC[][] = new int[gtClusters.size()][gtClusters.size()];
    	
    	for (int i = 0; i < classDistributionFC.length; i++)
    		for (int j = 0; j < classDistributionFC[0].length; j++)
    			classDistributionFC[i][j] = 0;
    	
    	for (int i = 0; i < classDistributionGC.length; i++)
    		for (int j = 0; j < classDistributionGC[0].length; j++)
    			classDistributionGC[i][j] = 0;
    	
    	// Class distribution of found clusters
    	for (int i = 0; i < foundClusters.size(); i++) {
    		Cluster fc = foundClusters.get(i);
    		List<Integer> relevantDims;
    		if (fc instanceof SubspaceSphereCluster) {
    			relevantDims = ((SubspaceSphereCluster) fc).getRelevantDims();
    		} else {
    			relevantDims = fullDims;
    		}
    		
    		List<SubspaceDataPoint> includedPoints = pointsInFC.get(i);
    		
    		for (SubspaceDataPoint p : includedPoints) {
    			for (int d : relevantDims) {
    				if (p.getClassLabel(d) != p.getNoiseLabel())	// Don't count noise subobjects
    					classDistributionFC[i][labelMap.get(p.getClassLabel(d))]++;
    			}
    		}
    	}
    	
    	// Class distribution of GT clusters
    	for (int i = 0; i < gtClusters.size(); i++) {
    		Cluster gc = gtClusters.get(i);
    		List<Integer> relevantDims;
    		if (gc instanceof SubspaceSphereCluster) {
    			relevantDims = ((SubspaceSphereCluster) gc).getRelevantDims();	
    		} else {
    			relevantDims = fullDims;
    		}
    		
    		List<SubspaceDataPoint> includedPoints = pointsInGC.get(i);
    		
    		for (SubspaceDataPoint p : includedPoints) {
    			for (int d : relevantDims) {
    				if (p.getClassLabel(d) != p.getNoiseLabel())	// Don't count noise subobjects
    					classDistributionGC[i][labelMap.get(p.getClassLabel(d))]++;
    			}
    		}
    	}
    	
    	// Mapping: found cluster -> GT cluster
    	for (int i = 0; i < foundClusters.size(); i++) {
    		int minSurplus = Integer.MAX_VALUE;
    		int minSurplusGC = -1;
    		List<Integer> fitGC = new ArrayList<Integer>();

    		// Calculate surpluses
    		for (int j = 0; j < gtClusters.size(); j++) {
    			int surplus = 0;
    			for (int c = 0; c < gtClusters.size(); c++) {
	    			int surplusPerClass = classDistributionFC[i][c] - classDistributionGC[j][c];
	    			if (surplusPerClass < 0) surplusPerClass = 0;
	    			surplus += surplusPerClass;
    			}

    			if (surplus == 0) {
    				fitGC.add(j);
    			} else if (surplus < minSurplus) {
    				minSurplus = surplus;
    				minSurplusGC = j;
    			}
    		}

    		if (!fitGC.isEmpty()) {
    			// Alternative: majority voting
    			int bestFitGC = fitGC.get(0);
                for (int j = 1; j < fitGC.size(); j++) {
                    int curFitGC = fitGC.get(j);
                    if (classDistributionFC[i][curFitGC] > classDistributionFC[i][bestFitGC])
                        bestFitGC = curFitGC;
                }
                map[i] = bestFitGC;
    		} else {
    			map[i] = minSurplusGC;
    		}
    	}
    }
    
    /*private void mappingWithRNIA() {
    	for (int i = 0; i < foundClusters.size(); i++) {
    		Cluster fc = foundClusters.get(i);
    		int numRelevantDimsFC;
    		boolean[] subspaceFC;
    		
    		if (fc instanceof SubspaceSphereCluster) {
    			numRelevantDimsFC = ((SubspaceSphereCluster) fc).getSubspaceSize();
    			subspaceFC = ((SubspaceSphereCluster) fc).getSubspace();
    		} else {
    			numRelevantDimsFC = numDims;
    			subspaceFC = fullSpace;
    		}
    		
    		int areaFC = pointsInFC.get(i).size() * numRelevantDimsFC;
    		
    		double minRNIA = Double.MAX_VALUE;
    		int minRNIAGC = -1;
    		
    		for (int j = 0; j < gtClusters.size(); j++) {
    			Cluster gc = gtClusters.get(j);
        		int numRelevantDimsGC;
        		boolean[] subspaceGC;
        		
        		if (gc instanceof SubspaceSphereCluster) {
        			numRelevantDimsGC = ((SubspaceSphereCluster) gc).getSubspaceSize();
        			subspaceGC = ((SubspaceSphereCluster) gc).getSubspace();
        		} else {
        			numRelevantDimsGC = numDims;
        			subspaceGC = fullSpace;
        		}
    			
    			int areaGC = pointsInGC.get(j).size() * numRelevantDimsGC;
    			
    			int numCommonDims = 0;
    			for (int d = 0; d < subspaceFC.length; d++) {
    				if (subspaceFC[d] == true && subspaceGC[d] == true)
    					numCommonDims++;
    			}
    			
    			int numCommonPts = 0;
    			for (int o = 0; o < pointsInFC.get(i).size(); o++) {
    				if (pointsInGC.get(j).contains(pointsInFC.get(i).get(o)))
    					numCommonPts++;
    			}
    			
    			int I = numCommonDims * numCommonPts;
    			int U = areaFC + areaGC - I;
    			double RNIA = (double)(U - I) / (double)U;
    			if (RNIA < minRNIA) {
    				minRNIA = RNIA;
    				minRNIAGC = j;
    			}
    		}
    		
    		map[i] = minRNIAGC;
    	}
    }*/
    
    
    
    
    /*********************************** Penalty calculation phase ***********************************/
    
    /**
     * Calculate penalties of fault subobjects and final value of SubCMM.
     * 
     * @param foundClusters
     * @param gtClusters
     * @param pointsInFC
     * @param pointsUnassigned
     * @param pointsInGC
     * @param CLnoise
     * @param pointsCLnoise
     * @param labelMap
     */
    private void penaltyCalculationPhase() {    	
		
		// Connectivity to its original GT cluster: All subobjects!!
		double weightedSumOfConnectivityToGT = 0.0;		// To normalize final CMM value
		for (int i = 0; i < points.size(); i++) {
			SubspaceDataPoint p = points.get(i);
			for (int j = 0; j < p.getClassLabels().length; j++) {
				double orgGTClusterLabel = p.getClassLabel(j);
				int orgGTClusterIndex = labelMap.get(orgGTClusterLabel);
				double con = 0.0;
				if (orgGTClusterLabel == p.getNoiseLabel()) {
					con = pointConnectivity(i, j, CLnoise, orgGTClusterIndex, pointsCLnoise);
				} else {
					con = pointConnectivity(i, j, gtClusters.get(orgGTClusterIndex), orgGTClusterIndex, pointsInGC.get(orgGTClusterIndex));
				}
				
				if (Double.isNaN(con)) {
					System.out.println("NaN error in connectivity of (" + i + ", " + j + ") to the cluster " + orgGTClusterIndex);
				}
				
				weightedSumOfConnectivityToGT += p.weight() * con;
				conToOrgGT[i][j] = con;
			}
		}

		double weightedSumOfPenalty = 0.0;
    	int modelError = 0, missed = 0, misplaced = 0, noise = 0;
    	
    	for (int i = 0; i < points.size(); i++) {
    		SubspaceDataPoint p = points.get(i);
    		
    		for (int f = 0; f < pointsInFC.size(); f++) {
    			if (pointsInFC.get(f).contains(p)) {
    				Cluster fc = foundClusters.get(i);
    	    		List<Integer> relevantDims;
    	    		if (fc instanceof SubspaceSphereCluster) {
    	    			relevantDims = ((SubspaceSphereCluster) fc).getAdjustedRelevantDims();
    	    		} else {
    	    			relevantDims = fullDims;
    	    		}
    	    		
        			for (int j : relevantDims) {
        				int orgGTClusterIndex = labelMap.get(p.getClassLabel(j));
        				int mapGTClusterIndex = map[f];
        				if (mapGTClusterIndex != orgGTClusterIndex) {
        					Cluster mappedGC = gtClusters.get(mapGTClusterIndex);
        					boolean JisRelevant;
        					if (mappedGC instanceof SubspaceSphereCluster) {
        						JisRelevant = ((SubspaceSphereCluster) mappedGC).isRelevant(j);
        					} else {
        						JisRelevant = true;
        					}
        					
        					if (pointsInGC.get(mapGTClusterIndex).contains(p) && JisRelevant) {		// Model error: ignored
        						modelError++;
        						continue;
        					}
        					
        					// Connectivities
        					if (p.getClassLabel(j) == p.getNoiseLabel()) {					/** Noise inclusion **/
        						noise++;
    
        					} else {										/** Misplaced **/
        						misplaced++;
    
        					}
        					double conToMapGT = pointConnectivity(i, j, gtClusters.get(mapGTClusterIndex), mapGTClusterIndex, pointsInGC.get(mapGTClusterIndex));
    
        					// Penalty
        					double pen = conToOrgGT[i][j] * (1 - conToMapGT);
        					if (pen > maxPen[i][j])
        						maxPen[i][j] = pen;
        				}
        			}
    			}
    		}
    		
    		if (pointsUnassigned.contains(p)) {
	    		for (int j = 0; j < p.getClassLabels().length; j++) {
					int orgGTClusterIndex = labelMap.get(p.getClassLabel(j));
					if (labelMap.get(p.getNoiseLabel()) != null && orgGTClusterIndex != labelMap.get(p.getNoiseLabel())) {
						continue;
					} else {		/** Missed **/
						if (pointsCLnoise.contains(p)) {		// Model error: ignored
							modelError++;
							continue;
						}
						
						missed++;
						// Connectivities
						double conToMapGT = pointConnectivity(i, j, null, (int)p.getNoiseLabel(), null);	// No mapping established
	    					
						// Penalty
						double pen = conToOrgGT[i][j] * (1 - conToMapGT);
						if (pen > maxPen[i][j])
							maxPen[i][j] = pen;
					}
				}
    		}
    		
    		// Update weighted sum of penalties
    		for (int j = 0; j < p.getClassLabels().length; j++) {
    			weightedSumOfPenalty += p.weight() * maxPen[i][j];
    		}
    	}
     	
    	double SubCMM = 1.0;
    	if (weightedSumOfConnectivityToGT != 0.0)
    		SubCMM = 1 - (weightedSumOfPenalty / weightedSumOfConnectivityToGT);
    	
    	// Final CMM value
    	addSubValue("SubCMM", SubCMM);
    	if (debug) {
	    	System.out.println("SubCMM = " + SubCMM);
	    	System.out.println("weightedSumOfPenalty = " + weightedSumOfPenalty + ", weightedSumOfConnectivityToGT = " + weightedSumOfConnectivityToGT);
	    	System.out.println("Missed = " + missed + ", Misplaced = " + misplaced + ", Noise inclusion = " + noise + ", Model error = " + modelError + " / Total #subobjects = " + (conToOrgGT.length * conToOrgGT[0].length));
	    	System.out.println();
    	}
    }
    
    
    /**
     * Subobject connectivity to a ground truth cluster.
     * 
     * @param i - object index
     * @param j - dimension of subobject
     * @param C - cluster (ground truth)
     * @param clusterIndex
     * @param pointsInC - (ground truth)
     * @return connectivity
     */
    private double pointConnectivity(int i, int j, Cluster C, int clusterIndex, List<SubspaceDataPoint> pointsInC) {
    	
    	SubspaceDataPoint p = points.get(i);
    	
    	if (C == null) {	// Connectivity to nothing
    		return 0.0;
    	}
    	
    	/* Prepare basic data about C */
    	List<Integer> relevantDimsC;
		int subspaceSizeC;
		boolean[] subspaceC = new boolean[numDims];
		
		if (C instanceof SubspaceSphereCluster) {
			relevantDimsC = ((SubspaceSphereCluster) C).getAdjustedRelevantDims();
			subspaceSizeC = ((SubspaceSphereCluster) C).getAdjustedSubspaceSize();
			subspaceC = ((SubspaceSphereCluster) C).getAdjustedSubspace();
		} else {
			relevantDimsC = fullDims;
			subspaceSizeC = numDims;
			subspaceC = fullSpace;
		}
    	
    	    	    	
    	// Subspace connectivity: avg. k-NN distance w.r.t dimensions
    	double subCon;
    	if (pointsInC == null) {				// p is Unassigned
    		subCon = 0;
    	} else {
    		
    		
	    	// knhDimDist(C)
	    	if (knhDimDist_C[clusterIndex] == -1.0) {
	    		double knhDimDistC = 0.0;
	    		
	    		for (int a : relevantDimsC) {
	    			ArrayList<Double> knhDimDistList = new ArrayList<Double>();
	    			
	    			for (int b : relevantDimsC) {
	    				if (b == a)
	    					continue;
	    				
	    				double dist = dimDistanceInCluster(a, b, pointsInC);
	    				if (knhDimDistList.size() < k									// First k cases
	    					|| dist < knhDimDistList.get(knhDimDistList.size() - 1)) {	// Smaller than the current k-th dist
	    					int index = 0;
	    					while (index < knhDimDistList.size() && dist > knhDimDistList.get(index)) {
	    						index++;
		                    }
		                    
	    					knhDimDistList.add(index, dist);
		                    if (knhDimDistList.size() > k) {
		                        knhDimDistList.remove(knhDimDistList.size() - 1);
		                    }
	    				}
	    			}
	    				
	    			double avgKnhDimDist = 0.0;
	    			for (int l = 0; l < knhDimDistList.size(); l++) {
	                    avgKnhDimDist += knhDimDistList.get(l);
	                }
	                if (knhDimDistList.size() != 0)
	                	avgKnhDimDist /= (double)knhDimDistList.size();
	                
	                knhDimDistC += avgKnhDimDist;
	            }
	    		
	    		
	    		knhDimDistC /= (double) subspaceSizeC;
	    		knhDimDist_C[clusterIndex] = knhDimDistC;
	    	}
	    	
	    	// knhDimDist(j,C)
	    	if (knhDimDist_jC[j][clusterIndex] == -1.0) {		
	    		ArrayList<Double> knhDimDistList = new ArrayList<Double>();
				
				for (int b : relevantDimsC) {
					if (b == j)
						continue;
					
					double dist = dimDistanceInCluster(j, b, pointsInC);
					if (knhDimDistList.size() < k									// First k cases
						|| dist < knhDimDistList.get(knhDimDistList.size() - 1)) {	// Smaller than the current k-th dist
						int index = 0;
						while (index < knhDimDistList.size() && dist > knhDimDistList.get(index)) {
							index++;
		                }
		                
						knhDimDistList.add(index, dist);
		                if (knhDimDistList.size() > k) {
		                    knhDimDistList.remove(knhDimDistList.size() - 1);
		                }
					}
				}
					
				double avgKnhDimDist = 0.0;
				for (int l = 0; l < knhDimDistList.size(); l++) {
	                avgKnhDimDist += knhDimDistList.get(l);
	            }
	            if (knhDimDistList.size() != 0)
	            	avgKnhDimDist /= (double)knhDimDistList.size();
	            
	            knhDimDist_jC[j][clusterIndex] = avgKnhDimDist;
	    	}
	    	
			// Now we have knhDimDist(C) and knhDimDist(j,C), so:
	    	if (knhDimDist_jC[j][clusterIndex] < knhDimDist_C[clusterIndex]) {
	    		subCon = 1.0;
	    	} else {
	    		subCon = knhDimDist_C[clusterIndex] / knhDimDist_jC[j][clusterIndex];
	    	}
	    	
	    	if (debug) {
	    		if (Double.isNaN(subCon)) {
	    			System.out.println("subCon NaN in (" + i + ", " + j + ") to the cluster " + clusterIndex);
	    			System.out.println("knhDimDist_C[" + clusterIndex + "] = " + knhDimDist_C[clusterIndex] + ", knhDimDist_jC[" + j + "][" + clusterIndex + "] = " + knhDimDist_jC[j][clusterIndex]);
	    			System.out.println("C.getAdjustedSubspaceSize() = " + subspaceSizeC);
	    		}
	    	}
    	}
    	
    	// Object connectivity: avg. k-NN distance w.r.t objects
    	double objCon;
    	if (pointsInC == null) {				// p is Unassigned
    		objCon = 0;
    	} else {
	    	// knhObjDist(C)
	    	if (knhObjDist_C[clusterIndex] == -1.0) {		
	    		double knhDistC = 0.0;
	    		
	    		for (SubspaceDataPoint x : pointsInC) {
	    			ArrayList<Double> knhObjDistList = new ArrayList<Double>();
	    			
	    			for (SubspaceDataPoint y : pointsInC) {
	    				if (y.equals(x))
	    					continue;
	    				
	    				double dist = objDistanceInSubspace(x, y, subspaceC);
	    				if (knhObjDistList.size() < k									// First k cases
	    					|| dist < knhObjDistList.get(knhObjDistList.size() - 1)) {	// Smaller than the current k-th dist
	    					int index = 0;
	    					while (index < knhObjDistList.size() && dist > knhObjDistList.get(index)) {
	    						index++;
		                    }
		                    
	    					knhObjDistList.add(index, dist);
		                    if (knhObjDistList.size() > k) {
		                        knhObjDistList.remove(knhObjDistList.size() - 1);
		                    }
	    				}
	    			}
	    				
	    			double avgKnhDist = 0.0;
	    			for (int l = 0; l < knhObjDistList.size(); l++) {
	                    avgKnhDist += knhObjDistList.get(l);
	                }
	                if (knhObjDistList.size() != 0)
	                	avgKnhDist /= (double)knhObjDistList.size();
	                
	                knhDistC += avgKnhDist;
	            }
	    		
	    		knhDistC /= (double)pointsInC.size();
	    		knhObjDist_C[clusterIndex] = knhDistC;
	    	}
	    	
	    	// knhObjDist(p,C)
	    	if (knhObjDist_pC[i][clusterIndex] == -1.0) {		
	    		ArrayList<Double> knhDistList = new ArrayList<Double>();
				
				for (SubspaceDataPoint y : pointsInC) {
					if (y.equals(p))
						continue;
					
					double dist = objDistanceInSubspace(p, y, subspaceC);
					if (knhDistList.size() < k									// First k cases
						|| dist < knhDistList.get(knhDistList.size() - 1)) {	// Smaller than the current k-th dist
						int index = 0;
						while (index < knhDistList.size() && dist > knhDistList.get(index)) {
							index++;
		                }
		                
						knhDistList.add(index, dist);
		                if (knhDistList.size() > k) {
		                    knhDistList.remove(knhDistList.size() - 1);
		                }
					}
				}
					
				double avgKnhDist = 0.0;
				for (int l = 0; l < knhDistList.size(); l++) {
	                avgKnhDist += knhDistList.get(l);
	            }
	            if (knhDistList.size() != 0)
	            	avgKnhDist /= (double)knhDistList.size();
	            
	            knhObjDist_pC[i][clusterIndex] = avgKnhDist;
	    	}
	    	
			// Now we have knhObjDist(C) and knhObjDist(p,C), so:
	    	if (knhObjDist_pC[i][clusterIndex] < knhObjDist_C[clusterIndex]) {
	    		objCon = 1.0;
	    	} else {
	    		objCon = knhObjDist_C[clusterIndex] / knhObjDist_pC[i][clusterIndex];
	    	}
	    	
	    	if (debug) {
	    		if (Double.isNaN(subCon))
	    			System.out.println("objCon NaN in (" + i + ", " + j + ") to the cluster " + clusterIndex);
	    	}
    	}
    	
    	// Overall connectivity
    	return subCon * objCon;
    }
    
    private double dimDistanceInCluster(int d1, int d2, List<SubspaceDataPoint> pointsInC) {
    	double distance = 0.0;
		for (SubspaceDataPoint p : pointsInC) {
			double d = p.value(d1) - p.value(d2);
			distance += d * d;
		}
		return Math.sqrt(distance);
    }
    
	private double objDistanceInSubspace(SubspaceDataPoint p1, SubspaceDataPoint p2, boolean[] subspace) {
    	double distance = 0.0;
		for (int j = 0; j < subspace.length; j++) {
			if (subspace[j]) {
				double d = p1.value(j) - p2.value(j);
				distance += d * d;
			}
		}
		return Math.sqrt(distance);
    }
}