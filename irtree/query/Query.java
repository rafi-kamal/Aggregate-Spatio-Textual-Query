package query;

import java.util.ArrayList;
import java.util.List;

import spatialindex.spatialindex.Point;

public class Query {
	public int id;
	public List<Integer> qwords;
	public Point qpoint;

	public Query(int id) {
		this.id = id;
		this.qwords = new ArrayList<Integer>();
	}

}
