package annk.domain;

import java.util.List;

import annk.aggregator.IAggregator;
import query.Query;

public class GNNKQuery extends AggregateQuery {

	public GNNKQuery(List<Query> queries, IAggregator aggregator) {
		this.queries = queries;
		this.aggregator = aggregator;
	}

	@Override
	public String toString() {
		return "GNNKQuery [" + aggregator.getName() 
			+ ", queries=" + queries + "]";
	}
	
}
