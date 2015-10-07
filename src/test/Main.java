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
	 * 1 - gnnk baseline
	 * 2 - sgnnk baseline
	 * 3 - gnnk
	 * 4 - sgnnk
	 * 5 - sgnnk * (n - m + 1)
	 * 6 - sgnnk extended
	 */
	public static void main(String[] args) throws Exception {
		if (args.length < 5) {
			System.out.println("Usage: Main index_file gnnk_query_file sgnnk_query_file topk alpha query_type");
			System.exit(-1);
		}

		String indexFile = args[0];
		String gnnkQueryFile = args[1];
		String sgnnkQueryFile = args[2];
		int topk = Integer.parseInt(args[3]);
		RTree.alpha_dist = Double.parseDouble(args[4]);
		int queryType = 6;
		if (args.length > 5)
			queryType = Integer.parseInt(args[5]);

		PropertySet ps = new PropertySet();
		ps.setProperty("FileName", indexFile + ".rtree");
		IStorageManager diskfile = new DiskStorageManager(ps);
		
		PropertySet ps2 = new PropertySet();
		int indexIdentifier = 1; // (in this case I know that it is equal to 1)
		ps2.setProperty("IndexIdentifier", indexIdentifier);

		int count = 0;
		long startTime = 0;
		long totalTime = 0;

		int invertedFileIO = 0;
		int treeIO = 0;
		int totalVisitedNodes = 0;
		
		boolean printInConsole = false;
		startTime = System.currentTimeMillis();

		ResultWriter writer;
		if (queryType == 1 || queryType == 3) {
			QueryFileReader reader = new QueryFileReader(gnnkQueryFile);
			List<GNNKQuery> gnnkQueries = reader.readGNNKQueries();
			writer = new ResultWriter(gnnkQueries.size(), printInConsole);
			
			for (GNNKQuery q : gnnkQueries) {
				InvertedFile invertedFile = new InvertedFile(indexFile, 4096);
				IRTree tree = new IRTree(ps2, diskfile);
				
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
				
				totalVisitedNodes += tree.noOfVisitedNodes;
				treeIO += tree.getIO();
				invertedFileIO += invertedFile.getIO();
				count++;
				
				writer.write("========================================");
			}
		}
		else {
			QueryFileReader reader = new QueryFileReader(sgnnkQueryFile);
			List<SGNNKQuery> sgnnkQueries = reader.readSGNNKQueries();
			writer = new ResultWriter(sgnnkQueries.size(), printInConsole);
			
			for (SGNNKQuery q : sgnnkQueries) {
				if (queryType == 6) {
					InvertedFile invertedFile = new InvertedFile(indexFile, 4096);
					IRTree tree = new IRTree(ps2, diskfile);
					
					if (printInConsole) System.out.println("SGNNK Extended");
					Map<Integer, List<SGNNKQuery.Result>> results = tree.sgnnkExtended(invertedFile, q, topk);
					List<Integer> subroupSizes = new ArrayList<Integer>(results.keySet());
					Collections.sort(subroupSizes);
					for (Integer subgroupSize : subroupSizes) {
						writer.write("Size " + subgroupSize);
						writer.writeSGNNKResult(results.get(subgroupSize));
					}
					
					totalVisitedNodes += tree.noOfVisitedNodes;
					treeIO += tree.getIO();
					invertedFileIO += invertedFile.getIO();
				}
				else if (queryType == 5) {
					if (printInConsole) System.out.println("SGNNK Extended Baseline");
					while (q.subGroupSize <= q.groupSize) {
						InvertedFile invertedFile = new InvertedFile(indexFile, 4096);
						IRTree tree = new IRTree(ps2, diskfile);
						
						writer.write("Size " + q.subGroupSize);
						List<Result> results = tree.sgnnk(invertedFile, q, topk);
						writer.writeSGNNKResult(results);
						
						q.subGroupSize++;
						
						totalVisitedNodes += tree.noOfVisitedNodes;
						treeIO += tree.getIO();
						invertedFileIO += invertedFile.getIO();
					}
				}
				else {
					InvertedFile invertedFile = new InvertedFile(indexFile, 4096);
					IRTree tree = new IRTree(ps2, diskfile);
					
					List<SGNNKQuery.Result> results;
					if (queryType == 2) {
						if (printInConsole) System.out.println("SGNNK Baseline");
						results = tree.sgnnkBaseline(invertedFile, q, topk);
					}
					else if (queryType == 4) {
						if (printInConsole) System.out.println("SGNNK");
						results = tree.sgnnk(invertedFile, q, topk);
					}
					results = tree.sgnnk(invertedFile, q, topk);
					writer.writeSGNNKResult(results);
					
					totalVisitedNodes += tree.noOfVisitedNodes;
					treeIO += tree.getIO();
					invertedFileIO += invertedFile.getIO();
				}
				count++;
				
				writer.write("========================================");
			}
		}
		
		totalTime = System.currentTimeMillis() - startTime;
		
		writer.write("Average nodes visited: " + totalVisitedNodes * 1.0 / count);
		writer.write("Total time millisecond: " + totalTime);
		writer.close();
		
		double averageCPUTime = totalTime * 1.0 / count;
		double averageIO = (treeIO + invertedFileIO) * 1.0 / count;
		if (printInConsole) {
			System.out.println("Average time millisecond: " + averageCPUTime);
			System.out.println("Average total IO: " + averageIO);
//			System.out.println("Average tree IO: " + tree.getIO() * 1.0 / count);
//			System.out.println("Average inverted index IO: " + ivIO * 1.0 / count);
		}
		else {
			System.out.println(averageCPUTime + " " + averageIO);
		}
	}
}
