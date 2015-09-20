package spatialindex.spatialindex;

import java.util.Comparator;

public class NNEntryComparator implements Comparator<NNEntry> {
	public int compare(NNEntry n1, NNEntry n2) {

		if (n1.cost < n2.cost)
			return -1;
		if (n1.cost > n2.cost)
			return 1;
		return 0;
	}
}