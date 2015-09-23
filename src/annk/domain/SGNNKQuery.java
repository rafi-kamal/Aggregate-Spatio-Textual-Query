package annk.domain;

import java.util.List;

import annk.aggregator.IAggregator;
import query.Query;

public class SGNNKQuery {
	public List<Query> queries;
	public int subGroupSize;
	public IAggregator aggregator;

	public SGNNKQuery(List<Query> queries, int subGroupSize, IAggregator aggregator) {
		this.queries = queries;
		this.subGroupSize = subGroupSize;
		this.aggregator = aggregator;
		
		assert subGroupSize <= queries.size() : 
			"Sub-group size must be less then the number of queries";
	}

	@Override
	public String toString() {
		return "SGNNKQuery [" + aggregator.getName() 
			+ ", queries=" + queries + ", subGroupSize=" + subGroupSize + "]";
	}
	
}
