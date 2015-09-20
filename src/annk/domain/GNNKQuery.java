package annk.domain;

import java.util.List;

import annk.aggregator.IAggregator;
import query.Query;

public class GNNKQuery {
	public List<Query> queries;
	public IAggregator aggregator;

	public GNNKQuery(List<Query> queries, IAggregator aggregator) {
		this.queries = queries;
		this.aggregator = aggregator;
	}

	@Override
	public String toString() {
		return "GNNKQuery [queries=" + queries + "]";
	}
	
}
