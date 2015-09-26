package spatialindex.spatialindex;

import java.util.Hashtable;

public class RtreeEntry implements IEntry {

	int id;
	public boolean isLeafEntry;

	public int treeid;
	public int wordhit;

	public Hashtable distmap;

	IShape mbr;

	public RtreeEntry(int id, boolean isLeafEntry) {
		this.id = id;
		this.isLeafEntry = isLeafEntry;
		distmap = new Hashtable();
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

	@Override
	public String toString() {
		return "RtreeEntry [id=" + id + ", isLeafEntry=" + isLeafEntry + ", treeid=" + treeid + ", wordhit=" + wordhit
				+ ", distmap=" + distmap + ", mbr=" + mbr + "]";
	}
	
	
}
