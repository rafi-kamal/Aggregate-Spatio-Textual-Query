package build;

import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.HashSet;

import spatialindex.dirtree.DIRTree;
import spatialindex.spatialindex.Region;
import spatialindex.storagemanager.DiskStorageManager;
import spatialindex.storagemanager.IStorageManager;
import spatialindex.storagemanager.PropertySet;
import storage.DocumentStore;

public class BuildDIRtree {

	public static void main(String[] args)throws Exception
	{
		if (args.length != 7)
		{
			System.err.println("Usage: BuildDIRtree location_file text_file index_file page_size fanout beta_area maxWord.");
			System.exit(-1);
		}
		
		//maxWord is used to control the number of words involved in tree building.
		//Large maxWord may incur high construction cost.
		
		
		String location_file = args[0];
		String text_file = args[1];
		String index_file = args[2];
		int page_size = Integer.parseInt(args[3]);
		int fanout = Integer.parseInt(args[4]);
		double beta_area = Double.parseDouble(args[5]);
		DocumentStore.maxWord = Integer.parseInt(args[6]);
		
		LineNumberReader location_reader = new LineNumberReader(new FileReader(location_file));
		
		// Create a disk based storage manager.
		PropertySet ps = new PropertySet();

		Boolean b = new Boolean(true);
		ps.setProperty("Overwrite", b);
			//overwrite the file if it exists.

		ps.setProperty("FileName", index_file + beta_area + ".rtree");
			// .idx and .dat extensions will be added.

		Integer i = new Integer(page_size);
		ps.setProperty("PageSize", i);
			// specify the page size. Since the index may also contain user defined data
			// there is no way to know how big a single node may become. The storage manager
			// will use multiple pages per node if needed. Off course this will slow down performance.

		IStorageManager diskfile = new DiskStorageManager(ps);

		// Create a new, empty, RTree with dimensionality 2, minimum load 70%, using "file" as
		// the StorageManager and the RSTAR splitting policy.
		PropertySet ps2 = new PropertySet();

		Double f = new Double(0.7);
		ps2.setProperty("FillFactor", f);

		i = new Integer(fanout);
		ps2.setProperty("IndexCapacity", i);
		ps2.setProperty("LeafCapacity", i);
			// Index capacity and leaf capacity may be different.

		i = new Integer(2);
		ps2.setProperty("Dimension", i);
		ps2.setProperty("DocumentFile", text_file);
		
		DIRTree tree = new DIRTree(ps2, diskfile, beta_area);
		
		
		int count = 0;
		int id;
		double x1, x2, y1, y2;
		double[] f1 = new double[2];
		double[] f2 = new double[2];
		String line;
		String[] temp;
		
		long start = System.currentTimeMillis();
		
		while ((line = location_reader.readLine()) != null)
		{
			temp = line.split(",");
			id = Integer.parseInt(temp[0]);
			x1 = Double.parseDouble(temp[1]);
			y1 = Double.parseDouble(temp[2]);
			
			f1[0] = x1; f1[1] = y1;
			f2[0] = x1; f2[1] = y1;
			Region r = new Region(f1, f2);
			
			HashSet doc = DIRTree.objstore.readSet(id);
			
			tree.insertData(null, r, id, doc);
			
			if ((count % 1000) == 0) System.err.println(count);

			count++;
		}
		
		long end = System.currentTimeMillis();
		System.err.println("Operations: " + count);
		System.err.println(tree);
		System.err.println("Minutes: " + ((end - start) / 1000.0f) / 60.0f);

		// since we created a new RTree, the PropertySet that was used to initialize the structure
		// now contains the IndexIdentifier property, which can be used later to reuse the index.
		// (Remember that multiple indices may reside in the same storage manager at the same time
		//  and every one is accessed using its unique IndexIdentifier).
		Integer indexID = (Integer) ps2.getProperty("IndexIdentifier");
		System.err.println("Index ID: " + indexID);

		boolean ret = tree.isIndexValid();
		if (ret == false) System.err.println("Structure is INVALID!");

		// flush all pending changes to persistent storage (needed since Java might not call finalize when JVM exits).
		tree.flush();
		location_reader.close();
		
	}
}
