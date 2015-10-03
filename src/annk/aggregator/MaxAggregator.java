package annk.aggregator;

import java.util.Collections;
import java.util.List;

public class MaxAggregator implements IAggregator {
	
	private double maximum;
	
	@Override
	public double getAggregateValue(List<Double> values) {
		return Collections.max(values);
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
	public void accumulate(Double value) {
		maximum = Math.max(maximum, value);
	}

	@Override
	public double getAccumulatedValue() {
		return maximum;
	}
	
}
