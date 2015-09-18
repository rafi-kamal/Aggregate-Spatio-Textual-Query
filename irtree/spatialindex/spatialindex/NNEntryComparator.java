package spatialindex.spatialindex;

import java.util.Comparator;

public class NNEntryComparator implements Comparator<NNEntry> {
	public int compare(NNEntry n1, NNEntry n2) {

		if (n1.minDist < n2.minDist)
			return -1;
		if (n1.minDist > n2.minDist)
			return 1;
		return 0;
	}
}