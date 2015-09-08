package storage;

import java.io.IOException;
import java.util.Properties;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.btree.BTree;
import jdbm.helper.DefaultSerializer;
import jdbm.helper.IntegerComparator;
import jdbm.helper.IntegerSerializer;

public class BtreeStore {

	private String        DATABASE;
	private String        BTREE_NAME;
	private RecordManager recordManager;
	private long          recid;
	private BTree         btree;
	private Properties    props;
	//private Hashtable     btrees;
	private int           pagesize;
	private int           count = 0;
	private int			  IO = 0;
	
	public BtreeStore(String filename, int pagesize) throws Exception{
		//btrees = new Hashtable();
    	props = new Properties();
    	DATABASE = filename;    	           
        recordManager = RecordManagerFactory.createRecordManager( DATABASE, props );
        this.pagesize = pagesize;
    }
	
	public void createBTree(int treeid) throws Exception{
    	
    	BTREE_NAME = String.valueOf(treeid);
    	recid = recordManager.getNamedObject( BTREE_NAME );
	    if ( recid != 0 ) {
	           System.out.println("Existing BTree: " + DATABASE + "," + treeid);
	           System.exit(-1);
	    } 
	    else {
	    	int fanout = pagesize/4-2;
	        btree = BTree.createInstance( recordManager, new IntegerComparator(), new IntegerSerializer(), new DefaultSerializer(), fanout);
	        recordManager.setNamedObject( BTREE_NAME, btree.getRecid() );	          
	    }
	   // btrees.put(treeid, btree);    	
    	
    }
    public void loadBTree(int treeid) throws Exception{
    	
    	BTREE_NAME = String.valueOf(treeid);
    	recid = recordManager.getNamedObject( BTREE_NAME );
	    if ( recid != 0 ) {
	    	btree = BTree.load( recordManager, recid );
	    } 
	    else {
	    	System.out.println("Can't find BTree: " + DATABASE + "," + treeid);
	        System.exit(-1);
	    }   	    	
    }
    
	public void flush() throws Exception
	{	
			recordManager.commit();					
	}
	
	public void write(int id, byte[] data)throws Exception{
					
			btree.insert(id, data, true);
			
			if(count % 10000 == 0)
				recordManager.commit();
			count++;		
	}
	
	public byte[] read(int id)throws IOException{
		
		byte[] data = null;		
		Object var = btree.find(id);
		if(var == null){
				//System.out.println("Couldn't find data " + id);
				//System.exit(-1);		
				return null;
		}			
		data = (byte[])var;	
		IO++;
		return data;
	}
	
	public String getTreeID(){
		return BTREE_NAME;
	}
	
	public int getIO(){
		return IO;
	}
	
	public void resetIO(){
		IO = 0;
	}
}
