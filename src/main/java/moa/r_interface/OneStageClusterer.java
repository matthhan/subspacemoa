package moa.r_interface;

import moa.cluster.Cluster;
import moa.cluster.Clustering;
import moa.cluster.SubspaceClustering;
import moa.cluster.SubspaceSphereCluster;
import moa.clusterers.SubspaceClusterer;
import moa.clusterers.hddstream.HDDStream;
import moa.clusterers.predeconstream.PreDeConStream;
import moa.core.SubspaceInstance;


public class OneStageClusterer extends RCompatibleDataStreamClusterer {
    private SubspaceClusterer clusterer;
    private OneStageClusterer(SubspaceClusterer clusterer) {
        this.clusterer = clusterer;
    }
    @Override
    public double[][] getMicroclusteringCenters() {
        Clustering clustering = this.clusterer.getMicroClusteringResult();
        if(clustering == null || clustering.size() == 0) {
            return new double[][] {{0}};
        }
        double[][] res = new double[clustering.size()][clustering.dimension()];
        for(int i = 0; i < clustering.size(); i++) {
            double[] center = clustering.get(i).getCenter();
            System.arraycopy(center, 0, res[i], 0, clustering.dimension());
        }
        return res;
    }

    @Override
    public double[] getMicroclusteringWeights() {
        Clustering clustering = this.clusterer.getMicroClusteringResult();
        if(clustering == null || clustering.size()==0) {
            return new double[]{0.0};
        }
        double[] res = new double[clustering.size()];
        for(int i = 0; i <clustering.size(); i++) {
            res[i] = clustering.get(i).getWeight();
        }
        return res;
    }
    @Override
    public double[][] getMacroclusteringCenters() {
        SubspaceClustering clustering = this.clusterer.getClusteringResult();
        if(clustering == null || clustering.size() == 0) {
            return new double[][] {{0}};
        }
        double[][] res = new double[clustering.size()][clustering.dimension()];
        for(int i = 0; i < clustering.size(); i++) {
            double[] center = clustering.get(i).getCenter();
            System.arraycopy(center, 0, res[i], 0, clustering.dimension());
        }
        return res;
    }


    @Override
    public double[] getMacroclusteringWeights() {
        SubspaceClustering clustering = this.clusterer.getClusteringResult();
        if(clustering == null || clustering.size()==0) {
            return new double[]{0.0};
        }
        double[] res = new double[clustering.size()];
        for(int i = 0; i <clustering.size(); i++) {
            res[i] = clustering.get(i).getWeight();
        }
        return res;
    }

    @Override
    public boolean isClusterInDimension(int i,int dim) {
        return true;
    }
    @Override
    public double[] getBordersOfClusterInDimension(int i,int dim) {
        return new double[]{Double.MIN_VALUE,Double.MAX_VALUE};
    }
    @Override
    public double getRadiusOfCluster(int i) {
        return 0;
    }

    @Override
    public void trainOn(double[] point) {
        this.clusterer.trainOnInstance(new SubspaceInstance(1,point));
    }

    @Override
    public SubspaceClustering getClusteringForEvaluation() {
        return this.clusterer.getClusteringResult();
    }

    @Override
    void trainOnInstance(SubspaceInstance inst) {
        this.clusterer.trainOnInstance(inst);
    }

    @Override
    boolean keepClassLabel() {
        return this.clusterer.keepClassLabel();
    }


    public static RCompatibleDataStreamClusterer hddstream(double epsilonN,
                                           double beta,
                                           int mu,
                                           double lambda,
                                           int initPoints,
                                           int pi,
                                           int kappa,
                                           double delta,
                                           double offline,
                                           int speed) {
        HDDStream res = new HDDStream();
        res.epsilonNOption.setValue(epsilonN);
        res.betaOption.setValue(beta);
        res.muOption.setValue(mu);
        res.lambdaOption.setValue(lambda);
        res.initPointsOption.setValue(initPoints);
        res.piOption.setValue(pi);
        res.kappaOption.setValue(kappa);
        res.deltaOption.setValue(delta);
        res.offlineOption.setValue(offline);
        res.speedOption.setValue(speed);
        res.prepareForUse();
        return new OneStageClusterer(res);
    }
    public static RCompatibleDataStreamClusterer predeconstream(double epsilonN,
                                                     double beta,
                                                     int muN,
                                                     int muF,
                                                     double lambda,
                                                     int initPoints,
                                                     int tau,
                                                     double kappa,
                                                     double delta,
                                                     double offline,
                                                     int speed) {
        PreDeConStream res = new PreDeConStream();
        res.epsilonNOption.setValue(epsilonN);
        res.betaOption.setValue(beta);
        res.muNOption.setValue(muN);
        res.muFOption.setValue(muF);
        res.lambdaOption.setValue(lambda);
        res.initPointsOption.setValue(initPoints);
        res.tauOption.setValue(tau);
        res.kappaOption.setValue(kappa);
        res.deltaOption.setValue(delta);
        res.offlineOption.setValue(offline);
        res.speedOption.setValue(speed);
        res.prepareForUse();
        return new OneStageClusterer(res);
    }
}
