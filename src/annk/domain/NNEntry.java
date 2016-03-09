package annk.domain;

import java.util.List;

import spatialindex.spatialindex.IEntry;

/**
 * A holder class for R-tree node and corresponding cost.
 */
public class NNEntry implements Comparable<NNEntry> {

	public IEntry node;
	public List<Integer> queryIndices;
	public List<Cost> queryCosts;
	public Cost cost;

	public NNEntry(IEntry node, List<Integer> queryIndices, Cost cost) {
		this.node = node;
		this.queryIndices = queryIndices;
		this.cost = cost;
	}

	public NNEntry(IEntry node, Cost cost, List<Cost> queryCosts) {
		this.node = node;
		this.queryCosts = queryCosts;
		this.cost = cost;
	}
	
	public NNEntry(IEntry node, Cost cost) {
		this(node, null, cost);
	}
	
	public int compareTo(NNEntry other) {
		if (this.cost.totalCost < other.cost.totalCost)
			return -1;
		if (this.cost.totalCost > other.cost.totalCost)
			return 1;
		return 0;
	}

	@Override
	public String toString() {
		return "NNEntry [node=" + node + ", cost=" + cost.totalCost + "]";
	}
	
}