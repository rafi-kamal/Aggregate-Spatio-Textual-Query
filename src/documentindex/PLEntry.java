package documentindex;

import java.io.Serializable;



public class PLEntry implements Serializable{

	int docid;
	double weight;
	int cluster;

	
	public PLEntry(int id, double w){
		docid = id;
		weight = w;
	}
	
	public PLEntry(int id, double w, int c){
		docid = id;
		weight = w;
		cluster = c;
	}
}
