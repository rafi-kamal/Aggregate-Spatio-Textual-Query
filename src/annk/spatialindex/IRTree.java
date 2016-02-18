package annk.spatialindex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import annk.domain.AggregateQuery;
import annk.domain.GNNKQuery;
import annk.domain.SGNNKQuery;
import documentindex.InvertedFile;
import query.Query;
import spatialindex.rtree.Node;
import spatialindex.rtree.RTree;
import spatialindex.spatialindex.NNEntry;
import spatialindex.spatialindex.RtreeEntry;
import spatialindex.storagemanager.IStorageManager;
import spatialindex.storagemanager.PropertySet;

public class IRTree extends RTree {
	
	// TODO temporary variable, delete when no longer needed
	private Map<Integer, List<AggregateQuery.Result>> levelVsVisitedNodes = new HashMap<>();

	public IRTree(PropertySet propertySet, IStorageManager storageManager) {
		super(propertySet, storageManager);
	}
	
	public List<GNNKQuery.Result> gnnkBaseline(InvertedFile invertedFile, GNNKQuery gnnkQuery, int topk)
			throws Exception {
		LinkedList<NNEntry> list = new LinkedList<>();
		NNEntry root = new NNEntry(new RtreeEntry(rootID, false), 0.0);
		list.add(root);

		// Current (at most) k best objects, sorted according to their decreasing value of cost.
		// So the highest cost object will always be on top.
		PriorityQueue<GNNKQuery.Result> currentBestObjects = 
				new PriorityQueue<>(topk, new WorstFirstNNEntryComparator());
		// Dummy objects
		for (int i = 0; i < topk; i++) {
			currentBestObjects.add(new GNNKQuery.Result(-1, Double.MAX_VALUE));
		}

		// Cost of the highest valued node of current best objects
		double costBound = Double.MAX_VALUE;

		while (list.size() != 0) {
			NNEntry first = list.poll();
			RtreeEntry rTreeEntry = (RtreeEntry) first.node;
			
			if (first.cost > costBound)
				continue;
			
			Node n = readNode(rTreeEntry.getIdentifier());
			noOfVisitedNodes++;

			HashMap<Integer, List<Double>> costs = calculateQueryCosts(invertedFile, gnnkQuery.queries, n);
			
			// Individual query costs are calculated, now calculate aggregate query cost
			for (int child = 0; child < n.children; child++) {
				List<Double> queryCosts = costs.get(child);
				double aggregateCost = gnnkQuery.aggregator.getAggregateValue(queryCosts, gnnkQuery.getWeights());
				
				int childId = n.pIdentifiers[child];
				if (n.level == 0) {
					currentBestObjects.add(new GNNKQuery.Result(childId, aggregateCost));
					currentBestObjects.poll();
					costBound = currentBestObjects.peek().cost;
				} else {
					rTreeEntry = new RtreeEntry(childId, false);
					NNEntry entry = new NNEntry(rTreeEntry, aggregateCost);
					list.addFirst(entry);
				}
			}
		}
		
		List<GNNKQuery.Result> results = new ArrayList<>(currentBestObjects);
		Collections.sort(results);
		return results;
	}
	
	/**
	 * @return A list of objects with size at most k, 
	 * where objects are sorted according to the decreasing value of their costs.
	 */
	public List<SGNNKQuery.Result> sgnnkBaseline(InvertedFile invertedFile, SGNNKQuery sgnnkQuery, int topk)
			throws Exception {
		LinkedList<NNEntry> list = new LinkedList<>();
		NNEntry root = new NNEntry(new RtreeEntry(rootID, false), 0.0);
		list.add(root);

		List<SGNNKQuery.Result> results = new ArrayList<>();

		// Current (at most) k best objects, sorted according to their decreasing value of cost.
		// So the highest cost object will always be on top.
		PriorityQueue<SGNNKQuery.Result> currentBestObjects = 
				new PriorityQueue<>(topk, new WorstFirstNNEntryComparator());
		// Dummy objects
		for (int i = 0; i < topk; i++) {
			currentBestObjects.add(new SGNNKQuery.Result(-1, Double.MAX_VALUE, null));
		}
		double costBound = Double.MAX_VALUE;

		while (list.size() != 0) {
			NNEntry first = list.poll();
			if (first.cost > costBound)
				continue;
			
			RtreeEntry rTreeEntry = (RtreeEntry) first.node;
			Node n = readNode(rTreeEntry.getIdentifier());
			
			noOfVisitedNodes++;
			
			HashMap<Integer, List<Double>> costs = calculateQueryCosts(invertedFile, sgnnkQuery.queries, n);
			
			// Individual query costs are calculated, now calculate aggregate query cost
			for (int child = 0; child < n.children; child++) {
				final List<Double> queryCosts = costs.get(child);
				
				List<Integer> minimumCostQueryIndices = new ArrayList<>();
				for (int queryIndex = 0; queryIndex < queryCosts.size(); queryIndex++) {
					minimumCostQueryIndices.add(queryIndex);
				}
				// Sort query indices according to increasing order of query cost
				Collections.sort(minimumCostQueryIndices, (i1, i2) -> {
                    if (queryCosts.get(i1) < queryCosts.get(i2)) return -1;
                    else if (queryCosts.get(i1) > queryCosts.get(i2)) return 1;
                    return 0;
                });
				
				// Now choose first m queries with lowest cost
				List<Double> minimumQueryCosts = new ArrayList<>();		
				List<Integer> minimumCostQueryIds = new ArrayList<>();	
				for (int i = 0; i < sgnnkQuery.subGroupSize; i++) {
					int queryIndex = minimumCostQueryIndices.get(i);
					minimumQueryCosts.add(queryCosts.get(queryIndex));
					minimumCostQueryIds.add(sgnnkQuery.queries.get(queryIndex).id);
				}
				
				minimumQueryCosts = minimumQueryCosts.subList(0, sgnnkQuery.subGroupSize);
				minimumCostQueryIndices = minimumCostQueryIndices.subList(0, sgnnkQuery.subGroupSize);
                List<Double> minimumQueryWeights = new ArrayList<>();
                for (Integer queryIndex : minimumCostQueryIndices) {
                    minimumQueryWeights.add(sgnnkQuery.queries.get(queryIndex).weight);
                }

                double aggregateCost = sgnnkQuery.aggregator.getAggregateValue(minimumQueryCosts, minimumQueryWeights);
				int childId = n.pIdentifiers[child];
				
				if (n.level == 0) {
					currentBestObjects.add(new SGNNKQuery.Result(childId, aggregateCost, minimumCostQueryIds));
					currentBestObjects.poll();
					costBound = currentBestObjects.peek().cost;
				} else {
					rTreeEntry = new RtreeEntry(childId, false);
					list.addFirst(new NNEntry(rTreeEntry, minimumCostQueryIds, aggregateCost));
				}

			}
		}

		Collections.sort(results);
		return results;
	}


	/**
	 * @return A list of objects with size at most k, 
	 * where objects are sorted according to the decreasing value of their costs.
	 */
	public List<GNNKQuery.Result> gnnk(InvertedFile invertedFile, GNNKQuery gnnkQuery, int topk)
			throws Exception {
		PriorityQueue<NNEntry> queue = new PriorityQueue<>();
		NNEntry root = new NNEntry(new RtreeEntry(rootID, false), 0.0);
		queue.add(root);

		List<GNNKQuery.Result> results = new ArrayList<>();
		
		while (queue.size() != 0) {
			NNEntry first = queue.poll();
			RtreeEntry rTreeEntry = (RtreeEntry) first.node;

			if (rTreeEntry.isLeafEntry) {
				if (results.size() < topk)
					results.add(new GNNKQuery.Result(first.node.getIdentifier(), first.cost));
				else 
					break;
			} else {
				Node n = readNode(rTreeEntry.getIdentifier());
				
				noOfVisitedNodes++;

				HashMap<Integer, List<Double>> costs = calculateQueryCosts(invertedFile, gnnkQuery.queries, n);
				
				// Individual query costs are calculated, now calculate aggregate query cost
				for (int child = 0; child < n.children; child++) {
					List<Double> queryCosts = costs.get(child);
					double aggregateCost = gnnkQuery.aggregator.getAggregateValue(queryCosts, gnnkQuery.getWeights());
					
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
	 * @return A list of objects with size at most k, 
	 * where objects are sorted according to the decreasing value of their costs.
	 */
	public List<SGNNKQuery.Result> sgnnk(InvertedFile invertedFile, SGNNKQuery sgnnkQuery, int topk)
			throws Exception {
		PriorityQueue<NNEntry> queue = new PriorityQueue<>();
		NNEntry root = new NNEntry(new RtreeEntry(rootID, false), 0.0);
		queue.add(root);

		List<SGNNKQuery.Result> results = new ArrayList<>();

		while (queue.size() != 0) {
			NNEntry first = queue.poll();
			RtreeEntry rTreeEntry = (RtreeEntry) first.node;

			if (rTreeEntry.isLeafEntry) {
				if (results.size() < topk)
					results.add(new SGNNKQuery.Result(first.node.getIdentifier(), first.cost, first.queryIndices));
				else 
					break;
			} else {
				Node n = readNode(rTreeEntry.getIdentifier());
				noOfVisitedNodes++;
				
				if (!levelVsVisitedNodes.containsKey(n.level))
					levelVsVisitedNodes.put(n.level, new ArrayList<>());
				levelVsVisitedNodes.get(n.level).add(new AggregateQuery.Result(n.identifier, first.cost));
				
				HashMap<Integer, List<Double>> costs = calculateQueryCosts(invertedFile, sgnnkQuery.queries, n);
				
				// Individual query costs are calculated, now calculate aggregate query cost
				for (int child = 0; child < n.children; child++) {
					final List<Double> queryCosts = costs.get(child);
					
					List<Integer> minimumCostQueryIndices = new ArrayList<>();
					for (int queryIndex = 0; queryIndex < queryCosts.size(); queryIndex++) {
						minimumCostQueryIndices.add(queryIndex);
					}
					// Sort query indices according to increasing order of query cost
					Collections.sort(minimumCostQueryIndices, (i1, i2) -> {
						if (queryCosts.get(i1) < queryCosts.get(i2)) return -1;
						else if (queryCosts.get(i1) > queryCosts.get(i2)) return 1;
						return 0;
					});
					
					// Now choose first m queries with lowest cost
					List<Double> minimumQueryCosts = new ArrayList<>();		
					List<Integer> minimumCostQueryIds = new ArrayList<>();	
					for (int i = 0; i < sgnnkQuery.subGroupSize; i++) {
						Integer queryIndex = minimumCostQueryIndices.get(i);
						minimumQueryCosts.add(queryCosts.get(queryIndex));
						minimumCostQueryIds.add(sgnnkQuery.queries.get(queryIndex).id);
					}
					
					minimumQueryCosts = minimumQueryCosts.subList(0, sgnnkQuery.subGroupSize);
                    minimumCostQueryIndices = minimumCostQueryIndices.subList(0, sgnnkQuery.subGroupSize);

                    List<Double> minimumQueryWeights = new ArrayList<>();
                    for (Integer queryIndex : minimumCostQueryIndices) {
                        minimumQueryWeights.add(sgnnkQuery.queries.get(queryIndex).weight);
                    }
					
					double aggregateCost = sgnnkQuery.aggregator.getAggregateValue(minimumQueryCosts, minimumQueryWeights);
					
					if (n.level == 0) {
						rTreeEntry = new RtreeEntry(n.pIdentifiers[child], true);
					} else {
						rTreeEntry = new RtreeEntry(n.pIdentifiers[child], false);
					}

					queue.add(new NNEntry(rTreeEntry, minimumCostQueryIds, aggregateCost));
				}
			}
		}

		Collections.sort(results);
		return results;
	}
	
	/**
	 * @return A list of objects with size at most k, 
	 * where objects are sorted according to the decreasing value of their costs.
	 */
	public Map<Integer, List<SGNNKQuery.Result>> sgnnkExtended(InvertedFile invertedFile, SGNNKQuery sgnnkQuery, int topk)
			throws Exception {
		PriorityQueue<NNEntry> queue = new PriorityQueue<>();
		NNEntry root = new NNEntry(new RtreeEntry(rootID, false), 0.0, null);
		queue.add(root);

		Map<Integer, PriorityQueue<SGNNKQuery.Result>> topResults = new HashMap<>();
		for (int i = sgnnkQuery.subGroupSize; i <= sgnnkQuery.groupSize; i++) {
			PriorityQueue<SGNNKQuery.Result> bestResults = new PriorityQueue<>(topk, new WorstFirstNNEntryComparator());
			for (int j = 0; j < topk; j++) {
				bestResults.add(new SGNNKQuery.Result(-1, Double.MAX_VALUE, null));
			}
			topResults.put(i, bestResults);
		}

		while (queue.size() != 0) {
			NNEntry first = queue.poll();
			RtreeEntry rTreeEntry = (RtreeEntry) first.node;
			
			// For root, querycosts will be null
			if (first.queryCosts != null) {		
				boolean pruneNode = true;
				for (int i = 0; i < first.queryCosts.size(); i++) {
					double prunningBound = topResults.get(i + sgnnkQuery.subGroupSize).peek().cost;
//					System.out.println(prunningBound);
					if (first.queryCosts.get(i) < prunningBound) 
						pruneNode = false;
				}
				if (pruneNode) continue;
			}

			Node n = readNode(rTreeEntry.getIdentifier());
			
			noOfVisitedNodes++;
			
			HashMap<Integer, List<Double>> costs = calculateQueryCosts(invertedFile, sgnnkQuery.queries, n);
			
			// Individual query costs are calculated, now calculate aggregate query cost
			for (int child = 0; child < n.children; child++) {
				final List<Double> queryCosts = costs.get(child);
				
				List<Integer> minimumCostQueryIndices = new ArrayList<>();
				for (int queryIndex = 0; queryIndex < queryCosts.size(); queryIndex++) {
					minimumCostQueryIndices.add(queryIndex);
				}
				// Sort query indices according to increasing order of query cost
				Collections.sort(minimumCostQueryIndices, (i1, i2) -> {
                    if (queryCosts.get(i1) < queryCosts.get(i2)) return -1;
                    else if (queryCosts.get(i1) > queryCosts.get(i2)) return 1;
                    return 0;
                });
				
				// Now choose first m queries with lowest cost
				List<Double> minimumQueryCosts = new ArrayList<>();		
				List<Integer> minimumCostQueryIds = new ArrayList<>();	
				for (int i = 0; i < sgnnkQuery.groupSize; i++) {
					Integer queryIndex = minimumCostQueryIndices.get(i);
					minimumQueryCosts.add(queryCosts.get(queryIndex));
					minimumCostQueryIds.add(sgnnkQuery.queries.get(queryIndex).id);
				}
				
				sgnnkQuery.aggregator.initializeAccmulator();
				for (int i = 0; i < sgnnkQuery.subGroupSize - 1; i++) {
                    int queryIndex = minimumCostQueryIndices.get(i);
					sgnnkQuery.aggregator.accumulate(minimumQueryCosts.get(i),
                            sgnnkQuery.queries.get(queryIndex).weight);
				}
				
				double totalQueryCost = 0;
				boolean prune = true;
				List<Double> aggregateQueryCosts = new ArrayList<>();
				for (int i = sgnnkQuery.subGroupSize - 1; i < sgnnkQuery.groupSize; i++) {
                    int queryIndex = minimumCostQueryIndices.get(i);
					sgnnkQuery.aggregator.accumulate(minimumQueryCosts.get(i),
                            sgnnkQuery.queries.get(queryIndex).weight);
					double queryCost = sgnnkQuery.aggregator.getAccumulatedValue();
					aggregateQueryCosts.add(queryCost);
					
					PriorityQueue<SGNNKQuery.Result> bestResults = topResults.get(i + 1);
					List<Integer> queryIds = minimumCostQueryIds.subList(0, i + 1);
					if (queryCost < bestResults.peek().cost) {
						prune = false;
						if (n.level == 0) {
							bestResults.add(new SGNNKQuery.Result(n.getChildIdentifier(child), queryCost, queryIds));
							bestResults.poll();
						}
					}
					
					totalQueryCost += queryCost;
				}
				double avgQueryCost = totalQueryCost / (sgnnkQuery.groupSize - sgnnkQuery.subGroupSize + 1);
				
				if (n.level > 0 && !prune) {
					rTreeEntry = new RtreeEntry(n.pIdentifiers[child], false);
					queue.add(new NNEntry(rTreeEntry, avgQueryCost, aggregateQueryCosts));
				}
			}
		}

		Map<Integer, List<SGNNKQuery.Result>> results = new HashMap<>();
		for (Integer subgroupSize : topResults.keySet()) {
			List<SGNNKQuery.Result> result = new ArrayList<>(topResults.get(subgroupSize));
			Collections.sort(result);
			results.put(subgroupSize, result);
		}
		return results;
	}


	/**
	 * For each child node, calculate the cost for all queries.
	 * The first parameter of the result map is the index of the child node, 
	 * second parameter is the corresponding list of costs calculated for individual queries.
	 */
	private HashMap<Integer, List<Double>> calculateQueryCosts(InvertedFile invertedFile, List<Query> queries, Node n)
			throws Exception {
		
		HashMap<Integer, List<Double>> costs = new HashMap<>();
		for (int child = 0; child < n.children; child++) {
			costs.put(child, new ArrayList<>());
		}
		
		invertedFile.load(n.identifier);
		for (Query q : queries) {
			HashMap<Integer, Double> similarities = invertedFile.rankingSum(q.keywords, q.keywordWeights);
			
			for (int child = 0; child < n.children; child++) {
				int childId = n.pIdentifiers[child];
				double irScore = 0;
				if (similarities.containsKey(childId)) 
					irScore = similarities.get(childId) / q.keywords.size();
				
				double spatialCost = n.pMBR[child].getMinimumDistance(q.location);
				double queryCost = combinedScore(spatialCost, irScore);
				costs.get(child).add(queryCost);
			}
		}
		return costs;
	}
	

	/**
	 * Put the entry with highest cost first
	 */
	private class WorstFirstNNEntryComparator implements Comparator<AggregateQuery.Result> {

		@Override
		public int compare(AggregateQuery.Result n1, AggregateQuery.Result n2) {
			if (n1.cost > n2.cost)
				return -1;
			if (n1.cost < n2.cost)
				return 1;
			return 0;
		}

	}
	
	/**
	 * @return A list of objects with size at most k, 
	 * where objects are sorted according to the decreasing value of their costs.
	 */
//	public List<NNEntry> gnnkWithQuerySupernode(InvertedFile invertedFile, GNNKQuery gnnkQuery, int topk)
//			throws Exception {
//		
//		PriorityQueue<NNEntry> queue = new PriorityQueue<>();
//		NNEntry root = new NNEntry(new RtreeEntry(rootID, false), 0.0);
//		queue.add(root);
//
//		// Current (at most) k best objects, sorted according to their decreasing value of cost.
//		// So the highest cost object will always be on top.
//		PriorityQueue<NNEntry> currentBestObjects = 
//				new PriorityQueue<>(topk, new WorstFirstNNEntryComparator());
//		// Dummy objects
//		for (int i = 0; i < topk; i++) {
//			currentBestObjects.add(new NNEntry(new RtreeEntry(-1, false), Double.MAX_VALUE));
//		}
//
//		// Cost of the highest valued node of current best objects
//		double costBound = Double.MAX_VALUE;
//		
//		Region queryMBR = gnnkQuery.getMBR();
//		List<Integer> combinedQueryKeywords = gnnkQuery.getCombinedKeywords();
//		
//		while (queue.size() != 0) {
//			NNEntry first = queue.poll();
//			if (first.cost > costBound)
//				continue;
//			
//			RtreeEntry rTreeEntry = (RtreeEntry) first.node;
//		
//			Node n = readNode(rTreeEntry.getIdentifier());
//			invertedFile.load(n.identifier);
//			
//			noOfVisitedNodes++;
//			if (n.level == 0) {
//				// For each child node, calculate the cost for all queries.
//				// The first parameter is the index of the child node, second parameter is the
//				// corresponding list of costs calculated for individual queries
//				HashMap<Integer, List<Double>> costs = new HashMap<>();
//				for (int child = 0; child < n.children; child++) {
//					costs.put(child, new ArrayList<Double>());
//				}
//
//				for (Query q : gnnkQuery.queries) {
//					HashMap<Integer, Double> similarities = invertedFile.rankingSum(q.keywords);
//					
//					for (int child = 0; child < n.children; child++) {
//						int childId = n.pIdentifiers[child];
//						double irScore = 0;
//						if (similarities.containsKey(childId)) 
//							irScore = similarities.get(childId) / q.keywords.size();
//						
//						double spatialCost = n.pMBR[child].getMinimumDistance(q.location);
//						double queryCost = combinedScore(spatialCost, irScore);
//						costs.get(child).add(queryCost);
//					}
//				}
//				
//				// Individual query costs are calculated, now calculate aggregate query cost
//				// and prune the children based on this cost.
//				for (int child = 0; child < n.children; child++) {
//					List<Double> queryCosts = costs.get(child);
//					double aggregateCost = gnnkQuery.aggregator.getAggregateValue(queryCosts);
//					
//					if (aggregateCost < costBound) {
//						// The current object is better than at least one object in currentBestObjects.
//						// So replace the worst object with current object
//						int childId = n.pIdentifiers[child];
//						currentBestObjects.poll();
//						currentBestObjects.add(new NNEntry(new RtreeEntry(childId, true), aggregateCost));
//					}
//					costBound = currentBestObjects.peek().cost;
//				}
//			}
//			else {
//				HashMap<Integer, Double> similarities = invertedFile.rankingSum(
//						combinedQueryKeywords);
//				
//				// Individual query costs are calculated, now calculate aggregate query cost
//				// and prune the children based on this cost.
//				for (int child = 0; child < n.children; child++) {
//					double spatialCost = n.pMBR[child].getMinimumDistance(queryMBR);
//					
//					int childId = n.pIdentifiers[child];
//					double irScore = 0;
//					if (similarities.containsKey(childId)) 
//						irScore = similarities.get(childId);
//					double querySuperNodeCost = gnnkQuery.queries.size() * combinedScore(spatialCost, irScore);
//					
//					if (querySuperNodeCost > costBound)
//						continue;
//					
//					rTreeEntry = new RtreeEntry(childId, false);
//
//					queue.add(new NNEntry(rTreeEntry, querySuperNodeCost));
//				}
//
//			}
//		}
//
//		List<NNEntry> results = new ArrayList<>(currentBestObjects);
//		Collections.sort(results);
//		return results;
//	}

}
