package documentindex;

import java.util.Comparator;



public class InvertedListEntryComparator implements Comparator{
	
	public int compare(Object o1, Object o2)
	{
		InvertedListEntry n1 = (InvertedListEntry) o1;
		InvertedListEntry n2 = (InvertedListEntry) o2;

		if (n1.term < n2.term) return -1;
		if (n1.term > n2.term) return 1;
		return 0;
	}

}
