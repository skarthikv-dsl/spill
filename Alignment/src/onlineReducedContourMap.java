import java.awt.Dimension;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


public class onlineReducedContourMap implements Serializable{
	private static final long serialVersionUID = 223L;
	HashMap<Integer,ArrayList<location>> ContourPointsMap = new HashMap<Integer,ArrayList<location>>();
	
	public onlineReducedContourMap(HashMap<Integer, ArrayList<location>> CMap) throws IOException{
		Iterator itr = CMap.keySet().iterator();
		while(itr.hasNext()){
			Integer key = (Integer)itr.next();
			for(int pt =0;pt<CMap.get(key).size();pt++){
				location p = CMap.get(key).get(pt);
				//int [] arr = new int[p.dim_values.length];
				//for(int d=0;d<p.dim_values.length;d++)
					//arr[d] = p.get_dimension(d);
				//point_generic p_temp = new point_generic(arr,p.get_plan_no(),p.get_cost(), p.getPredicateOrder());
				location l_temp = new location(p);
				if(!ContourPointsMap.containsKey(key)){
					ArrayList<location> al = new ArrayList<location>();
					al.add(l_temp);
					ContourPointsMap.put(key,al);
				}
				else{
					ContourPointsMap.get(key).add(l_temp);
			}
			}
		}
		
	}
	
	public HashMap<Integer,ArrayList<location>> getContourMap(){
		return ContourPointsMap;
	}
	
}
