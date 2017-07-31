import iisc.dsl.picasso.common.ds.DataValues;
import iisc.dsl.picasso.server.ADiagramPacket;
import iisc.dsl.picasso.server.PicassoException;
import iisc.dsl.picasso.server.network.ServerMessageUtil;
import iisc.dsl.picasso.server.plan.Node;
import iisc.dsl.picasso.server.plan.Plan;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.postgresql.jdbc3.Jdbc3PoolingDataSource;


public class onlinePB {
	
	static int plans[];
	static double OptimalCost[];
	static int totalPlans;
	static int dimension;
	static int resolution;
	static int totalPoints;
	static DataValues[] data;
	static float selectivity[];
	static String apktPath;
	static String qtName ;
	//static Jdbc3PoolingDataSource source;
	static String query;
	static Connection conn = null;
	static int database_conn=1;
	static double h_cost;
	static String select_query;
	static String predicates;
	static ArrayList<Integer> learntDim = new ArrayList<Integer>();
	static float qrun_sel[];
	static float qrun_sel_rev[];
	static float beta;
	static int opt_call = 0;
	static int fpc_call = 0;
	static HashMap<Integer,ArrayList<location>> ContourPointsMap = new HashMap<Integer,ArrayList<location>>();
	static HashMap<Integer,ArrayList<location>> non_ContourPointsMap = new HashMap<Integer,ArrayList<location>>();
	static ArrayList<location> contour_points = new ArrayList<location>();
	static ArrayList<location> non_contour_points = new ArrayList<location>();
	static String XMLPath = null;
	
	//parameters to set
	static float minimum_selectivity = 0.000064f;
	//static float minimum_selectivity = 0.001f;
	static float alpha = 2;
	static int decimalPrecision = 5;
	static boolean DEBUG_LEVEL_2 = false;
	static boolean DEBUG_LEVEL_1 = false;
	static boolean visualisation_2D = true;
	static boolean enhancement = true; 
	static boolean memoization = true;
	 

	
	public static void main(String[] args) throws IOException, SQLException, PicassoException {
	
		long startTime = System.nanoTime();
		onlinePB obj = new onlinePB();  
		obj.loadPropertiesFile();
		String pktPath = apktPath + qtName + ".apkt" ;
		String pktPath_new = apktPath + qtName + "_new9.4.apkt";
		System.out.println("Query Template is "+qtName);
		minimum_selectivity = obj.roundToDouble(minimum_selectivity);
		
		ADiagramPacket gdp = obj.getGDP(new File(pktPath_new));
		dimension = gdp.getDimension();
		resolution = gdp.getMaxResolution();
		totalPoints = (int) Math.pow(resolution, dimension);
	
		
		obj.readpkt(gdp, true);
		obj.loadPropertiesFile();
		
		

		
		try{
			System.out.println("entered DB conn1");
			Class.forName("org.postgresql.Driver");

			//Settings
			//System.out.println("entered DB conn2");
			if(database_conn==0){
				conn = DriverManager
						.getConnection("jdbc:postgresql://localhost:5431/tpch-ai",
								"sa", "database");
			}
			else{
				System.out.println("entered DB tpcds");
				conn = DriverManager
						.getConnection("jdbc:postgresql://localhost:5432/tpcds-ai",
								"sa", "database");

			}
			System.out.println("Opened database successfully");
		}
		catch ( Exception e ) {
			System.out.println("entered DB err");
			System.err.println( e.getClass().getName()+": "+ e.getMessage() );
		}
		
		
		//generating the contours contourwise
		
		int i;double min_cost;

		
		if(visualisation_2D){
			obj.dimension = 2;
			float [] h_loc_arr = {1.0f,1.0f};
			location h_loc = new location(h_loc_arr,obj);
			h_cost = h_loc.get_cost();
			
			float [] l_loc_arr = {minimum_selectivity,minimum_selectivity};
			location l_loc = new location(l_loc_arr,obj);
			min_cost = l_loc.get_cost();
		}
		else{
			h_cost = obj.getOptimalCost(obj.totalPoints-1);
			qrun_sel = new float[dimension];
			for(int d=0;d<dimension;d++)
				qrun_sel[d] = minimum_selectivity;
			min_cost = obj.getFPCCost(qrun_sel, -1);

		}
		
		
		qrun_sel = new float[dimension];
		qrun_sel_rev = new float[dimension];
		for(int d=0;d<dimension;d++){
			qrun_sel[d] = -1.0f;
			qrun_sel_rev[d] = -1.0f;
		}
		
		XMLPath = new String(apktPath+"onlinePB.xml");
		beta = (float)Math.pow(alpha,(1.0 / dimension*1.0));
		double cost = min_cost;
		double ratio = h_cost/min_cost;
		assert (h_cost >= min_cost) : "maximum cost is less than the minimum cost";
		System.out.println("the ratio of C_max/c_min is "+ratio);

		//reset the values to -1 for the rest of the code 
		for(int d=0;d<dimension;d++)
			qrun_sel[d] = -1.0f;
		
		i = 1;
		
		ArrayList<Integer> order = new ArrayList<Integer>();
		order.clear(); 
		for(int d=0;d<obj.dimension;d++)
			order.add(d);
		
		
		while(cost < 2*h_cost)
		{
			if(cost>h_cost)
				cost = h_cost;
			System.out.println("---------------------------------------------------------------------------------------------\n");
			System.out.println("Contour "+i+" cost : "+cost);
			contour_points.clear();			
			non_contour_points.clear();
			if(i==5)
				System.out.println("Interesting");
			obj.generateCoveringContours(order,cost);

			if(visualisation_2D)
				writeContourPointstoFile(i);
			System.out.println("The running optimization calls are "+opt_call);
			System.out.println("The running FPC calls are "+fpc_call);
			int size_of_contour = contour_points.size();
			ContourPointsMap.put(i, new ArrayList<location>(contour_points)); //storing the contour points
			non_ContourPointsMap.put(i, new ArrayList<location>(non_contour_points)); //storing the contour points
			System.out.println("Size of contour"+size_of_contour );

			cost *=2;
			i++;

		}
		System.out.println("the number of optimization calls are "+opt_call);
		System.out.println("the number of FPC calls are "+fpc_call);
		
		

		if (conn != null) {
	        try { conn.close(); } catch (SQLException e) {}
	    }
		
		long endTime = System.nanoTime();
		System.out.println("Took "+(endTime - startTime)/1000000000 + " sec");

	}
	
	private void printSelectivityCost(float  sel_array[], double cost){
		
		System.out.println();
		for(int i=0;i<dimension;i++)
			System.out.print("\t"+sel_array[i]);
		
		System.out.println("\t has cost = "+cost);
		
	}
	
	
	private static void writeContourPointstoFile(int contour_no) {

		try {
	    
//	    String content = "This is the content to write into file";


         File file = new File("/home/srinivas/spillBound/data/others/covering_contours/"+qtName+contour_no+".txt"); 
           
	    // if file doesn't exists, then create it
	    if (!file.exists()) {
	        file.createNewFile();
	    }
	    

	    FileWriter writer = new FileWriter(file, false);

	    
	    PrintWriter pw = new PrintWriter(writer);
	    //Take iterator over the list
	    for(location p : contour_points) {		 
	   	 pw.print(p.get_dimension(0) + "\t"+p.get_dimension(1)+"\n");
	    }
	    for(location p : non_contour_points) {		 
		   	 pw.print(p.get_dimension(0) + "\t"+p.get_dimension(1)+"\n");
		 }
	    pw.close();
//	    pwaz.close();
	    writer.close();
	    
		} catch (IOException e) {
	    e.printStackTrace();
	}
		
	}

	
	public void generateCoveringContours(ArrayList<Integer> order,double cost) throws IOException, SQLException, PicassoException
	{
		String funName = "generateCoveringContours";
		//learntDim contains the dimensions already learnt (which is null initially)
		//learntDimIndices contains the exact point in the space for the learnt dimensions
		
		ArrayList<Integer> remainingDimList = new ArrayList<Integer>();
		for(int i=0;i<order.size();i++)
		{
			if(learntDim.contains(order.get(i))!=true)
			{
				remainingDimList.add(order.get(i));
				qrun_sel[order.get(i)] = -1.0f;
			}			
		}

		//following code was used to test the recursion
//		if(remainingDimList.size()==2 ){
//			for(int i=0;i<dimension;i++)
//				System.out.print("\t"+qrun_sel[i]);
//			System.out.println();
//			return;
//		}
		
		if(remainingDimList.size()==2)
		{ 
			int last_dim1=-1, last_dim2=-1;

			for(int i=0;i<dimension;i++)
			{
				if(learntDim.contains(i))
				{
					assert(qrun_sel[i] != -1.0) : "the selectivity values are assigned"; 
					//System.out.print(arr[i]+",");
				}
				else if(last_dim1 == -1)
				{
					assert(last_dim2 == -1) : last_dim2+" dimension should not be set at this point of time";
					assert(qrun_sel[i] == -1.0) : "the selectivity values are not yet assigned";
					last_dim1 = i;
				}
				else 
				{ 	 
					assert(last_dim1 != -1) : last_dim1+" dimension should be set at this point of time";
					assert(qrun_sel[i] == -1.0) : "the selectivity values are not yet assigned";
					last_dim2 = i;
				}
			}

			assert (last_dim1>=0 && last_dim1<dimension) :funName+ " : last dim1 index problem ";
			assert (last_dim2>=0 && last_dim2<dimension) :funName+ " : last dim2  index problem ";
			assert (remainingDimList.size() + learntDim.size() == dimension) : funName+" : learnt dimension data structures size not matching";

			//for the last two dimensions last_dim1 and last_dim2
			location loc1, loc2;
			
			
			qrun_sel[last_dim1] = qrun_sel[last_dim2] = minimum_selectivity;
			if((loc1 =locationAlreadyExist(qrun_sel)) == null){
				loc1 = new location(qrun_sel,this);
				non_contour_points.add(loc1);
				opt_call++;
			}
			
			assert(loc1 != null): "loc1 is null";
			double optimization_cost_low = loc1.get_cost();
			
			
			qrun_sel[last_dim1] = qrun_sel[last_dim2] = 1.0f;
			if((loc2 = locationAlreadyExist(qrun_sel)) == null){
				loc2 = new location(qrun_sel,this);
				non_contour_points.add(loc2);
				opt_call++;
			}
			
			assert(loc2 !=null): "loc2 is null";
			double optimization_cost_high = loc2.get_cost();
			non_contour_points.add(loc2);
			
			assert(optimization_cost_low <= optimization_cost_high): "violation of PCM";
			//check if the origin of this 2D slice is greater cost
			//OR
			//check if the terminal of this 2D slice has lesser cost
			
			if(optimization_cost_low > cost || optimization_cost_high < cost){
				//do not process the 2D plane
				
				return;
			}
			
			
			//now we need to explore the 2D surface
			else{
				
				//just the initialize the last two dimensions;
				qrun_sel[last_dim2] = 1.0f;
				qrun_sel[last_dim1] = minimum_selectivity;
				location loc;
				
				if((loc = locationAlreadyExist(qrun_sel)) == null){
					loc = new location(qrun_sel, this);
					non_contour_points.add(loc);
					opt_call++;
				}
				assert(loc != null): "location is null";
				double optimization_cost = loc.get_cost(); 
				
				
				location loc_rev;
				double optimization_cost_rev = Double.MIN_VALUE;
				
				if(enhancement){
				//just the initialize the last two dimensions;
				qrun_sel_rev[last_dim2] = minimum_selectivity;
				qrun_sel_rev[last_dim1] = 1.0f;
				
				if((loc = locationAlreadyExist(qrun_sel_rev)) == null){
					loc = new location(qrun_sel_rev, this);
					non_contour_points.add(loc);
					opt_call++;
				}
				assert(loc != null): "location is null";
				optimization_cost_rev = loc.get_cost(); 
				if(DEBUG_LEVEL_2)
					printSelectivityCost(qrun_sel_rev, optimization_cost);
				
				}
				
				boolean increase_along_dim1 = true;
		
				//we are reverse jumping along last_dim2 dimension
				
				double target_cost1 = Math.pow(beta, dimension-2)*cost;
				double target_cost2 = Math.pow(beta, dimension-1)*cost;
				double target_cost3 = Math.pow(beta, dimension)*cost;
				int orig_dim1 = last_dim1;
				int orig_dim2 = last_dim2;
				double opt_cost_copy = Double.MIN_VALUE;
				
				for(;;){
					
					float [] qrun_copy = new float [dimension];
					for(int i=0; i < dimension ; i++){
						if(increase_along_dim1){
							qrun_copy[i] = qrun_sel[i];
							last_dim1 = orig_dim1;
							last_dim2 = orig_dim2;
							opt_cost_copy = optimization_cost; 
						}
						else{ 
							qrun_copy[i] = qrun_sel_rev[i];
							last_dim1 = orig_dim2;
							last_dim2 = orig_dim1;
							opt_cost_copy = optimization_cost_rev;
						}
					}
					
					boolean came_inside_dim2_loop = false;
					while((qrun_copy[last_dim2] > minimum_selectivity) && (target_cost1 <= opt_cost_copy))
					{

						came_inside_dim2_loop = true;
						
						if (opt_cost_copy <=  target_cost2)
							break;
						
						float old_sel = qrun_copy[last_dim2];
						
						qrun_copy[last_dim2] = roundToDouble(old_sel/beta);

						
						if(qrun_copy[last_dim2] <= minimum_selectivity)
							qrun_copy[last_dim2] = minimum_selectivity;
						
						assert(qrun_copy[last_dim2] <= old_sel) : "selectivity not decreasing, even if it has to";
						if(DEBUG_LEVEL_2)
						System.out.println("Selectivity learnt "+old_sel/(qrun_copy[last_dim2]*beta));
						if((loc = locationAlreadyExist(qrun_copy)) == null){
							loc = new location(qrun_copy, this);
							//non_contour_points.add(loc);
							//counting the optimization calls
							opt_call++;
						}
						
						non_contour_points.add(loc);
						assert(loc != null) : "location is null";
						opt_cost_copy = loc.get_cost();
						
						if(qrun_copy[last_dim1] > minimum_selectivity)
							assert (opt_cost_copy >= cost): "covering locaiton has less than contour cost: i.e. covering_cost = "+opt_cost_copy+" and contour cost = "+cost;
						
						if(DEBUG_LEVEL_2)
							printSelectivityCost(qrun_copy, opt_cost_copy);
						
						
					}
					
					//if we hit the boundary then we are done
					if(qrun_copy[last_dim2] <= minimum_selectivity){
						qrun_copy[last_dim2] = minimum_selectivity;
						if(!ContourLocationAlreadyExist(loc.dim_values))
							contour_points.add(loc);
						break;
					}
					
					
					if(came_inside_dim2_loop)
						assert(opt_cost_copy <= target_cost2 && opt_cost_copy >=target_cost1) : "not in the valid cost range for dim2";
					


					//we are forward jumping along last_dim1 dimension
					//qrun_sel[last_dim1] = minimum_selectivity;
//					if((loc = locationAlreadyExist(qrun_sel)) == null){
//						loc = new location(qrun_sel, this);
//						contour_points.add(loc);
//						opt_call++;
//					}
//					assert(loc != null) : "location is null";
//					optimization_cost = loc.get_cost();
					
					boolean came_inside_dim1_loop = false;
					
					while((qrun_copy[last_dim1] < 1.0f) && (opt_cost_copy <= target_cost3))
					{

						came_inside_dim1_loop = true;
						
						if (opt_cost_copy >=  target_cost2){
							
							if(!ContourLocationAlreadyExist(loc.dim_values))
								contour_points.add(loc);//check this again!
							break;
						}
						
						
						// the argument for calculate jump size is the dimension along which we need to traverse				
						double forward_jump = calculateJumpSize(qrun_copy,last_dim1,opt_cost_copy);

						if(DEBUG_LEVEL_2)
							System.out.println("The forward jump size is "+(beta -1)*forward_jump+" from selectivity "+qrun_copy[last_dim1]);
						
						float old_sel = qrun_copy[last_dim1]; 
						qrun_copy[last_dim1] += (beta -1)*forward_jump; //check this again!
						
						assert((beta -1)*forward_jump > 0.0f) : "jump in the negative direction";
						
						if(qrun_copy[last_dim1]/(old_sel*beta) < 1.0f)
							qrun_copy[last_dim1] = old_sel*beta;
						
						//just rounding up the float value
						qrun_copy[last_dim1] = roundToDouble(qrun_copy[last_dim1]); 
						
						assert(qrun_copy[last_dim1] >= old_sel) : "selectivity not increasing, even if it has to";
						
						if(qrun_copy[last_dim1] >= 1.0f){

							if(!ContourLocationAlreadyExist(loc.dim_values))
								contour_points.add(loc);//check this again!
							qrun_copy[last_dim1] = 1.0f;
						}
						
						if(DEBUG_LEVEL_2)
						System.out.println("Selectivity learnt "+qrun_copy[last_dim1]/(old_sel*beta));
						
						if((loc = locationAlreadyExist(qrun_copy)) == null){
							loc = new location(qrun_copy, this);
							//non_contour_points.add(loc);
							//counting the optimization calls
							opt_call++;
						}
						
						non_contour_points.add(loc);
						assert(loc != null) : "location is null";
						opt_cost_copy = loc.get_cost();
						
						if(qrun_copy[last_dim2] < 1.0)
							assert (opt_cost_copy >= cost): "covering locaiton has less than contour cost: i.e. covering_cost = "+optimization_cost+" and contour cost = "+cost;
						
							if(DEBUG_LEVEL_2)
							printSelectivityCost(qrun_copy, opt_cost_copy);
						
					}
					
					//if we hit the boundary then we are done
					if(qrun_copy[last_dim1] >= 1.0f){
						if(!ContourLocationAlreadyExist(loc.dim_values))
							contour_points.add(loc);//check this again!
						break;
					}
					
					if(came_inside_dim1_loop)
						assert(opt_cost_copy <= target_cost3 && opt_cost_copy >= target_cost2) : "dim1 is not in the range"; 
					
					//
					if(opt_cost_copy > Math.pow(beta, dimension)*cost){
						System.out.println("How is this possible? and cost increase is "+opt_cost_copy/Math.pow(beta, dimension)*cost);
						opt_cost_copy = Math.pow(beta, dimension)*cost;
					}
					
					
					//copy back the copy-variable to the original data structure
					for(int i=0; i < dimension ; i++){
						if(increase_along_dim1){
							qrun_sel[i] = qrun_copy[i];
							optimization_cost = opt_cost_copy; 
						}
						else{ 
							qrun_sel_rev[i] = qrun_copy[i];
							optimization_cost_rev  = opt_cost_copy;
						}
					}
					
					if(enhancement){
						increase_along_dim1 = increase_along_dim1 ? false : true;
						
						if((qrun_sel[orig_dim1] >= qrun_sel_rev[orig_dim1]) || (qrun_sel[orig_dim2] <= qrun_sel_rev[orig_dim2]))
							break;
					}
				}
			}	

			return;
		}

		
		Integer curDim = remainingDimList.get(0); 

		for(qrun_sel[curDim] = minimum_selectivity; qrun_sel[curDim] <= 1.0; qrun_sel[curDim] *= beta)
		{	
//			if(qrun_sel[0] == minimum_selectivity)
//				continue;
			learntDim.add(curDim);
			generateCoveringContours(order, cost);
			learntDim.remove(learntDim.indexOf(curDim));
		}
	}
	
	
	private location locationAlreadyExist(float[] arr) {
		
		if(!memoization)
			return null;
		boolean flag = false;
		assert(ContourPointsMap.keySet().size() == non_ContourPointsMap.keySet().size()) : "sizes mismatch for the contour and non_contoumaps";
		for(int c = 1; c<=ContourPointsMap.keySet().size(); c++){
			for(location loc: ContourPointsMap.get(c)){
				flag = true;
				for(int i=0;i<dimension;i++){
					if(loc.get_dimension(i)!= arr[i]){
						flag = false;
						break;
					}
				}
				if(flag==true)
					return loc;
			}
			
			for(location loc: non_ContourPointsMap.get(c)){
				flag = true;
				for(int i=0;i<dimension;i++){
					if(loc.get_dimension(i)!= arr[i]){
						flag = false;
						break;
					}
				}
				if(flag==true)
					return loc;
			}
		}
		return null;
	}

	private boolean ContourLocationAlreadyExist(float[] arr) {
		//TODO: need to test this
		boolean flag = false;
		for(location loc: contour_points){
			flag = true;
			for(int i=0;i<dimension;i++){
				if(loc.get_dimension(i)!= arr[i]){
					flag = false;
					break;
				}
			}
			if(flag==true)
				return true;
		}
		
		return false;
}

	
	private double calculateJumpSize(float[] qrun_copy, int dim, double base_cost) throws SQLException {

		getFPCCost(qrun_copy, -3);
		float delta[] = {0.1f,0.2f,0.3f};
			float sum_slope = 0, divFactor =0;
			for(float del: delta){
				float sel[] = new float[dimension];

				for(int d=0; d<dimension;d++)
					sel[d] = qrun_copy[d];

				sel[dim] = sel[dim]*(1+del);
				double fpc_cost = 0;
				fpc_cost = getFPCCost(sel, -2);
				
				//to remove the effects of jumps but this satisfies the concavity assumption
				if(fpc_cost >=  (1+del)*base_cost)
					fpc_cost = (1+del)*base_cost;
				
				float sel_diff = del*(sel[dim]/(1+del));
				sum_slope += (fpc_cost - base_cost)/(sel_diff);
				if(DEBUG_LEVEL_2)
				{
					System.out.println("Dim = "+dim+" fpc = "+(fpc_cost)+" base cost = "+base_cost+" neighbour location = "+sel[dim]+" base location = "+sel[dim]/(1+del));
				}
				if(fpc_cost != base_cost)
					divFactor ++;
			}
			
			double avg_slope =1;
			if(divFactor >0 )
				 avg_slope = sum_slope/divFactor;
			 //TODO: really need to do something else for it!
			
		//now return the jump_size which is cost/slope or F/m;
			double jump_size = base_cost/avg_slope;
			
			if(DEBUG_LEVEL_1)
				System.out.println("jump size is "+jump_size);
			
			fpc_call ++;
			return jump_size;
		}


//	public void concavityValidation(boolean useFPC, boolean optimalPlan, int fpc_plan) throws SQLException{
//	String funName = "concavityValidation";
//	System.out.println(funName+" enterring");
//	
//	double delta[] = {0.1,0.2,0.3}, tolerance =200;
//	
//	double slope[][] = new double[dimension][totalPoints];
//	File f_slope = new File(apktPath+"slope.dat");
//	
//	
//	if(f_slope.exists() && READSLOPE){
//
//		try {
//			FileInputStream fis = new FileInputStream(f_slope);
//			ObjectInputStream iis = new ObjectInputStream(fis);
//			slope = (double[][]) iis.readObject();
//
//		} catch (Exception e) {
//
//		}
//
//	}
//	else{
//
//		for(int loc=0; loc < data.length; loc++){
//			System.out.println("loc = "+loc);
////			if(loc < 2135)
////				continue;
//			int plan = plans[loc];
//			int arr [] = getCoordinates(dimension, resolution, loc);
//			double base_cost =0;
//
//			if(optimalPlan)
//				base_cost = getOptimalCost(loc);
//			else if(fpc_plan > 0)
//				base_cost = fpc_cost_generic(arr, fpc_plan);
//			else
//				assert(true) : "this should not come here"; 
//
//
//			for(int dim =0; dim < dimension; dim++){
//
//				if(useFPC && arr[dim]<resolution-1){
//					double sum_slope = 0, divFactor =0;
//					for(double del: delta){
//						double sel[] = new double[dimension];
//
//						for(int d=0; d<dimension;d++)
//							sel[d] = selectivity[arr[d]];
//
//						sel[dim] = sel[dim]*(1+del);
//						double fpc_cost = 0;
//						if(optimalPlan)
//							fpc_cost = getFPCCost(sel, plan);
//						else 
//							fpc_cost = getFPCCost(sel, fpc_plan);
//						
//						sum_slope += (fpc_cost - base_cost)/(del*(sel[dim]/((1+del))));
//						if(slope[dim][loc] > (double)1 && DEBUG)
//						{
//							System.out.println("Dim = "+dim+" loc ="+loc+" fpc = "+(fpc_cost)+" base cost = "+base_cost+" neighbour location = "+sel[dim]+" base location = "+sel[dim]/(1+del));
//						}
//						if(fpc_cost != base_cost)
//							divFactor ++;
//					}
//					if(divFactor >0 )
//						slope[dim][loc] = sum_slope/divFactor;
//					
//				}				
//				else if(arr[dim]<resolution-1 ){
//					arr[dim]++;
//					if(loc ==9300 && dim==1 && DEBUG)
//						System.out.println("interesting");
//					double fpc_cost = 0;
//					if(optimalPlan)
//						fpc_cost = fpc_cost_generic(arr, plan);
//					else
//						fpc_cost = fpc_cost_generic(arr, fpc_plan);
//					
//					slope[dim][loc] = (fpc_cost - base_cost)/(selectivity[arr[dim]]- selectivity[arr[dim]-1]);
//					if(slope[dim][loc] > (double)1 && DEBUG)
//					{  if(optimalPlan)
//						System.out.println("loc ="+loc+" fpc = "+(fpc_cost_generic(arr, plan))+" base cost = "+base_cost+" neighbour location = "+selectivity[arr[dim]]+" base location = "+selectivity[arr[dim]-1]);
//					else
//						System.out.println("loc ="+loc+" fpc = "+(fpc_cost_generic(arr, fpc_plan))+" base cost = "+base_cost+" neighbour location = "+selectivity[arr[dim]]+" base location = "+selectivity[arr[dim]-1]);
//					}
//					
//					arr[dim]--;
//				}
//			}
//		}
//
//		if(!f_slope.exists() && WRITESLOPE)
//			writeSlopeObject(slope);
//
//
//	}
//	//checking violation
//	int violation5 =0, violation20 =0, violation50 =0, totalCount = 0;
//	for(int loc =0; loc < data.length; loc++){
//		int arr [] = getCoordinates(dimension, resolution, loc);
//		for(int dim =0; dim < dimension; dim++){
//			if(arr[dim]<resolution-1 && selectivity[arr[dim]] > (double)0.00001){
//				arr[dim]++;
//				int locN = getIndex(arr, resolution);
//				if(slope[dim][loc]>0) {
//					if ((slope[dim][loc]*1.5) < (slope[dim][locN])){
//						violation50++;
//						violation20++;
//						violation5++;
//						System.out.println("Dim = "+dim+" loc = "+loc+" slope = "+(slope[dim][loc]*1)+" locN ="+locN+" slope = "+slope[dim][locN]);
//					}
//					else if((slope[dim][loc]*1.2) < (slope[dim][locN])){
//						violation20++;
//						violation5++;
//					}
//					else if((slope[dim][loc]*1.05) < (slope[dim][locN])){
//						violation5++;
//					}
//					
//					
//				}
//				totalCount ++;
//				arr[dim]--;
//			}	
//		}
//	}
//	System.out.println("total count = "+totalCount+" with violation50 = "+violation50+" violation20 = "+violation20+" violation5 ="+violation5);
//	
//	checkJumpAssumption(slope);
////	viewslope(slope, 0);
////	viewslope(slope, 1);
//
//	System.exit(0);
//}
//

	
	public double getFPCCost(float selectivity[], int p_no) throws SQLException{
		//Get the path to p_no.xml
		
		
		String xml_path = apktPath+"planStructureXML/"+p_no+".xml";
		
		 String regx;
	     Pattern pattern;
	     Matcher matcher;
	     double execCost=-1;
	     
		//create the FPC query : 
	     //Statement stmt = null;
	     //conn = source.getConnection();
	     //System.out.println("Plan No = "+p_no);
			try{      	
				
				
				Statement stmt = conn.createStatement();
				String exp_query = new String("Selectivity ( "+predicates+ ") (");
				for(int i=0;i<dimension;i++){
					if(i !=dimension-1){
						exp_query = exp_query + (selectivity[i])+ ", ";
					}
					else{
						exp_query = exp_query + (selectivity[i]) + " )";
					}
				}
				//this is for selectivity injection plus fpc
				exp_query = exp_query + select_query;
				String xml_query = null;
				//this is for pure fpc
				//exp_query = select_query;
				if((p_no == -2 || p_no == -3) && XMLPath!=null){
					exp_query = "explain " + exp_query + " fpc "+XMLPath;
					xml_query = new String("explain (format xml) "+ select_query) ;
				}
				else if(p_no ==-1){
					exp_query = "explain " + exp_query ;
					xml_query = new String("explain (format xml) "+ select_query) ;
				}
				else {
					assert(p_no >=0): "plan number is less than zero";
					exp_query = "explain " + exp_query + " fpc "+xml_path;
				}
					
				//exp_query = new String("explain Selectivity ( customer_demographics.cd_demo_sk = catalog_sales.cs_bill_cdemo_sk and date_dim.d_date_sk = catalog_sales.cs_sold_date_sk  and item.i_item_sk = catalog_sales.cs_item_sk and promotion.p_promo_sk = catalog_sales.cs_promo_sk) (0.005, 0.99, 0.001, 0.00005 ) select i_item_id,  avg(cs_quantity) , avg(cs_list_price) ,  avg(cs_coupon_amt) ,  avg(cs_sales_price)  from catalog_sales, customer_demographics, date_dim, item, promotion where cs_sold_date_sk = d_date_sk and cs_item_sk = i_item_sk and   cs_bill_cdemo_sk = cd_demo_sk and   cs_promo_sk = p_promo_sk and cd_gender = 'F' and  cd_marital_status = 'U' and  cd_education_status = 'Unknown' and  (p_channel_email = 'N' or p_channel_event = 'N') and  d_year = 2002 and i_current_price <= 100 group by i_item_id order by i_item_id  fpc /home/lohitkrishnan/ssd256g/data/DSQT264DR20_E/planStructureXML/3.xml");
				//System.out.println(exp_query);
				//System.exit(1);
				//String exp_query = new String(query_opt_spill);
				
				stmt.execute("set work_mem = '100MB'");
				//NOTE,Settings: 4GB for DS and 1GB for H
				if(database_conn==0){
					stmt.execute("set effective_cache_size='1GB'");
				}
				else{
					stmt.execute("set effective_cache_size='4GB'");
				}

				//NOTE,Settings: need not set the page cost's
				stmt.execute("set  seq_page_cost = 1");
				stmt.execute("set  random_page_cost=4");
				stmt.execute("set cpu_operator_cost=0.0025");
				stmt.execute("set cpu_index_tuple_cost=0.005");
				stmt.execute("set cpu_tuple_cost=0.01");
				
				
				//XML Query

				if(p_no == -3 && xml_query!=null){
					ResultSet rs_xml = stmt.executeQuery(xml_query);
					StringBuilder xplan = new StringBuilder();
					while(rs_xml.next())  {
						xplan.append(rs_xml.getString(1)); 
					}
					rs_xml.close();
					if(xplan!=null){
						
						try{
							FileWriter fw_xml = new FileWriter(XMLPath, false); 
							fw_xml.write(xplan.toString());
							fw_xml.close();
						}
						catch(Exception e){
							e.printStackTrace();
							ServerMessageUtil.SPrintToConsole("Cannot get plan from postgres: "+e);
							throw new PicassoException("Error getting plan: "+e);
						}
					}
					return -1;
				}
				
				ResultSet rs = stmt.executeQuery(exp_query);
				rs.next();
				String str1 = rs.getString(1);
				
				

				//System.out.println(str1);
				
				//System.out.println(str1);
				//Do the pattern match to get the cost here
				regx = Pattern.quote("..") + "(.*?)" + Pattern.quote("rows=");
				
				pattern = Pattern.compile(regx);
				matcher = pattern.matcher(str1);
				while(matcher.find()){
					execCost = Float.parseFloat(matcher.group(1));
				//	System.out.println("execCost = "+execCost);
				}
				rs.close();
				stmt.close();	
				
			}
			catch(Exception e){
				e.printStackTrace();
				ServerMessageUtil.SPrintToConsole("Cannot get plan from postgres: "+e);
				
			}

			
	
			assert (execCost > 1 ): "execCost is less than 1";
			return execCost; 
		
	}

	public  float roundToDouble(float d) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPrecision, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }
	
	double getOptimalCost(int index)
	{
		return this.OptimalCost[index];
	}


	public void loadPropertiesFile() {
		/*
		 * Need dimension.
		 */

		Properties prop = new Properties();
		InputStream input = null;

		try {
			input = new FileInputStream("./src/Constants.properties");
			// load a properties file
			prop.load(input);

			// get the property value and print it out
			apktPath = prop.getProperty("apktPath");
			qtName = prop.getProperty("qtName");


			select_query = prop.getProperty("select_query");
			predicates= prop.getProperty("predicates");


			/*
			 * 0 for database_conn is used for tpch type queries
			 */
			database_conn = Integer.parseInt(prop.getProperty("database_conn"));
			input.close();

		} catch (IOException ex) {
			ex.printStackTrace();
		}
		
	}
	
	void readpkt(ADiagramPacket gdp, boolean allPlanCost) throws IOException
	{
		String funName="readpkt";
		//ADiagramPacket gdp = getGDP(new File(pktPath));
		totalPlans = gdp.getMaxPlanNumber();
		dimension = gdp.getDimension();
		resolution = gdp.getMaxResolution();
		data = gdp.getData();
		totalPoints = (int) Math.pow(resolution, dimension);
		//System.out.println("\nthe total plans are "+totalPlans+" with dimensions "+dimension+" and resolution "+resolution);

		assert (totalPoints==data.length) : "Data length and the resolution didn't match !";

		plans = new int [data.length];
		OptimalCost = new double [data.length]; 
		for (int i = 0;i < data.length;i++)
		{
			this.OptimalCost[i]= data[i].getCost();
			this.plans[i] = data[i].getPlanNumber();

		}

		//To get the number of points for each plan
		int  [] plan_count = new int[totalPlans];
		for(int p=0;p<data.length;p++){
			plan_count[plans[p]]++;
		}
		//printing the above
		for(int p=0;p<plan_count.length;p++){
			System.out.println("Plan "+p+" has "+plan_count[p]+" points");
		}

	}
	
	public int[] getCoordinates(int dimensions, int res, int location){
		int [] index = new int[dimensions];

		for(int i=0; i<dimensions; i++){
			index[i] = location % res;

			location /= res;
		}
		return index;
	}

	public int getIndex(int[] index,int res)
	{
		int tmp=0;

		for(int i=index.length-1; i>=0; i--){
			if(index[i] > 0)
				tmp=tmp * res + index[i];
			else
				tmp=tmp * res;
		}

		return tmp;
	}
	
	private ADiagramPacket getGDP(File file) {
		ADiagramPacket gdp = null;
		FileInputStream fis;
		try {
			fis = new FileInputStream(file);
			ObjectInputStream ois = new ObjectInputStream(fis);
			Object obj = ois.readObject();
			gdp = (ADiagramPacket) obj;

			ois.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return gdp;
	}


}

class location implements Serializable
{
	private static final long serialVersionUID = 223L;
	static int dimension;
	static int database_conn;
	static String predicates;
	static Connection conn;
	static String select_query;
	int fpc_plan=-1;
	double fpc_cost = Double.MAX_VALUE;
	int opt_plan_no;
	double opt_cost;
	static String apktPath;
	int idx;
	static int leafid=0;
	float [] dim_values;
	static Vector<Plan> plans_vector = new Vector<Plan>();
	static int decimalPrecision;

	
	
	location(location loc) {
	
		this.apktPath = loc.apktPath;
		this.dimension = loc.dimension;
		this.fpc_plan = loc.fpc_plan;
		this.fpc_cost = loc.fpc_cost;
		this.opt_plan_no = loc.opt_plan_no;
		this.opt_cost = loc.opt_cost;
		this.decimalPrecision = loc.decimalPrecision;
		this.dim_values = new float[dimension];
		//System.arraycopy(pg.dim_values, 0, dim_values, 0, dimension);
		for(int it=0;it<dimension;it++)
			this.dim_values[it] = loc.dim_values[it];

	}
	
	location(float arr[], onlinePB obj) throws  IOException, PicassoException{
		
		this.dimension = obj.dimension;
		this.conn = obj.conn;
		this.select_query = new String(obj.select_query);
		this.predicates  = new String(obj.predicates);
		this.database_conn = obj.database_conn;
		this.apktPath = obj.apktPath;
		this.decimalPrecision = obj.decimalPrecision;
		dim_values = new float[dimension];
		for(int i=0;i<dimension;i++){
			dim_values[i] = roundToDouble(arr[i]);
			//		System.out.print(arr[i]+",");
		}
		
		getPlan();
		
	}
	
	location(float arr[], OfflinePB obj) throws  IOException, PicassoException{
		
		this.dimension = obj.dimension;
		this.conn = obj.conn;
		this.select_query = new String(obj.select_query);
		this.predicates  = new String(obj.predicates);
		this.database_conn = obj.database_conn;
		this.apktPath = obj.apktPath;
		this.decimalPrecision = obj.decimalPrecision;
		dim_values = new float[dimension];
		for(int i=0;i<dimension;i++){
			dim_values[i] = (arr[i]);
			//		System.out.print(arr[i]+",");
		}
		
		getPlan();
	}
	
	/*
	 * get the selectivity of the dimension
	 */
	public float get_dimension(int d){
		return dim_values[d];
	}

	/*
	 * get the plan number for this point
	 */
	public int get_plan_no(){
		return opt_plan_no;

	}

	public double get_cost(){
		return opt_cost;
	}
	
	int getfpc_plan(){
		return this.fpc_plan;
	}
	
	void putfpc_plan(int p_no){
		this.fpc_plan=p_no;
	}

	double getfpc_cost(){
		return this.fpc_cost;
	}
	
	void putfpc_cost(double cost){
		this.fpc_cost=cost;
	}

	public int get_no_of_dimension(){
		return dimension;
	}

	public void printLocation(){

		for(int i=0;i<dimension;i++){
			System.out.print(dim_values[i]+",");
		}
		System.out.println("   having cost = "+opt_cost+" and plan "+opt_plan_no);
	}

	public  float roundToDouble(float d) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPrecision, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }
	
	public void getPlan() throws PicassoException, IOException {

		Vector textualPlan = new Vector();
		StringBuilder XML_Plan = new StringBuilder();
		Plan plan = new Plan();
		String xml_query = null;
		String cost_str = null;
		try{      	
			Statement stmt = conn.createStatement();
			String exp_query = new String("Selectivity ( "+predicates+ ") ( ");
			for(int i=0;i<dimension;i++){
				if(i !=dimension-1){
					exp_query = exp_query + dim_values[i]+ ", ";
				}
				else{
					exp_query = exp_query + dim_values[i] + " ) ";
				}
			}
			exp_query = exp_query + select_query;
			xml_query = "explain (format xml) "+ exp_query ;
			exp_query = "explain " + exp_query ;
			//String exp_query = new String(query_opt_spill);
			//System.out.println(exp_query);
			stmt.execute("set work_mem = '100MB'");
			//NOTE,Settings: 4GB for DS and 1GB for H
			if(database_conn==0){
				stmt.execute("set effective_cache_size='1GB'");
			}
			else{
				stmt.execute("set effective_cache_size='4GB'");
			}

			stmt.execute("set  seq_page_cost = 1");
			stmt.execute("set  random_page_cost=4");
			stmt.execute("set cpu_operator_cost=0.0025");
			stmt.execute("set cpu_index_tuple_cost=0.005");
			stmt.execute("set cpu_tuple_cost=0.01");
			
			ResultSet rs = stmt.executeQuery(exp_query);
			//System.out.println("coming here");
			while(rs.next())  {
				textualPlan.add(rs.getString(1)); 
			}
			
			cost_str = new String(textualPlan.get(0).toString());
			String regx;
		     Pattern pattern;
		     Matcher matcher;
		     double execCost=-1;
			regx = Pattern.quote("..") + "(.*?)" + Pattern.quote("rows=");
			assert(textualPlan.get(0).toString().contains("rows")): "this string does not rows! should be a mistake";
			pattern = Pattern.compile(regx);
			matcher = pattern.matcher(cost_str);
			while(matcher.find()){
				execCost = Float.parseFloat(matcher.group(1));
			//	System.out.println("execCost = "+execCost);
			}
			
			assert(execCost>0): "execution cost is less than or equal to zero";
			
			this.opt_cost = execCost;
			assert(textualPlan.size()>=0): "Empty plan from the optimizer call";
				
			
			ResultSet rs_xml = stmt.executeQuery(xml_query);
			
			while(rs_xml.next())  {
				XML_Plan.append(rs_xml.getString(1)); 
			}			
			
			rs.close();
			rs_xml.close();
			stmt.close();
			
			
		}
		catch(Exception e){
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("Cannot get plan from postgres: "+e);
			throw new PicassoException("Error getting plan: "+e);
		}

		String str = (String)textualPlan.remove(0);
		CreateNode(plan, str, 0, -1);
		//plan.isOptimal = true;
		FindChilds(plan, 0, 1, textualPlan, 2);
		//if(PicassoConstants.saveExtraPlans == false ||  PicassoConstants.topkquery == false)
		SwapSORTChilds(plan);
		 
		
		int planNumber = -1;
		//plan = database.getPlan(newQuery,query);
		String planDiffLevel = "SUB-OPERATOR";
			plan.computeHash(planDiffLevel);
			planNumber = plan.getIndexInVector(plans_vector);                  // see if the plan is new or already seen
		
			
		plan.setPlanNo(planNumber);
		if(planNumber == -1) {
			plans_vector.add(plan);
			planNumber=plans_vector.size() - 1;
			plan.setPlanNo(planNumber);
			//Dump xml plan
			String xml_path = apktPath+"onlinePB/"+"planStructureXML/"+plan.getPlanNo()+".xml";
			File dir = new File(apktPath+"onlinePB/"+"planStructureXML/");
			//check if the dir exists
			if(!dir.exists())
				dir.mkdirs();
				
			File xml_file =new File(xml_path);
			//Execute the xml query
			
			try{
				FileWriter fw_xml = new FileWriter(xml_file, false); 
				fw_xml.write(XML_Plan.toString());
				fw_xml.close();
			}
			catch(Exception e){
				e.printStackTrace();
				ServerMessageUtil.SPrintToConsole("Cannot get plan from postgres: "+e);
				throw new PicassoException("Error getting plan: "+e);
			}
 
		}
		
		//set the optimal plan and cost here
		this.opt_plan_no = planNumber;
		
	}
	
	int CreateNode(Plan plan, String str, int id, int parentid) {

		int id1; //by Srinivas
		if(id==1)
			leafid=-1;
		Node node = new Node();

		if(str.indexOf("->")>=0)
			str=str.substring(str.indexOf("->")+2).trim();
		String cost = str.substring(str.indexOf("..") + 2, str.indexOf("rows") - 1);
		String card = str.substring(str.indexOf("rows") + 5, str.indexOf("width")-1);
		//by Srinivas
		//-------------------------------------------------------------------
		//	        String actual_substring = str.substring(str.indexOf("actual") + 2);
		//	        String actual_cost = actual_substring.substring(str.indexOf("..") + 2, str.indexOf("rows") - 1);
		//	        String actual_card = actual_substring.substring(str.indexOf("rows") + 5, str.indexOf("width")-1);

		//-------------------------------------------------------------------

		if(str.indexOf(" on ") != -1 ||str.startsWith("Subquery Scan")) {
			node.setId(id++);
			node.setParentId(parentid);
			node.setCost(Double.parseDouble(cost));
			node.setCard(Double.parseDouble(card));
			//By Srinivas
			//------------------------------------------------------
			id1 = id-1;
			node.setCost(Double.parseDouble(cost));
			node.setCard(Double.parseDouble(card));
			//-------------------------------------------------------
			if(str.startsWith("Index Scan")){
				node.setName("Index Scan");
			}
			else if(str.startsWith("Subquery Scan")){
				node.setName("Subquery Scan");
			}
			else{
				node.setName(str.substring(0,str.indexOf(" on ")).trim());
			}
			plan.setNode(node,plan.getSize());
			node = new Node();
			node.setId(leafid--);
			node.setParentId(id-1);
			node.setCost(0.0);
			node.setCard(0.0);
			if(str.startsWith("Subquery Scan"))
				node.setName(str.trim().substring("Subquery Scan".length(),str.indexOf("(")).trim());
			else
				node.setName(str.substring(str.indexOf(" on ")+3,str.indexOf("(")).trim());
			plan.setNode(node,plan.getSize());
		} else {
			node.setId(id++);
			node.setParentId(parentid);
			node.setCost(Double.parseDouble(cost));
			node.setCard(Double.parseDouble(card));
			if(!str.substring(str.indexOf("("),str.indexOf(")")+1).trim().contains("cost"))
				node.setPredicate(str.substring(str.indexOf("("),str.indexOf(")")+1).trim());
			node.setName(str.substring(0,str.indexOf("(")).trim());
			//node.setName(str.substring(0,str.indexOf(")")).trim()); //changed by Srinivas
			//id1 = id -1;
			//node.setName(str.substring(0,str.indexOf("(")).trim()+id1); //changed by Srinivas
			plan.setNode(node,plan.getSize());
		}

		return id;
	}


	boolean optFlag;
	int FindChilds(Plan plan, int parentid, int id, Vector text, int childindex) {
		String str ="";
		int oldchildindex=-5;
		while(text.size()>0) {
			int stindex;            
			str = (String)text.remove(0);


			//  System.out.println("findling_childs");




			if (str.indexOf("Plan Type: STABLE")>=0)
				optFlag = false;
			if(str.trim().startsWith("InitPlan"))
				stindex=str.indexOf("InitPlan");
			else if(str.trim().startsWith("SubPlan"))
				stindex=str.indexOf("SubPlan");
			else
				stindex=str.indexOf("->");


			if(stindex==-1)
				continue;
			if(stindex==oldchildindex) {
				childindex=oldchildindex;
				oldchildindex=-5;
			}
			if(stindex < childindex) {
				text.add(0,str);
				break;
			}


			if(stindex>childindex) {
				if(str.trim().startsWith("InitPlan")||str.trim().startsWith("SubPlan")) {
					str = (String)text.remove(0);
					stindex=str.indexOf("->");
					oldchildindex=childindex;
					childindex=str.indexOf("->");
				}
				text.add(0,str);
				id = FindChilds(plan, id-1, id, text, stindex);
				continue;
			}

			if(str.trim().startsWith("InitPlan")||str.trim().startsWith("SubPlan")) {
				str = (String)text.remove(0);
				stindex=str.indexOf("->");
				oldchildindex=childindex;
				childindex=str.indexOf("->");
			}



			if(stindex==childindex)
				id = CreateNode(plan,str, id, parentid);
		}
		return id;
	}

	void SwapSORTChilds(Plan plan) {
		for(int i =0;i<plan.getSize();i++) {
			Node node = plan.getNode(i);
			if(node.getName().equals("Sort")) {
				int k=0;
				Node[] chnodes = new Node[2];
				for(int j=0;j<plan.getSize();j++) {
					if(plan.getNode(j).getParentId() == node.getId()) {
						if(k==0)chnodes[0]=plan.getNode(j);
						else chnodes[1]=plan.getNode(j);
						k++;
					}
				}
				if(k>=2) {
					k=chnodes[0].getId();
					chnodes[0].setId(chnodes[1].getId());
					chnodes[1].setId(k);

					for(int j=0;j<plan.getSize();j++) {
						if(plan.getNode(j).getParentId() == chnodes[0].getId())
							plan.getNode(j).setParentId(chnodes[1].getId());
						else if(plan.getNode(j).getParentId() == chnodes[1].getId())
							plan.getNode(j).setParentId(chnodes[0].getId());
					}
				}
			}
		}
	}

	
}
