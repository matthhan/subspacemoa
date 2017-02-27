/**
 * [PreDeConCluster.java] for Subspace MOA
 * 
 * PreDeConStream: a set of [MicroCluster]s
 * 
 * @author Yunsu Kim
 * 		   based on the implementation by Stephan Wels
 * Data Management and Data Exploration Group, RWTH Aachen University
 */
package moa.clusterers.predeconstream;

import java.util.ArrayList;
import java.util.List;

import moa.cluster.CFCluster;

public class PreDeConCluster extends ArrayList<MicroCluster> {

	private static final long serialVersionUID = 1L;
	
	public List<CFCluster> toCFClusterList() {
		List<CFCluster> cfClusterList = new ArrayList<CFCluster>();
		for (MicroCluster p : this) {
			cfClusterList.add(p);
		}
		return cfClusterList;
	}
	public List<MicroCluster> toMicroClusterList() {
		List<MicroCluster> cfClusterList = new ArrayList<MicroCluster>();
		for (MicroCluster p : this) {
			cfClusterList.add(p);
		}
		return cfClusterList;
	}
}