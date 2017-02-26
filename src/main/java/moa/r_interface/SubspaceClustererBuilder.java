package moa.r_interface;

import moa.clusterers.hddstream.HDDStream;
import moa.clusterers.predeconStream.PreDeConStream;
//The purpose of this class is to facilitate creating Objects from R.
//The classes HDDStream and PreDeConStream do not take arguments in their
//constructors. Instead, all the parameters are private attributes and
//for each private attribute x there is another attribute xOption that
//can be used to set this attribute. Doing this would be extremely tedious with
//the R Java interface, so instead we provide static methods that construct
//the objects by doing all of this.
class SubspaceClustererBuilder {
    public static HDDStream buildHDDStream(double epsilonN,
                                           double beta,
                                           int mu,
                                           double lambda,
                                           int initPoints,
                                           int pi,
                                           int kappa,
                                           double delta,
                                           double offline,
                                           int speed) {
        HDDStream res = new HDDStream();
        res.epsilonNOption.setValue(epsilonN);
        res.betaOption.setValue(beta);
        res.muOption.setValue(mu);
        res.lambdaOption.setValue(lambda);
        res.initPointsOption.setValue(initPoints);
        res.piOption.setValue(pi);
        res.kappaOption.setValue(kappa);
        res.deltaOption.setValue(delta);
        res.offlineOption.setValue(offline);
        res.speedOption.setValue(speed);
        res.prepareForUse();
        return res;
    }
    public static PreDeConStream buildPreDeConStream(double epsilonN,
                                                     double beta,
                                                     int muN,
                                                     int muF,
                                                     double lambda,
                                                     int initPoints,
                                                     int tau,
                                                     double kappa,
                                                     double delta,
                                                     double offline,
                                                     int speed) {
        PreDeConStream res = new PreDeConStream();
        res.epsilonNOption.setValue(epsilonN);
        res.betaOption.setValue(beta);
        res.muNOption.setValue(muN);
        res.muFOption.setValue(muF);
        res.lambdaOption.setValue(lambda);
        res.initPointsOption.setValue(initPoints);
        res.tauOption.setValue(tau);
        res.kappaOption.setValue(kappa);
        res.deltaOption.setValue(delta);
        res.offlineOption.setValue(offline);
        res.speedOption.setValue(speed);
        res.prepareForUse();
        return res;
    }
    public static void main(String[] args) {
        buildHDDStream(1,1,1,1,1,1,1,1,1,1);
        buildPreDeConStream(1,1,1,1,1,1,1,1,1,1,1);
        System.out.println("boilerplate clusterers created successfully. It looks like everything is alright.");
    }
}
