package test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import annk.domain.GNNKQuery;
import annk.domain.SGNNKQuery;
import annk.domain.SGNNKQuery.Result;
import annk.spatialindex.IRTree;
import documentindex.InvertedFile;
import io.QueryFileReader;
import io.ResultWriter;
import spatialindex.rtree.RTree;
import spatialindex.storagemanager.DiskStorageManager;
import spatialindex.storagemanager.IStorageManager;
import spatialindex.storagemanager.PropertySet;

public class Main {
	/**
	 * Query types
	 * 0 - gnnk
	 * 1 - gnnk baseline
	 * 2 - sgnnk
	 * 3 - sgnnk baseline
	 * 4 - sgnnk extended
	 * 5 - sgnnk * (n - m + 1)
	 */
	public static void main(String[] args) throws Exception {
		if (args.length < 6) {
			System.out.println("Usage: Main index_file gnnk_query_file sgnnk_query_file "
					+ "topk alpha query_type [keyword_drop_percentage]");
			System.exit(-1);
		}

		String indexFile = args[0];
		String gnnkQueryFile = args[1];
		String sgnnkQueryFile = args[2];
		int topk = Integer.parseInt(args[3]);
		RTree.alpha_dist = Double.parseDouble(args[4]);
		int queryType = Integer.parseInt(args[5]);
		int keywordDropPercentage = 0;
		if (args.length > 6) 
			keywordDropPercentage = Integer.parseInt(args[6]);

		PropertySet ps = new PropertySet();
		ps.setProperty("FileName", indexFile + ".rtree");
		IStorageManager diskfile = new DiskStorageManager(ps);
		
		PropertySet ps2 = new PropertySet();
		int indexIdentifier = 1; // (in this case I know that it is equal to 1)
		ps2.setProperty("IndexIdentifier", indexIdentifier);

		boolean printInConsole = false;
		long startTime = System.currentTimeMillis();

		ResultWriter writer;
		
		InvertedFile invertedFile = new InvertedFile(indexFile, 4096);
		invertedFile.resetIO();
		IRTree tree = new IRTree(ps2, diskfile);
		
		int numberOfQueries;
		double totalCost = 0; // It doesn't keep track of MSGNNK cost - too much extra work 
		double spatialCost = 0;
		double irCost = 0;
		
		if (queryType == 0 || queryType == 1) {
			QueryFileReader reader = new QueryFileReader(gnnkQueryFile, keywordDropPercentage);
			List<GNNKQuery> gnnkQueries = reader.readGNNKQueries();
			numberOfQueries = gnnkQueries.size();
			
			writer = new ResultWriter(gnnkQueries.size(), printInConsole);
			
			for (GNNKQuery q : gnnkQueries) {
				List<GNNKQuery.Result> results;
				if (queryType == 1) {
					if (printInConsole) System.out.println("GNNK Baseline");
					results = tree.gnnkBaseline(invertedFile, q, topk);
				}
				else {
					if (printInConsole) System.out.println("GNNK");
					results = tree.gnnk(invertedFile, q, topk);
				}
				writer.writeGNNKResult(results);
				totalCost += results.get(0).cost.totalCost;
				spatialCost += results.get(0).cost.spatialCost;
				irCost += results.get(0).cost.irCost;
				
				writer.write("========================================");
			}
		}
		else {
			QueryFileReader reader = new QueryFileReader(sgnnkQueryFile, keywordDropPercentage);
			List<SGNNKQuery> sgnnkQueries = reader.readSGNNKQueries();
			numberOfQueries = sgnnkQueries.size();
			
			writer = new ResultWriter(sgnnkQueries.size(), printInConsole);
			
			for (SGNNKQuery q : sgnnkQueries) {
				if (queryType == 4) {
					if (printInConsole) System.out.println("SGNNK Extended");
					Map<Integer, List<SGNNKQuery.Result>> results = tree.sgnnkExtended(invertedFile, q, topk);
					List<Integer> subroupSizes = new ArrayList<>(results.keySet());
					Collections.sort(subroupSizes);
					for (Integer subgroupSize : subroupSizes) {
						writer.write("Size " + subgroupSize);
						writer.writeSGNNKResult(results.get(subgroupSize));
					}
					
				}
				else if (queryType == 5) {
					if (printInConsole) System.out.println("SGNNK Extended Baseline");
					while (q.subGroupSize <= q.groupSize) {
						writer.write("Size " + q.subGroupSize);
						List<Result> results = tree.sgnnk(invertedFile, q, topk);
						writer.writeSGNNKResult(results);
						
						totalCost += results.get(0).cost.totalCost;
						spatialCost += results.get(0).cost.spatialCost;
						irCost += results.get(0).cost.irCost;
						
						q.subGroupSize++;
					}
				}
				else {
					List<SGNNKQuery.Result> results;
					if (queryType == 3) {
						if (printInConsole) System.out.println("SGNNK Baseline");
						results = tree.sgnnkBaseline(invertedFile, q, topk);
					}
					else if (queryType == 2) {
						if (printInConsole) System.out.println("SGNNK");
						results = tree.sgnnk(invertedFile, q, topk);
					}
					results = tree.sgnnk(invertedFile, q, topk);

					totalCost += results.get(0).cost.totalCost;
					spatialCost += results.get(0).cost.spatialCost;
					irCost += results.get(0).cost.irCost;
					
					writer.writeSGNNKResult(results);
				}
				
				writer.write("========================================");
			}
		}

		long totalTime = System.currentTimeMillis() - startTime;
		
		int averageTime = (int) (totalTime / numberOfQueries);
		int averageFileIO = (tree.getIO() + invertedFile.getIO()) / numberOfQueries;
		double averageNodesVisited = tree.noOfVisitedNodes * 1.0 / numberOfQueries;
		double averageSpatialCost = spatialCost / numberOfQueries;
		
		writer.write("Average nodes visited: " + averageNodesVisited);
		writer.write("Total time millisecond: " + totalTime);
		writer.close();
		
		if (printInConsole) {
			System.out.println("Average time millisecond: " + averageTime);
			System.out.println("Average total IO: " + averageFileIO);
//			System.out.println("Average tree IO: " + tree.getIO() * 1.0 / count);
//			System.out.println("Average inverted index IO: " + ivIO * 1.0 / count);
		}
		else {
			System.out.printf("%d %d %.6f\n", averageTime, averageFileIO, averageSpatialCost);
		}
	}
}
