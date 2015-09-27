package annk.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import annk.aggregator.IAggregator;
import query.Query;
import spatialindex.spatialindex.Point;
import spatialindex.spatialindex.Region;

public class AggregateQuery {
	public List<Query> queries;
	public IAggregator aggregator;

	public Region getMBR() {
		double lowerX = Double.MAX_VALUE;
		double lowerY = Double.MAX_VALUE;
		double upperX = -Double.MAX_VALUE;
		double upperY = -Double.MAX_VALUE;
		
		for (Query q : queries) {
			lowerX = Math.min(lowerX, q.location.m_pCoords[0]);
			lowerY = Math.min(lowerY, q.location.m_pCoords[1]);
			upperX = Math.max(upperX, q.location.m_pCoords[0]);
			upperY = Math.max(upperY, q.location.m_pCoords[1]);
		}
		
		Point lower = new Point(new double[] {lowerX, lowerY});
		Point upper = new Point(new double[] {upperX, upperY});
		
		return new Region(lower, upper);
	}
	
	public List<Integer> getCombinedKeywords() {
		Set<Integer> keywords = new HashSet<>();
		
		for (Query q : queries) {
			keywords.addAll(q.keywords);
		}
		
		return new ArrayList<>(keywords);
	}
}
