package annk.aggregator;

import java.util.List;

public interface IAggregator {
	/**
	 * Apply the aggregate function over the values and return the result.
	 * 
	 * @return f(value[0], value[1], ... , value[n - 1])
	 */
	double getAggregateValue(List<Double> values, List<Double> weights);
	
	void initializeAccmulator();
	void accumulate(Double value, Double weight);
	double getAccumulatedValue();
	
	/**
	 * Apply the aggregate function <code>m</code> times over <code>value</code>
	 * 
	 * @return f(value, value, ... , value)
	 */
	double getAggregateValue(Double value, int m);
	
	String getName();
}
