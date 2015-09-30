package spatialindex.spatialindex;

import java.util.List;

import query.Query;

/**
 * A holder class for R-tree node and corresponding cost.
 */
public class NNEntry implements Comparable<NNEntry> {

	public IEntry node;
	public List<Integer> queryIndices;
	public double cost;
	// TODO temporary var, remove this
	public int level = 0;

	public NNEntry(IEntry node, List<Integer> queryIndices, double cost) {
		this.node = node;
		this.queryIndices = queryIndices;
		this.cost = cost;
	}
	
	public NNEntry(IEntry node, double cost) {
		this(node, null, cost);
	}
	
	public double getCost() {
		return Math.pow(level, 10) + cost;
	}
	
	public int compareTo(NNEntry other) {
		if (this.cost < other.cost)
			return -1;
		if (this.cost > other.cost)
			return 1;
		return 0;
	}

	@Override
	public String toString() {
		return "NNEntry [node=" + node + ", cost=" + cost + "]";
	}
	
}