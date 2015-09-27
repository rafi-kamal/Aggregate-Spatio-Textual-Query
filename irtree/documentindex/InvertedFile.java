package documentindex;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import spatialindex.rtree.RTree;
import storage.BtreeStore;
import storage.WeightEntry;

public class InvertedFile {

	private BtreeStore store = null;
	private ArrayList InvertedList;
	private Hashtable InvertedLists;

	public InvertedFile(String filename, int pagesize) throws Exception {
		store = new BtreeStore(filename + ".invertedfile", pagesize);
		InvertedLists = new Hashtable();
	}

	public void create(int treeid) throws Exception {
		store.createBTree(treeid);
		InvertedList = new ArrayList();
		InvertedLists.put(treeid, InvertedList);
	}

	public synchronized void load(int treeid) throws Exception {
		store.loadBTree(treeid);
	}

	public void addDocument(int treeid, int id, Vector document) {

		InvertedList = (ArrayList) InvertedLists.get(treeid);
		for (int i = 0; i < document.size(); i++) {
			WeightEntry de = (WeightEntry) document.get(i);
			InvertedListEntry ie = new InvertedListEntry(de.word);
			int loc = Collections.binarySearch(InvertedList, ie, new InvertedListEntryComparator());
			if (loc >= 0) {
				ie = (InvertedListEntry) InvertedList.get(loc);
				PlEntry ple = new PlEntry(id, de.weight);
				ie.add(ple);
			} else {
				PlEntry ple = new PlEntry(id, de.weight);
				ie.add(ple);
				InvertedList.add((-loc - 1), ie);
			}

		}
	}

	public void addDocument(int treeid, int id, Vector document, int cluster) {

		InvertedList = (ArrayList) InvertedLists.get(treeid);

		for (int i = 0; i < document.size(); i++) {
			WeightEntry de = (WeightEntry) document.get(i);
			InvertedListEntry ie = new InvertedListEntry(de.word);
			int loc = Collections.binarySearch(InvertedList, ie, new InvertedListEntryComparator());
			if (loc >= 0) {
				ie = (InvertedListEntry) InvertedList.get(loc);
				PlEntry ple = new PlEntry(id, de.weight, cluster);
				ie.add(ple);
			} else {
				PlEntry ple = new PlEntry(id, de.weight, cluster);
				ie.add(ple);
				InvertedList.add((-loc - 1), ie);
			}

		}

	}

	public Vector store(int treeid) throws Exception {

		Vector pseudoDoc = new Vector();
		InvertedList = (ArrayList) InvertedLists.get(treeid);
		store.loadBTree(treeid);

		for (int i = 0; i < InvertedList.size(); i++) {
			InvertedListEntry ie = (InvertedListEntry) InvertedList.get(i);
			ByteArrayOutputStream bs = new ByteArrayOutputStream();
			ObjectOutputStream ds = new ObjectOutputStream(bs);

			ds.writeObject(ie.pl);
			ds.flush();
			store.write(ie.term, bs.toByteArray());

			double maxweight = Double.NEGATIVE_INFINITY;
			for (int j = 0; j < ie.pl.size(); j++) {
				PlEntry ple = (PlEntry) ie.pl.get(j);
				maxweight = Math.max(maxweight, ple.weight);
			}
			WeightEntry de = new WeightEntry(ie.term, maxweight);

			pseudoDoc.add(de);
		}
		store.flush();
		InvertedList.clear();
		InvertedLists.remove(treeid);

		return pseudoDoc;
	}

	public Vector[] storeClusterEnhance(int treeid) throws Exception {
		Vector[] pseudoDoc = new Vector[RTree.numOfClusters];
		for (int i = 0; i < pseudoDoc.length; i++) {
			pseudoDoc[i] = new Vector();
		}
		InvertedList = (ArrayList) InvertedLists.get(treeid);
		store.loadBTree(treeid);
		for (int i = 0; i < InvertedList.size(); i++) {
			InvertedListEntry ie = (InvertedListEntry) InvertedList.get(i);
			ByteArrayOutputStream bs = new ByteArrayOutputStream();
			ObjectOutputStream ds = new ObjectOutputStream(bs);
			ds.writeObject(ie.pl);
			ds.flush();

			store.write(ie.term, bs.toByteArray());

			double[] maxweight = new double[RTree.numOfClusters];
			for (int j = 0; j < maxweight.length; j++) {
				maxweight[j] = Double.NEGATIVE_INFINITY;
			}

			for (int j = 0; j < ie.pl.size(); j++) {
				PlEntry ple = (PlEntry) ie.pl.get(j);
				maxweight[ple.cluster] = Math.max(maxweight[ple.cluster], ple.weight);
			}

			for (int j = 0; j < maxweight.length; j++) {
				if (maxweight[j] != Double.NEGATIVE_INFINITY) {
					WeightEntry de = new WeightEntry(ie.term, maxweight[j]);
					pseudoDoc[j].add(de);
				}
			}

		}
		store.flush();
		InvertedList.clear();
		InvertedLists.remove(treeid);
		return pseudoDoc;
	}

	public void flush() throws Exception {
		store.flush();
	}

	/**
	 * Read the posting list of this keyword i.e. the documents which contains this keyword 
	 */
	public synchronized Vector<PlEntry> read(int keyword) throws Exception {

		Vector<PlEntry> doclist = null;

		byte[] data = store.read(keyword);
		if (data == null)
			return null;
		ObjectInputStream ds = new ObjectInputStream(new ByteArrayInputStream(data));
		doclist = (Vector<PlEntry>) ds.readObject();

		return doclist;
	}

	public int getIO() {
		return store.getIO();
	}

	public void resetIO() {
		store.resetIO();
	}

	/**
	 * Calculates the total similarity of each document (node), where weights are
	 * calculated summing over the weight of the terms that matches the query keywords
	 * 
	 * Corresponding tree must be loaded by using load(treeId) before calling this method.
	 * 
	 * @return A map of (document ID, similarity) pairs
	 */
	public HashMap<Integer, Double> rankingSum(List<Integer> keywords) throws Exception {

		HashMap<Integer, Double> filter = new HashMap<Integer, Double>();
//		load(treeid);

		for (int j = 0; j < keywords.size(); j++) {
			int keyword = keywords.get(j);
			
			Vector<PlEntry> doclist = read(keyword);
			if (doclist == null)
				continue;
			for (int k = 0; k < doclist.size(); k++) {
				PlEntry ple = doclist.get(k);

				if (filter.containsKey(ple.documentId)) {
					double similarity = (Double) filter.get(ple.documentId);
					similarity = similarity + ple.weight;
					filter.put(ple.documentId, similarity);
				} else
					filter.put(ple.documentId, ple.weight);
			}
		}

		return filter;
	}

	public HashMap rankingSumClusterEnhance(List<Integer> words) throws Exception {
		HashMap filter = new HashMap();
		HashMap filterfinal = new HashMap();
		
		for (int j = 0; j < words.size(); j++) {
			int word = (Integer) words.get(j);
			Vector doclist = read(word);
			if (doclist == null)
				continue;
			for (int k = 0; k < doclist.size(); k++) {
				PlEntry ple = (PlEntry) doclist.get(k);
				String key = ple.documentId + "," + ple.cluster;
				if (filter.containsKey(key)) {
					double w = (Double) filter.get(key);
					w = w + ple.weight;
					filter.put(key, w);
				} else
					filter.put(key, ple.weight);

			}
		}

		Iterator iter = filter.keySet().iterator();
		while (iter.hasNext()) {
			String key = (String) iter.next();
			double value = (Double) filter.get(key);
			String[] temp = key.split(",");
			int id = Integer.parseInt(temp[0]);
			if (filterfinal.containsKey(id)) {
				double w = (Double) filterfinal.get(id);
				if (w < value)
					filterfinal.put(id, value);
			} else
				filterfinal.put(id, value);
		}

		return filterfinal;
	}
}
