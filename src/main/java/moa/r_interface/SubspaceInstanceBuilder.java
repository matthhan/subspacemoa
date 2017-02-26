package moa.r_interface;

import moa.core.SubspaceInstance;

class SubspaceInstanceBuilder {
  public static SubspaceInstance makeSubspaceInstance(double weight,double[] attValues) {
    return new SubspaceInstance(weight,attValues);
  }
}
