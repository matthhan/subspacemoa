package moa.clusterers.macrosubspace;

import org.apache.commons.math3.distribution.NormalDistribution;
class MultipleNormalDistributions {
	private NormalDistribution[] distributions;
	MultipleNormalDistributions(double[] means,double[] stds) {
		assert(means.length == stds.length);
		this.distributions = new NormalDistribution[means.length];
		for(int i = 0; i < this.distributions.length;i++){
			if(stds[i] == 0) this.distributions[i] = new ZeroVarianceNormalDistribution(means[i]);
			else this.distributions[i] = new NormalDistribution(means[i],stds[i]);
		}
	}
	double[] sample() {
		double[] res = new double[this.distributions.length];
		for(int i= 0; i < res.length;i++) {
			res[i] = this.distributions[i].sample();
		}
		return res;
	}
}
