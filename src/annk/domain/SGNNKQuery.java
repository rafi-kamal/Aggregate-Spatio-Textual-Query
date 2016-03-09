package annk.domain;

import java.util.List;

import annk.aggregator.IAggregator;
import query.Query;

public class SGNNKQuery extends AggregateQuery {
	public int subGroupSize;

	public SGNNKQuery(List<Query> queries, int subGroupSize, IAggregator aggregator) {
		super(queries, aggregator);
		this.subGroupSize = subGroupSize;
		
		assert subGroupSize <= queries.size() : 
			"Sub-group size must be less then the number of queries";
	}

	@Override
	public String toString() {
		return "SGNNKQuery [" + aggregator.getName() 
			+ ", queries=" + queries + ", subGroupSize=" + subGroupSize + "]";
	}
	
	public static class Result extends AggregateQuery.Result {
		public List<Integer> queryIds;

		public Result(int id, Cost cost, List<Integer> queryIds) {
			super(id, cost);
			this.queryIds = queryIds;
		}

		@Override
		public String toString() {
			return "Result [id=" + id + ", cost=" + cost + ", queryIds=" + queryIds + "]";
		}
		
	}
	
}
