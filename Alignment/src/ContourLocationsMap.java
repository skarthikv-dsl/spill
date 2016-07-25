import java.awt.Dimension;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;


public class ContourLocationsMap implements Serializable{
	private static final long serialVersionUID = 223L;
	HashMap<Integer,ArrayList<point_generic>> ContourPointsMap = new HashMap<Integer,ArrayList<point_generic>>();
	
	public ContourLocationsMap(HashMap<Integer,ArrayList<point_generic>> CMap) throws IOException{
		for(int c=1;c<=CMap.size();c++){
			for(int pt =0;pt<CMap.get(c).size();pt++){
				point_generic p = CMap.get(c).get(pt);
				//int [] arr = new int[p.dim_values.length];
				//for(int d=0;d<p.dim_values.length;d++)
					//arr[d] = p.get_dimension(d);
				//point_generic p_temp = new point_generic(arr,p.get_plan_no(),p.get_cost(), p.getPredicateOrder());
				point_generic p_temp = new point_generic(p);
				if(!ContourPointsMap.containsKey(c)){
					ArrayList<point_generic> al = new ArrayList<point_generic>();
					al.add(p_temp);
					ContourPointsMap.put(c,al);
				}
				else{
					ContourPointsMap.get(c).add(p_temp);
			}
			}
		}
		
	}
	
	public HashMap<Integer,ArrayList<point_generic>> getContourMap(){
		return ContourPointsMap;
	}
	
}
