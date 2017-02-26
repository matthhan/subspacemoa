/**
 * SubspaceInstance.java
 * 
 * Subspace version of DenseInstance class
 * 
 * @author Yunsu Kim (yunsukim86@gmail.com)
 * 
 * RWTH Aachen University
 * i9: Data Management and Data Exploration Group
 * 
 */

package moa.core;

import java.util.HashMap;
import java.util.Set;

import weka.core.DenseInstance;
import weka.core.Instance;

public class SubspaceInstance extends DenseInstance {

	private static final long serialVersionUID = 1L;

	double[] classLabels;		// Class label for each attribute value
	
	public SubspaceInstance(double weight, double[] attValues) {
		super(weight, attValues);
		this.classLabels = new double[attValues.length];
	}
	
	public SubspaceInstance(double weight, double[] attValues, double[] classLabels) {
		super(weight, attValues);
		this.classLabels = classLabels;
	}
	
	public SubspaceInstance(Instance inst) {
		super(inst);
		if (inst instanceof SubspaceInstance) {
			setClassLabels(((SubspaceInstance) inst).getClassLabels());
		}
	}
	
	public double[] getClassLabels() {
		return classLabels;
	}
	
	public void setClassLabels(double[] classLabels) {
		this.classLabels = classLabels;
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
}