package query;

import java.util.HashSet;
import java.util.Vector;

import spatialindex.spatialindex.Point;


public class Query {
	public int id;
	public Vector qwords;
	public Point qpoint;

	
	public Query(int id){
		this.id = id;
		this.qwords = new Vector();
		
	}
	
	
}
