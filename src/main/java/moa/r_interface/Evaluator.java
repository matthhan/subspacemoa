package moa.r_interface;

import moa.cluster.Clustering;
import moa.cluster.SubspaceClustering;
import moa.core.SubspaceInstance;
import moa.evaluation.*;
import moa.gui.subspacevisualization.SubspaceDataPoint;

import java.util.*;

public class Evaluator {

    private Set<SubspaceMeasureCollection> parseMeasures(String[] measures) {
        Set<SubspaceMeasureCollection> measureObjs = new LinkedHashSet<>();

        SubspaceMeasureCollection ce = new CE();
        SubspaceMeasureCollection cmm_s = new CMM_S();
        SubspaceMeasureCollection entropy_s = new EntropySubspace();
        SubspaceMeasureCollection f1_s = new F1Subspace();
        SubspaceMeasureCollection purity = new Purity();
        SubspaceMeasureCollection rand_statistic = new RandStatistic();
        SubspaceMeasureCollection subcmm = new SubCMM();
        for(String measure:measures) {
            if ("clustering error".equals(measure.toLowerCase())) {
                measureObjs.add(ce);
            } else if ("cmm subspace".equals(measure.toLowerCase())) {
                measureObjs.add(cmm_s);
            } else if ("entropy subspace".equals(measure.toLowerCase())) {
                measureObjs.add(entropy_s);
            } else if ("f1 subspace".equals(measure.toLowerCase())) {
                measureObjs.add(f1_s);
            } else if ("purity".equals(measure.toLowerCase())) {
                measureObjs.add(purity);
            } else if ("rand statistic".equals(measure.toLowerCase())) {
                measureObjs.add(rand_statistic);
            } else if ("subcmm".equals(measure.toLowerCase())) {
                measureObjs.add(subcmm);
            }
        }
        return measureObjs;
    }
    public RCompatibleEvaluationResult evaluate(RCompatibleDataStreamClusterer clusterer,RCompatibleDataStream stream,int n,String[] measureStrings,boolean alsoTrainOn) {

        Set<SubspaceMeasureCollection> measures = parseMeasures(measureStrings);
        List<SubspaceDataPoint> pointBuffer = new ArrayList<>(n);
;

        for (int timestamp = 0;timestamp < n;timestamp++) {
            SubspaceInstance next = stream.nextInstance();
            SubspaceDataPoint point = new SubspaceDataPoint(next, timestamp);
            pointBuffer.add(point);

            // Train clusterers
            SubspaceInstance trainInst = new SubspaceInstance(point);
            if (clusterer.keepClassLabel()) {
                trainInst.setDataset(point.dataset());
            } else {
                trainInst.deleteAttributeAt(point.classIndex());
            }
            if(alsoTrainOn) clusterer.trainOnInstance(trainInst);
        }

        SubspaceClustering result = clusterer.getClusteringForEvaluation();
        SubspaceClustering gtClustering = new SubspaceClustering(pointBuffer);

        RCompatibleEvaluationResult res = new RCompatibleEvaluationResult();
        for (SubspaceMeasureCollection measure:measures) {
            try {
                measure.subEvaluateClusteringPerformance(result, gtClustering, pointBuffer);
                measure.averageSubEvaluations();
                for (int i = 0; i < measure.getNumMeasures(); i++) {
                    res.addMeasureValue(measure.getName(i),measure.getLastValue(i));
                }
            } catch (Exception e) {
                System.out.println("error processing measures: " + measure.toString());
            }
        }
        res.setPoints(asDoubleArr(pointBuffer));
        return res;

    }
    private static double[][] asDoubleArr(List<SubspaceDataPoint> lis) {
        double[][] res = new double[lis.size()][lis.get(0).toDoubleArray().length];
        for (int i = 0; i < res.length; i++) {
            res[i] = lis.get(i).toDoubleArray();
        }
        return res;
    }
}
