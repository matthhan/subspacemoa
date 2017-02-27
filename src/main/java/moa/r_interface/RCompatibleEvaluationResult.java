package moa.r_interface;

import java.util.List;

public class RCompatibleEvaluationResult {
    private double[] values;
    private String[] names;
    RCompatibleEvaluationResult(List<String> measureNames, List<Double> measureValues) {
        this.values = new double[measureValues.size()];
        for (int i = 0; i < measureValues.size(); i++) {
            this.values[i] = measureValues.get(i);
        }
        this.names = measureNames.toArray(new String[0]);
    }
    double[] getValues() { return this.values; }
    String[] getNames() { return this.names; }
}
