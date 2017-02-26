/**
 * [DenPoint.java] for Subspace MOA
 * 
 * HDDStream: incoming stream point
 * 
 * @author Yunsu Kim
 * 		   based on the implementation by Pascal Spaus
 * Data Management and Data Exploration Group, RWTH Aachen University
 */

package moa.clusterers.hddstream;

import weka.core.DenseInstance;
import weka.core.Instance;

public class DenPoint extends DenseInstance {
	
	private static final long serialVersionUID = 1L;
	
	protected long creationTimestamp;
	protected boolean covered;

	public DenPoint(Instance nextInstance, long timestamp) {
		super(nextInstance);
		this.setDataset(nextInstance.dataset());
		this.creationTimestamp = timestamp;
		this.covered = false;
	}
	
	public long getCreationTimestamp() {
		return creationTimestamp;
	}
	
	/* For iterations */
	public boolean isCovered() {
		return covered;
	}
	
	public void setCovered() {
		this.covered = true;
	}
}