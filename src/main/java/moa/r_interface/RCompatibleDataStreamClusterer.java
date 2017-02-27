package moa.r_interface;

import moa.cluster.SubspaceClustering;
import moa.evaluation.*;

import java.util.*;

public abstract class RCompatibleDataStreamClusterer {
    public abstract double[][] getMicroclusteringCenters();
    public abstract double[][] getMacroclusteringCenters();
    public abstract double[] getMicroclusteringWeights();
    public abstract double[] getMacroclusteringWeights();
    public abstract void trainOn(double[] point);
    public abstract SubspaceClustering getClusteringForEvaluation();


}
