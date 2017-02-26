package moa.r_interface;
import weka.core.Instance;
import moa.core.SubspaceInstance;
import moa.clusterers.AbstractSubspaceClusterer;
class TrainingHandler {
  public static void trainOn(double[] point,AbstractSubspaceClusterer clusterer) {
    clusterer.trainOnInstanceImpl((Instance) new SubspaceInstance(1,point));
  }
}

