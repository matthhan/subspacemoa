package moa.clusterers.macrosubspace;
import org.apache.commons.math3.distribution.NormalDistribution;
public class ZeroVarianceNormalDistribution extends NormalDistribution {
	double mean;
	public ZeroVarianceNormalDistribution(double mean) {
		this.mean= mean;
	}
	public double sample() {
		return this.mean;
	}
}
