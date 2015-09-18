package annk.domain;

import java.util.List;

import query.Query;

public class SGNNKQuery {
	public List<Query> queries;
	public int subGroupSize;

	public SGNNKQuery(List<Query> queries, int subGroupSize) {
		this.queries = queries;
		this.subGroupSize = subGroupSize;
		
		assert subGroupSize <= queries.size() : 
			"Sub-group size must be less then the number of queries";
	}

	@Override
	public String toString() {
		return "SGNNKQuery [queries=" + queries + ", subGroupSize=" + subGroupSize + "]";
	}
	
}
