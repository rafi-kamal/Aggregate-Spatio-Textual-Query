package annk.domain;

import java.util.ArrayList;
import java.util.List;

import annk.aggregator.IAggregator;
import query.Query;

public class GNNKQuery extends AggregateQuery {

	public GNNKQuery(List<Query> queries, IAggregator aggregator) {
		super(queries, aggregator);
	}

	@Override
	public String toString() {
		return "GNNKQuery [" + aggregator.getName() 
			+ ", queries=" + queries + "]";
	}
	
	public static class Result extends AggregateQuery.Result {

		public Result(int id, double cost) {
			super(id, cost);
		}
		
	}
	
}
