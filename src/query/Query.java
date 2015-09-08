package query;

import java.util.HashSet;
import java.util.Vector;

import spatialindex.spatialindex.Point;


public class Query {
	public int id;
	public Vector keywords;
	public Point point;

	
	public Query(int id){
		this.id = id;
		this.keywords = new Vector();
		
	}
	
	
}
