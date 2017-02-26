package moa.r_interface;

import moa.clusterers.macrosubspace.SUBCLU;
import moa.clusterers.macrosubspace.CLIQUE;
import moa.clusterers.macrosubspace.PROCLUS;
import moa.clusterers.macrosubspace.P3C;
import moa.clusterers.macrosubspace.MacroSubspaceClusterer;

class MacroClustererBuilder {
    public static MacroSubspaceClusterer buildClique(int xi, double tau) {
        CLIQUE res = new CLIQUE();
        res.xiOption.setValue(xi);
        res.tauOption.setValue(tau);
        res.prepareForUse();
        return res;
    }
    public static MacroSubspaceClusterer buildSubclu(double epsilon,int minSupport, int minOutputDim) {
        SUBCLU res = new SUBCLU();
        res.epsilonOption.setValue(epsilon);
        res.minSupportOption.setValue(minSupport);
        res.minOutputDimOption.setValue(minOutputDim);
        res.prepareForUse();
        return res;
    }
    public static MacroSubspaceClusterer buildProclus(int numOfClusters,int avgDimensions) {
        PROCLUS res = new PROCLUS();
        res.numOfClustersOption.setValue(numOfClusters);
        res.avgDimensionsOption.setValue(avgDimensions);
        res.prepareForUse();
        return res;
    }
    public static MacroSubspaceClusterer buildP3c(int poissonThreshold,double chiSquareAlpha) {
        P3C res = new P3C();
        res.poissonThresholdOption.setValue(poissonThreshold);
        res.chiSquareAlphaOption.setValue(chiSquareAlpha);
        res.prepareForUse();
        return res;
    }
}
