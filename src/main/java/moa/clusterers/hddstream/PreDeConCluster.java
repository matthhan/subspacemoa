/**
 * [PreDeConCluster.java] for Subspace MOA
 * 
 * HDDStream: a set of 'PreDeConPoint's
 * 
 * @author Yunsu Kim (yunsu.kim@cs.rwth-aachen.de)
 * 		   based on the implementation by Pascal Spaus
 * Data Management and Data Exploration Group, RWTH Aachen University
 */

package moa.clusterers.hddstream;

import java.util.ArrayList;
import java.util.List;

import moa.cluster.CFCluster;

public class PreDeConCluster extends ArrayList<PreDeConPoint> {

	private static final long serialVersionUID = 1L;
	
	public List<CFCluster> toCFClusterList() {
		List<CFCluster> cfClusterList = new ArrayList<CFCluster>();
		for (PreDeConPoint p : this) {
			cfClusterList.add(p.getCFCluster());
		}
		return cfClusterList;
	}
	public List<weightedCFCluster> toWeightedCFClusterList() {
		List<weightedCFCluster> lis = new ArrayList<weightedCFCluster>();
		for (PreDeConPoint p : this) {
			lis.add(new weightedCFCluster(p.getCFCluster(),p.getWeight()));
		}
		return lis;
	}
}