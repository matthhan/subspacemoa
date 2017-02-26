/**
 * [SubspaceMeasureCollection.java] for Subspace MOA
 * 
 * Evaluation measure: Base class for subspace measures
 * 
 * @author Yunsu Kim
 * Data Management and Data Exploration Group, RWTH Aachen University
 */

package moa.evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import moa.AbstractMOAObject;
import moa.cluster.SubspaceClustering;
import moa.gui.subspacevisualization.SubspaceDataPoint;

public abstract class SubspaceMeasureCollection extends AbstractMOAObject{
    
	private static final long serialVersionUID = 1L;
	
	private String[] names;
    private ArrayList<Double>[] values;
    private ArrayList<Double>[] sortedValues;
    private ArrayList<String> events;
    
    private double[] minValue;
    private double[] maxValue;
    private double[] sumValues;
    private boolean[] enabled;
    private int[] corrupted;
    private double time;
    private boolean debug = true;
    private MembershipMatrix mm = null;

    private HashMap<String, Integer> map;

    private int numMeasures = 0;
    
    private ArrayList<Double>[] subValues;


     public SubspaceMeasureCollection() {
        names = getNames();
        numMeasures = names.length;
        map = new HashMap<String, Integer>(numMeasures);        
        for (int i = 0; i < names.length; i++) {
             map.put(names[i],i);
        }
        values = (ArrayList<Double>[]) new ArrayList[numMeasures];
        subValues = (ArrayList<Double>[]) new ArrayList[numMeasures];
        sortedValues = (ArrayList<Double>[]) new ArrayList[numMeasures];
        maxValue = new double[numMeasures];
        minValue = new double[numMeasures];
        sumValues = new double[numMeasures];
        corrupted = new int[numMeasures];
        enabled = getDefaultEnabled();
        time = 0;
        events = new ArrayList<String>();

        for (int i = 0; i < numMeasures; i++) {
            values[i] = new ArrayList<Double>();
            subValues[i] = new ArrayList<Double>();
            sortedValues[i] = new ArrayList<Double>();
            maxValue[i] = Double.MIN_VALUE;
            minValue[i] = Double.MAX_VALUE;
            corrupted[i] = 0;
            sumValues[i] = 0.0;
        }
    }

    protected abstract String[] getNames();

    public void addValue(int index, double value) {
        if (Double.isNaN(value)) {
        	if (debug) System.out.println("NaN for " + names[index]);
        	corrupted[index]++;
        } else if (value < 0) {
        	if (debug) System.out.println("Negative value for " + names[index]);
        	corrupted[index]++;
        } else {
	        sumValues[index] += value;
        
	        if (value < minValue[index]) minValue[index] = value;
	        if (value > maxValue[index]) maxValue[index] = value;
        }
        
        values[index].add(value);
    }

    protected void addValue(String name, double value){
        if (map.containsKey(name)) {
            addValue(map.get(name), value);
        } else {
            System.out.println(name + " is not a valid measure key, no value added");
        }
    }
    
    protected void addSubValue(String name, double value) {
    	if (map.containsKey(name)) {
    		subValues[map.get(name)].add(value);
    	} else {
            System.out.println(name + " is not a valid measure key, no value added");
        }
    }
     
    //add an empty entry e.g. if evaluation crashed internally
    public void addEmptySubValue(int index) {
        subValues[index].add(Double.NaN);
    }

    public int getNumMeasures(){
        return numMeasures;
    }

    public String getName(int index){
    	return names[index];
    }

    public double getMaxValue(int index){
        return maxValue[index];
    }

    public double getMinValue(int index){
        return minValue[index];
    }

    public double getLastValue(int index){
         if (values[index].size() < 1) return Double.NaN;
         double value = values[index].get(values[index].size() - 1);
         if (Double.isNaN(value))
        	 return Double.NaN;
         else
        	 return value;
     }

     public double getMean(int index){
         if (values[index].size() < 1)
             return Double.NaN;

         return sumValues[index] / (values[index].size() - corrupted[index]);
     }

     private void updateSortedValues(int index){
         //naive implementation of insertion sort
         for (int i = sortedValues[index].size(); i < values[index].size(); i++) {
             double v = values[index].get(i);
             int insertIndex = 0;
             while(!sortedValues[index].isEmpty() && insertIndex < sortedValues[index].size() && v > sortedValues[index].get(insertIndex))
                 insertIndex++;
             sortedValues[index].add(insertIndex,v);
         }
//         for (int i = 0; i < sortedValues[index].size(); i++) {
//             System.out.print(sortedValues[index].get(i)+" ");
//         }
//         System.out.println();
     }

     public void clean(int index){
         sortedValues[index].clear();
     }

     public double getMedian(int index){
         updateSortedValues(index);
         int size = sortedValues[index].size();

         if(size > 0){
             if(size%2 == 1)
                 return sortedValues[index].get((int)(size/2));
             else
                 return (sortedValues[index].get((size-1)/2)+sortedValues[index].get((size-1)/2+1))/2.0;
         }
         return Double.NaN;
    }

     public double getLowerQuartile(int index){
         updateSortedValues(index);
         int size = sortedValues[index].size();
         if(size > 11){
             return sortedValues[index].get(Math.round(size*0.25f));
         }
         return Double.NaN;
     }

     public double getUpperQuartile(int index){
         updateSortedValues(index);
         int size = sortedValues[index].size();
         if(size > 11){
             return sortedValues[index].get(Math.round(size*0.75f-1));
         }
         return Double.NaN;
     }


     public int getNumberOfValues(int index){
         return values[index].size();
     }

     public double getValue(int index, int i){
         if(i>=values[index].size()) return Double.NaN;
         return values[index].get(i);
     }

     public ArrayList<Double> getAllValues(int index){
         return values[index];
     }

     public void setEnabled(int index, boolean value){
         enabled[index] = value;
     }

     public boolean isEnabled(int index){
         return enabled[index];
     }

     public double getMeanRunningTime(){
         if(values[0].size()!=0)
            return (time/10e5/values[0].size());
         else
             return 0;
     }

     protected boolean[] getDefaultEnabled(){
         boolean[] defaults = new boolean[numMeasures];
         for (int i = 0; i < defaults.length; i++) {
             defaults[i] = true;
         }
         return defaults;
     }

    protected abstract void subEvaluateSubspaceClustering(SubspaceClustering clustering, SubspaceClustering trueClustering, List<SubspaceDataPoint> points) throws Exception;

     /*
      * Evaluate Clustering
      *
      * return Time in milliseconds
      */
    public double subEvaluateClusteringPerformance(SubspaceClustering clustering, SubspaceClustering trueClustering, List<SubspaceDataPoint> points) throws Exception{
        long start = System.nanoTime();
        subEvaluateSubspaceClustering(clustering, trueClustering, points);
        long duration = System.nanoTime()-start;
        time+=duration;
        duration/=10e5;
        return duration;
    }
     
    public void averageSubEvaluations() {
    	for (int i = 0; i < subValues.length; i++) {
    		double sum = 0.0;
    		for (int j = 0; j < subValues[i].size(); j++) {
    			sum += subValues[i].get(j);
    		}
    		addValue(names[i], sum / subValues[i].size());
    	}
    	
    	subValues = (ArrayList<Double>[]) new ArrayList[numMeasures];
    	for (int i = 0; i < numMeasures; i++) {
    		subValues[i] = new ArrayList<Double>();
    	}
    }

    public void getDescription(StringBuilder sb, int indent) {

    }

    public void addEventType(String type){
    	events.add(type);
    }
    
    public String getEventType(int index){
    	if(index < events.size())
    		return events.get(index);
    	else
    		return null;
    }
}

