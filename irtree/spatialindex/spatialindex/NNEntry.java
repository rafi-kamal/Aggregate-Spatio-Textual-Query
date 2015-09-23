package spatialindex.spatialindex;

/**
 * A holder class for R-tree node and corresponding cost.
 */
public class NNEntry implements Comparable<NNEntry> {

	public IEntry node;
	public double cost;

	public NNEntry(IEntry node, double cost) {
		this.node = node;
		this.cost = cost;
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