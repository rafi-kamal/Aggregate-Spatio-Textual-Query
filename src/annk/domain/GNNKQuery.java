package annk.domain;

import java.util.List;

import query.Query;

public class GNNKQuery {
	public List<Query> queries;

	public GNNKQuery(List<Query> queries) {
		this.queries = queries;
	}

	@Override
	public String toString() {
		return "GNNKQuery [queries=" + queries + "]";
	}
	
}
