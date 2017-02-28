package moa.r_interface;

import java.util.*;

public class RCompatibleEvaluationResult {
    private Map<String,Double> valueForMeasure;
    private List<String> insertionOrder;
    RCompatibleEvaluationResult() {
        this.valueForMeasure = new LinkedHashMap<>();
        this.insertionOrder = new LinkedList<>();
    }
    void addMeasureValue(String name,double value) {
        this.valueForMeasure.put(name,value);
        this.insertionOrder.add(name);
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
}
