package annk.aggregator;

import java.util.List;

import annk.domain.Cost;

public class SumAggregator implements IAggregator {
	
	double totalSum;
	double spatialSum;
	double irSum;
	
	public SumAggregator() {
		initializeAccmulator();
	}

	@Override
	public Cost getAggregateValue(List<Cost> values, List<Double> weights) {
		double totalSum = 0;
		double spatialSum = 0;
		double irSum = 0;
		for (int i = 0; i < values.size(); i++) {
			totalSum += values.get(i).totalCost * weights.get(i);
			spatialSum += values.get(i).spatialCost * weights.get(i);
			irSum += values.get(i).irCost * weights.get(i);
		}
		return new Cost(irSum, spatialSum, totalSum);
	}

	@Override
	public String getName() {
		return "SUM";
	}

	@Override
	public Cost getAggregateValue(Cost value, int m) {
		return new Cost(m * value.irCost, m * value.spatialCost, m * value.totalCost);
	}

	@Override
	public void initializeAccmulator() {
		totalSum = 0;
		spatialSum = 0;
		irSum = 0;
	}

	@Override
	public void accumulate(Cost value, Double weight) {
		totalSum += value.totalCost * weight;
		spatialSum += value.spatialCost * weight;
		irSum += value.irCost * weight;
	}

	@Override
	public Cost getAccumulatedValue() {
		return new Cost(irSum, spatialSum, totalSum);
	}
	
}
