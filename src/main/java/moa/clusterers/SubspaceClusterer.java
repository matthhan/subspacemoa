/**
 * [SubspaceClusterer.java] for Subspace MOA
 * 
 * Subspace version of [Clusterer] interface.
 * 
 * @author Yunsu Kim
 * Data Management and Data Exploration Group, RWTH Aachen University
 */

package moa.clusterers;

import moa.MOAObject;
import moa.cluster.Clustering;
import moa.cluster.SubspaceClustering;
import moa.core.InstancesHeader;
import moa.core.Measurement;
import moa.gui.AWTRenderable;
import moa.options.OptionHandler;
import weka.core.Instance;

public interface SubspaceClusterer extends MOAObject, OptionHandler, AWTRenderable {

	public void setModelContext(InstancesHeader ih);

	public InstancesHeader getModelContext();

	public boolean isRandomizable();

	public void setRandomSeed(int s);

	public boolean trainingHasStarted();

	public double trainingWeightSeenByModel();

	public void resetLearning();

	public void trainOnInstance(Instance inst);

	public double[] getVotesForInstance(Instance inst);

	public Measurement[] getModelMeasurements();

	public SubspaceClusterer[] getSubClusterers();

	public SubspaceClusterer copy();

    public SubspaceClustering getClusteringResult();

    public boolean implementsMicroClusterer();

    public Clustering getMicroClusteringResult();
    
    public boolean keepClassLabel();

}
