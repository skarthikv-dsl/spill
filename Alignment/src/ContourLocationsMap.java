import java.awt.Dimension;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


public class ContourLocationsMap implements Serializable{
	private static final long serialVersionUID = 223L;
	HashMap<Integer,ArrayList<point_generic>> ContourPointsMap = new HashMap<Integer,ArrayList<point_generic>>();
	
	public ContourLocationsMap(HashMap<Integer,ArrayList<point_generic>> CMap) throws IOException{
		Iterator itr = CMap.keySet().iterator();
		while(itr.hasNext()){
			Integer key = (Integer)itr.next();
			for(int pt =0;pt<CMap.get(key).size();pt++){
				point_generic p = CMap.get(key).get(pt);
				//int [] arr = new int[p.dim_values.length];
				//for(int d=0;d<p.dim_values.length;d++)
					//arr[d] = p.get_dimension(d);
				//point_generic p_temp = new point_generic(arr,p.get_plan_no(),p.get_cost(), p.getPredicateOrder());
				point_generic p_temp = new point_generic(p);
				if(!ContourPointsMap.containsKey(key)){
					ArrayList<point_generic> al = new ArrayList<point_generic>();
					al.add(p_temp);
					ContourPointsMap.put(key,al);
				}
				else{
					ContourPointsMap.get(key).add(p_temp);
			}
			}
		}
		
	}
	
	public HashMap<Integer,ArrayList<point_generic>> getContourMap(){
		return ContourPointsMap;
	}
	
}
