package moa.r_interface;

import moa.cluster.SubspaceClustering;
import moa.core.SubspaceInstance;
import moa.evaluation.*;
import moa.gui.subspacevisualization.SubspaceDataPoint;

import java.util.*;

public class Evaluator {
    public RCompatibleEvaluationResult evaluate(RCompatibleDataStreamClusterer clusterer,double[][] points,double[] classes,String[] measures) {
        Set<SubspaceMeasureCollection> measureObjects = parseMeasures(measures);
        SubspaceClustering foundClustering = clusterer.getClusteringForEvaluation();
        SubspaceClustering groundTruthClustering = buildGroundTruthClustering(points,classes);
        List<SubspaceDataPoint> pointsAsList = buildDatapointlist(points);

        List<String> measureNames = new ArrayList<>();
        List<Double> measureValues = new ArrayList<>();
        for(SubspaceMeasureCollection measure:measureObjects) {
            try {
                measure.subEvaluateClusteringPerformance(foundClustering,groundTruthClustering,pointsAsList);
                for(int i = 0; i < measure.getNumMeasures();i++) {
                    measureNames.add(measure.getName(i));
                    measureValues.add(measure.getLastValue(i));
                }
            } catch (Exception e) {
                System.out.println("error processing measure " + measure);
            }
        }
        return new RCompatibleEvaluationResult(measureNames,measureValues);
    }

    //TODO Make sure that it is alright to just always pass 1 as the timestamp
    //TODO Make sure that we do not have to pass the class
    private static List<SubspaceDataPoint> buildDatapointlist(double[][] points) {
        List<SubspaceDataPoint> res  = new ArrayList<>(points.length);
        for(double[] point:points) {
            SubspaceInstance asInst = new SubspaceInstance(1,point);
            res.add(new SubspaceDataPoint(asInst,1));
        }
        return res;
    }


    private static SubspaceClustering buildGroundTruthClustering(double[][] points, double[] classes) {

        SubspaceClustering res = new SubspaceClustering();
        Set<Double> distinctClasses = new HashSet<>();

        for(double klass:classes) { distinctClasses.add(klass); }
        for(double klass:distinctClasses) {
            ListCluster clus = new ListCluster();
            for (int i = 0; i < points.length; i++) {
                if(classes[i] == klass) {
                    clus.add(points[i]);
                }
            }
            res.add(clus);
        }
        return res;
    }

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
}
