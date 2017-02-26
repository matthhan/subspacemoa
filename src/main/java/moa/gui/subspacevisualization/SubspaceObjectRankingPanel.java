/**
 * [SubspaceObjectRankingPanel.java] for Subspace MOA
 * 
 * Object ranking function (linked to OpenSubspace).
 * 
 * @author Yunsu Kim
 * 		   based on the implementation of Timm Jansen
 * Data Management and Data Exploration Group, RWTH Aachen University
 */

package moa.gui.subspacevisualization;

import i9.subspace.base.Cluster;

import java.awt.BorderLayout;
import java.util.ArrayList;

import javax.swing.JPanel;

import moa.cluster.SubspaceClustering;
import moa.cluster.SubspaceSphereCluster;
import moa.core.AutoExpandVector;
import weka.core.Attribute;
import weka.core.Instances;
import weka.gui.visualize.subspace.SubspaceVisualData;
import weka.gui.visualize.subspace.inDepth.InDepthGUI;

public class SubspaceObjectRankingPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	
	private static boolean debug = false;
	
	//private SubspaceRunVisualizer m_visualizer;
	private Instances m_instances;
	private ArrayList<Cluster> m_clustering;
	private InDepthGUI m_indepthGUI;
	private SubspaceVisualData m_svp;
	

	public SubspaceObjectRankingPanel(SubspaceRunVisualizer visualizer, int panelNo) {
		setLayout(new BorderLayout());
		setSize(500, 600);
		
		// Variable settings
		//m_visualizer = visualizer;
		m_instances = wekaInstances(visualizer.getPointsInLastWindow());
		if (panelNo == 0) {
			m_clustering = wekaSubspaceClustering(visualizer.getLastFoundClustering0(), visualizer.getPointsInLastWindow());
		} else if (panelNo == 1) {
			m_clustering = wekaSubspaceClustering(visualizer.getLastFoundClustering1(), visualizer.getPointsInLastWindow());
		} else {
			System.out.println("SubspaceObjectRankingPanel: Reference clustering should be of either 0 or 1");
		}
		
		if (debug) {
			System.out.println("m_Instances.size() = " + m_instances.size());
			System.out.println("m_clustering.size() = " + m_clustering.size());
		}
		
		// Calculate the visualization
		m_svp = new SubspaceVisualData();
		m_svp.calculateVisual(m_clustering, m_instances);
		if (debug) {
			System.out.println("m_svp.getInDepth().size() = " + m_svp.getInDepth().size());
		}
		
		// Visualize
		m_indepthGUI = new InDepthGUI(m_instances);
		add(m_indepthGUI);
		m_indepthGUI.plotRanking(m_svp.getInDepth());
	}
	
	/**
	 * Convert a MOA SubspaceClustering object to a list of WEKA OpenSubspace clusters.
	 *  
	 * @param moaSubspaceClustering
	 * @return
	 */
	private ArrayList<Cluster> wekaSubspaceClustering(SubspaceClustering moaSubspaceClustering, ArrayList<SubspaceDataPoint> moaSubspacePoints) {
		AutoExpandVector<moa.cluster.Cluster> moaClusters = moaSubspaceClustering.getClustering();
		ArrayList<Cluster> wekaClusters = new ArrayList<Cluster>();
		
		double inclusionProbability = 0.5;
		for (moa.cluster.Cluster c : moaClusters) {
			ArrayList<Integer> clusteredObjects = new ArrayList<Integer>();
			for (int p = 0; p < moaSubspacePoints.size(); p++) {
				if (c.getInclusionProbability(moaSubspacePoints.get(p)) > inclusionProbability) {
					clusteredObjects.add(p);
				}
			}
			
			int numDims = moaSubspacePoints.get(0).numAttributes();
			boolean[] subspace = new boolean[numDims];
			if (c instanceof SubspaceSphereCluster) {
				subspace = ((SubspaceSphereCluster) c).getAdjustedSubspace();
			} else {
				for (int j = 0; j < numDims; j++) {
					subspace[j] = true; 
				}
			}
			
			wekaClusters.add(new Cluster(subspace, clusteredObjects));
		}
		
		return wekaClusters;
	}
	
	/**
	 * Convert a list of MOA SubspaceDataPoint objects to a WEKA Instances object.
	 * 
	 * @param moaSubspacePoints
	 * @return
	 */
	private Instances wekaInstances(ArrayList<SubspaceDataPoint> moaSubspacePoints) {
		
		// Initialize "Instances" (to return)
		String name = "instances";
		ArrayList<Attribute> attInfo = new ArrayList<Attribute>();
		for (int i = 0; i < moaSubspacePoints.get(0).numAttributes(); i++) {
			attInfo.add(new Attribute("dim" + i));
		}
		int capacity = moaSubspacePoints.size();
		Instances wekaInstances = new Instances(name, attInfo, capacity);
		
		// Convert
		for (SubspaceDataPoint sdp : moaSubspacePoints) {
			wekaInstances.add(sdp);
		}
		
		return wekaInstances;
	}
}
