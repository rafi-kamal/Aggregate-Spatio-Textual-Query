package annk.aggregator;

import java.util.List;

public class SumAggregator implements IAggregator {
	
	private double totalAccumulatedValue;

	@Override
	public double getAggregateValue(List<Double> values, List<Double> weights) {
		double aggregateValue = 0;
		for (int i = 0; i < values.size(); i++) {
			aggregateValue += values.get(i) / weights.get(i);
		}
		return aggregateValue;
	}

	@Override
	public String getName() {
		return "SUM";
	}

	@Override
	public double getAggregateValue(Double value, int m) {
		return m * value;
	}

	@Override
	public void initializeAccmulator() {
		totalAccumulatedValue = 0;
	}

	@Override
	public void accumulate(Double value, Double weight) {
		totalAccumulatedValue += value * weight;
	}

	@Override
	public double getAccumulatedValue() {
		return totalAccumulatedValue;
	}
	
}
