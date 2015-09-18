package query;

import java.util.ArrayList;
import java.util.List;

import spatialindex.spatialindex.Point;

public class Query {
	public int id;
	public Point location;
	public List<Integer> keywords;

	public Query(int id) {
		this.id = id;
		this.keywords = new ArrayList<Integer>();
	}

	public Query(int id, Point location, List<Integer> keywords) {
		this.id = id;
		this.location = location;
		this.keywords = keywords;
	}

	@Override
	public String toString() {
		return "Query [id=" + id + ", location=" + location + ", keywords=" + keywords + "]";
	}
}
