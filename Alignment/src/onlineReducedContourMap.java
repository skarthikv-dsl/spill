import java.awt.Dimension;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


public class onlineReducedContourMap implements Serializable{
	private static final long serialVersionUID = 223L;
	HashMap<Integer,ArrayList<Integer>> reducedContourMap = new HashMap<Integer,ArrayList<Integer>>();
	
	public onlineReducedContourMap(HashMap<Integer, ArrayList<Integer>> CMap) throws IOException{
		Iterator itr = CMap.keySet().iterator();
		while(itr.hasNext()){
			Integer key = (Integer)itr.next();
			if(!reducedContourMap.containsKey(key)){
				ArrayList<Integer> al = new ArrayList<Integer>(CMap.get(itr));
				reducedContourMap.put(key,al);
			}
			else{
				assert(false) : "reduced contour map already contains the key";
			}

		}
		
	}
	
	public HashMap<Integer,ArrayList<Integer>> getContourMap(){
		return reducedContourMap;
	}
	
}
