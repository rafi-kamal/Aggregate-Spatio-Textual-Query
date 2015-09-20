package query;

public class QueryResult implements Comparable<QueryResult> {
	public int objectId;
	public double cost;
	
	@Override
	public int compareTo(QueryResult o) {
		if (cost < o.cost)
			return -1;
		else if (cost > o.cost)
			return 1;
		else
			return 0;
	}
}
