package spatialindex.spatialindex;

import java.util.Hashtable;

public class NNEntry {

	
		public IEntry m_pEntry;
		public double m_minDist;
		public double m_maxDist;
		

		public NNEntry(IEntry e, double f) { 
			m_pEntry = e; 
			m_minDist = f; 
			
		}
	
}