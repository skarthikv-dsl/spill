import java.awt.Dimension;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


public class onlineLocationsMap implements Serializable{
	private static final long serialVersionUID = 223L;
	HashMap<Short,ArrayList<location>> ContourPointsMap = new HashMap<Short,ArrayList<location>>();
	
	public onlineLocationsMap(HashMap<Short, ArrayList<location>> CMap) throws IOException{
		Iterator itr = CMap.keySet().iterator();
		while(itr.hasNext()){
			Short key = (Short)itr.next();
			for(int pt =0;pt<CMap.get(key).size();pt++){
				location p = CMap.get(key).get(pt);
				//int [] arr = new int[p.dim_values.length];
				//for(int d=0;d<p.dim_values.length;d++)
					//arr[d] = p.get_dimension(d);
				//point_generic p_temp = new point_generic(arr,p.get_plan_no(),p.get_cost(), p.getPredicateOrder());
				//location l_temp = new location(p);
				if(!ContourPointsMap.containsKey(key)){
					ArrayList<location> al = new ArrayList<location>();
					al.add(p);
					ContourPointsMap.put(key,al);
				}
				else{
					ContourPointsMap.get(key).add(p);
			}
			}
		}
		
	}
	
	public HashMap<Short,ArrayList<location>> getContourMap(){
		return ContourPointsMap;
	}
	
}
