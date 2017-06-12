import iisc.dsl.picasso.common.ds.DataValues;
import iisc.dsl.picasso.server.ADiagramPacket;
import iisc.dsl.picasso.server.network.ServerMessageUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;
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
	static double selectivity[];
	static String apktPath;
	static String qtName ;
	static Jdbc3PoolingDataSource source;
	static String query;
	static Connection conn = null;
	static int database_conn=1;
	static double h_cost;
	static String select_query;
	static String predicates;
	static double slope[][];
	public static void main(String[] args) throws IOException, SQLException {
	
		onlinePB obj = new onlinePB();  
		obj.loadPropertiesFile();
		String pktPath = apktPath + qtName + ".apkt" ;
		String pktPath_new = apktPath + qtName + "_new9.4.apkt";
		System.out.println("Query Template is "+qtName);


		ADiagramPacket gdp = obj.getGDP(new File(pktPath_new));
		dimension = gdp.getDimension();
		resolution = gdp.getMaxResolution();
		totalPoints = (int) Math.pow(resolution, dimension);
		slope = new double [dimension][totalPoints];
		
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
		
		
		if (conn != null) {
	        try { conn.close(); } catch (SQLException e) {}
	    }
		
		//generating the contours contourwise
		
		int i;
		h_cost = obj.getOptimalCost(obj.totalPoints-1);
		double min_cost = obj.getOptimalCost(0);
		double cost = obj.getOptimalCost(0);
		double ratio = h_cost/min_cost;
		assert (h_cost >= min_cost) : "maximum cost is less than the minimum cost";
		System.out.println("the ratio of C_max/c_min is "+ratio);

		i = 1;
		
		while(cost < 2*h_cost)
		{
			if(cost>h_cost)
				cost = h_cost;
			System.out.println("---------------------------------------------------------------------------------------------\n");
			System.out.println("Contour "+i+" cost : "+cost+"\n");
			
			obj.generateCoveringContours(order,cost);

		}
		

	}
	
	public void generateCoveringContours(ArrayList<Integer> order,double cost)throws IOException
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
			}			
		}

		if(remainingDimList.size()==2 )
		{ 
			int last_dim=-1;
			int [] arr = new int[dimension];

			for(int i=0;i<dimension;i++)
			{
				if(learntDim.contains(i))
				{
					arr[i] = learntDimIndices.get(i);
					//System.out.print(arr[i]+",");
				}
				else
					last_dim = i;
			}

			assert (learntDim.size() == learntDimIndices.size()) : funName+" : learnt dimension data structures size not matching";
			assert (last_dim>=0 && last_dim<dimension) :funName+ " : index problem ";
			assert (remainingDimList.size() + learntDim.size() == dimension) : funName+" : learnt dimension data structures size not matching";

			//for the last two dimensions last_dim1 and last_dim2
			//check if the origin of this 2D slice is greater cost
			if(){
			}
			
			//check if the terminal of this 2D slice has lesser cost
			else if(){
			}
			
			//now we need to explore the 2D contour
			else{
				calculateSlope();
			}	
			//Search the whole line and return all the points which fall into the contour
			for(int i=0;i< resolution;i++)
			{
				arr[last_dim] = i;
				double cur_val = cost_generic(arr);
				double targetval = cost;

				if(cur_val == targetval)
				{
					if(!pointAlreadyExist(arr)){ //No need to check if the point already exist
						//if(true){								// its okay to have redundancy
						point_generic p;

						/*
						 * The following If condition checks whether any earlier point in all_contour_points 
						 * had the same plan. If so, no need to open the .../predicateOrder/plan.txt again
						 */

						if(planVisited(getPlanNumber_generic(arr))!=null)
							p = new point_generic(arr,getPlanNumber_generic(arr),cur_val, remainingDim,planVisited(getPlanNumber_generic(arr)).getPredicateOrder());
						else
							p = new point_generic(arr,getPlanNumber_generic(arr),cur_val, remainingDim);
						all_contour_points.add(p);
						isContourPoint[getIndex(arr, resolution)] = true;

					}
				}
				else if(i!=0){
					arr[last_dim]--; //in order to look at the cost at lower value on the last index
					double cur_val_l = cost_generic(arr);
					arr[last_dim]++; //restore the index back 
					if( cur_val > targetval  && cur_val_l < targetval ) //NOTE : changed the inequality to strict inequality
					{
						if(!pointAlreadyExist(arr)){ //No need to check if the point already exist
							//if(true){								// its okay to have redundancy
							point_generic p; 
							if(planVisited(getPlanNumber_generic(arr))!=null)
								p = new point_generic(arr,getPlanNumber_generic(arr),cur_val, remainingDim,planVisited(getPlanNumber_generic(arr)).getPredicateOrder());
							else
								p = new point_generic(arr,getPlanNumber_generic(arr), cur_val,remainingDim);
							all_contour_points.add(p);
							isContourPoint[getIndex(arr, resolution)] = true;
						}
					}

				}
			}


			return;
		}

		Integer curDim = remainingDimList.get(0); //index of 0 or size-1 does not matter
		Integer cur_index = resolution -1;

		while(cur_index >= 0)
		{
			learntDim.add(curDim);
			learntDimIndices.put(curDim,cur_index);
			getContourPoints(order,cost);
			learntDim.remove(learntDim.indexOf(curDim));
			learntDimIndices.remove(curDim);
			cur_index = cur_index - 1;
		}


		
	}

//	public void concavityValidation(boolean useFPC, boolean optimalPlan, int fpc_plan) throws SQLException{
//		String funName = "concavityValidation";
//		System.out.println(funName+" enterring");
//		
//		double delta[] = {0.1,0.2,0.3}, tolerance =200;
//		
//		double slope[][] = new double[dimension][totalPoints];
//		File f_slope = new File(apktPath+"slope.dat");
//		
//		
//		if(f_slope.exists() && READSLOPE){
//
//			try {
//				FileInputStream fis = new FileInputStream(f_slope);
//				ObjectInputStream iis = new ObjectInputStream(fis);
//				slope = (double[][]) iis.readObject();
//
//			} catch (Exception e) {
//
//			}
//
//		}
//		else{
//
//			for(int loc=0; loc < data.length; loc++){
//				System.out.println("loc = "+loc);
////				if(loc < 2135)
////					continue;
//				int plan = plans[loc];
//				int arr [] = getCoordinates(dimension, resolution, loc);
//				double base_cost =0;
//
//				if(optimalPlan)
//					base_cost = getOptimalCost(loc);
//				else if(fpc_plan > 0)
//					base_cost = fpc_cost_generic(arr, fpc_plan);
//				else
//					assert(true) : "this should not come here"; 
//
//
//				for(int dim =0; dim < dimension; dim++){
//
//					if(useFPC && arr[dim]<resolution-1){
//						double sum_slope = 0, divFactor =0;
//						for(double del: delta){
//							double sel[] = new double[dimension];
//
//							for(int d=0; d<dimension;d++)
//								sel[d] = selectivity[arr[d]];
//
//							sel[dim] = sel[dim]*(1+del);
//							double fpc_cost = 0;
//							if(optimalPlan)
//								fpc_cost = getFPCCost(sel, plan);
//							else 
//								fpc_cost = getFPCCost(sel, fpc_plan);
//							
//							sum_slope += (fpc_cost - base_cost)/(del*(sel[dim]/((1+del))));
//							if(slope[dim][loc] > (double)1 && DEBUG)
//							{
//								System.out.println("Dim = "+dim+" loc ="+loc+" fpc = "+(fpc_cost)+" base cost = "+base_cost+" neighbour location = "+sel[dim]+" base location = "+sel[dim]/(1+del));
//							}
//							if(fpc_cost != base_cost)
//								divFactor ++;
//						}
//						if(divFactor >0 )
//							slope[dim][loc] = sum_slope/divFactor;
//						
//					}				
//					else if(arr[dim]<resolution-1 ){
//						arr[dim]++;
//						if(loc ==9300 && dim==1 && DEBUG)
//							System.out.println("interesting");
//						double fpc_cost = 0;
//						if(optimalPlan)
//							fpc_cost = fpc_cost_generic(arr, plan);
//						else
//							fpc_cost = fpc_cost_generic(arr, fpc_plan);
//						
//						slope[dim][loc] = (fpc_cost - base_cost)/(selectivity[arr[dim]]- selectivity[arr[dim]-1]);
//						if(slope[dim][loc] > (double)1 && DEBUG)
//						{  if(optimalPlan)
//							System.out.println("loc ="+loc+" fpc = "+(fpc_cost_generic(arr, plan))+" base cost = "+base_cost+" neighbour location = "+selectivity[arr[dim]]+" base location = "+selectivity[arr[dim]-1]);
//						else
//							System.out.println("loc ="+loc+" fpc = "+(fpc_cost_generic(arr, fpc_plan))+" base cost = "+base_cost+" neighbour location = "+selectivity[arr[dim]]+" base location = "+selectivity[arr[dim]-1]);
//						}
//						
//						arr[dim]--;
//					}
//				}
//			}
//
//			if(!f_slope.exists() && WRITESLOPE)
//				writeSlopeObject(slope);
//
//
//		}
//		//checking violation
//		int violation5 =0, violation20 =0, violation50 =0, totalCount = 0;
//		for(int loc =0; loc < data.length; loc++){
//			int arr [] = getCoordinates(dimension, resolution, loc);
//			for(int dim =0; dim < dimension; dim++){
//				if(arr[dim]<resolution-1 && selectivity[arr[dim]] > (double)0.00001){
//					arr[dim]++;
//					int locN = getIndex(arr, resolution);
//					if(slope[dim][loc]>0) {
//						if ((slope[dim][loc]*1.5) < (slope[dim][locN])){
//							violation50++;
//							violation20++;
//							violation5++;
//							System.out.println("Dim = "+dim+" loc = "+loc+" slope = "+(slope[dim][loc]*1)+" locN ="+locN+" slope = "+slope[dim][locN]);
//						}
//						else if((slope[dim][loc]*1.2) < (slope[dim][locN])){
//							violation20++;
//							violation5++;
//						}
//						else if((slope[dim][loc]*1.05) < (slope[dim][locN])){
//							violation5++;
//						}
//						
//						
//					}
//					totalCount ++;
//					arr[dim]--;
//				}	
//			}
//		}
//		System.out.println("total count = "+totalCount+" with violation50 = "+violation50+" violation20 = "+violation20+" violation5 ="+violation5);
//		
//		checkJumpAssumption(slope);
////		viewslope(slope, 0);
////		viewslope(slope, 1);
//
//		System.exit(0);
//	}
//	

	
	private void calculateSlope() {
		// TODO Auto-generated method stub
		if(useFPC && arr[dim]<resolution-1){
			double sum_slope = 0, divFactor =0;
			for(double del: delta){
				double sel[] = new double[dimension];

				for(int d=0; d<dimension;d++)
					sel[d] = selectivity[arr[d]];

				sel[dim] = sel[dim]*(1+del);
				double fpc_cost = 0;
				if(optimalPlan)
					fpc_cost = getFPCCost(sel, plan);
				else 
					fpc_cost = getFPCCost(sel, fpc_plan);
				
				sum_slope += (fpc_cost - base_cost)/(del*(sel[dim]/((1+del))));
				if(slope[dim][loc] > (double)1 && DEBUG)
				{
					System.out.println("Dim = "+dim+" loc ="+loc+" fpc = "+(fpc_cost)+" base cost = "+base_cost+" neighbour location = "+sel[dim]+" base location = "+sel[dim]/(1+del));
				}
				if(fpc_cost != base_cost)
					divFactor ++;
			}
			if(divFactor >0 )
				slope[dim][loc] = sum_slope/divFactor;
			
		}
	}

	public double getFPCCost(double selectivity[], int p_no) throws SQLException{
		//Get the path to p_no.xml
		
		
		String xml_path = apktPath+"planStructureXML/"+p_no+".xml";
		String pcst_path = apktPath+"pcstFiles/"+p_no+".pcst";
		 String regx;
	     Pattern pattern;
	     Matcher matcher;
	     double execCost=-1;
	     
		//create the FPC query : 
	     Statement stmt;
	     //conn = source.getConnection();
	     //System.out.println("Plan No = "+p_no);
			try{      	
				
				
				 stmt = conn.createStatement();
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
				
				//this is for pure fpc
				//exp_query = select_query;
				
				exp_query = "explain " + exp_query + " fpc "+xml_path;
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
					execCost = Double.parseDouble(matcher.group(1));
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
