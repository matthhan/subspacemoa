package moa.r_interface;

import java.util.*;

public class RCompatibleEvaluationResult {
    private Map<String,Double> valueForMeasure;
    private List<String> insertionOrder;
    double[][] points;
    RCompatibleEvaluationResult() {
        this.valueForMeasure = new LinkedHashMap<>();
        this.insertionOrder = new LinkedList<>();
    }
    void addMeasureValue(String name,double value) {
        this.valueForMeasure.put(name,value);
        this.insertionOrder.add(name);
    }
    void setPoints(double[][] points) {
        this.points = points;
    }
    double[] getValues() {
        double[] res = new double[this.valueForMeasure.size()];
        for(int i = 0;i<this.valueForMeasure.size();i++) {
            res[i] = this.valueForMeasure.get(this.insertionOrder.get(i));
        }
        return res;
    }
    String[] getNames() {
        String[] res = new String[this.insertionOrder.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = this.insertionOrder.get(i);
        }
        return res;
    }
    /*
     * Gets the data points that the evaluation was run over. This is necessary
     * because we want to
     *
     * 1. get some data points
     * 2. train the clusterer on them
     * 3. evaluate over them
     * 4. plot them
     *
     * in the R package. However, we cannot do these steps separately because the evaluation needs the points
     * as instances of subspaceInstance, which we cannot properly recreate, once we have passed them into R.
     *
     * Therefore we must be able to return BOTH the evaluation result and the points over which it was obtained.
     */
    double[][] getPoints() {
        return this.points;
    }
}
