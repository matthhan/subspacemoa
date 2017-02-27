package moa.r_interface;

import moa.cluster.Clustering;
import moa.cluster.SubspaceClustering;
import moa.clusterers.AbstractClusterer;
import moa.clusterers.macrosubspace.MacroSubspaceClusterer;
import moa.core.SubspaceInstance;

public class ThreeStageClusterer extends RCompatibleDataStreamClusterer {
    private MacroSubspaceClusterer macro;
    private AbstractClusterer micro;
    private SubspaceClustering currentMacroClustering = null;
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
        if(macroClusteringDirty) {
            this.computeMacroclustering();
        }
        if(this.currentMacroClustering == null || this.currentMacroClustering.size() == 0) {
            return new double[][] {{0}};
        }
        double[][] res = new double[this.currentMacroClustering.size()][this.currentMacroClustering.dimension()];
        for(int i = 0; i < this.currentMacroClustering.size(); i++) {
            double[] center = this.currentMacroClustering.get(i).getCenter();
            System.arraycopy(center, 0, res[i], 0, this.currentMacroClustering.dimension());
        }
        return res;
    }



    @Override
    public double[] getMacroclusteringWeights() {
        if(macroClusteringDirty) {
            this.computeMacroclustering();
        }
        if(this.currentMacroClustering == null || this.currentMacroClustering.size() == 0) {
            return new double[] {0};
        }
        double[] res = new double[this.currentMacroClustering.size()];
        for(int i = 0; i < this.currentMacroClustering.size();i++) {
            res[i] = this.currentMacroClustering.get(i).getWeight();
        }
        return res;
    }

    @Override
    public void trainOn(double[] point) {
        this.micro.trainOnInstanceImpl(new SubspaceInstance(1,point));
        this.macroClusteringDirty = true;
    }
    private void computeMacroclustering() {
        this.currentMacroClustering = this.macro.getClusteringResult(this.micro.getMicroClusteringResult());
        this.macroClusteringDirty = false;
    }
    public static RCompatibleDataStreamClusterer threeStage(AbstractClusterer micro,MacroSubspaceClusterer macro) {
        return new ThreeStageClusterer(macro,micro);
    }
}
