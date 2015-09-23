package annk.aggregator;

public class AggregatorFactory {
	public static IAggregator getAggregator(String aggregatorName) {
		String name = aggregatorName.trim().toUpperCase();
		
		if (name.equals("SUM"))
			return new SumAggregator();
		else if (name.equals("MAX"))
			return new MaxAggregator();
		else
			return null;
	}
}
