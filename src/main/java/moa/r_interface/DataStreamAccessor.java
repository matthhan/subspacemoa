package moa.r_interface;

import moa.streams.clustering.SubspaceClusteringStream;
import moa.streams.clustering.SubspaceARFFStream;
import moa.streams.clustering.RandomRBFSubspaceGeneratorEvents;
import moa.core.SubspaceInstance;

class DataStreamAccessor {
    public static double[][] getPoints(RandomRBFSubspaceGeneratorEvents gen,int n) {
        return getPoints((SubspaceClusteringStream) gen,n);
    }
    public static double[][] getPoints(SubspaceARFFStream gen,int n) {
        return getPoints((SubspaceClusteringStream) gen,n);
    }
    public static double[][] getPoints(SubspaceClusteringStream gen, 
                                       int n) {
        int numberDims = gen.numAttsOption.getValue();
        double[][] res = new double[n][numberDims];
        for(int i = 0; i < n; i++) {
            res[i] = getOnePoint(gen);
        }
        return res;
    }
    private static double[] getOnePoint(SubspaceClusteringStream gen) {
        SubspaceInstance inst = gen.nextInstance();
        return inst.toDoubleArray();
    }
    public static int getNumAtts(SubspaceClusteringStream stream) {
        return stream.numAttsOption.getValue();
    }
    public static int getNumAtts(RandomRBFSubspaceGeneratorEvents stream) {
        return getNumAtts((SubspaceClusteringStream) stream);
    }
    public static int getNumAtts(SubspaceARFFStream stream) {
        return getNumAtts((SubspaceClusteringStream) stream);
    }
}

