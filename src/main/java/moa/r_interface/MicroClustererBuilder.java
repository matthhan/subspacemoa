package moa.r_interface;

import moa.clusterers.AbstractClusterer;
import moa.clusterers.clustream.Clustream;
import moa.clusterers.denstream.DenStream;
class MicroClustererBuilder {
    public static AbstractClusterer buildClustream(int timeWindow,
                                                   int maxNumKernels, 
                                                   int kernelRadiFactor,
                                                   int streamSpeed) {
        Clustream res = new Clustream();
        res.timeWindowOption.setValue(timeWindow);
        res.maxNumKernelsOption.setValue(maxNumKernels);
        res.kernelRadiFactorOption.setValue(kernelRadiFactor);
        //res.streamSpeedOption.setValue(streamSpeed);
        res.prepareForUse();
        return res;
    }
    public static AbstractClusterer buildDenstream(int horizon,
                                                   double epsilon,
                                                   int minPoints,
                                                   double beta,
                                                   double mu,
                                                   int initPoints,
                                                   int speed) {
        DenStream res= new DenStream();
        res.horizonOption.setValue(horizon);
        res.epsilonOption.setValue(epsilon);
        res.minPointsOption.setValue(minPoints);
        res.betaOption.setValue(beta);
        res.muOption.setValue(mu);
        res.initPointsOption.setValue(initPoints);
        res.streamSpeedOption.setValue(speed);
        res.prepareForUse();
        return res;
    }

}
