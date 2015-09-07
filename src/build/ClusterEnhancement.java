package build;

import java.util.Properties;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.btree.BTree;
import spatialindex.rtree.RTree;
import spatialindex.storagemanager.DiskStorageManager;
import spatialindex.storagemanager.IStorageManager;
import spatialindex.storagemanager.PropertySet;
import storage.DocumentStore;
import documentindex.InvertedFile;


//cluster information is stored in a B-tree.

public class ClusterEnhancement {

	public static void main(String[] args)throws Exception
	{
		if (args.length != 5)
		{
			System.err.println("Usage: ClusterEnhancement text_file index_file cluster_file #cluster pagesize.");
			System.exit(-1);
		}
		String text_file = args[0];
		String index_file = args[1];
		String cluster_file = args[2];
		RTree.numOfClusters = Integer.parseInt(args[3]);
		int pagesize = Integer.parseInt(args[4]);		
		DocumentStore ds = new DocumentStore(text_file, pagesize);
		ds.load(0);
		
		PropertySet ps = new PropertySet();
		ps.setProperty("FileName", index_file + ".rtree");
		IStorageManager diskfile = new DiskStorageManager(ps);
		
		PropertySet ps2 = new PropertySet();
		Integer i = new Integer(1); // INDEX_IDENTIFIER_GOES_HERE (suppose I know that in this case it is equal to 1);
		ps2.setProperty("IndexIdentifier", i);
		
		RTree tree = new RTree(ps2, diskfile);
		
		InvertedFile invertedFile = new InvertedFile(index_file + "." + RTree.numOfClusters, pagesize);
				
		RecordManager recman;
        long          recid;	       
        BTree         btree = null;
        Properties    props;
        props = new Properties();
        String DATABASE = cluster_file;
        String BTREE_NAME = "0";
        
     // open database and setup an object cache
        recman = RecordManagerFactory.createRecordManager( DATABASE, props );

        // try to reload an existing B+Tree
        recid = recman.getNamedObject( BTREE_NAME );
        if ( recid != 0 ) {
            btree = BTree.load( recman, recid );
            System.out.println( "Reloaded existing BTree with " + btree.size()
                                + " records." );
            
        } else {
            // create a new B+Tree data structure and use a StringComparator
            // to order the records based on people's name.
        	
            System.out.println( "No cluster BTree" );           
            System.exit(-1);
        }
		long start = System.currentTimeMillis();
		tree.clusterEnhance(btree, ds, invertedFile);
		long end = System.currentTimeMillis();
		System.err.println("Minutes: " + ((end - start) / 1000.0f) / 60.0f);
		
		boolean ret = tree.isIndexValid();
		if (ret == false) System.err.println("Structure is INVALID!");
		
		tree.flush();
		invertedFile.flush();
	}
}
