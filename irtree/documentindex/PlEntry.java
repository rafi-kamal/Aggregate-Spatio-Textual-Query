package documentindex;

import java.io.Serializable;

public class PlEntry implements Serializable {

	private static final long serialVersionUID = 2353190903991441447L;
	
	public int documentId;
	public double weight;
	public int cluster;

	public PlEntry(int id, double w) {
		this.documentId = id;
		this.weight = w;
	}

	public PlEntry(int id, double w, int c) {
		this.documentId = id;
		this.weight = w;
		this.cluster = c;
	}
}
