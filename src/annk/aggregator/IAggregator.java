package annk.aggregator;

import java.util.List;

public interface IAggregator {
	double getAggregateValue(List<Double> values);
}
