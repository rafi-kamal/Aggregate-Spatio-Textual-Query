package spatialindex.spatialindex;

import java.util.Comparator;



public class NNEntryComparator implements Comparator
{
	public int compare(Object o1, Object o2)
	{
		NNEntry n1 = (NNEntry) o1;
		NNEntry n2 = (NNEntry) o2;

		if (n1.m_minDist < n2.m_minDist) return -1;
		if (n1.m_minDist > n2.m_minDist) return 1;
		return 0;
	}
}