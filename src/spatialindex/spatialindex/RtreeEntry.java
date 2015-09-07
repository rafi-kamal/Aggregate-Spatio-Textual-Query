package spatialindex.spatialindex;

import java.util.Hashtable;

public class RtreeEntry implements IEntry{
	
	int id;
	public boolean isLeafEntry;
	
	public int treeid;
	public int wordhit;
	
	public Hashtable distmap;
	
	IShape mbr;
	public double irscore;

	
	public RtreeEntry(int id, boolean f){
		this.id = id;
		isLeafEntry = f;		
		distmap = new Hashtable();
	}
	public RtreeEntry(int id, double ir){
		this.id = id;		
		this.irscore = ir;
	}
	public RtreeEntry(int id, Region mbr, double ir){
		this.id = id;
		this.mbr = new Region(mbr);
		this.irscore = ir;
	}
	public RtreeEntry(int id, Region mbr, boolean f){
		this.id = id;
		isLeafEntry = f;
		this.mbr = new Region(mbr);
		distmap = new Hashtable();
	}

	public int getIdentifier(){
		return id;
	}
	public IShape getShape(){
		return mbr;
	}
	
	public void setIdentifier(int id){
		this.id = id;
	}
}
