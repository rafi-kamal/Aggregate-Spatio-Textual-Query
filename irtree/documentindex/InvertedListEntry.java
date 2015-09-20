package documentindex;

import java.util.Vector;

import spatialindex.spatialindex.Region;


public class InvertedListEntry {

	int term;
	Vector pl = null;

	
	public InvertedListEntry(int term){
		this.term = term;
		this.pl = new Vector();
		
	}
	

	
	public void add(PlEntry ple){
		pl.add(ple);
	}
	

	
}
