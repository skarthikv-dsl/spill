//package net.codejava.collections;
 
import java.util.Comparator;
 
import org.apache.commons.lang3.builder.CompareToBuilder;

public class pointComparator implements Comparator<point_generic> {
	
	
	public int compare(point_generic p1, point_generic p2) {
		
		if(p1.get_no_of_dimension()==2){
			
			if(p1.dim_values != null && p2.dim_values != null){
	        return new CompareToBuilder()
            .append(p1.get_dimension(1), p2.get_dimension(1))
            .append(p1.get_dimension(0), p2.get_dimension(0)).toComparison();
			}
			else if(p1.dim_sel_values != null && p2.dim_sel_values != null){
				return new CompareToBuilder()
            .append(p1.get_selOfdimension(1), p2.get_selOfdimension(1))
            .append(p1.get_selOfdimension(0), p2.get_selOfdimension(0)).toComparison();
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
		        .append(p1.get_selOfdimension(2), p2.get_selOfdimension(2))
	            .append(p1.get_selOfdimension(1), p2.get_selOfdimension(1))
	            .append(p1.get_selOfdimension(0), p2.get_selOfdimension(0)).toComparison();
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
		        .append(p1.get_selOfdimension(3), p2.get_selOfdimension(3))
		        .append(p1.get_selOfdimension(2), p2.get_selOfdimension(2))
	            .append(p1.get_selOfdimension(1), p2.get_selOfdimension(1))
	            .append(p1.get_selOfdimension(0), p2.get_selOfdimension(0)).toComparison();
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
		        .append(p1.get_selOfdimension(4), p2.get_selOfdimension(4))
		        .append(p1.get_selOfdimension(3), p2.get_selOfdimension(3))
		        .append(p1.get_selOfdimension(2), p2.get_selOfdimension(2))
	            .append(p1.get_selOfdimension(1), p2.get_selOfdimension(1))
	            .append(p1.get_selOfdimension(0), p2.get_selOfdimension(0)).toComparison();
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
				.append(p1.get_selOfdimension(5), p2.get_selOfdimension(5))
		        .append(p1.get_selOfdimension(4), p2.get_selOfdimension(4))
		        .append(p1.get_selOfdimension(3), p2.get_selOfdimension(3))
		        .append(p1.get_selOfdimension(2), p2.get_selOfdimension(2))
	            .append(p1.get_selOfdimension(1), p2.get_selOfdimension(1))
	            .append(p1.get_selOfdimension(0), p2.get_selOfdimension(0)).toComparison();
				
			}
		}
		return -1;
    }

}	
