package annk.domain;

public class Cost {
	public double irCost;
	public double spatialCost;
	public double totalCost;

	public Cost(double irCost, double spatialCost, double totalCost) {
		this.irCost = irCost;
		this.spatialCost = spatialCost;
		this.totalCost = totalCost;
	}

	@Override
	public String toString() {
		return "Cost [irCost=" + irCost + ", spatialCost=" + spatialCost + ", totalCost=" + totalCost + "]";
	}

}
