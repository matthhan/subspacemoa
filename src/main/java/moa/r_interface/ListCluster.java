package moa.r_interface;


import moa.cluster.Cluster;
import weka.core.Instance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

class ListCluster extends Cluster {
    private List<double[]> elements;
    ListCluster() {
        this.elements = new ArrayList<>();
    }
    public void add(double[] point) {
        this.elements.add(point);
    }
    @Override
    public double[] getCenter() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public double getWeight() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public double getInclusionProbability(Instance instance) {
        for(double[] elem:this.elements) {
            if(Arrays.equals(elem, instance.toDoubleArray())) return 1.0;
        }
        return 0.0;
    }

    @Override
    public Instance sample(Random random) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
