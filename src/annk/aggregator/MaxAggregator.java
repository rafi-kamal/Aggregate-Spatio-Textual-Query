package annk.aggregator;

import java.util.List;

public class MaxAggregator implements IAggregator {
	
	private double maximum;
	
	@Override
	public double getAggregateValue(List<Double> values, List<Double> weights) {
        double max = 0;
        for (int i = 0; i < values.size(); i++) {
            if (max < values.get(i) * weights.get(i)) {
                max = values.get(i) * weights.get(i);
            }
        }
		return max;
	}

	@Override
	public String getName() {
		return "MAX";
	}

	@Override
	public double getAggregateValue(Double value, int m) {
		return value;
	}

	@Override
	public void initializeAccmulator() {
		maximum = Double.MIN_VALUE;
	}

	@Override
	public void accumulate(Double value, Double weight) {
		maximum = Math.max(maximum * weight, value);
	}

	@Override
	public double getAccumulatedValue() {
		return maximum;
	}
	
}
