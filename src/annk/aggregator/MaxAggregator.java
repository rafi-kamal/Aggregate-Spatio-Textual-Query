package annk.aggregator;

import java.util.List;

import annk.domain.Cost;

public class MaxAggregator implements IAggregator {
	
	private Cost maximum;
	
	public MaxAggregator() {
		initializeAccmulator();
	}
	
	@Override
	public Cost getAggregateValue(List<Cost> values, List<Double> weights) {
        double maxTotal = -1;
        Cost max = null;
        for (int i = 0; i < values.size(); i++) {
            if (maxTotal < values.get(i).totalCost * weights.get(i)) {
                max = new Cost(values.get(i).irCost * weights.get(i), 
                		values.get(i).spatialCost * weights.get(i),
                		values.get(i).totalCost * weights.get(i));
            }
        }
		return max;
	}

	@Override
	public String getName() {
		return "MAX";
	}

	@Override
	public Cost getAggregateValue(Cost value, int m) {
		return value;
	}

	@Override
	public void initializeAccmulator() {
		maximum = new Cost(0, 0, 0);
	}

	@Override
	public void accumulate(Cost value, Double weight) {
		if (maximum.totalCost < value.totalCost * weight)
			maximum = new Cost(value.irCost * weight, value.spatialCost * weight, value.totalCost * weight);
	}

	@Override
	public Cost getAccumulatedValue() {
		return maximum;
	}
	
}
