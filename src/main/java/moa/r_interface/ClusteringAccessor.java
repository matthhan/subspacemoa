package moa.r_interface;

import moa.clusterers.SubspaceClusterer;
import moa.core.AutoExpandVector;
import moa.cluster.Clustering;
import moa.cluster.SubspaceClustering;
import moa.cluster.Cluster;
import moa.clusterers.AbstractClusterer;
import moa.clusterers.AbstractSubspaceClusterer;
class ClusteringAccessor {

    //This method is meant to facilitate Accessing the micro clusters of a SubspaceClusterers:
    //Doing this in R would be unneccessarily complicated and probably VERY slow.
    //This procedure returns a matrix, in which each row represents a microcluster and each
    //column represents the position of the microcluster in one of the dimensions.
    //
    //This method accesses the micro clustering results from SubspaceClusterer Objects
    //(i.e. our one stop algorithms)
    public static double[][] getMicroClusteringResult(AbstractSubspaceClusterer clusterer) {
        Clustering clustering = clusterer.getMicroClusteringResult(); 
        return getMicroClusteringResult(clustering);
    }
    //This one accesses them from AbstractClusterer Objects (the online phases of three step
    //algorithms). Note that AbstractClusterer and SubspaceClusterer do not have
    //a shared superclass that can access this result, so this duplication is neccessary.
    public static double[][] getMicroClusteringResult(AbstractClusterer clusterer) {
        Clustering clustering;
        clustering = clusterer.getMicroClusteringResult(); 
        return getMicroClusteringResult(clustering);
    }
    public static double[][] getMicroClusteringResult(Clustering clustering) {
        if(clustering == null || clustering.size() == 0) {
            return new double[][] {{0}};
        }
        double[][] res = new double[clustering.size()][clustering.dimension()];
        for(int i = 0; i < clustering.size(); i++) {
            double[] center = clustering.get(i).getCenter();
            for(int j = 0; j < clustering.dimension();j++) {
                res[i][j] = center[j];
            }
        } 
        return res;
    }

    // This method gets macro clustering results from SubspaceClusterer objects.
    public static double[][] getClusteringResult(AbstractSubspaceClusterer clusterer) {
        SubspaceClustering clustering = clusterer.getClusteringResult(); 
        return getClusteringResult(clustering);
    }
    public static double[][] getClusteringResult(SubspaceClustering clustering) {
        if(clustering == null || clustering.size() == 0) {
            return new double[][] {{0}};
        }
        double[][] res = new double[clustering.size()][clustering.dimension()];
        for(int i = 0; i < clustering.size(); i++) {
            double[] center = clustering.get(i).getCenter();
            for(int j = 0; j < clustering.dimension();j++) {
                res[i][j] = center[j];
            }
        } 
        return res;
    }

    public static double[] getMicroClusteringWeights(Clustering clustering){
       return getWeightsOfClustering(clustering.getClustering()); 
    }
    public static double[] getClusteringWeights(Clustering clustering) {
       return getWeightsOfClustering(clustering.getClustering()); 
    }
    public static double[] getClusteringWeights(SubspaceClustering clustering) {
       return getWeightsOfClustering(clustering.getClustering()); 
    }
    public static double[] getMacroClusteringWeights(SubspaceClustering clustering){
       return getWeightsOfClustering(clustering.getClustering()); 
    }
    //All of these functions are just different ways to access this one. This is necessary
    //because e.g. SubspaceClustering and Clustering do NOT share a common interface or are
    //Subclasses of some Abstract superclass. However they all share the method getClustering()
    //that returns an AutoExpandVector of clusters
    public static double[] getWeightsOfClustering(AutoExpandVector<Cluster> clustering) {
        if(clustering == null || clustering.size() == 0) {
            return new double[] {0};
        }
        double[] res = new double[clustering.size()];
        for(int i = 0; i < clustering.size();i++) {
            res[i] = clustering.get(i).getWeight();
        }
        return res;
    }
    /*
    public static void output2DArray(double[][] arr) {
        for(int i = 0; i < arr.length;i++) {
            for(int j=0; j < arr[i].length;j++) {
                System.out.print(arr[i][j] + ", ");
            }
            System.out.println("");
        }
    }
    */
}
