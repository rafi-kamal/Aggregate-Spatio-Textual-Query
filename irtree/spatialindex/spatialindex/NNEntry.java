package spatialindex.spatialindex;

public class NNEntry {

	public IEntry pEntry;
	public double minDist;
	public double maxDist;

	public NNEntry(IEntry e, double f) {
		pEntry = e;
		minDist = f;
	}

}