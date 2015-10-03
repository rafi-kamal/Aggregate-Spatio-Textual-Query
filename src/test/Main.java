package test;

import java.util.List;
import java.util.Map;

import annk.domain.GNNKQuery;
import annk.domain.SGNNKQuery;
import annk.spatialindex.IRTree;
import documentindex.InvertedFile;
import io.QueryFileReader;
import io.ResultWriter;
import spatialindex.rtree.RTree;
import spatialindex.spatialindex.NNEntry;
import spatialindex.storagemanager.DiskStorageManager;
import spatialindex.storagemanager.IStorageManager;
import spatialindex.storagemanager.PropertySet;

public class Main {
	public static void main(String[] args) throws Exception {
		if (args.length != 4) {
			System.out.println("Usage: Main index_file query_file topk alpha");
			System.exit(-1);
		}

		String indexFile = args[0];
		String queryFile = args[1];
		int topk = Integer.parseInt(args[2]);
		RTree.alpha_dist = Double.parseDouble(args[3]);

		PropertySet ps = new PropertySet();
		ps.setProperty("FileName", indexFile + ".rtree");
		IStorageManager diskfile = new DiskStorageManager(ps);
		
		PropertySet ps2 = new PropertySet();
		int indexIdentifier = 1; // (in this case I know that it is equal to 1)
		ps2.setProperty("IndexIdentifier", indexIdentifier);

		int count = 0;
		long startTime = 0;
		long totalTime = 0;

		int ivIO = 0;
		int totalVisitedNodes = 0;

		QueryFileReader reader = new QueryFileReader(queryFile);
//		List<GNNKQuery> gnnkQueries = reader.readGNNKQueries();
		List<SGNNKQuery> gnnkQueries = reader.readSGNNKQueries();
		
		startTime = System.currentTimeMillis();
		ResultWriter writer = new ResultWriter(gnnkQueries.size(), true);
		for (SGNNKQuery q : gnnkQueries) {
			InvertedFile invertedFile = new InvertedFile(indexFile, 4096);
			IRTree tree = new IRTree(ps2, diskfile);
//			List<NNEntry> results = tree.gnnkWithQuerySupernode(invertedFile, q, topk);
//			List<NNEntry> results = tree.gnnkWithPrunning(invertedFile, q, topk);
//			List<NNEntry> results = tree.gnnk(invertedFile, q, topk);
//			writer.writeGNNKResult(results);
			
			List<SGNNKQuery.Result> results = tree.sgnnk(invertedFile, q, topk);
			writer.writeSGNNKResult(results);

//			Map<Integer, List<SGNNKQuery.Result>> results = tree.sgnnkExtended(invertedFile, q, topk);
//			for (Integer subgroupSize : results.keySet()) {
//				writer.write("Size " + subgroupSize);
//				writer.writeSGNNKResult(results.get(subgroupSize));
//			}

			totalVisitedNodes += tree.noOfVisitedNodes;
			ivIO += invertedFile.getIO();
			count++;
			
			writer.write("========================================");
		}
		
		totalTime = System.currentTimeMillis() - startTime;
		
		writer.write("Average nodes visited: " + totalVisitedNodes * 1.0 / count);
		writer.write("Total time millisecond: " + totalTime);
		writer.close();

		System.out.println("Average time millisecond: " + totalTime * 1.0 / count);
//		System.out.println("Average total IO: " + (tree.getIO() + ivIO) * 1.0 / count);
//		System.out.println("Average tree IO: " + tree.getIO() * 1.0 / count);
//		System.out.println("Average inverted index IO: " + ivIO * 1.0 / count);
	}
}
