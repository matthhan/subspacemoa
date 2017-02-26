/**
 * [SubspaceDataPoint.java] for Subspace MOA
 * 
 * A streaming instance for subspace clustering.
 * (not just for visualization)
 * 
 * @author Yunsu Kim
 * 		   based on the implementation of Timm Jansen
 * Data Management and Data Exploration Group, RWTH Aachen University
 */

package moa.gui.subspacevisualization;

import java.util.HashMap;
import java.util.Set;

import moa.core.SubspaceInstance;
import moa.gui.visualization.DataPoint;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

public class SubspaceDataPoint extends DataPoint {

	private static final long serialVersionUID = 1L;
	
	protected double[] classLabels;		// Class label for each attribute value
	
	public SubspaceDataPoint(SubspaceInstance nextInstance, Integer timestamp) {
		super(nextInstance, timestamp);
		this.classLabels = nextInstance.getClassLabels();
	}
	
	public SubspaceDataPoint(SubspaceDataPoint point, Integer timestamp) {
		super(point, timestamp);
		this.classLabels = point.getClassLabels();
	}
		
	public double[] getClassLabels() {
		return classLabels;
	}
	
	public double getClassLabel(int attIndex) {
		return this.classLabels[attIndex];
	}
	
	public Set<Double> getClassLabelSet() {		
		HashMap<Double, Integer> classLabelMap = new HashMap<Double, Integer>();
		
		int index = 0;
		for (double label : classLabels) {
			if (!classLabelMap.containsKey(label)) {
				classLabelMap.put(label, index);
				index++;
			}
		}
		return classLabelMap.keySet();
	}
	

	/**
	 * Extract the subspace of a given class label.
	 * If a point is not in the cluster of the given label, returns null. 
	 * 
	 * @param classLabel
	 * @return 
	 */
	public boolean[] getSubspace(double classLabel) {
		boolean[] subspace = new boolean[classLabels.length];
		boolean valid = false;
		for (int j = 0; j < classLabels.length; j++) {
			if (classLabels[j] == classLabel) {
				subspace[j] = true;
				valid = true;
			} else {
				subspace[j] = false;
			}
		}
		
		if (!valid) {
			System.out.println("SubspaceDataPoint.getSubspace(): given label is not valid");
			return null;
		}

		return subspace;
	}
	
	/**
	 * Helper: Get a subspace indicating all non-noise dimensions.
	 * 
	 * @return
	 */
	public boolean[] getSubspace() {
		boolean[] subspace = new boolean[classLabels.length];
		for (int j = 0; j < classLabels.length; j++) {
			if (classLabels[j] == noiseLabel)
				subspace[j] = false;
			else
				subspace[j] = true;
		}
		return subspace;
	}
	
	
	/**
	 * Class label by dimension.
	 * 
	 * @param dim
	 * @return
	 */
	public double classValue(int dim) {
		double res;
		try {
			res=this.classLabels[dim];
		} catch (java.lang.ArrayIndexOutOfBoundsException e) {
			System.out.println("classLabels[" + dim + "] not found");
			res = 0;
		}
		return res;
	}
	
	@Override
	public String getInfo(int x_dim, int y_dim) {
        StringBuffer sb = new StringBuffer();
        sb.append("<html><table>");
        sb.append("<tr><td>Point</td><td>"+timestamp+"</td></tr>");
        sb.append("<tr><td>"+"Dim"+"</td><td>"+"Value"+"</td><td>"+"ClassLabel"+"</td></tr>");
        for (int j = 0; j < m_AttValues.length-1; j++) {
            String dim = "Dim " + j;
            if (j == x_dim) {
            	dim = "<b>X</b>";
            } else if (j == y_dim) {
                dim = "<b>Y</b>";
            }
            sb.append("<tr><td>"+dim+"</td><td>"+value(j)+"</td><td align=right>"+(int)classValue(j)+"</td></tr>");
        }
        //These lines and the -1 in the for-loop condition were added by M.Hansen on 28.Oct.2015 to address a 
        //bug, that only occured on the last iteration of this particular loop. The original code was apparently written
        //Under the assumption the m_attValues and this.classLabels are always of the same length. This is not the
        //case and therefore the last iteration has been changed so that classValue(j) is not requested anymore.
        int j = m_AttValues.length-1;
        String dim = "Dim " + j;
        if (j == x_dim) {
        	dim = "<b>X</b>";
        } else if (j == y_dim) {
            dim = "<b>Y</b>";
        }
        sb.append("<tr><td>"+dim+"</td><td>"+value(j));
        
        
        sb.append("<tr><td>Decay</td><td>"+weight()+"</td></tr>");
        sb.append("</table>");
        sb.append("<br>");
        sb.append("<table>");
        
        return sb.toString();
    }
	
	/**
	 * Return the superclass type (DataPoint) object.
	 * - Class label for each dimension is removed
	 * - Returned object has the class label of 'representingLabel' in the last dimension
	 * 
	 * @return DataPoint
	 */
	public DataPoint getDataPoint() {
		int numAtts = numAttributes();
		double[] attValues = new double[numAtts];
		for (int j = 0; j < numAtts; j++) {
			attValues[j] = value(j);
		}
		
		DenseInstance inst = new DenseInstance(this.m_Weight, attValues);
		Instances insts = new Instances(this.m_Dataset);
		inst.setDataset(insts);
		
		return new DataPoint(inst, this.timestamp);
	}
	
	
	
	/* Meaningless */
	
	@Override
	public Attribute classAttribute() {
		return this.attribute(classIndex());
	}

	@Override
	public int classIndex() {
		return numAttributes() - 1;
	}

	@Override
	public boolean classIsMissing() {
		return false;
	}
}
