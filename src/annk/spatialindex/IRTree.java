package annk.spatialindex;

import java.util.ArrayList;
import java.util.Arrays;
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
import spatialindex.spatialindex.Region;
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

		// Current (at most) k best objects, sorted according to their decreasing value of cost.
		// So the highest cost object will always be on top.
		PriorityQueue<NNEntry> currentBestObjects = 
				new PriorityQueue<>(topk, new WorstFirstNNEntryComparator());
		// Dummy objects
		for (int i = 0; i < topk; i++) {
			currentBestObjects.add(new NNEntry(new RtreeEntry(-1, false), Double.MAX_VALUE));
		}

		// Cost of the highest valued node of current best objects
		double costBound = Double.MAX_VALUE;
		
		while (queue.size() != 0) {
			NNEntry first = queue.poll();
			if (first.cost > costBound)
				continue;
			
			RtreeEntry rTreeEntry = (RtreeEntry) first.node;
		
			Node n = readNode(rTreeEntry.getIdentifier());
			invertedFile.load(n.identifier);
			
			noOfVisitedNodes++;
			if (n.level == 0) {
				// For each child node, calculate the cost for all queries.
				// The first parameter is the index of the child node, second parameter is the
				// corresponding list of costs calculated for individual queries
				HashMap<Integer, List<Double>> costs = new HashMap<>();
				for (int child = 0; child < n.children; child++) {
					costs.put(child, new ArrayList<Double>());
				}

				for (Query q : gnnkQuery.queries) {
					HashMap<Integer, Double> similarities = invertedFile.rankingSum(q.keywords);
					
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
					
					if (aggregateCost < costBound) {
						// The current object is better than at least one object in currentBestObjects.
						// So replace the worst object with current object
						int childId = n.pIdentifiers[child];
						currentBestObjects.poll();
						currentBestObjects.add(new NNEntry(new RtreeEntry(childId, true), aggregateCost));
					}
					costBound = currentBestObjects.peek().cost;
				}
			}
			else {
				List<Integer> combinedQueryKeywords = gnnkQuery.getCombinedKeywords();
				HashMap<Integer, Double> similarities = invertedFile.rankingSum(
						combinedQueryKeywords);
				
				// Individual query costs are calculated, now calculate aggregate query cost
				// and prune the children based on this cost.
				for (int child = 0; child < n.children; child++) {
					Region queryMBR = gnnkQuery.getMBR();
					double spatialCost = n.pMBR[child].getMinimumDistance(queryMBR);
					
					int childId = n.pIdentifiers[child];
					double irScore = 0;
					if (similarities.containsKey(childId)) 
						irScore = similarities.get(childId);
					double querySuperNodeCost = gnnkQuery.queries.size() * combinedScore(spatialCost, irScore);
					
					if (querySuperNodeCost > costBound)
						continue;
					
					rTreeEntry = new RtreeEntry(childId, false);

					queue.add(new NNEntry(rTreeEntry, querySuperNodeCost));
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
				
				noOfVisitedNodes++;
				
				// For each child node, calculate the cost for all queries.
				// The first parameter is the index of the child node, second parameter is the
				// corresponding list of costs calculated for individual queries
				HashMap<Integer, List<Double>> costs = new HashMap<>();
				for (int child = 0; child < n.children; child++) {
					costs.put(child, new ArrayList<Double>());
				}
//				int[] children = Arrays.copyOfRange(n.pIdentifiers, 0, n.children);
//				System.out.println(n.identifier + ", " + n.level + ": " + Arrays.toString(children));
				
				invertedFile.load(n.identifier);
				for (Query q : gnnkQuery.queries) {
					HashMap<Integer, Double> similarities = invertedFile.rankingSum(q.keywords);
					
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
	private class WorstFirstNNEntryComparator implements Comparator<NNEntry> {

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
