import java.awt.Dimension;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;


public class ContourLocationsMap implements Serializable{
	private static final long serialVersionUID = 223L;
	HashMap<Integer,ArrayList<point_generic_opt_sb>> ContourPointsMap = new HashMap<Integer,ArrayList<point_generic_opt_sb>>();
	
	public ContourLocationsMap(HashMap<Integer,ArrayList<point_generic_opt_sb>> CMap) throws IOException{
		for(int c=1;c<=CMap.size();c++){
			for(int pt =0;pt<CMap.get(c).size();pt++){
				point_generic_opt_sb p = CMap.get(c).get(pt);
				int [] arr = new int[p.dim_values.length];
				for(int d=0;d<p.dim_values.length;d++)
					arr[d] = p.get_dimension(d);
				point_generic_opt_sb p_temp = new point_generic_opt_sb(arr,p.get_plan_no(),p.get_cost(), p.getPredicateOrder());
				if(!ContourPointsMap.containsKey(c)){
					ArrayList<point_generic_opt_sb> al = new ArrayList<point_generic_opt_sb>();
					al.add(p_temp);
					ContourPointsMap.put(c,al);
				}
				else{
					ContourPointsMap.get(c).add(p_temp);
			}
			}
		}
		
	}
	
	public HashMap<Integer,ArrayList<point_generic_opt_sb>> getContourMap(){
		return ContourPointsMap;
	}
	
}
