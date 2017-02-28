package moa.r_interface;

import moa.cluster.SubspaceClustering;
import moa.core.SubspaceInstance;
import moa.evaluation.*;

import java.util.*;

public abstract class RCompatibleDataStreamClusterer {
    /*
      * public methods are the ones that
      * should be used from within the R package
     */
    public abstract double[][] getMicroclusteringCenters();
    public abstract double[][] getMacroclusteringCenters();
    public abstract double[] getMicroclusteringWeights();
    public abstract double[] getMacroclusteringWeights();
    public abstract void trainOn(double[] point);

    abstract SubspaceClustering getClusteringForEvaluation();
    abstract void trainOnInstance(SubspaceInstance inst);
    abstract boolean keepClassLabel();

}
