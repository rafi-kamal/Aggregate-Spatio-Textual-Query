import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.Hashtable;
import java.util.PriorityQueue;
import java.util.Vector;

import query.Query;
import spatialindex.rtree.RTree;
import spatialindex.spatialindex.NNEntry;
import spatialindex.spatialindex.NNEntryComparator;
import spatialindex.spatialindex.Point;
import spatialindex.spatialindex.RtreeEntry;
import storage.DocumentStore;
import storage.WeightEntry;

public class Test {

	public static void main(String[] args) throws Exception {
		if (args.length != 5) {
			System.err.println("Usage: Test text_file location_file query_file topk alpha.");
			System.exit(-1);
		}
		String text_file = args[0];
		String location_file = args[1];
		String query_file = args[2];
		int topk = Integer.parseInt(args[3]);
		double alpha = Double.parseDouble(args[4]);
		DocumentStore ds = new DocumentStore(text_file, 4096);
		ds.load(0);

		LineNumberReader locationfile = new LineNumberReader(new FileReader(location_file));
		LineNumberReader queryfile = new LineNumberReader(new FileReader(query_file));

		String line;
		String[] temp;
		int id = -1;
		double x, y;
		double[] f = new double[2];

		PriorityQueue queue = new PriorityQueue(100, new NNEntryComparator());
		RTree.alpha_dist = alpha;

		while ((line = queryfile.readLine()) != null) {
			temp = line.split(",");

			id = Integer.parseInt(temp[0]);
			x = Double.parseDouble(temp[1]);
			y = Double.parseDouble(temp[2]);
			f[0] = x;
			f[1] = y;

			Query query = new Query(id);
			for (int j = 3; j < temp.length; j++) {
				query.keywords.add(Integer.parseInt(temp[j]));
			}
			query.point = new Point(f);

			System.out.println("query " + query.id);

			while ((line = locationfile.readLine()) != null) {
				temp = line.split(",");
				id = Integer.parseInt(temp[0]);
				x = Double.parseDouble(temp[1]);
				y = Double.parseDouble(temp[2]);
				f[0] = x;
				f[1] = y;
				Point point = new Point(f);

				Vector document = ds.read(id);
				Hashtable words = new Hashtable();
				for (int i = 0; i < document.size(); i++) {
					WeightEntry de = (WeightEntry) document.get(i);
					words.put(de.word, de.weight);
				}

				double ir = 0;
				for (int i = 0; i < query.keywords.size(); i++) {
					int word = (Integer) query.keywords.get(i);
					if (words.containsKey(word))
						ir += (Double) words.get(word);
				}

				double d = query.point.getMinimumDistance(point);
				double score = RTree.combinedScore(d, ir);

				RtreeEntry e = new RtreeEntry(id, false);
				queue.add(new NNEntry(e, score));
			}

			while (!queue.isEmpty() && topk > 0) {
				topk--;
				NNEntry first = (NNEntry) queue.poll();
				System.out.println(first.m_pEntry.getIdentifier() + "," + first.m_minDist);
			}
		}
		locationfile.close();
		queryfile.close();
	}
}
