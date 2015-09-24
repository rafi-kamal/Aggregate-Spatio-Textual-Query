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
	public List<NNEntry> gnnk(InvertedFile invertedFile, GNNKQuery gnnkQuery, int topk)
			throws Exception {
		
		PriorityQueue<NNEntry> queue = new PriorityQueue<>();
		NNEntry root = new NNEntry(new RtreeEntry(rootID, false), 0.0);
		queue.add(root);

		// The object with the worst cost will always be on top
		PriorityQueue<NNEntry> currentBestObjects = new PriorityQueue<>(100, new WorstFirstNNEntryComparator());

		while (queue.size() != 0) {
			NNEntry first = queue.poll();
			RtreeEntry rTreeEntry = (RtreeEntry) first.node;

			if (rTreeEntry.isLeafEntry) {
				if (currentBestObjects.size() < topk)
					currentBestObjects.add(first);
				else {
					NNEntry worstResult = currentBestObjects.peek();
					if (worstResult.cost > first.cost) {
						// The current object is better than at least one object in currentBestObjects.
						// So replace the worst object with current object
						currentBestObjects.poll();
						currentBestObjects.add(first);
					}
					break;
				}
			} else {
				Node n = readNode(rTreeEntry.getIdentifier());
				
				// For each child node, calculate the cost for all queries.
				// The first parameter is the index of the child node, second parameter is the
				// corresponding list of costs calculated for individual queries
				HashMap<Integer, List<Double>> costs = new HashMap<>();
				for (int child = 0; child < n.children; child++) {
					costs.put(child, new ArrayList<Double>());
				}
//				int[] children = Arrays.copyOfRange(n.pIdentifiers, 0, n.children);
//				System.out.println(n.identifier + ", " + n.level + ": " + Arrays.toString(children));
				
				for (Query q : gnnkQuery.queries) {
					HashMap<Integer, Double> similarities = invertedFile.rankingSum(n.identifier, q.keywords);
					
					for (int child = 0; child < n.children; child++) {
						int childId = n.pIdentifiers[child];
						double irScore = 0;
						if (similarities.containsKey(childId)) 
							irScore = similarities.get(childId);
						
						double spatialCost = n.pMBR[child].getMinimumDistance(q.location);
						double queryCost = combinedScore(spatialCost, irScore);
						costs.get(child).add(queryCost);
					}
				}
				
				// Individual query costs are calculated, now calculate aggregate query cost
				// and prune the children based on this cost.
				for (int child = 0; child < n.children; child++) {
					List<Double> queryCosts = costs.get(child);
					double aggregateCost = gnnkQuery.aggregator.getAggregateValue(queryCosts);
					
					if (n.level == 0) {
						rTreeEntry = new RtreeEntry(n.pIdentifiers[child], true);
					} else {
						rTreeEntry = new RtreeEntry(n.pIdentifiers[child], false);
					}

					queue.add(new NNEntry(rTreeEntry, aggregateCost));
				}
			}
		}

		List<NNEntry> results = new ArrayList<>(currentBestObjects);
		Collections.sort(results);
		return results;
	}

	/**
	 * @return A list of objects with size at most k, 
	 * where objects are sorted according to the decreasing value of their costs.
	 */
	public List<NNEntry> gnnkBaseline(InvertedFile invertedFile, GNNKQuery gnnkQuery, int topk)
			throws Exception {
		
		PriorityQueue<NNEntry> queue = new PriorityQueue<>();
		NNEntry root = new NNEntry(new RtreeEntry(rootID, false), 0.0);
		queue.add(root);

		List<NNEntry> results = new ArrayList<>();

		while (queue.size() != 0) {
			NNEntry first = queue.poll();
			RtreeEntry rTreeEntry = (RtreeEntry) first.node;

			if (rTreeEntry.isLeafEntry) {
				if (results.size() < topk)
					results.add(first);
				else 
					break;
			} else {
				Node n = readNode(rTreeEntry.getIdentifier());
				
				// For each child node, calculate the cost for all queries.
				// The first parameter is the index of the child node, second parameter is the
				// corresponding list of costs calculated for individual queries
				HashMap<Integer, List<Double>> costs = new HashMap<>();
				for (int child = 0; child < n.children; child++) {
					costs.put(child, new ArrayList<Double>());
				}
//				int[] children = Arrays.copyOfRange(n.pIdentifiers, 0, n.children);
//				System.out.println(n.identifier + ", " + n.level + ": " + Arrays.toString(children));
				
				for (Query q : gnnkQuery.queries) {
					HashMap<Integer, Double> similarities = invertedFile.rankingSum(n.identifier, q.keywords);
					
					for (int child = 0; child < n.children; child++) {
						int childId = n.pIdentifiers[child];
						double irScore = 0;
						if (similarities.containsKey(childId)) 
							irScore = similarities.get(childId);
						
						double spatialCost = n.pMBR[child].getMinimumDistance(q.location);
						double queryCost = combinedScore(spatialCost, irScore);
						costs.get(child).add(queryCost);
					}
				}
				
				// Individual query costs are calculated, now calculate aggregate query cost
				for (int child = 0; child < n.children; child++) {
					List<Double> queryCosts = costs.get(child);
					double aggregateCost = gnnkQuery.aggregator.getAggregateValue(queryCosts);
					
					if (n.level == 0) {
						rTreeEntry = new RtreeEntry(n.pIdentifiers[child], true);
					} else {
						rTreeEntry = new RtreeEntry(n.pIdentifiers[child], false);
					}

					queue.add(new NNEntry(rTreeEntry, aggregateCost));
				}
			}
		}

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
