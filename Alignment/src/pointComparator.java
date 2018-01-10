//package net.codejava.collections;
 
import java.util.ArrayList;
import java.util.Comparator;

import org.apache.commons.lang3.builder.CompareToBuilder;

public class pointComparator implements Comparator<point_generic> {
	
	int sort_order [];
	
	pointComparator(int dimensions){
		
		sort_order = new int[dimensions];
		
		for(int i=0; i < dimensions; i++)
			sort_order[i] = i;
	}
	
	pointComparator(int dimensions, ArrayList<Integer> remainingDim){
		
		sort_order = new int[dimensions];
		int i;
		
		ArrayList<Integer> learntDims = new ArrayList<Integer>();
		for(i=0; i < dimensions; i++){
			if(!remainingDim.contains(new Integer(i))) //learnt dimension
				learntDims.add(i);
		}
		
		for(i=0; i < learntDims.size(); i++){
				sort_order[i] = learntDims.get(i).intValue();
		}
		
		for(; i < dimensions; i++)
			sort_order[i] = remainingDim.get(i-learntDims.size()).intValue();
	}
	
	public int compare(point_generic p1, point_generic p2) {
		
		if(p1.get_no_of_dimension()==2){
			
			if(p1.dim_values != null && p2.dim_values != null){
	        return new CompareToBuilder()
            .append(p1.get_dimension(1), p2.get_dimension(1))
            .append(p1.get_dimension(0), p2.get_dimension(0)).toComparison();
			}
			else if(p1.dim_sel_values != null && p2.dim_sel_values != null){
				return new CompareToBuilder()
            .append(p1.get_selOfdimension(sort_order[0]), p2.get_selOfdimension(sort_order[0]))
            .append(p1.get_selOfdimension(sort_order[1]), p2.get_selOfdimension(sort_order[1])).toComparison();
			}

		}
		else if(p1.get_no_of_dimension()==3){
			
			if(p1.dim_values != null && p2.dim_values!=null){
	        return new CompareToBuilder()
	        .append(p1.get_dimension(2), p2.get_dimension(2))
            .append(p1.get_dimension(1), p2.get_dimension(1))
            .append(p1.get_dimension(0), p2.get_dimension(0)).toComparison();
			}
			else if(p1.dim_sel_values != null && p2.dim_sel_values!=null){
		        return new CompareToBuilder()
		        .append(p1.get_selOfdimension(sort_order[0]), p2.get_selOfdimension(sort_order[0]))
	            .append(p1.get_selOfdimension(sort_order[1]), p2.get_selOfdimension(sort_order[1]))
	            .append(p1.get_selOfdimension(sort_order[2]), p2.get_selOfdimension(sort_order[2])).toComparison();
				}
		}
		else if(p1.get_no_of_dimension()==4){
			
			if(p1.dim_values != null && p2.dim_values != null){
	        return new CompareToBuilder()
	        .append(p1.get_dimension(3), p2.get_dimension(3))
	        .append(p1.get_dimension(2), p2.get_dimension(2))
            .append(p1.get_dimension(1), p2.get_dimension(1))
            .append(p1.get_dimension(0), p2.get_dimension(0)).toComparison();
			}
			else if(p1.dim_sel_values != null && p2.dim_sel_values != null){
		        return new CompareToBuilder()
		        .append(p1.get_selOfdimension(sort_order[0]), p2.get_selOfdimension(sort_order[0]))
		        .append(p1.get_selOfdimension(sort_order[1]), p2.get_selOfdimension(sort_order[1]))
	            .append(p1.get_selOfdimension(sort_order[2]), p2.get_selOfdimension(sort_order[2]))
	            .append(p1.get_selOfdimension(sort_order[3]), p2.get_selOfdimension(sort_order[3])).toComparison();
				}
		}
		
		else if(p1.get_no_of_dimension()==5){
			if(p1.dim_values != null && p2.dim_values != null){
	        return new CompareToBuilder()
	        .append(p1.get_dimension(4), p2.get_dimension(4))
	        .append(p1.get_dimension(3), p2.get_dimension(3))
	        .append(p1.get_dimension(2), p2.get_dimension(2))
            .append(p1.get_dimension(1), p2.get_dimension(1))
            .append(p1.get_dimension(0), p2.get_dimension(0)).toComparison();
			}
			else if(p1.dim_sel_values != null && p2.dim_sel_values != null){
				return new CompareToBuilder()
		        .append(p1.get_selOfdimension(sort_order[0]), p2.get_selOfdimension(sort_order[0]))
		        .append(p1.get_selOfdimension(sort_order[1]), p2.get_selOfdimension(sort_order[1]))
		        .append(p1.get_selOfdimension(sort_order[2]), p2.get_selOfdimension(sort_order[2]))
	            .append(p1.get_selOfdimension(sort_order[3]), p2.get_selOfdimension(sort_order[3]))
	            .append(p1.get_selOfdimension(sort_order[4]), p2.get_selOfdimension(sort_order[4])).toComparison();
			}
		}
		else if(p1.get_no_of_dimension()==6){
			if(p1.dim_values != null && p2.dim_values != null){
	        return new CompareToBuilder()
	        .append(p1.get_dimension(5), p2.get_dimension(5))
	        .append(p1.get_dimension(4), p2.get_dimension(4))
	        .append(p1.get_dimension(3), p2.get_dimension(3))
	        .append(p1.get_dimension(2), p2.get_dimension(2))
            .append(p1.get_dimension(1), p2.get_dimension(1))
            .append(p1.get_dimension(0), p2.get_dimension(0)).toComparison();
			}
			else if(p1.dim_sel_values != null && p2.dim_sel_values != null){
				return new CompareToBuilder()
				.append(p1.get_selOfdimension(sort_order[0]), p2.get_selOfdimension(sort_order[0]))
		        .append(p1.get_selOfdimension(sort_order[1]), p2.get_selOfdimension(sort_order[1]))
		        .append(p1.get_selOfdimension(sort_order[2]), p2.get_selOfdimension(sort_order[2]))
		        .append(p1.get_selOfdimension(sort_order[3]), p2.get_selOfdimension(sort_order[3]))
	            .append(p1.get_selOfdimension(sort_order[4]), p2.get_selOfdimension(sort_order[4]))
	            .append(p1.get_selOfdimension(sort_order[5]), p2.get_selOfdimension(sort_order[5])).toComparison();
				
			}
		}
		return -1;
    }

}	
