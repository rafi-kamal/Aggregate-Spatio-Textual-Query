package spatialindex.spatialindex;

import java.util.Hashtable;

public class RtreeEntry implements IEntry {

	int id;
	public boolean isLeafEntry;

	public int treeid;
	public int wordhit;

	public Hashtable distmap;

	IShape mbr;
	public double irscore;

	public RtreeEntry(int id, boolean isLeafEntry) {
		this.id = id;
		this.isLeafEntry = isLeafEntry;
		distmap = new Hashtable();
	}

	public RtreeEntry(int id, double ir) {
		this.id = id;
		this.irscore = ir;
	}

	public RtreeEntry(int id, Region mbr, double ir) {
		this.id = id;
		this.mbr = new Region(mbr);
		this.irscore = ir;
	}

	public RtreeEntry(int id, Region mbr, boolean isLeafEntry) {
		this.id = id;
		this.isLeafEntry = isLeafEntry;
		this.mbr = new Region(mbr);
		distmap = new Hashtable();
	}

	public int getIdentifier() {
		return id;
	}

	public IShape getShape() {
		return mbr;
	}

	public void setIdentifier(int id) {
		this.id = id;
	}
}
