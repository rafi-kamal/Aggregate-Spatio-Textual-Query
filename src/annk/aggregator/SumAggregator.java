package annk.aggregator;

import java.util.List;

public class SumAggregator implements IAggregator {

	@Override
	public double getAggregateValue(List<Double> values) {
		double aggregateValue = 0;
		for (int i = 0; i < values.size(); i++) {
			aggregateValue += values.get(i);
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
	
}
