package storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Vector;



public class DocumentStore {

	private BtreeStore docstore = null;
	public static int maxWord;
	
	public DocumentStore(String filename, int pagesize)throws Exception{
		docstore = new BtreeStore(filename + ".store", pagesize);
		
	}
	
	public void write(int id, Vector words)throws Exception{
		
			ByteArrayOutputStream bs = new ByteArrayOutputStream();
			ObjectOutputStream ds = new ObjectOutputStream(bs);
			ds.writeObject(words);
			ds.flush();			
			docstore.write(id, bs.toByteArray());				
	}
	
	public Vector read(int id)throws Exception{
		
		Vector words = null;
		byte[] data = docstore.read(id);
		if(data == null)
			return null;
		ObjectInputStream ds = new ObjectInputStream(new ByteArrayInputStream(data));
		words = (Vector)ds.readObject();			
		return words;
	}
	
	public HashSet readSet(int id)throws Exception{
		
		Vector words = null;
		byte[] data = docstore.read(id);
		if(data == null)
			return null;
		ObjectInputStream ds = new ObjectInputStream(new ByteArrayInputStream(data));
		words = (Vector)ds.readObject();		
		
		HashSet s = new HashSet();
		for(int i = 0; i < words.size(); i++){
			WeightEntry de = (WeightEntry)words.get(i);
			if(de.word <= maxWord)
				s.add(de.word);
		}
		return s;
	}

	public void flush() throws Exception
	{	
			docstore.flush();					
	}
	
	public void create(int treeid)throws Exception{
		docstore.createBTree(treeid);
	}
	
	public void load(int treeid)throws Exception{
		docstore.loadBTree(treeid);
	}
}
