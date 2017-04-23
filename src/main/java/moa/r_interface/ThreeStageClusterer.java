package moa.r_interface;

import moa.cluster.Clustering;
import moa.cluster.SubspaceClustering;
import moa.cluster.SubspaceSphereCluster;
import moa.clusterers.AbstractClusterer;
import moa.clusterers.macrosubspace.MacroSubspaceClusterer;
import moa.core.SubspaceInstance;
import weka.datagenerators.clusterers.SubspaceCluster;

public class ThreeStageClusterer extends RCompatibleDataStreamClusterer {
    private MacroSubspaceClusterer macro;
    private AbstractClusterer micro;
    private SubspaceClustering macroClustering = null;
    private boolean macroClusteringDirty = true;

    private ThreeStageClusterer(MacroSubspaceClusterer macro, AbstractClusterer micro) {
        this.macro = macro;
        this.micro = micro;
    }
    @Override
    public double[][] getMicroclusteringCenters() {
        Clustering clustering = this.micro.getMicroClusteringResult();
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
        Clustering clustering = this.micro.getMicroClusteringResult();
        if(clustering == null || clustering.size() == 0) {
            return new double[] {0};
        }
        double[] res = new double[clustering.size()];
        for(int i = 0; i < clustering.size();i++) {
            res[i] = clustering.get(i).getWeight();
        }
        return res;
    }

    @Override
    public double[][] getMacroclusteringCenters() {
        SubspaceClustering macroclustering = this.getMacroClustering();
        if(macroclustering == null || macroclustering.size() == 0) {
            return new double[][] {{0}};
        }
        double[][] res = new double[macroclustering.size()][macroclustering.dimension()];
        for(int i = 0; i < macroclustering.size(); i++) {
            double[] center = macroclustering.get(i).getCenter();
            System.arraycopy(center, 0, res[i], 0, macroclustering.dimension());
        }
        return res;
    }



    @Override
    public double[] getMacroclusteringWeights() {
        SubspaceClustering macroclustering = this.getMacroClustering();
        if(macroclustering == null || macroclustering.size() == 0) {
            return new double[] {0};
        }
        double[] res = new double[macroclustering.size()];
        for(int i = 0; i < macroclustering.size(); i++) {
            res[i] = macroclustering.get(i).getWeight();
        }
        return res;
    }

    @Override
    public void trainOn(double[] point) {
        this.micro.trainOnInstanceImpl(new SubspaceInstance(1,point));
        this.macroClusteringDirty = true;
    }

    @Override
    public boolean isClusterInDimension(int i, int dim) {
        return ((SubspaceSphereCluster)this.getMacroClustering().get(i)).isRelevant(dim);
    }

    @Override
    public double[] getBordersOfClusterInDimension(int i, int dim) {
        SubspaceSphereCluster res =  ((SubspaceSphereCluster)this.getMacroClustering().get(i));
        return new double[]{ res.getLeftBoundary(dim), res.getRightBoundary(dim)};
    }

    @Override
    public double getRadiusOfCluster(int i) {
        SubspaceSphereCluster res =  ((SubspaceSphereCluster)this.getMacroClustering().get(i));
        return res.getRadius();
    }

    @Override
    public SubspaceClustering getClusteringForEvaluation() {
       return this.getMacroClustering();
    }

    private SubspaceClustering getMacroClustering() {
        if(this.macroClusteringDirty) {
            this.computeMacroclustering();
        }
        return this.macroClustering;
    }
    private void computeMacroclustering() {
        this.macroClustering = this.macro.getClusteringResult(this.micro.getMicroClusteringResult());
        this.macroClusteringDirty = false;
    }

    @Override
    void trainOnInstance(SubspaceInstance inst ) {
        this.micro.trainOnInstance(inst);
    }

    @Override
    boolean keepClassLabel() {
        return this.micro.keepClassLabel();
    }
    public static RCompatibleDataStreamClusterer threeStage(AbstractClusterer micro,MacroSubspaceClusterer macro) {
        return new ThreeStageClusterer(macro,micro);
    }
}
