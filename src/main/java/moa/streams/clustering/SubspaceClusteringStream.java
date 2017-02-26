/**
 * [SubspaceClusteringStream.java] for Subspace MOA
 * 
 * Subspace version of ClusteringStream class
 * 
 * @author Yunsu Kim
 * Data Management and Data Exploration Group, RWTH Aachen University
 */

package moa.streams.clustering;

import moa.core.InstancesHeader;
import moa.core.SubspaceInstance;
import moa.options.AbstractOptionHandler;
import moa.options.FlagOption;
import moa.options.FloatOption;
import moa.options.IntOption;
import moa.streams.InstanceStream;

public abstract class SubspaceClusteringStream extends AbstractOptionHandler implements InstanceStream {

	private static final long serialVersionUID = 1L;
	
	public IntOption decayHorizonOption = new IntOption("decayHorizon", 'h',
            "Decay horizon", 1000, 0, Integer.MAX_VALUE);

	public FloatOption decayThresholdOption = new FloatOption("decayThreshold", 't',
            "Decay horizon threshold", 0.1, 0, 1);

	public IntOption evaluationFrequencyOption = new IntOption("evaluationFrequency", 'e',
            "Evaluation frequency.", 1000, 0, Integer.MAX_VALUE);
	
	public FlagOption subEvaluationOption = new FlagOption("subEvaluation", 'u',
			"Subevaluation which is done within an evaluation frequency. If this is on, " +
			"evaluation is done by every subEvaluationFrequency and they are averaged every " +
			"evaluationFrequency.");  

	public IntOption subEvaluationFrequencyOption = new IntOption("subEvaluationFrequency", 'b',
            "Subevaluation frequency.", 200, 0, Integer.MAX_VALUE);

	public IntOption numAttsOption = new IntOption("numAtts", 'a',
            "The number of attributes to generate.", 10, 0, Integer.MAX_VALUE);


	public int getDecayHorizon() {
		return decayHorizonOption.getValue();
	}

	public double getDecayThreshold() {
		return decayThresholdOption.getValue();
	}

	public int getEvaluationFrequency() {
		return evaluationFrequencyOption.getValue();
	}
	
	public int getSubEvaluationFrequency() {
		if (subEvaluationOption.isSet()) {
			return subEvaluationFrequencyOption.getValue();
		} else {
			return -1;
		}
	}
	

	protected InstancesHeader streamHeader;
    
	public InstancesHeader getHeader() {
		return streamHeader;
	}
	
	@Override
	public abstract SubspaceInstance nextInstance();
}
