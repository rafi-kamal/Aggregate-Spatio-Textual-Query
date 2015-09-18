package build;

import spatialindex.rtree.RTree;
import spatialindex.storagemanager.DiskStorageManager;
import spatialindex.storagemanager.IBuffer;
import spatialindex.storagemanager.IStorageManager;
import spatialindex.storagemanager.PropertySet;
import spatialindex.storagemanager.RandomEvictionsBuffer;
import storage.DocumentStore;
import documentindex.InvertedFile;

public class BuildIRtree {

	public static void main(String[] args)throws Exception
	{
		if (args.length != 3)
		{
			System.err.println("Usage: BuildIRtree text_file index_file pagesize.");
			System.exit(-1);
		}
		String text_file = args[0];
		String index_file = args[1];
		int pagesize = Integer.parseInt(args[2]);		
		DocumentStore ds = new DocumentStore(text_file, pagesize);
		ds.load(0);
		
		PropertySet ps = new PropertySet();
		ps.setProperty("FileName", index_file + ".rtree");
		IStorageManager diskfile = new DiskStorageManager(ps);
		
		PropertySet ps2 = new PropertySet();
		Integer i = new Integer(1); // INDEX_IDENTIFIER_GOES_HERE (suppose I know that in this case it is equal to 1);
		ps2.setProperty("IndexIdentifier", i);
		
		RTree tree = new RTree(ps2, diskfile);
		
		InvertedFile invertedFile = new InvertedFile(index_file, pagesize);
		
		long start = System.currentTimeMillis();
		tree.ir(ds, invertedFile);
		long end = System.currentTimeMillis();
		System.err.println("Minutes: " + ((end - start) / 1000.0f) / 60.0f);
		
		boolean ret = tree.isIndexValid();
		if (ret == false) System.err.println("Structure is INVALID!");
		
		tree.flush();
		invertedFile.flush();
	}
}
