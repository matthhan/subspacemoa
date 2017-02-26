package moa.r_interface;

import moa.streams.clustering.RandomRBFSubspaceGeneratorEvents;
import moa.streams.clustering.SubspaceARFFStream;
import moa.streams.clustering.SubspaceClusteringStream;
import java.io.File;
public class DataStreamBuilder{
    public static RandomRBFSubspaceGeneratorEvents buildRandomRBFSubspaceGeneratorEvents(
                                                    int modelRandomSeed,
                                                    int instanceRandomSeed,
                                                    int numCluster,
                                                    int numClusterRange,
                                                    int avgSubspaceSize,
                                                    int avgSubspaceSizeRange,
                                                    double kernelRadii,
                                                    double kernelRadiiRange,
                                                    int numOverlappedCluster,
                                                    double overlappingDegree,
                                                    double densityRange,
                                                    double noiseLevel,
                                                    boolean noiseInCluster,
                                                    int speed,
                                                    int speedRange,
                                                    int eventFrequency,
                                                    boolean eventMergeSplit,
                                                    boolean eventDeleteCreate,
                                                    int subspaceEventFrequency,
                                                    int decayHorizon,
                                                    double decayThreshold,
                                                    int evaluationFrequency,
                                                    boolean subEvaluation,
                                                    int subEvaluationFrequency,
                                                    int numAtts) {
        RandomRBFSubspaceGeneratorEvents gen = new RandomRBFSubspaceGeneratorEvents();
        gen.modelRandomSeedOption.setValue(modelRandomSeed);
        gen.instanceRandomSeedOption.setValue(instanceRandomSeed);
        gen.numClusterOption.setValue(numCluster);
        gen.numClusterRangeOption.setValue(numClusterRange);
        gen.avgSubspaceSizeOption.setValue(avgSubspaceSize);
        gen.avgSubspaceSizeRangeOption.setValue(avgSubspaceSizeRange);
        gen.kernelRadiiOption.setValue(kernelRadii);
        gen.kernelRadiiRangeOption.setValue(kernelRadiiRange);
        gen.numOverlappedClusterOption.setValue(numOverlappedCluster);
        gen.overlappingDegreeOption.setValue(overlappingDegree);
        gen.densityRangeOption.setValue(densityRange);
        gen.noiseLevelOption.setValue(noiseLevel);
        gen.noiseInClusterOption.setValue(noiseInCluster);
        gen.speedOption.setValue(speed);
        gen.speedRangeOption.setValue(speedRange);
        gen.eventFrequencyOption.setValue(eventFrequency);
        gen.eventMergeSplitOption.setValue(eventMergeSplit);
        gen.eventDeleteCreateOption.setValue(eventDeleteCreate);
        gen.subspaceEventFrequencyOption.setValue(subspaceEventFrequency);
        gen.decayHorizonOption.setValue(decayHorizon);
        gen.decayThresholdOption.setValue(decayThreshold);
        gen.evaluationFrequencyOption.setValue(evaluationFrequency);
        gen.subEvaluationOption.setValue(subEvaluation);
        gen.subEvaluationFrequencyOption.setValue(subEvaluationFrequency);
        gen.numAttsOption.setValue(numAtts);
        //Call prepare for use so that the object is initialized properly
        gen.prepareForUse();
        return gen;
    }
    public static SubspaceARFFStream buildSubspaceARFFStream(String filepath) {
        SubspaceARFFStream res = new SubspaceARFFStream();
        res.arffFileOption.setValue(filepath);
        res.prepareForUse();
        return res;
    }

    public static void main(String[] args) {
        /*System.out.println("Testing");
        buildRandomRBFSubspaceGeneratorEvents(0,0,1,0,1, 
                                              0,0,0,0,0, 
                                              0,0,false,1,0, 
                                              0,false,false,0,0, 
                                              0,0,false,0,10);
        System.out.println("Creating boilerplate Object succeeded. Everything should be fine");*/
        System.out.println("Testing DataStreamBuilder");
        SubspaceClusteringStream res = buildSubspaceARFFStream("./test_dataset.arff");
        System.out.println(res.nextInstance());
        System.out.println("Test Successful");
    
    }
}
