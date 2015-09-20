package annk.spatialindex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

import annk.domain.GNNKQuery;
import documentindex.InvertedFile;
import query.Query;
import query.QueryResult;
import spatialindex.rtree.Node;
import spatialindex.rtree.RTree;
import spatialindex.spatialindex.NNEntry;
import spatialindex.spatialindex.RtreeEntry;
import spatialindex.storagemanager.IStorageManager;
import spatialindex.storagemanager.PropertySet;

public class IRTree extends RTree {

	public IRTree(PropertySet propertySet, IStorageManager storageManager) {
		super(propertySet, storageManager);
	}

	/**
	 * @return A list of objects with size at most k, 
	 * where objects are sorted according to the decreasing value of their costs.
	 */
	public List<NNEntry> gnnk(InvertedFile invertedFile, GNNKQuery gnnkQuery, int topk, boolean pruningEnabled)
			throws Exception {
		
		PriorityQueue<NNEntry> queue = new PriorityQueue<>();
		RtreeEntry e = new RtreeEntry(rootID, false);
		queue.add(new NNEntry(e, 0.0));

		// The object with the worst cost will always be on top
		PriorityQueue<NNEntry> currentBestObjects = new PriorityQueue<>(100, new WorstFirstNNEntryComparator());

		while (queue.size() != 0) {
			NNEntry first = queue.poll();
			e = (RtreeEntry) first.node;

			if (e.isLeafEntry) {
				if (currentBestObjects.size() < topk)
					currentBestObjects.add(first);
				else {
					NNEntry worstResult = currentBestObjects.peek();
					if (worstResult.cost > first.cost) {
						// Replace the worst object with current object
						currentBestObjects.poll();
						currentBestObjects.add(first);
					}
				}

				// System.out.println(e.getIdentifier() + "," + first.cost);
			} else {
				Node n = readNode(e.getIdentifier());

				for (int child = 0; child < n.children; child++) {
					List<Double> queryCosts = new ArrayList<>();
					
					for (Query q : gnnkQuery.queries) {
						HashMap<Integer, Double> filter = invertedFile.rankingSum(n.identifier, q.keywords);
						
						double irscore;
						Object var = filter.get(n.pIdentifiers[child]);
						if (var == null)
							continue;
						else
							irscore = (Double) var;
						
						double queryCost = combinedScore(n.pMBR[child].getMinimumDistance(q.location), irscore);
						queryCosts.add(queryCost);
					}
					
					double aggregateCost = gnnkQuery.aggregator.getAggregateValue(queryCosts);
					
					if (n.level == 0) {
						e = new RtreeEntry(n.pIdentifiers[child], true);
					} else {
						e = new RtreeEntry(n.pIdentifiers[child], false);
					}

					queue.add(new NNEntry(e, aggregateCost));
				}
			}
		}

		List<NNEntry> results = new ArrayList<>(currentBestObjects);
		Collections.sort(results);
		return results;
	}

	/**
	 * Put the entry with highest cost first
	 */
	public class WorstFirstNNEntryComparator implements Comparator<NNEntry> {

		@Override
		public int compare(NNEntry n1, NNEntry n2) {
			if (n1.cost > n2.cost)
				return -1;
			if (n1.cost < n2.cost)
				return 1;
			return 0;
		}

	}
}
