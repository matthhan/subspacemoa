package moa.r_interface;

public abstract class RCompatibleDataStreamClusterer {
    public abstract double[][] getMicroclusteringCenters();
    public abstract double[][] getMacroclusteringCenters();
    public abstract double[] getMicroclusteringWeights();
    public abstract double[] getMacroclusteringWeights();
    public abstract void trainOn(double[] point);
}
