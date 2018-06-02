import iisc.dsl.picasso.common.ds.DataValues;
import iisc.dsl.picasso.server.ADiagramPacket;
import iisc.dsl.picasso.server.PicassoException;
import iisc.dsl.picasso.server.network.ServerMessageUtil;
import iisc.dsl.picasso.server.plan.Node;
import iisc.dsl.picasso.server.plan.Plan;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

import javax.print.attribute.ResolutionSyntax;

import oracle.core.lmx.CoreException;

import org.postgresql.jdbc3.Jdbc3PoolingDataSource;


public class dimensionReduction {
	
	static double AllPlanCosts[][];
	static int nPlans;
	static int plans[];
	static double OptimalCost[];
	static int totalPlans;
	static int dimension;
	static int resolution;
	static DataValues[] data;
	static int totalPoints;
	static double selectivity[];
	static String apktPath;
	static String qtName ;
	static Jdbc3PoolingDataSource source;
	static String varyingJoins;
	static double JS_multiplier [];
	static String query;
	static String query_opt_spill;
	static String cardinalityPath;
	static int sel_distribution; 
	static boolean FROM_CLAUSE;
	static Connection conn = null;
	static int database_conn=1;
	static double h_cost;
	static int leafid=0;
	static String select_query;
	static String predicates;
	static double slope[][];
	static boolean DEBUG = true;
	static boolean READSLOPE = false;
	static boolean WRITESLOPE = false;
	static boolean WRITESLOPE_TO_FILE = false;
	static boolean CheckAllPlan = true;
	public static void main(String[] args) throws IOException, SQLException, PicassoException {
	
		dimensionReduction obj = new dimensionReduction();  
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
		obj.loadSelectivity();
		
		try{
			System.out.println("entered DB conn1");
			Class.forName("org.postgresql.Driver");

			//Settings
			//System.out.println("entered DB conn2");
			if(database_conn==0){
				conn = DriverManager
						.getConnection("jdbc:postgresql://localhost:5432/tpch9.4",
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
		
		obj.varying_execution_times();
		
//		obj.computeSelectivityUpperandLowerBound();
//		obj.writeCostToFile();
//		System.exit(0);
		
		int coor[] = new int[dimension];
		int old_dims = dimension;
		String old_qtname = new String(qtName);
		if(old_dims >1)
			coor[1] = 40;
//		obj.CostSlopeRatio(0,coor);
		
		for(int i=0; i < old_dims; i++) {
			for(int j= i+1; j < old_dims; j++) {
				System.out.println("for varying dim = "+i+" extreme dim = "+j);
				obj.ExtremeSlopeComputation(i,  coor, j, 1000);
			}
		}
		
		System.exit(0);
		
		if(old_qtname.contains("BaseOperators")){
			for(int itr = 0; itr < 3; itr++){
				obj.CostSlopeRatio_synthetic(0,coor,1000, Operators.values()[itr]);
			}
		}
		
		else if(old_qtname.contains("SortOperator")){

			obj.CostSlopeRatio_synthetic(0,coor,1000, Operators.values()[3]);
		}
		
		else if(old_qtname.contains("JoinOperators")){
			for(int itr = 4; itr < Operators.values().length; itr++){
				obj.CostSlopeRatio_synthetic(0,coor,1000, Operators.values()[itr]);
			}
		}
		
		else{
			for(int dim=0; dim<old_dims; dim++){
				//			if(dim!=2)
				//				continue;
				obj.CostSlopeRatio_synthetic(dim,coor,1000, Operators.None);
			}
		}
		
		System.exit(0);
		
		if(!CheckAllPlan)
			obj.concavityValidation(true,true,-1);
		else{
			for(int plan=0; plan < totalPlans; plan++){
				System.out.println("checking for plan:"+plan);
				obj.concavityValidation(true,false, plan);
			}
		}
		
		if (conn != null) {
	        try { conn.close(); } catch (SQLException e) {}
	    }
		
		
		//obj.maxPenalty();

	}
	
	

	private void computeSelectivityUpperandLowerBound() {
		
		
		Statement stmt;
		String tableName = new String("time_dim");
		String colName = new String("t_minute");
			try{      	
				
				
				 stmt = conn.createStatement();
				 String query = new String("select  histogram_bounds from pg_stats where tablename = '"+tableName+"' and attname = '"+colName+"'");
				 ResultSet rs = stmt.executeQuery(query);
					rs.next();
					String str = rs.getString(1);
					System.out.println(str);
					String[] stringArray = str.split("\\s*,\\s*");
					int num_of_buckets = stringArray.length; 
					float sel_upper_bound = (float) (1f/(num_of_buckets*1f));
					System.out.println("Upperbound is "+sel_upper_bound);
					rs.close();
					stmt.close();	
					
			}
			catch(Exception e){	
				e.printStackTrace();
				ServerMessageUtil.SPrintToConsole("Cannot get plan from postgres: "+e);
				
			}
			System.exit(0);
			
	}



	public void concavityValidation(boolean useFPC, boolean optimalPlan, int fpc_plan) throws SQLException{
		String funName = "concavityValidation";
		System.out.println(funName+" enterring");
		
		double delta[] = {0.1,0.2,0.3}, tolerance =200;
		
		double slope[][] = new double[dimension][totalPoints];
		File f_slope = new File(apktPath+"slope.dat");
		
		
		if(f_slope.exists() && READSLOPE){

			try {
				FileInputStream fis = new FileInputStream(f_slope);
				ObjectInputStream iis = new ObjectInputStream(fis);
				slope = (double[][]) iis.readObject();

			} catch (Exception e) {

			}

		}
		else{

			for(int loc=0; loc < data.length; loc++){
				System.out.println("loc = "+loc);
//				if(loc < 2135)
//					continue;
				int plan = plans[loc];
				int arr [] = getCoordinates(dimension, resolution, loc);
				double base_cost =0;

				if(optimalPlan)
					base_cost = getOptimalCost(loc);
				else if(fpc_plan > 0)
					base_cost = fpc_cost_generic(arr, fpc_plan);
				else
					assert(true) : "this should not come here"; 


				for(int dim =0; dim < dimension; dim++){

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
					else if(arr[dim]<resolution-1 ){
						arr[dim]++;
//						if(loc ==9300 && dim==1 && DEBUG)
//							System.out.println("interesting");
						double fpc_cost = 0;
						if(optimalPlan)
							fpc_cost = fpc_cost_generic(arr, plan);
						else
							fpc_cost = fpc_cost_generic(arr, fpc_plan);
						
						slope[dim][loc] = (fpc_cost - base_cost)/(selectivity[arr[dim]]- selectivity[arr[dim]-1]);
						if(slope[dim][loc] > (double)1 && DEBUG)
						{  if(optimalPlan)
							System.out.println("loc ="+loc+" fpc = "+(fpc_cost_generic(arr, plan))+" base cost = "+base_cost+" neighbour location = "+selectivity[arr[dim]]+" base location = "+selectivity[arr[dim]-1]);
						else
							System.out.println("loc ="+loc+" fpc = "+(fpc_cost_generic(arr, fpc_plan))+" base cost = "+base_cost+" neighbour location = "+selectivity[arr[dim]]+" base location = "+selectivity[arr[dim]-1]);
						}
						
						arr[dim]--;
					}
				}
			}

			if(!f_slope.exists() && WRITESLOPE)
				writeSlopeObject(slope);


		}
		//checking violation
		int violation5 =0, violation20 =0, violation50 =0, totalCount = 0;
		for(int loc =0; loc < data.length; loc++){
			int arr [] = getCoordinates(dimension, resolution, loc);
			for(int dim =0; dim < dimension; dim++){
				if(arr[dim]<resolution-1 && selectivity[arr[dim]] > (double)0.00001){
					arr[dim]++;
					int locN = getIndex(arr, resolution);
					if(slope[dim][loc]>0) {
						if ((slope[dim][loc]*1.5) < (slope[dim][locN])){
							violation50++;
							violation20++;
							violation5++;
							System.out.println("Dim = "+dim+" loc = "+loc+" slope = "+(slope[dim][loc]*1)+" locN ="+locN+" slope = "+slope[dim][locN]);
						}
						else if((slope[dim][loc]*1.2) < (slope[dim][locN])){
							violation20++;
							violation5++;
						}
						else if((slope[dim][loc]*1.05) < (slope[dim][locN])){
							violation5++;
						}
						
						
					}
					totalCount ++;
					arr[dim]--;
				}	
			}
		}
		
		assert (violation50 <= violation20) : " violation50 is less than violation20";
		assert (violation20 <= violation5) : " violation20 is less than violation5";
		System.out.println("total count = "+totalCount+" with violation50 = "+violation50+" violation20 = "+violation20+" violation5 ="+violation5);
		
		checkJumpAssumption(slope);
//		viewslope(slope, 0);
//		viewslope(slope, 1);

		System.exit(0);
	}
	
	public int nextLocAlongDim(int loc, int dim){
		int loc_coord[]  = getCoordinates(dimension, resolution, loc);
		for(int l = loc+1; l<totalPoints; l++){
			int l_coord[] = getCoordinates(dimension, resolution, l);
			if(l_coord[dim] == (loc_coord[dim]+1)){
				//asserting that all the other dimension values remaining the same;
				for(int d = 0; d<dimension; d++){
					if(d!=dim)
					assert(loc_coord[d] == l_coord[d]) : "not really the neighbouring location";
				}
				return l;
			}
		}
		//there is not neighbouring location for the input location 'loc'
		assert (loc_coord[dim]==(resolution-1)): "some issue with the neighbouring location"; 
		return -1;
	}
	
	public void CostSlopeRatio(int varyDim,  int otherDims[]) throws SQLException{
		
		double delta[] = {0.1,0.2,0.3};
		int arr[] =  new int[dimension];
		for(int d =0; d < dimension; d++){
			if(d != varyDim)
				arr[d] = otherDims[d];
		}
		
		//store the plans in this 1D array
		HashMap<Integer,Integer> al = new HashMap<Integer,Integer>();
		int value =0;
		for(int i =0;i<resolution;i++){
			arr[varyDim] = i;
			int loc = getIndex(arr, resolution);
			if(!al.containsKey(plans[loc])){
				al.put(plans[loc],value);
				value++;
			}
		}
		assert(al.size()>=1) : "no plans in this 1D slice";
		assert(al.size() < resolution): "all points in this 1D slice has different plans (suspicious)";
		double costSlope[][] = new double[al.size()+1][resolution];
		double cost[][] = new double[al.size()+1][resolution];
		
		//first find the slope of all the plans at all the locations
		for(int plan: al.keySet()){
			
			for(int i=0;i<resolution;i++){
				arr[varyDim] = i;
				costSlope[al.get(plan)][i] = getParticularSlope(varyDim, true, false, delta, plan, getIndex(arr, resolution));
				cost[al.get(plan)][i] = getFPCCost(convertIndextoSelectivity(arr), plan);
				assert(costSlope[al.get(plan)][i] != (double) -1): "the slope cannot be -1 at "+i;
			}	 
		}
		
		//slope for the optimal plan
		for(int i=0;i<resolution;i++){
			arr[varyDim] = i;
			costSlope[al.size()][i] = getParticularSlope(varyDim, true, true, delta, -1, getIndex(arr, resolution));
			cost[al.size()][i] = getFPCCost(convertIndextoSelectivity(arr), -1);			
			assert(costSlope[al.size()][i] != (double) -1): "the slope of optimal plan cannot be -1 at "+i;
		}	
		
		for(int plan: al.keySet()){
//			System.out.print("the slopes are ");
			for(int i=0;i<resolution;i++){
//			if(DEBUG)
//				System.out.print(" "+costSlope[al.get(plan)][i]);
			arr[varyDim] = i;
			int loc = getIndex(arr, resolution);
			//costSlope[al.get(plan)][i] = fpc_cost_generic(arr, plan)/costSlope[al.get(plan)][i]; 	
			}	 
		}
		
		if(DEBUG){
			for(int i=0;i<resolution;i++){
				arr[varyDim] = i;
				System.out.print(al.get(plans[getIndex(arr, resolution)])+"\t"+selectivity[i]);
				for(int plan=0; plan<=al.keySet().size(); plan++){
					System.out.print("\t"+cost[plan][i]+"\t"+costSlope[plan][i]);
				}	
				System.out.print("\n");
			}
		}
		
		
		System.out.println("Printing the Cost/Slope ratio here");
		
		
		if(DEBUG){
			for(int i=0;i<resolution;i++){
				arr[varyDim] = i;
				//System.out.print(al.get(plans[getIndex(arr, resolution)])+"\t"+selectivity[i]);
				for(int plan=0; plan<al.keySet().size(); plan++){
					System.out.print("\t"+costSlope[plan][i]);
				}	
				System.out.print("\n");
			}
		}
		
		for(int i=0;i<al.size();i++){
			for(int j=i+1;j<al.size();j++){
				boolean swap = true;
				for(int k=0;k<resolution;k++){
					if(costSlope[i][k] > costSlope[j][k]){
						swap = false;
						//System.out.println("Problem ");
					}
				}
				
			}
		}
		
	}
	
	
	public void varying_execution_times() {
		
		double[] sel_array = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
		String query =  "select * from customer, customer_address where c_current_addr_sk = ca_address_sk and c_birth_month in (10,9,7,5,1,3) and ca_state in ('CA','CA','WV','OH','UT','ID','VA')";


		Statement stmt;
		try{      	


			stmt = conn.createStatement();
			stmt.execute("set work_mem = '10MB'");
			stmt.execute("set effective_cache_size='1GB'");
			
			String exp_query = "explain analyze selectivity (c_birth_month in (10,9,7,5,1,3)) ("+sel_array[0]+" ) "+query;
			ResultSet rs = stmt.executeQuery(exp_query);

		}
		catch(Exception e){
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("Cannot get plan from postgres: "+e);
			
		}
		
	}
	
public void CostSlopeRatio_synthetic(int varyDim,  int otherDims[], int res, Operators op) throws SQLException, PicassoException, IOException{
		
		int old_res = resolution;
		resolution = res;
		boolean POSP_Known = false;
		double delta[] = {0.1,0.2,0.3};
		int arr[] =  new int[dimension];
		for(int d =0; d < dimension; d++){
			if(d != varyDim)
				arr[d] = otherDims[d];
		}
		PlanGen pg = new PlanGen();
		HashMap<Integer,Integer> al = new HashMap<Integer,Integer>();
		//first get the list of plans for this
		if(!POSP_Known){
			al = generatePOSPList(varyDim, arr, res, pg, op);
			assert(al.size() < resolution): "all points in this 1D slice has different plans (suspicious)";
			
		}
		else{
			//store the plans in this 1D array from the earlier packet

			int value =0;
			for(int i =0;i<old_res;i++){
				arr[varyDim] = i;
				int loc = getIndex(arr, old_res);
				if(!al.containsKey(plans[loc])){
					al.put(plans[loc],value);
					value++;
				}
			}
			
			assert(al.size() < old_res): "all points in this 1D slice has different plans (suspicious)";
		}
		assert(al.size()>=1) : "no plans in this 1D slice";
		
		
		selectivity = new double[resolution];
		loadSelectivity();

		//double costSlope[][] = new double[al.size()+1][resolution];
		double cost[][] = new double[al.size()+1][resolution];
		String old_qtname = new String(qtName);
		if(pg.apktPath!= null && pg.qtName!= null && pg.predicates!= null && pg.select_query!=null){ 
			apktPath = pg.apktPath;
			qtName = pg.qtName;
			select_query = pg.select_query;
			predicates = pg.predicates;
			dimension = pg.dimension;
			resolution = pg.resolution ;
		}
		//first find the slope of all the plans at all the locations
		for(int plan: al.keySet()){
			
			for(int i=0;i<resolution;i++){
				
				if(!POSP_Known){
					assert(dimension==1) : "dimensions should be 1 in this case"; 
					double sel [] = new double[2];
					sel[0] = selectivity[i]; sel[1] = -1.0;
					cost[al.get(plan)][i] = getFPCCost(sel, plan);
				}
				else{
				arr[varyDim] = i;
				//costSlope[al.get(plan)][i] = getParticularSlope(varyDim, true, false, delta, plan, getIndex(arr, resolution));
				cost[al.get(plan)][i] = getFPCCost(convertIndextoSelectivity(arr), plan);
				//assert(costSlope[al.get(plan)][i] != (double) -1): "the slope cannot be -1 at "+i;
				}
			}	 
		}
		
		//TODO: write a assert to check if the optimal plan cost is equal to min of POSP cost
		
		//slope for the optimal plan
		if(op.equals(Operators.None)){
			for(int i=0;i<resolution;i++){

				if(!POSP_Known){
					assert(dimension==1) : "dimensions should be 1 in this case"; 
					double sel [] = new double[2];
					sel[0] = selectivity[i]; sel[1] = -1.0;
					cost[al.size()][i] = getFPCCost(sel, -1);
				}
				else{
					arr[varyDim] = i;
					cost[al.size()][i] = getFPCCost(convertIndextoSelectivity(arr), -1);		
				}
				//costSlope[al.size()][i] = getParticularSlope(varyDim, true, true, delta, -1, getIndex(arr, resolution));

				//assert(costSlope[al.size()][i] != (double) -1): "the slope of optimal plan cannot be -1 at "+i;
			}	
		}

		//copy contents to the new directory for future use

		File directory = new File(pg.apktPath+"planStructureXML");
		if(directory.exists() && directory.isDirectory() && !op.equals(Operators.None)){
			assert(directory.renameTo(new File(pg.apktPath+"planStructureXML_"+op.toString()))): "renaming not sucessfull"; 
		}	
		 
		directory = new File(pg.apktPath+"planStructure_new");
		if(directory.exists() && directory.isDirectory() && !op.equals(Operators.None)){
			assert(directory.renameTo(new File(pg.apktPath+"planStructure_new_"+op.toString()))): "renaming not sucessfull";
		}
		
//		for(int plan: al.keySet()){
//			System.out.print("the slopes are ");
//			for(int i=0;i<resolution;i++){
//			if(DEBUG)
//				System.out.print(" "+costSlope[al.get(plan)][i]);
//			arr[varyDim] = i;
//			int loc = getIndex(arr, resolution);
			//costSlope[al.get(plan)][i] = fpc_cost_generic(arr, plan)/costSlope[al.get(plan)][i]; 	
//			}	 
//		}
		
		
		double[] prevratio = new double[al.size()+1];
		double[] currratio = new double[al.size()+1];
		double[] intercept = new double[al.size()+1];
		double[] violatioCnt = new double[al.size()+1];
		double[] violatioCnt10 = new double[al.size()+1];
 		PrintWriter writer = null; 
 		PrintWriter writer_slope = null;
 		if(op.equals(Operators.None) && WRITESLOPE_TO_FILE){
 			writer = new PrintWriter(apktPath+"plots/cost_"+old_qtname+"-dim"+varyDim+".txt", "UTF-8");
 			writer_slope = new PrintWriter(apktPath+"plots/slope_"+old_qtname+"-dim"+varyDim+".txt", "UTF-8");
 		}
 		else if(WRITESLOPE_TO_FILE){
 			writer = new PrintWriter(apktPath+"plots/cost_"+old_qtname+"_"+op.toString()+"-dim"+varyDim+".txt", "UTF-8");
 			writer_slope = new PrintWriter(apktPath+"plots/slope_"+old_qtname+"_"+op.toString()+"-dim"+varyDim+".txt", "UTF-8");
 		}
 			
		if(DEBUG){
			for(int i=0;i<resolution;i++){
				
				//arr[varyDim] = i;
				if(WRITESLOPE_TO_FILE) {
					if(i==0){
						writer.print("selectivity");
						writer_slope.print("selectivity");
						for(int plan=0; plan<al.keySet().size(); plan++){
							writer.print("\t"+"plan"+plan);
							writer_slope.print("\t"+"plan"+plan);
						}
						if(op.equals(Operators.None)){
							writer.println("\t"+"Optimal Plan");
							writer_slope.println("\t"+"Optimal Plan");
						}
					}

					writer.print(selectivity[i]);

					if(i>0)
						writer_slope.print(selectivity[i]);
				}
				for(int plan=0; plan<=al.keySet().size(); plan++){
					
					if(plan == al.keySet().size() && !op.equals(Operators.None))
						continue;
					
					if(WRITESLOPE_TO_FILE)
						writer.print("\t"+cost[plan][i]);
					//if(i==0){
					//Here ratio means slope
					if(i<=1){
						prevratio[plan] = 1;
						//writer_slope.print("\t"+"1");
					}
					else{
						intercept[plan] =  (selectivity[1]*cost[plan][0] - selectivity[0]*cost[plan][1])/(selectivity[1] - selectivity[0]);
						currratio[plan] = ((cost[plan][i] - cost[plan][i-1]) < 10) ? prevratio[plan] : (cost[plan][i] - cost[plan][i-1])/(selectivity[i] - selectivity[i-1]);
						//currratio[plan] = ((cost[plan][i] - cost[plan][i-2]) < 10) ? prevratio[plan] : (cost[plan][i] - cost[plan][i-2])/(2*(selectivity[i] - selectivity[i-1]));
						if(WRITESLOPE_TO_FILE)
							writer_slope.print("\t"+((currratio[plan])<=0 ? 1 : currratio[plan]));
					}
				
					if((prevratio[plan]*1.01 < currratio[plan]) &&(prevratio[plan] >0) && (i >1)){
						
						System.out.println("i = "+i+": cost[i-2] = "+cost[plan][i-2]+" cost[i-1] = "+cost[plan][i-1]+"cost[i] = "+cost[plan][i]+" prevratio = "+prevratio[plan]+" ratio = "+currratio[plan]);
						
						violatioCnt[plan] ++;
					}
					
					if((prevratio[plan]*1.1 < currratio[plan]) &&(prevratio[plan] >0) && (i >1)){
						
						System.out.println("i = "+i+": cost[i-2] = "+cost[plan][i-2]+" cost[i-1] = "+cost[plan][i-1]+"cost[i] = "+cost[plan][i]+" prevratio = "+prevratio[plan]+" ratio = "+currratio[plan]);
						
						violatioCnt10[plan] ++;
					}
					prevratio[plan] = currratio[plan];
					
				}	
				if(WRITESLOPE_TO_FILE) {
					writer.print("\n");
					writer_slope.println();
				}
			}
			
			
		}
		if(WRITESLOPE_TO_FILE) {
			writer.close();
			writer_slope.close();
		}
		
		if(DEBUG){
			
				System.out.println("\n the violations of 1 % are ");
				for(int plan=0; plan<al.keySet().size(); plan++){
					if(plan == al.keySet().size() && !op.equals(Operators.None))
						continue;
					System.out.print("\t"+violatioCnt[plan]);
				}	
				
				System.out.println();
				
				System.out.println("\n the violations of 10 % are ");
				for(int plan=0; plan<al.keySet().size(); plan++){
					if(plan == al.keySet().size() && !op.equals(Operators.None))
						continue;
					System.out.print("\t"+violatioCnt10[plan]);
				}
				System.out.println();
				
				System.out.println("\n the intercepts at origin are ");
				for(int plan=0; plan<al.keySet().size(); plan++){
					if(plan == al.keySet().size())
						continue;
					System.out.print("\t"+intercept[plan]);
				}
				System.out.println();

		}
		
		
		
		for(int i=0;i<al.size();i++){
			for(int j=i+1;j<al.size();j++){
				boolean swap = true;
				for(int k=0;k<resolution;k++){
//					if(costSlope[i][k] > costSlope[j][k]){
						swap = false;
						//System.out.println("Problem ");
//					}
				}
				
			}
		}
		resolution = old_res;
		loadSelectivity();
		loadPropertiesFile();
	}
	


public void ExtremeSlopeComputation(int varyDim,  int otherDims[], int extremeDim, int res) throws SQLException, PicassoException, IOException{
	
	int old_res = resolution;
	resolution = res;
	boolean POSP_Known = false;
	double delta[] = {0.1,0.2,0.3};
	int arr_min[] =  new int[dimension];
	int arr_max[] =  new int[dimension];
	
	for(int d = 0; d < dimension; d++){
		if(d == extremeDim) {
			arr_min[d] = 0;
			arr_max[d] = resolution -1;
		}
		else if (d != varyDim){
			arr_min[d] = otherDims[d];
			arr_max[d] = otherDims[d];
		}
	}
	
	PlanGen pg = new PlanGen();
	//update the PlanGen constant.properties
	pg.apktPath = this.apktPath;
	pg.qtName = this.qtName;
	pg.select_query = this.select_query;
	pg.predicates = new String(this.predicates.split("and", 0)[varyDim].trim()+" and "+this.predicates.split("and", 0)[extremeDim].trim());
	pg.dimension = 2;
	pg.resolution = res;
	pg.FROM_CLAUSE = this.FROM_CLAUSE;
	pg.sel_distribution = this.sel_distribution;
	pg.database_conn = this.database_conn;
	pg.conn = this.conn;
	pg.loadSelectivity();
	
	
	
	selectivity = new double[resolution];
	loadSelectivity();

	//double costSlope[][] = new double[al.size()+1][resolution];
	double cost[][] = new double[2][resolution];
	String old_qtname = new String(qtName);
	if(pg.apktPath!= null && pg.qtName!= null && pg.predicates!= null && pg.select_query!=null){ 
		apktPath = pg.apktPath;
		qtName = pg.qtName;
		select_query = pg.select_query;
		predicates = pg.predicates;
		dimension = pg.dimension;
		resolution = pg.resolution ;
	}
	
	//TODO: write a assert to check if the optimal plan cost is equal to min of POSP cost
	
	//slope for the optimal plan
	
		for(int i=0;i<resolution;i++){

			if(!POSP_Known){
				assert(dimension==2) : "dimensions should be 2 in this case"; 
				double sel_min [] = new double[2];
				sel_min[0] = selectivity[i]; sel_min[1] = 0.0001f;
				cost[0][i] = getFPCCost(sel_min, -1);
				
				double sel_max [] = new double[2];
				sel_max[0] = selectivity[i]; sel_max[1] = 1.0f;
				cost[1][i] = getFPCCost(sel_max, -1);
			}
			else{
				//TODO: need to complete if we want to use the packets

//				arr_min[varyDim] = i;
//				cost[al.size()][i] = getFPCCost(convertIndextoSelectivity(arr), -1);		
			}
			//costSlope[al.size()][i] = getParticularSlope(varyDim, true, true, delta, -1, getIndex(arr, resolution));

			//assert(costSlope[al.size()][i] != (double) -1): "the slope of optimal plan cannot be -1 at "+i;
		}	
	
	
	
	double[] prevratio = new double[2];
	double[] currratio = new double[2];
	double[] intercept = new double[2];
	double violatioCnt = 0;
	double violatioCnt5 = 0;
	double violatioCnt10 = 0;
			
	System.out.println("The extremem slopes are ");
	if(DEBUG){
		for(int i=0;i<resolution;i++){
			

			for(int j=0; j<=1; j++){
							
				if(i<=1){
					prevratio[j] = 1;
					//writer_slope.print("\t"+"1");
				}
				else{
					intercept[j] =  (selectivity[1]*cost[j][0] - selectivity[0]*cost[j][1])/(selectivity[1] - selectivity[0]);
					//currratio[j] = ((cost[j][i] - cost[j][i-1]) < 10) ? prevratio[j] : (cost[j][i] - cost[j][i-1])/(selectivity[i] - selectivity[i-1]);
					currratio[j] = (cost[j][i] - cost[j][i-1])/(selectivity[i] - selectivity[i-1]);
					//currratio[plan] = ((cost[plan][i] - cost[plan][i-2]) < 10) ? prevratio[plan] : (cost[plan][i] - cost[plan][i-2])/(2*(selectivity[i] - selectivity[i-1]));
				}
				prevratio[j] = currratio[j];
				
			}
			
			System.out.println("i = "+i+" min_slope = "+currratio[0]+" min_cost(i) = "+cost[0][i]+ " max_slope = "+currratio[1]+" max_cost(i) = "+cost[1][i]);
			
			if(currratio[0] < currratio[1])
				violatioCnt++;

			if(currratio[0]*1.05 < currratio[1])
				violatioCnt5++;

			
			if(currratio[0]*1.1 < currratio[1])
				violatioCnt10++;

		}
	}
	
	if(DEBUG){
		
			System.out.print("\n the violations  % are ");
			System.out.print("\t"+violatioCnt);
			
			System.out.println();
			
			System.out.print("\n the violations of 5 % are ");
			System.out.print("\t"+violatioCnt5);
			System.out.println();
			
			System.out.print("\n the violations of 10 % are ");
			System.out.print("\t"+violatioCnt10);
			System.out.println();
	}
	
	
	resolution = old_res;
	loadSelectivity();
	loadPropertiesFile();
}


public HashMap<Integer,Integer> generatePOSPList(int varyDim, int[] arr, int res, 	PlanGen pg, Operators op) throws PicassoException, IOException, SQLException {
	
	
		
		//update the PlanGen constant.properties
		pg.apktPath = "/home/dsladmin/Srinivas/data/Test/";
		pg.qtName = "Test";
		pg.select_query = this.select_query;
		pg.predicates = this.predicates.split("and", 0)[varyDim].trim();
		pg.dimension = 1;
		pg.resolution = res;
		pg.FROM_CLAUSE = this.FROM_CLAUSE;
		pg.sel_distribution = this.sel_distribution;
		pg.database_conn = this.database_conn;
		pg.conn = this.conn;
		pg.loadSelectivity();
		
		
		//clear all the data in the planStucture_new and planStructureXML directory
		File directory = new File(pg.apktPath+"planStructureXML");
		
		if(directory.exists()){
			File[] files = directory.listFiles();
			if(files.length > 0){
				for (File file : files)
					if (!file.delete())
						System.out.println("Failed to delete "+file);
			}
		}
		else{
			directory.mkdir();
		}
		 
		directory = new File(pg.apktPath+"planStructure_new");
		if(directory.exists()){			
			File[] files = directory.listFiles();
			if(files.length>0){
				for (File file : files)
					if (!file.delete())
						System.out.println("Failed to delete "+file);
			}
		}
		else{
			directory.mkdir();
		}

		if(pg.plans_vector != null)
			pg.plans_vector.clear();
		
		for(int i =0;i<res;i++){
			if(op.equals(Operators.None))
				pg.getNativePlan(i);
			else
				pg.getNativePlanOperators(i,op);
		}
		int numplans =new File(pg.apktPath+"planStructureXML").listFiles().length ;
		
		assert(numplans >=1) : "no plans in the POSP";
		HashMap<Integer,Integer> al = new HashMap<Integer,Integer>();
		for(int i=0;i<numplans;i++){
			al.put(i, i);
		}
		
	
		return al;
	
}
	
	



	// Return the index near to the selecitivity=mid;
	public int findNearestPoint(double mid)
	{
		int i;
		int return_index = 0;
		double diff;
		if(mid >= selectivity[resolution-1])
			return resolution-1;
		for(i=0;i<resolution;i++)
		{
			diff = mid - selectivity[i];
			if(diff <= 0)
			{
				return_index = i;
				break;
			}
		}

		/*
		 * sanity check
		 */


		//System.out.println("return_index="+return_index);
		return return_index;
	}
	
	private double[] convertIndextoSelectivity(int[] point_Index) {

		String funName = "convertIndextoSelectivity";

		double [] point_selec = new double[point_Index.length];
		assert (point_Index.length == dimension): funName+" ERROR: point index length not matching with dimension"; 
		for(int d=0; d<dimension;d++)
			point_selec[d] = selectivity[point_Index[d]];
		return point_selec;
	}

	private int[] convertSelectivitytoIndex (double[] point_sel) {

		String funName = "convertSelectivitytoIndex";

		int [] point_index = new int[point_sel.length];
		assert (point_sel.length == dimension): funName+" ERROR: point index length not matching with dimension"; 
		for(int d=0; d<dimension;d++)
			point_index[d] = findNearestPoint(point_sel[d]);
		return point_index;
	}
	
	public double getParticularSlope(int dim, boolean useFPC, boolean optimalPlan, double delta[], int fpc_plan, int loc) throws SQLException{
		int arr[] = getCoordinates(dimension, resolution, loc);
		int plan = plans[loc];
		double base_cost =0;

		if(optimalPlan)
			base_cost = getOptimalCost(loc);
		else if(fpc_plan > 0)
			base_cost = fpc_cost_generic(arr, fpc_plan);
		else
			assert(true) : "this should not come here"; 
			
//		if(fpc_plan == 9 )
//			System.out.println("interesting");
		if(useFPC && arr[dim]<resolution-1){
			double sum_slope = 0, divFactor =0, max_slope=Double.MIN_VALUE;
			for(double del: delta){
				double sel[] = new double[dimension];

				for(int d=0; d<dimension;d++)
					sel[d] = selectivity[arr[d]];

				sel[dim] = sel[dim]*(1+del);
				assert(sel[dim] >= selectivity[arr[dim]]) : "the selectivity is not increased for the slope";
				
				double fpc_cost = 0;
				if(optimalPlan)
					fpc_cost = getFPCCost(sel, -1);
				else 
					fpc_cost = getFPCCost(sel, fpc_plan);
				
				assert(fpc_cost >= base_cost) : "violating the PCM assumption";
				double cost_diff = (fpc_cost - base_cost);
				double curr_slope = (cost_diff)/(del*(sel[dim]/((1+del))));
				
				// if the cost_diff is too less then set curr_slope to zero
				if(fpc_cost <= base_cost*(1+(del/1000)))
					curr_slope = 0;
				
				sum_slope += curr_slope;
				
				//for testing 
				if(curr_slope >= max_slope)
					max_slope = curr_slope;
				
				assert((del*(sel[dim]/(1+del)) <= 1.01*(sel[dim] - selectivity[arr[dim]]) ) || (del*(sel[dim]/(1+del))*1.01 <= (sel[dim] - selectivity[arr[dim]]) ) ) : "denominator is not consistent for the slope";


				if(curr_slope > 0)
					divFactor ++;
			}

			
			if(divFactor>0){
				assert(sum_slope/divFactor <= max_slope) : "something wrong in slope calculation";	
				return  (sum_slope/divFactor);
			}
			else{
				assert (sum_slope ==0)	: "sum of slopes should be  zero";
				return 0;
			}
			
		}				
		else if(arr[dim]<resolution-1 ){
			arr[dim]++;
			double fpc_cost = 0;
			if(optimalPlan)
				fpc_cost = fpc_cost_generic(arr, plan);
			else
				fpc_cost = fpc_cost_generic(arr, fpc_plan);
			
			double slope = (fpc_cost - base_cost)/(selectivity[arr[dim]]- selectivity[arr[dim]-1]);
			 
			assert(selectivity[arr[dim]]>= selectivity[arr[dim]-1]) : "the selectivity is not increased for the slope";
			assert(fpc_cost >= base_cost) : "violating the PCM assumption";
			
			if(slope > (double)1 && DEBUG)
			{  if(optimalPlan)
				System.out.println("loc ="+loc+" fpc = "+(fpc_cost_generic(arr, plan))+" base cost = "+base_cost+" neighbour location = "+selectivity[arr[dim]]+" base location = "+selectivity[arr[dim]-1]);
			else
				System.out.println("loc ="+loc+" fpc = "+(fpc_cost_generic(arr, fpc_plan))+" base cost = "+base_cost+" neighbour location = "+selectivity[arr[dim]]+" base location = "+selectivity[arr[dim]-1]);
			}

			return slope;
		}
		else if(arr[dim]==resolution-1){
			return 0;
		}
		else return -1;
	}
	public void checkJumpAssumption(double[][] slope2) {
		System.out.println("Entering checkJumpAssumption");
		double violation =0, totalcount =0;
		for(int dim=0; dim<dimension; dim++){
			for(int loc =0; loc < data.length; loc++){
				int arr[] = getCoordinates(dimension, resolution, loc);
				if(arr[dim]==0 && slope2[dim][loc] > 0){
					double ratio = getOptimalCost(loc)/slope2[dim][loc];
					
					//System.out.println("Jump ratio = "+ratio+" and minimum selectivity is "+selectivity[arr[dim]]);
					if(ratio < selectivity[arr[dim]]){
						System.out.println("violation of jump assumption");
						violation++;
					}
					totalcount++;
				}
			}
		}
		System.out.println("checkJumpAssumption: "+"total count = "+totalcount+" with violation = "+violation);
	}
	private void writeSlopeObject(double[][] slope2) {
		
		try {
	        FileOutputStream fos = new FileOutputStream(apktPath+"slope.dat");
	        ObjectOutputStream oos = new ObjectOutputStream(fos);
	        oos.writeObject(slope2);


	    } catch (Exception e) {

	    }

		
	}

	public void viewslope(double[][] slope2, int dim) {
		
		int curr_arr [], prev_arr[], base_arr[];
		prev_arr = getCoordinates(dimension, resolution, 0);
		//base_arr = getCoordinates(dimension, resolution, 0);
		for(int loc=0;loc<totalPoints;loc++){
				curr_arr = getCoordinates(dimension, resolution, loc);
				if(curr_arr[dim]==0 || (curr_arr[dim]==prev_arr[dim]+1)){
					if(curr_arr[dim]==0){
						System.out.print("\n dim"+dim+" : ");
						prev_arr = getCoordinates(dimension, resolution, loc);
					}
					System.out.print("\t"+slope2[dim][loc]);
					System.arraycopy(curr_arr,0 , prev_arr, 0, dimension);
				}	
			}
	}

	
//	public void viewslope2d(double[][] slope2, int dim) {
//		
//		for(int i=0;i<resolution;i++){
//			System.out.print("dim1 = "+i+" : ");
//			for(int j=0;j<resolution;j++){
//					int arr [] = new int[dimension];
//					arr[0] = i; arr[1] = j; 
//					int idx = getIndex(arr, resolution);
//					System.out.print("\t"+slope2[dim][idx]);
//			}
//			System.out.println();
//		}
//		
//	}

	double fpc_cost_generic(int arr[], int plan)
	{

		int index = getIndex(arr,resolution);


		return AllPlanCosts[plan][index];
	}

	
	public double getFPCCost(double selectivity[], int p_no) throws SQLException{
		//Get the path to p_no.xml
		
		
		String xml_path = apktPath+"planStructureXML/"+p_no+".xml";
		
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
				if(p_no !=-1)
					exp_query = "explain " + exp_query + " fpc "+xml_path;
				else 
					exp_query = "explain " + exp_query ;
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

	public void maxPenalty(){
		//lets see the penalty increase for removing a dimension
		
		for(int dim=0;dim < dimension; dim++){
			double max_ratio = Double.MIN_VALUE;
			for(int loc =0;loc < totalPoints;loc++){
				int [] arr = getCoordinates(dimension, resolution, loc);
				if(arr[dim] == resolution-1){
					double high_cost = getOptimalCost(loc);
					arr[dim] = 0;
					double low_cost = getOptimalCost(getIndex(arr, resolution));
					assert(low_cost<=(high_cost*1.01)) : "low cost is higher than the higher cost";
					double ratio = high_cost/low_cost;
					if(ratio > max_ratio)
						max_ratio = ratio;
					
				}
			}
			System.out.println("The max ratio for dimension "+dim+" is "+max_ratio);
			 
		}
		
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


			int from_clause_int_val = Integer.parseInt(prop.getProperty("FROM_CLAUSE"));

			if(from_clause_int_val == 1)
				FROM_CLAUSE = true;
			else
				FROM_CLAUSE = false;

			/*
			 * 0 for sel_distribution is used for tpch type queries
			 */
			sel_distribution = Integer.parseInt(prop.getProperty("sel_distribution"));

			/*
			 * 0 for database_conn is used for tpch type queries
			 */
			database_conn = Integer.parseInt(prop.getProperty("database_conn"));
			input.close();

		} catch (IOException ex) {
			ex.printStackTrace();
		}
		
	}
	void loadSelectivity()
	{
		String funName = "loadSelectivity: ";
		System.out.println(funName+" Resolution = "+resolution);
		double sel;
		this.selectivity = new double [resolution];

		if(resolution == 10){
			if(sel_distribution == 0){

				//This is for TPCH queries 
				selectivity[0] = 0.0005;	selectivity[1] = 0.005;selectivity[2] = 0.01;	selectivity[3] = 0.02;
				selectivity[4] = 0.05;		selectivity[5] = 0.10;	selectivity[6] = 0.20;	selectivity[7] = 0.40;
				selectivity[8] = 0.60;		selectivity[9] = 0.95;                                   // oct - 2012
			}
			else if( sel_distribution ==1){

				//This is for TPCDS queries
				selectivity[0] = 0.00005;	selectivity[1] = 0.0005;selectivity[2] = 0.005;	selectivity[3] = 0.02;
				selectivity[4] = 0.05;		selectivity[5] = 0.10;	selectivity[6] = 0.15;	selectivity[7] = 0.25;
				selectivity[8] = 0.50;		selectivity[9] = 0.99;                                // dec - 2012
			}
			else
				assert (false) :funName+ "ERROR: should not come here";

		}		
		if(resolution == 20){

			if(sel_distribution == 0){

				selectivity[0] = 0.0005;   selectivity[1] = 0.0008;		selectivity[2] = 0.001;	selectivity[3] = 0.002;
				selectivity[4] = 0.004;   selectivity[5] = 0.006;		selectivity[6] = 0.008;	selectivity[7] = 0.01;
				selectivity[8] = 0.03;	selectivity[9] = 0.05;	selectivity[10] = 0.08;	selectivity[11] = 0.10;
				selectivity[12] = 0.200;	selectivity[13] = 0.300;	selectivity[14] = 0.400;	selectivity[15] = 0.500;
				selectivity[16] = 0.600;	selectivity[17] = 0.700;	selectivity[18] = 0.800;	selectivity[19] = 0.99;
			}
			else if( sel_distribution ==1){

				selectivity[0] = 0.00005;   selectivity[1] = 0.00008;		selectivity[2] = 0.0001;	selectivity[3] = 0.0002;
				selectivity[4] = 0.0004;   selectivity[5] = 0.0006;		selectivity[6] = 0.0008;	selectivity[7] = 0.001;
				selectivity[8] = 0.003;	selectivity[9] = 0.005;	selectivity[10] = 0.008;	selectivity[11] = 0.01;
				selectivity[12] = 0.05;	selectivity[13] = 0.1;	selectivity[14] = 0.15;	selectivity[15] = 0.25;
				selectivity[16] = 0.40;	selectivity[17] = 0.60;	selectivity[18] = 0.80;	selectivity[19] = 0.99;
			}
			else
				assert (false) :funName+ "ERROR: should not come here";


		}

		if(resolution == 30){

			if(sel_distribution == 0){
				//tpch
				selectivity[0] = 0.0005;  selectivity[1] = 0.0008;	selectivity[2] = 0.001;	selectivity[3] = 0.002;
				selectivity[4] = 0.004;   selectivity[5] = 0.006;	selectivity[6] = 0.008;	selectivity[7] = 0.01;
				selectivity[8] = 0.03;	selectivity[9] = 0.05;
				selectivity[10] = 0.07;	selectivity[11] = 0.1;	selectivity[12] = 0.15;	selectivity[13] = 0.20;
				selectivity[14] = 0.25;	selectivity[15] = 0.30;	selectivity[16] = 0.35;	selectivity[17] = 0.40;
				selectivity[18] = 0.45;	selectivity[19] = 0.50;	selectivity[20] = 0.55;	selectivity[21] = 0.60;
				selectivity[22] = 0.65;	selectivity[23] = 0.70;	selectivity[24] = 0.75;	selectivity[25] = 0.80;
				selectivity[26] = 0.85;	selectivity[27] = 0.90;	selectivity[28] = 0.95;	selectivity[29] = 0.99;
			}

			else if(sel_distribution == 1){
				selectivity[0] = 0.00001;  selectivity[1] = 0.00005;	selectivity[2] = 0.00010;	selectivity[3] = 0.00050;
				selectivity[4] = 0.0010;   selectivity[5] = 0.005;	selectivity[6] = 0.0100;	selectivity[7] = 0.0200;
				selectivity[8] = 0.0300;	selectivity[9] = 0.0400;	selectivity[10] = 0.0500;	selectivity[11] = 0.0600;
				selectivity[12] = 0.0700;	selectivity[13] = 0.0800;	selectivity[14] = 0.0900;	selectivity[15] = 0.1000;
				selectivity[16] = 0.1200;	selectivity[17] = 0.1400;	selectivity[18] = 0.1600;	selectivity[19] = 0.1800;
				selectivity[20] = 0.2000;	selectivity[21] = 0.2500;	selectivity[22] = 0.3000;	selectivity[23] = 0.4000;
				selectivity[24] = 0.5000;	selectivity[25] = 0.6000;	selectivity[26] = 0.7000;	selectivity[27] = 0.8000;
				selectivity[28] = 0.9000;	selectivity[29] = 0.9950;
			}

			else
				assert (false) :funName+ "ERROR: should not come here";
		}

		if(resolution==100){

			if(sel_distribution == 1){
				selectivity[0] = 0.000064; 	selectivity[1] = 0.000093; 	selectivity[2] = 0.000126; 	selectivity[3] = 0.000161; 	selectivity[4] = 0.000198;
				selectivity[5] = 0.000239; 	selectivity[6] = 0.000284; 	selectivity[7] = 0.000332; 	selectivity[8] = 0.000384; 	selectivity[9] = 0.000440;
				selectivity[10] = 0.000501; 	selectivity[11] = 0.000567; 	selectivity[12] = 0.000638; 	selectivity[13] = 0.000716; 	selectivity[14] = 0.000800;
				selectivity[15] = 0.000890; 	selectivity[16] = 0.000989; 	selectivity[17] = 0.001095; 	selectivity[18] = 0.001211; 	selectivity[19] = 0.001335;
				selectivity[20] = 0.001471; 	selectivity[21] = 0.001617; 	selectivity[22] = 0.001776; 	selectivity[23] = 0.001948; 	selectivity[24] = 0.002134;
				selectivity[25] = 0.002335; 	selectivity[26] = 0.002554; 	selectivity[27] = 0.002790; 	selectivity[28] = 0.003046; 	selectivity[29] = 0.003323;
				selectivity[30] = 0.003624; 	selectivity[31] = 0.003949; 	selectivity[32] = 0.004301; 	selectivity[33] = 0.004683; 	selectivity[34] = 0.005096;
				selectivity[35] = 0.005543; 	selectivity[36] = 0.006028; 	selectivity[37] = 0.006552; 	selectivity[38] = 0.007121; 	selectivity[39] = 0.007736;
				selectivity[40] = 0.008403; 	selectivity[41] = 0.009125; 	selectivity[42] = 0.009907; 	selectivity[43] = 0.010753; 	selectivity[44] = 0.011670;
				selectivity[45] = 0.012663; 	selectivity[46] = 0.013739; 	selectivity[47] = 0.014904; 	selectivity[48] = 0.016165; 	selectivity[49] = 0.017531;
				selectivity[50] = 0.019011; 	selectivity[51] = 0.020613; 	selectivity[52] = 0.022348; 	selectivity[53] = 0.024228; 	selectivity[54] = 0.026263;
				selectivity[55] = 0.028467; 	selectivity[56] = 0.030854; 	selectivity[57] = 0.033440; 	selectivity[58] = 0.036240; 	selectivity[59] = 0.039272;
				selectivity[60] = 0.042556; 	selectivity[61] = 0.046113; 	selectivity[62] = 0.049965; 	selectivity[63] = 0.054136; 	selectivity[64] = 0.058654;
				selectivity[65] = 0.063547; 	selectivity[66] = 0.068845; 	selectivity[67] = 0.074584; 	selectivity[68] = 0.080799; 	selectivity[69] = 0.087530;
				selectivity[70] = 0.094819; 	selectivity[71] = 0.102714; 	selectivity[72] = 0.111263; 	selectivity[73] = 0.120523; 	selectivity[74] = 0.130550;
				selectivity[75] = 0.141411; 	selectivity[76] = 0.153172; 	selectivity[77] = 0.165910; 	selectivity[78] = 0.179705; 	selectivity[79] = 0.194645;
				selectivity[80] = 0.210825; 	selectivity[81] = 0.228348; 	selectivity[82] = 0.247325; 	selectivity[83] = 0.267877; 	selectivity[84] = 0.290136;
				selectivity[85] = 0.314241; 	selectivity[86] = 0.340348; 	selectivity[87] = 0.368621; 	selectivity[88] = 0.399241; 	selectivity[89] = 0.432403;
				selectivity[90] = 0.468316; 	selectivity[91] = 0.507211; 	selectivity[92] = 0.549334; 	selectivity[93] = 0.594953; 	selectivity[94] = 0.644359;
				selectivity[95] = 0.697865; 	selectivity[96] = 0.755812; 	selectivity[97] = 0.818569; 	selectivity[98] = 0.886535; 	selectivity[99] = 0.990142;

			}
			else if(sel_distribution == 0){
				selectivity[0] = 0.005995; 	selectivity[1] = 0.015985; 	selectivity[2] = 0.025975; 	selectivity[3] = 0.035965; 	selectivity[4] = 0.045955; 	
				selectivity[5] = 0.055945; 	selectivity[6] = 0.065935; 	selectivity[7] = 0.075925; 	selectivity[8] = 0.085915; 	selectivity[9] = 0.095905; 	
				selectivity[10] = 0.105895; 	selectivity[11] = 0.115885; 	selectivity[12] = 0.125875; 	selectivity[13] = 0.135865; 	selectivity[14] = 0.145855; 	
				selectivity[15] = 0.155845; 	selectivity[16] = 0.165835; 	selectivity[17] = 0.175825; 	selectivity[18] = 0.185815; 	selectivity[19] = 0.195805; 	
				selectivity[20] = 0.205795; 	selectivity[21] = 0.215785; 	selectivity[22] = 0.225775; 	selectivity[23] = 0.235765; 	selectivity[24] = 0.245755; 	
				selectivity[25] = 0.255745; 	selectivity[26] = 0.265735; 	selectivity[27] = 0.275725; 	selectivity[28] = 0.285715; 	selectivity[29] = 0.295705; 	
				selectivity[30] = 0.305695; 	selectivity[31] = 0.315685; 	selectivity[32] = 0.325675; 	selectivity[33] = 0.335665; 	selectivity[34] = 0.345655; 	
				selectivity[35] = 0.355645; 	selectivity[36] = 0.365635; 	selectivity[37] = 0.375625; 	selectivity[38] = 0.385615; 	selectivity[39] = 0.395605; 	
				selectivity[40] = 0.405595; 	selectivity[41] = 0.415585; 	selectivity[42] = 0.425575; 	selectivity[43] = 0.435565; 	selectivity[44] = 0.445555; 	
				selectivity[45] = 0.455545; 	selectivity[46] = 0.465535; 	selectivity[47] = 0.475525; 	selectivity[48] = 0.485515; 	selectivity[49] = 0.495505; 	
				selectivity[50] = 0.505495; 	selectivity[51] = 0.515485; 	selectivity[52] = 0.525475; 	selectivity[53] = 0.535465; 	selectivity[54] = 0.545455; 	
				selectivity[55] = 0.555445; 	selectivity[56] = 0.565435; 	selectivity[57] = 0.575425; 	selectivity[58] = 0.585415; 	selectivity[59] = 0.595405; 	
				selectivity[60] = 0.605395; 	selectivity[61] = 0.615385; 	selectivity[62] = 0.625375; 	selectivity[63] = 0.635365; 	selectivity[64] = 0.645355; 	
				selectivity[65] = 0.655345; 	selectivity[66] = 0.665335; 	selectivity[67] = 0.675325; 	selectivity[68] = 0.685315; 	selectivity[69] = 0.695305; 	
				selectivity[70] = 0.705295; 	selectivity[71] = 0.715285; 	selectivity[72] = 0.725275; 	selectivity[73] = 0.735265; 	selectivity[74] = 0.745255; 	
				selectivity[75] = 0.755245; 	selectivity[76] = 0.765235; 	selectivity[77] = 0.775225; 	selectivity[78] = 0.785215; 	selectivity[79] = 0.795205; 	
				selectivity[80] = 0.805195; 	selectivity[81] = 0.815185; 	selectivity[82] = 0.825175; 	selectivity[83] = 0.835165; 	selectivity[84] = 0.845155; 	
				selectivity[85] = 0.855145; 	selectivity[86] = 0.865135; 	selectivity[87] = 0.875125; 	selectivity[88] = 0.885115; 	selectivity[89] = 0.895105; 	
				selectivity[90] = 0.905095; 	selectivity[91] = 0.915085; 	selectivity[92] = 0.925075; 	selectivity[93] = 0.935065; 	selectivity[94] = 0.945055; 	
				selectivity[95] = 0.955045; 	selectivity[96] = 0.965035; 	selectivity[97] = 0.975025; 	selectivity[98] = 0.985015; 	selectivity[99] = 0.995005;
			}

			else
				assert (false) :funName+ "ERROR: should not come here";
		}
		if(resolution == 1000){

			selectivity[0] = 0.00051000; 	selectivity[1] = 0.00150999; 	selectivity[2] = 0.00250998; 	selectivity[3] = 0.00350997; 	selectivity[4] = 0.00450996; 	
			selectivity[5] = 0.00550995; 	selectivity[6] = 0.00650994; 	selectivity[7] = 0.00750993; 	selectivity[8] = 0.00850992; 	selectivity[9] = 0.00950990; 	
			selectivity[10] = 0.01050989; 	selectivity[11] = 0.01150988; 	selectivity[12] = 0.01250987; 	selectivity[13] = 0.01350986; 	selectivity[14] = 0.01450985; 	
			selectivity[15] = 0.01550984; 	selectivity[16] = 0.01650983; 	selectivity[17] = 0.01750982; 	selectivity[18] = 0.01850981; 	selectivity[19] = 0.01950980; 	
			selectivity[20] = 0.02050979; 	selectivity[21] = 0.02150978; 	selectivity[22] = 0.02250977; 	selectivity[23] = 0.02350976; 	selectivity[24] = 0.02450975; 	
			selectivity[25] = 0.02550974; 	selectivity[26] = 0.02650973; 	selectivity[27] = 0.02750972; 	selectivity[28] = 0.02850971; 	selectivity[29] = 0.02950970; 	
			selectivity[30] = 0.03050969; 	selectivity[31] = 0.03150968; 	selectivity[32] = 0.03250967; 	selectivity[33] = 0.03350966; 	selectivity[34] = 0.03450965; 	
			selectivity[35] = 0.03550964; 	selectivity[36] = 0.03650963; 	selectivity[37] = 0.03750962; 	selectivity[38] = 0.03850961; 	selectivity[39] = 0.03950960; 	
			selectivity[40] = 0.04050959; 	selectivity[41] = 0.04150958; 	selectivity[42] = 0.04250957; 	selectivity[43] = 0.04350956; 	selectivity[44] = 0.04450955; 	
			selectivity[45] = 0.04550954; 	selectivity[46] = 0.04650953; 	selectivity[47] = 0.04750952; 	selectivity[48] = 0.04850951; 	selectivity[49] = 0.04950950; 	
			selectivity[50] = 0.05050949; 	selectivity[51] = 0.05150948; 	selectivity[52] = 0.05250947; 	selectivity[53] = 0.05350946; 	selectivity[54] = 0.05450945; 	
			selectivity[55] = 0.05550944; 	selectivity[56] = 0.05650943; 	selectivity[57] = 0.05750942; 	selectivity[58] = 0.05850941; 	selectivity[59] = 0.05950940; 	
			selectivity[60] = 0.06050939; 	selectivity[61] = 0.06150938; 	selectivity[62] = 0.06250937; 	selectivity[63] = 0.06350936; 	selectivity[64] = 0.06450935; 	
			selectivity[65] = 0.06550934; 	selectivity[66] = 0.06650933; 	selectivity[67] = 0.06750933; 	selectivity[68] = 0.06850932; 	selectivity[69] = 0.06950931; 	
			selectivity[70] = 0.07050930; 	selectivity[71] = 0.07150929; 	selectivity[72] = 0.07250928; 	selectivity[73] = 0.07350927; 	selectivity[74] = 0.07450926; 	
			selectivity[75] = 0.07550925; 	selectivity[76] = 0.07650924; 	selectivity[77] = 0.07750923; 	selectivity[78] = 0.07850922; 	selectivity[79] = 0.07950921; 	
			selectivity[80] = 0.08050920; 	selectivity[81] = 0.08150919; 	selectivity[82] = 0.08250918; 	selectivity[83] = 0.08350917; 	selectivity[84] = 0.08450916; 	
			selectivity[85] = 0.08550915; 	selectivity[86] = 0.08650914; 	selectivity[87] = 0.08750913; 	selectivity[88] = 0.08850912; 	selectivity[89] = 0.08950911; 	
			selectivity[90] = 0.09050910; 	selectivity[91] = 0.09150909; 	selectivity[92] = 0.09250908; 	selectivity[93] = 0.09350907; 	selectivity[94] = 0.09450906; 	
			selectivity[95] = 0.09550905; 	selectivity[96] = 0.09650904; 	selectivity[97] = 0.09750903; 	selectivity[98] = 0.09850902; 	selectivity[99] = 0.09950901; 	
			selectivity[100] = 0.10050900; 	selectivity[101] = 0.10150899; 	selectivity[102] = 0.10250898; 	selectivity[103] = 0.10350897; 	selectivity[104] = 0.10450896; 	
			selectivity[105] = 0.10550895; 	selectivity[106] = 0.10650894; 	selectivity[107] = 0.10750893; 	selectivity[108] = 0.10850892; 	selectivity[109] = 0.10950891; 	
			selectivity[110] = 0.11050890; 	selectivity[111] = 0.11150889; 	selectivity[112] = 0.11250888; 	selectivity[113] = 0.11350887; 	selectivity[114] = 0.11450886; 	
			selectivity[115] = 0.11550885; 	selectivity[116] = 0.11650884; 	selectivity[117] = 0.11750883; 	selectivity[118] = 0.11850882; 	selectivity[119] = 0.11950881; 	
			selectivity[120] = 0.12050880; 	selectivity[121] = 0.12150879; 	selectivity[122] = 0.12250878; 	selectivity[123] = 0.12350877; 	selectivity[124] = 0.12450876; 	
			selectivity[125] = 0.12550875; 	selectivity[126] = 0.12650874; 	selectivity[127] = 0.12750873; 	selectivity[128] = 0.12850872; 	selectivity[129] = 0.12950871; 	
			selectivity[130] = 0.13050870; 	selectivity[131] = 0.13150869; 	selectivity[132] = 0.13250868; 	selectivity[133] = 0.13350867; 	selectivity[134] = 0.13450866; 	
			selectivity[135] = 0.13550865; 	selectivity[136] = 0.13650864; 	selectivity[137] = 0.13750863; 	selectivity[138] = 0.13850862; 	selectivity[139] = 0.13950861; 	
			selectivity[140] = 0.14050860; 	selectivity[141] = 0.14150859; 	selectivity[142] = 0.14250858; 	selectivity[143] = 0.14350857; 	selectivity[144] = 0.14450856; 	
			selectivity[145] = 0.14550855; 	selectivity[146] = 0.14650854; 	selectivity[147] = 0.14750853; 	selectivity[148] = 0.14850852; 	selectivity[149] = 0.14950851; 	
			selectivity[150] = 0.15050850; 	selectivity[151] = 0.15150849; 	selectivity[152] = 0.15250848; 	selectivity[153] = 0.15350847; 	selectivity[154] = 0.15450846; 	
			selectivity[155] = 0.15550845; 	selectivity[156] = 0.15650844; 	selectivity[157] = 0.15750843; 	selectivity[158] = 0.15850842; 	selectivity[159] = 0.15950841; 	
			selectivity[160] = 0.16050840; 	selectivity[161] = 0.16150839; 	selectivity[162] = 0.16250838; 	selectivity[163] = 0.16350837; 	selectivity[164] = 0.16450836; 	
			selectivity[165] = 0.16550835; 	selectivity[166] = 0.16650834; 	selectivity[167] = 0.16750833; 	selectivity[168] = 0.16850832; 	selectivity[169] = 0.16950831; 	
			selectivity[170] = 0.17050830; 	selectivity[171] = 0.17150829; 	selectivity[172] = 0.17250828; 	selectivity[173] = 0.17350827; 	selectivity[174] = 0.17450826; 	
			selectivity[175] = 0.17550825; 	selectivity[176] = 0.17650824; 	selectivity[177] = 0.17750823; 	selectivity[178] = 0.17850822; 	selectivity[179] = 0.17950821; 	
			selectivity[180] = 0.18050820; 	selectivity[181] = 0.18150819; 	selectivity[182] = 0.18250818; 	selectivity[183] = 0.18350817; 	selectivity[184] = 0.18450816; 	
			selectivity[185] = 0.18550815; 	selectivity[186] = 0.18650814; 	selectivity[187] = 0.18750813; 	selectivity[188] = 0.18850812; 	selectivity[189] = 0.18950811; 	
			selectivity[190] = 0.19050810; 	selectivity[191] = 0.19150809; 	selectivity[192] = 0.19250808; 	selectivity[193] = 0.19350807; 	selectivity[194] = 0.19450806; 	
			selectivity[195] = 0.19550805; 	selectivity[196] = 0.19650804; 	selectivity[197] = 0.19750803; 	selectivity[198] = 0.19850802; 	selectivity[199] = 0.19950801; 	
			selectivity[200] = 0.20050800; 	selectivity[201] = 0.20150799; 	selectivity[202] = 0.20250798; 	selectivity[203] = 0.20350797; 	selectivity[204] = 0.20450796; 	
			selectivity[205] = 0.20550795; 	selectivity[206] = 0.20650794; 	selectivity[207] = 0.20750793; 	selectivity[208] = 0.20850792; 	selectivity[209] = 0.20950791; 	
			selectivity[210] = 0.21050790; 	selectivity[211] = 0.21150789; 	selectivity[212] = 0.21250788; 	selectivity[213] = 0.21350787; 	selectivity[214] = 0.21450786; 	
			selectivity[215] = 0.21550785; 	selectivity[216] = 0.21650784; 	selectivity[217] = 0.21750783; 	selectivity[218] = 0.21850782; 	selectivity[219] = 0.21950781; 	
			selectivity[220] = 0.22050780; 	selectivity[221] = 0.22150779; 	selectivity[222] = 0.22250778; 	selectivity[223] = 0.22350777; 	selectivity[224] = 0.22450776; 	
			selectivity[225] = 0.22550775; 	selectivity[226] = 0.22650774; 	selectivity[227] = 0.22750773; 	selectivity[228] = 0.22850772; 	selectivity[229] = 0.22950771; 	
			selectivity[230] = 0.23050770; 	selectivity[231] = 0.23150769; 	selectivity[232] = 0.23250768; 	selectivity[233] = 0.23350767; 	selectivity[234] = 0.23450766; 	
			selectivity[235] = 0.23550765; 	selectivity[236] = 0.23650764; 	selectivity[237] = 0.23750763; 	selectivity[238] = 0.23850762; 	selectivity[239] = 0.23950761; 	
			selectivity[240] = 0.24050760; 	selectivity[241] = 0.24150759; 	selectivity[242] = 0.24250758; 	selectivity[243] = 0.24350757; 	selectivity[244] = 0.24450756; 	
			selectivity[245] = 0.24550755; 	selectivity[246] = 0.24650754; 	selectivity[247] = 0.24750753; 	selectivity[248] = 0.24850752; 	selectivity[249] = 0.24950751; 	
			selectivity[250] = 0.25050750; 	selectivity[251] = 0.25150749; 	selectivity[252] = 0.25250748; 	selectivity[253] = 0.25350747; 	selectivity[254] = 0.25450746; 	
			selectivity[255] = 0.25550745; 	selectivity[256] = 0.25650744; 	selectivity[257] = 0.25750743; 	selectivity[258] = 0.25850742; 	selectivity[259] = 0.25950741; 	
			selectivity[260] = 0.26050740; 	selectivity[261] = 0.26150739; 	selectivity[262] = 0.26250738; 	selectivity[263] = 0.26350737; 	selectivity[264] = 0.26450736; 	
			selectivity[265] = 0.26550735; 	selectivity[266] = 0.26650734; 	selectivity[267] = 0.26750733; 	selectivity[268] = 0.26850732; 	selectivity[269] = 0.26950731; 	
			selectivity[270] = 0.27050730; 	selectivity[271] = 0.27150729; 	selectivity[272] = 0.27250728; 	selectivity[273] = 0.27350727; 	selectivity[274] = 0.27450726; 	
			selectivity[275] = 0.27550725; 	selectivity[276] = 0.27650724; 	selectivity[277] = 0.27750723; 	selectivity[278] = 0.27850722; 	selectivity[279] = 0.27950721; 	
			selectivity[280] = 0.28050720; 	selectivity[281] = 0.28150719; 	selectivity[282] = 0.28250718; 	selectivity[283] = 0.28350717; 	selectivity[284] = 0.28450716; 	
			selectivity[285] = 0.28550715; 	selectivity[286] = 0.28650714; 	selectivity[287] = 0.28750713; 	selectivity[288] = 0.28850712; 	selectivity[289] = 0.28950711; 	
			selectivity[290] = 0.29050710; 	selectivity[291] = 0.29150709; 	selectivity[292] = 0.29250708; 	selectivity[293] = 0.29350707; 	selectivity[294] = 0.29450706; 	
			selectivity[295] = 0.29550705; 	selectivity[296] = 0.29650704; 	selectivity[297] = 0.29750703; 	selectivity[298] = 0.29850702; 	selectivity[299] = 0.29950701; 	
			selectivity[300] = 0.30050700; 	selectivity[301] = 0.30150699; 	selectivity[302] = 0.30250698; 	selectivity[303] = 0.30350697; 	selectivity[304] = 0.30450696; 	
			selectivity[305] = 0.30550695; 	selectivity[306] = 0.30650694; 	selectivity[307] = 0.30750693; 	selectivity[308] = 0.30850692; 	selectivity[309] = 0.30950691; 	
			selectivity[310] = 0.31050690; 	selectivity[311] = 0.31150689; 	selectivity[312] = 0.31250688; 	selectivity[313] = 0.31350687; 	selectivity[314] = 0.31450686; 	
			selectivity[315] = 0.31550685; 	selectivity[316] = 0.31650684; 	selectivity[317] = 0.31750683; 	selectivity[318] = 0.31850682; 	selectivity[319] = 0.31950681; 	
			selectivity[320] = 0.32050680; 	selectivity[321] = 0.32150679; 	selectivity[322] = 0.32250678; 	selectivity[323] = 0.32350677; 	selectivity[324] = 0.32450676; 	
			selectivity[325] = 0.32550675; 	selectivity[326] = 0.32650674; 	selectivity[327] = 0.32750673; 	selectivity[328] = 0.32850672; 	selectivity[329] = 0.32950671; 	
			selectivity[330] = 0.33050670; 	selectivity[331] = 0.33150669; 	selectivity[332] = 0.33250668; 	selectivity[333] = 0.33350667; 	selectivity[334] = 0.33450666; 	
			selectivity[335] = 0.33550665; 	selectivity[336] = 0.33650664; 	selectivity[337] = 0.33750663; 	selectivity[338] = 0.33850662; 	selectivity[339] = 0.33950661; 	
			selectivity[340] = 0.34050660; 	selectivity[341] = 0.34150659; 	selectivity[342] = 0.34250658; 	selectivity[343] = 0.34350657; 	selectivity[344] = 0.34450656; 	
			selectivity[345] = 0.34550655; 	selectivity[346] = 0.34650654; 	selectivity[347] = 0.34750653; 	selectivity[348] = 0.34850652; 	selectivity[349] = 0.34950651; 	
			selectivity[350] = 0.35050650; 	selectivity[351] = 0.35150649; 	selectivity[352] = 0.35250648; 	selectivity[353] = 0.35350647; 	selectivity[354] = 0.35450646; 	
			selectivity[355] = 0.35550645; 	selectivity[356] = 0.35650644; 	selectivity[357] = 0.35750643; 	selectivity[358] = 0.35850642; 	selectivity[359] = 0.35950641; 	
			selectivity[360] = 0.36050640; 	selectivity[361] = 0.36150639; 	selectivity[362] = 0.36250638; 	selectivity[363] = 0.36350637; 	selectivity[364] = 0.36450636; 	
			selectivity[365] = 0.36550635; 	selectivity[366] = 0.36650634; 	selectivity[367] = 0.36750633; 	selectivity[368] = 0.36850632; 	selectivity[369] = 0.36950631; 	
			selectivity[370] = 0.37050630; 	selectivity[371] = 0.37150629; 	selectivity[372] = 0.37250628; 	selectivity[373] = 0.37350627; 	selectivity[374] = 0.37450626; 	
			selectivity[375] = 0.37550625; 	selectivity[376] = 0.37650624; 	selectivity[377] = 0.37750623; 	selectivity[378] = 0.37850622; 	selectivity[379] = 0.37950621; 	
			selectivity[380] = 0.38050620; 	selectivity[381] = 0.38150619; 	selectivity[382] = 0.38250618; 	selectivity[383] = 0.38350617; 	selectivity[384] = 0.38450616; 	
			selectivity[385] = 0.38550615; 	selectivity[386] = 0.38650614; 	selectivity[387] = 0.38750613; 	selectivity[388] = 0.38850612; 	selectivity[389] = 0.38950611; 	
			selectivity[390] = 0.39050610; 	selectivity[391] = 0.39150609; 	selectivity[392] = 0.39250608; 	selectivity[393] = 0.39350607; 	selectivity[394] = 0.39450606; 	
			selectivity[395] = 0.39550605; 	selectivity[396] = 0.39650604; 	selectivity[397] = 0.39750603; 	selectivity[398] = 0.39850602; 	selectivity[399] = 0.39950601; 	
			selectivity[400] = 0.40050600; 	selectivity[401] = 0.40150599; 	selectivity[402] = 0.40250598; 	selectivity[403] = 0.40350597; 	selectivity[404] = 0.40450596; 	
			selectivity[405] = 0.40550595; 	selectivity[406] = 0.40650594; 	selectivity[407] = 0.40750593; 	selectivity[408] = 0.40850592; 	selectivity[409] = 0.40950591; 	
			selectivity[410] = 0.41050590; 	selectivity[411] = 0.41150589; 	selectivity[412] = 0.41250588; 	selectivity[413] = 0.41350587; 	selectivity[414] = 0.41450586; 	
			selectivity[415] = 0.41550585; 	selectivity[416] = 0.41650584; 	selectivity[417] = 0.41750583; 	selectivity[418] = 0.41850582; 	selectivity[419] = 0.41950581; 	
			selectivity[420] = 0.42050580; 	selectivity[421] = 0.42150579; 	selectivity[422] = 0.42250578; 	selectivity[423] = 0.42350577; 	selectivity[424] = 0.42450576; 	
			selectivity[425] = 0.42550575; 	selectivity[426] = 0.42650574; 	selectivity[427] = 0.42750573; 	selectivity[428] = 0.42850572; 	selectivity[429] = 0.42950571; 	
			selectivity[430] = 0.43050570; 	selectivity[431] = 0.43150569; 	selectivity[432] = 0.43250568; 	selectivity[433] = 0.43350567; 	selectivity[434] = 0.43450566; 	
			selectivity[435] = 0.43550565; 	selectivity[436] = 0.43650564; 	selectivity[437] = 0.43750563; 	selectivity[438] = 0.43850562; 	selectivity[439] = 0.43950561; 	
			selectivity[440] = 0.44050560; 	selectivity[441] = 0.44150559; 	selectivity[442] = 0.44250558; 	selectivity[443] = 0.44350557; 	selectivity[444] = 0.44450556; 	
			selectivity[445] = 0.44550555; 	selectivity[446] = 0.44650554; 	selectivity[447] = 0.44750553; 	selectivity[448] = 0.44850552; 	selectivity[449] = 0.44950551; 	
			selectivity[450] = 0.45050550; 	selectivity[451] = 0.45150549; 	selectivity[452] = 0.45250548; 	selectivity[453] = 0.45350547; 	selectivity[454] = 0.45450546; 	
			selectivity[455] = 0.45550545; 	selectivity[456] = 0.45650544; 	selectivity[457] = 0.45750543; 	selectivity[458] = 0.45850542; 	selectivity[459] = 0.45950541; 	
			selectivity[460] = 0.46050540; 	selectivity[461] = 0.46150539; 	selectivity[462] = 0.46250538; 	selectivity[463] = 0.46350537; 	selectivity[464] = 0.46450536; 	
			selectivity[465] = 0.46550535; 	selectivity[466] = 0.46650534; 	selectivity[467] = 0.46750533; 	selectivity[468] = 0.46850532; 	selectivity[469] = 0.46950531; 	
			selectivity[470] = 0.47050530; 	selectivity[471] = 0.47150529; 	selectivity[472] = 0.47250528; 	selectivity[473] = 0.47350527; 	selectivity[474] = 0.47450526; 	
			selectivity[475] = 0.47550525; 	selectivity[476] = 0.47650524; 	selectivity[477] = 0.47750523; 	selectivity[478] = 0.47850522; 	selectivity[479] = 0.47950521; 	
			selectivity[480] = 0.48050520; 	selectivity[481] = 0.48150519; 	selectivity[482] = 0.48250518; 	selectivity[483] = 0.48350517; 	selectivity[484] = 0.48450516; 	
			selectivity[485] = 0.48550515; 	selectivity[486] = 0.48650514; 	selectivity[487] = 0.48750513; 	selectivity[488] = 0.48850512; 	selectivity[489] = 0.48950511; 	
			selectivity[490] = 0.49050510; 	selectivity[491] = 0.49150509; 	selectivity[492] = 0.49250508; 	selectivity[493] = 0.49350507; 	selectivity[494] = 0.49450506; 	
			selectivity[495] = 0.49550505; 	selectivity[496] = 0.49650504; 	selectivity[497] = 0.49750503; 	selectivity[498] = 0.49850502; 	selectivity[499] = 0.49950501; 	
			selectivity[500] = 0.50050500; 	selectivity[501] = 0.50150499; 	selectivity[502] = 0.50250498; 	selectivity[503] = 0.50350497; 	selectivity[504] = 0.50450496; 	
			selectivity[505] = 0.50550495; 	selectivity[506] = 0.50650494; 	selectivity[507] = 0.50750493; 	selectivity[508] = 0.50850492; 	selectivity[509] = 0.50950491; 	
			selectivity[510] = 0.51050490; 	selectivity[511] = 0.51150489; 	selectivity[512] = 0.51250488; 	selectivity[513] = 0.51350487; 	selectivity[514] = 0.51450486; 	
			selectivity[515] = 0.51550485; 	selectivity[516] = 0.51650484; 	selectivity[517] = 0.51750483; 	selectivity[518] = 0.51850482; 	selectivity[519] = 0.51950481; 	
			selectivity[520] = 0.52050480; 	selectivity[521] = 0.52150479; 	selectivity[522] = 0.52250478; 	selectivity[523] = 0.52350477; 	selectivity[524] = 0.52450476; 	
			selectivity[525] = 0.52550475; 	selectivity[526] = 0.52650474; 	selectivity[527] = 0.52750473; 	selectivity[528] = 0.52850472; 	selectivity[529] = 0.52950471; 	
			selectivity[530] = 0.53050470; 	selectivity[531] = 0.53150469; 	selectivity[532] = 0.53250468; 	selectivity[533] = 0.53350467; 	selectivity[534] = 0.53450466; 	
			selectivity[535] = 0.53550465; 	selectivity[536] = 0.53650464; 	selectivity[537] = 0.53750463; 	selectivity[538] = 0.53850462; 	selectivity[539] = 0.53950461; 	
			selectivity[540] = 0.54050460; 	selectivity[541] = 0.54150459; 	selectivity[542] = 0.54250458; 	selectivity[543] = 0.54350457; 	selectivity[544] = 0.54450456; 	
			selectivity[545] = 0.54550455; 	selectivity[546] = 0.54650454; 	selectivity[547] = 0.54750453; 	selectivity[548] = 0.54850452; 	selectivity[549] = 0.54950451; 	
			selectivity[550] = 0.55050450; 	selectivity[551] = 0.55150449; 	selectivity[552] = 0.55250448; 	selectivity[553] = 0.55350447; 	selectivity[554] = 0.55450445; 	
			selectivity[555] = 0.55550444; 	selectivity[556] = 0.55650443; 	selectivity[557] = 0.55750442; 	selectivity[558] = 0.55850441; 	selectivity[559] = 0.55950440; 	
			selectivity[560] = 0.56050439; 	selectivity[561] = 0.56150438; 	selectivity[562] = 0.56250437; 	selectivity[563] = 0.56350436; 	selectivity[564] = 0.56450435; 	
			selectivity[565] = 0.56550434; 	selectivity[566] = 0.56650433; 	selectivity[567] = 0.56750432; 	selectivity[568] = 0.56850431; 	selectivity[569] = 0.56950430; 	
			selectivity[570] = 0.57050429; 	selectivity[571] = 0.57150428; 	selectivity[572] = 0.57250427; 	selectivity[573] = 0.57350426; 	selectivity[574] = 0.57450425; 	
			selectivity[575] = 0.57550424; 	selectivity[576] = 0.57650423; 	selectivity[577] = 0.57750422; 	selectivity[578] = 0.57850421; 	selectivity[579] = 0.57950420; 	
			selectivity[580] = 0.58050419; 	selectivity[581] = 0.58150418; 	selectivity[582] = 0.58250417; 	selectivity[583] = 0.58350416; 	selectivity[584] = 0.58450415; 	
			selectivity[585] = 0.58550414; 	selectivity[586] = 0.58650413; 	selectivity[587] = 0.58750412; 	selectivity[588] = 0.58850411; 	selectivity[589] = 0.58950410; 	
			selectivity[590] = 0.59050409; 	selectivity[591] = 0.59150408; 	selectivity[592] = 0.59250407; 	selectivity[593] = 0.59350406; 	selectivity[594] = 0.59450405; 	
			selectivity[595] = 0.59550404; 	selectivity[596] = 0.59650403; 	selectivity[597] = 0.59750402; 	selectivity[598] = 0.59850401; 	selectivity[599] = 0.59950400; 	
			selectivity[600] = 0.60050399; 	selectivity[601] = 0.60150398; 	selectivity[602] = 0.60250397; 	selectivity[603] = 0.60350396; 	selectivity[604] = 0.60450395; 	
			selectivity[605] = 0.60550394; 	selectivity[606] = 0.60650393; 	selectivity[607] = 0.60750392; 	selectivity[608] = 0.60850391; 	selectivity[609] = 0.60950390; 	
			selectivity[610] = 0.61050389; 	selectivity[611] = 0.61150388; 	selectivity[612] = 0.61250387; 	selectivity[613] = 0.61350386; 	selectivity[614] = 0.61450385; 	
			selectivity[615] = 0.61550384; 	selectivity[616] = 0.61650383; 	selectivity[617] = 0.61750382; 	selectivity[618] = 0.61850381; 	selectivity[619] = 0.61950380; 	
			selectivity[620] = 0.62050379; 	selectivity[621] = 0.62150378; 	selectivity[622] = 0.62250377; 	selectivity[623] = 0.62350376; 	selectivity[624] = 0.62450375; 	
			selectivity[625] = 0.62550374; 	selectivity[626] = 0.62650373; 	selectivity[627] = 0.62750372; 	selectivity[628] = 0.62850371; 	selectivity[629] = 0.62950370; 	
			selectivity[630] = 0.63050369; 	selectivity[631] = 0.63150368; 	selectivity[632] = 0.63250367; 	selectivity[633] = 0.63350366; 	selectivity[634] = 0.63450365; 	
			selectivity[635] = 0.63550364; 	selectivity[636] = 0.63650363; 	selectivity[637] = 0.63750362; 	selectivity[638] = 0.63850361; 	selectivity[639] = 0.63950360; 	
			selectivity[640] = 0.64050359; 	selectivity[641] = 0.64150358; 	selectivity[642] = 0.64250357; 	selectivity[643] = 0.64350356; 	selectivity[644] = 0.64450355; 	
			selectivity[645] = 0.64550354; 	selectivity[646] = 0.64650353; 	selectivity[647] = 0.64750352; 	selectivity[648] = 0.64850351; 	selectivity[649] = 0.64950350; 	
			selectivity[650] = 0.65050349; 	selectivity[651] = 0.65150348; 	selectivity[652] = 0.65250347; 	selectivity[653] = 0.65350346; 	selectivity[654] = 0.65450345; 	
			selectivity[655] = 0.65550344; 	selectivity[656] = 0.65650343; 	selectivity[657] = 0.65750342; 	selectivity[658] = 0.65850341; 	selectivity[659] = 0.65950340; 	
			selectivity[660] = 0.66050339; 	selectivity[661] = 0.66150338; 	selectivity[662] = 0.66250337; 	selectivity[663] = 0.66350336; 	selectivity[664] = 0.66450335; 	
			selectivity[665] = 0.66550334; 	selectivity[666] = 0.66650333; 	selectivity[667] = 0.66750332; 	selectivity[668] = 0.66850331; 	selectivity[669] = 0.66950330; 	
			selectivity[670] = 0.67050329; 	selectivity[671] = 0.67150328; 	selectivity[672] = 0.67250327; 	selectivity[673] = 0.67350326; 	selectivity[674] = 0.67450325; 	
			selectivity[675] = 0.67550324; 	selectivity[676] = 0.67650323; 	selectivity[677] = 0.67750322; 	selectivity[678] = 0.67850321; 	selectivity[679] = 0.67950320; 	
			selectivity[680] = 0.68050319; 	selectivity[681] = 0.68150318; 	selectivity[682] = 0.68250317; 	selectivity[683] = 0.68350316; 	selectivity[684] = 0.68450315; 	
			selectivity[685] = 0.68550314; 	selectivity[686] = 0.68650313; 	selectivity[687] = 0.68750312; 	selectivity[688] = 0.68850311; 	selectivity[689] = 0.68950310; 	
			selectivity[690] = 0.69050309; 	selectivity[691] = 0.69150308; 	selectivity[692] = 0.69250307; 	selectivity[693] = 0.69350306; 	selectivity[694] = 0.69450305; 	
			selectivity[695] = 0.69550304; 	selectivity[696] = 0.69650303; 	selectivity[697] = 0.69750302; 	selectivity[698] = 0.69850301; 	selectivity[699] = 0.69950300; 	
			selectivity[700] = 0.70050299; 	selectivity[701] = 0.70150298; 	selectivity[702] = 0.70250297; 	selectivity[703] = 0.70350296; 	selectivity[704] = 0.70450295; 	
			selectivity[705] = 0.70550294; 	selectivity[706] = 0.70650293; 	selectivity[707] = 0.70750292; 	selectivity[708] = 0.70850291; 	selectivity[709] = 0.70950290; 	
			selectivity[710] = 0.71050289; 	selectivity[711] = 0.71150288; 	selectivity[712] = 0.71250287; 	selectivity[713] = 0.71350286; 	selectivity[714] = 0.71450285; 	
			selectivity[715] = 0.71550284; 	selectivity[716] = 0.71650283; 	selectivity[717] = 0.71750282; 	selectivity[718] = 0.71850281; 	selectivity[719] = 0.71950280; 	
			selectivity[720] = 0.72050279; 	selectivity[721] = 0.72150278; 	selectivity[722] = 0.72250277; 	selectivity[723] = 0.72350276; 	selectivity[724] = 0.72450275; 	
			selectivity[725] = 0.72550274; 	selectivity[726] = 0.72650273; 	selectivity[727] = 0.72750272; 	selectivity[728] = 0.72850271; 	selectivity[729] = 0.72950270; 	
			selectivity[730] = 0.73050269; 	selectivity[731] = 0.73150268; 	selectivity[732] = 0.73250267; 	selectivity[733] = 0.73350266; 	selectivity[734] = 0.73450265; 	
			selectivity[735] = 0.73550264; 	selectivity[736] = 0.73650263; 	selectivity[737] = 0.73750262; 	selectivity[738] = 0.73850261; 	selectivity[739] = 0.73950260; 	
			selectivity[740] = 0.74050259; 	selectivity[741] = 0.74150258; 	selectivity[742] = 0.74250257; 	selectivity[743] = 0.74350256; 	selectivity[744] = 0.74450255; 	
			selectivity[745] = 0.74550254; 	selectivity[746] = 0.74650253; 	selectivity[747] = 0.74750252; 	selectivity[748] = 0.74850251; 	selectivity[749] = 0.74950250; 	
			selectivity[750] = 0.75050249; 	selectivity[751] = 0.75150248; 	selectivity[752] = 0.75250247; 	selectivity[753] = 0.75350246; 	selectivity[754] = 0.75450245; 	
			selectivity[755] = 0.75550244; 	selectivity[756] = 0.75650243; 	selectivity[757] = 0.75750242; 	selectivity[758] = 0.75850241; 	selectivity[759] = 0.75950240; 	
			selectivity[760] = 0.76050239; 	selectivity[761] = 0.76150238; 	selectivity[762] = 0.76250237; 	selectivity[763] = 0.76350236; 	selectivity[764] = 0.76450235; 	
			selectivity[765] = 0.76550234; 	selectivity[766] = 0.76650233; 	selectivity[767] = 0.76750232; 	selectivity[768] = 0.76850231; 	selectivity[769] = 0.76950230; 	
			selectivity[770] = 0.77050229; 	selectivity[771] = 0.77150228; 	selectivity[772] = 0.77250227; 	selectivity[773] = 0.77350226; 	selectivity[774] = 0.77450225; 	
			selectivity[775] = 0.77550224; 	selectivity[776] = 0.77650223; 	selectivity[777] = 0.77750222; 	selectivity[778] = 0.77850221; 	selectivity[779] = 0.77950220; 	
			selectivity[780] = 0.78050219; 	selectivity[781] = 0.78150218; 	selectivity[782] = 0.78250217; 	selectivity[783] = 0.78350216; 	selectivity[784] = 0.78450215; 	
			selectivity[785] = 0.78550214; 	selectivity[786] = 0.78650213; 	selectivity[787] = 0.78750212; 	selectivity[788] = 0.78850211; 	selectivity[789] = 0.78950210; 	
			selectivity[790] = 0.79050209; 	selectivity[791] = 0.79150208; 	selectivity[792] = 0.79250207; 	selectivity[793] = 0.79350206; 	selectivity[794] = 0.79450205; 	
			selectivity[795] = 0.79550204; 	selectivity[796] = 0.79650203; 	selectivity[797] = 0.79750202; 	selectivity[798] = 0.79850201; 	selectivity[799] = 0.79950200; 	
			selectivity[800] = 0.80050199; 	selectivity[801] = 0.80150198; 	selectivity[802] = 0.80250197; 	selectivity[803] = 0.80350196; 	selectivity[804] = 0.80450195; 	
			selectivity[805] = 0.80550194; 	selectivity[806] = 0.80650193; 	selectivity[807] = 0.80750192; 	selectivity[808] = 0.80850191; 	selectivity[809] = 0.80950190; 	
			selectivity[810] = 0.81050189; 	selectivity[811] = 0.81150188; 	selectivity[812] = 0.81250187; 	selectivity[813] = 0.81350186; 	selectivity[814] = 0.81450185; 	
			selectivity[815] = 0.81550184; 	selectivity[816] = 0.81650183; 	selectivity[817] = 0.81750182; 	selectivity[818] = 0.81850181; 	selectivity[819] = 0.81950180; 	
			selectivity[820] = 0.82050179; 	selectivity[821] = 0.82150178; 	selectivity[822] = 0.82250177; 	selectivity[823] = 0.82350176; 	selectivity[824] = 0.82450175; 	
			selectivity[825] = 0.82550174; 	selectivity[826] = 0.82650173; 	selectivity[827] = 0.82750172; 	selectivity[828] = 0.82850171; 	selectivity[829] = 0.82950170; 	
			selectivity[830] = 0.83050169; 	selectivity[831] = 0.83150168; 	selectivity[832] = 0.83250167; 	selectivity[833] = 0.83350166; 	selectivity[834] = 0.83450165; 	
			selectivity[835] = 0.83550164; 	selectivity[836] = 0.83650163; 	selectivity[837] = 0.83750162; 	selectivity[838] = 0.83850161; 	selectivity[839] = 0.83950160; 	
			selectivity[840] = 0.84050159; 	selectivity[841] = 0.84150158; 	selectivity[842] = 0.84250157; 	selectivity[843] = 0.84350156; 	selectivity[844] = 0.84450155; 	
			selectivity[845] = 0.84550154; 	selectivity[846] = 0.84650153; 	selectivity[847] = 0.84750152; 	selectivity[848] = 0.84850151; 	selectivity[849] = 0.84950150; 	
			selectivity[850] = 0.85050149; 	selectivity[851] = 0.85150148; 	selectivity[852] = 0.85250147; 	selectivity[853] = 0.85350146; 	selectivity[854] = 0.85450145; 	
			selectivity[855] = 0.85550144; 	selectivity[856] = 0.85650143; 	selectivity[857] = 0.85750142; 	selectivity[858] = 0.85850141; 	selectivity[859] = 0.85950140; 	
			selectivity[860] = 0.86050139; 	selectivity[861] = 0.86150138; 	selectivity[862] = 0.86250137; 	selectivity[863] = 0.86350136; 	selectivity[864] = 0.86450135; 	
			selectivity[865] = 0.86550134; 	selectivity[866] = 0.86650133; 	selectivity[867] = 0.86750132; 	selectivity[868] = 0.86850131; 	selectivity[869] = 0.86950130; 	
			selectivity[870] = 0.87050129; 	selectivity[871] = 0.87150128; 	selectivity[872] = 0.87250127; 	selectivity[873] = 0.87350126; 	selectivity[874] = 0.87450125; 	
			selectivity[875] = 0.87550124; 	selectivity[876] = 0.87650123; 	selectivity[877] = 0.87750122; 	selectivity[878] = 0.87850121; 	selectivity[879] = 0.87950120; 	
			selectivity[880] = 0.88050119; 	selectivity[881] = 0.88150118; 	selectivity[882] = 0.88250117; 	selectivity[883] = 0.88350116; 	selectivity[884] = 0.88450115; 	
			selectivity[885] = 0.88550114; 	selectivity[886] = 0.88650113; 	selectivity[887] = 0.88750112; 	selectivity[888] = 0.88850111; 	selectivity[889] = 0.88950110; 	
			selectivity[890] = 0.89050109; 	selectivity[891] = 0.89150108; 	selectivity[892] = 0.89250107; 	selectivity[893] = 0.89350106; 	selectivity[894] = 0.89450105; 	
			selectivity[895] = 0.89550104; 	selectivity[896] = 0.89650103; 	selectivity[897] = 0.89750102; 	selectivity[898] = 0.89850101; 	selectivity[899] = 0.89950100; 	
			selectivity[900] = 0.90050099; 	selectivity[901] = 0.90150098; 	selectivity[902] = 0.90250097; 	selectivity[903] = 0.90350096; 	selectivity[904] = 0.90450095; 	
			selectivity[905] = 0.90550094; 	selectivity[906] = 0.90650093; 	selectivity[907] = 0.90750092; 	selectivity[908] = 0.90850091; 	selectivity[909] = 0.90950090; 	
			selectivity[910] = 0.91050089; 	selectivity[911] = 0.91150088; 	selectivity[912] = 0.91250087; 	selectivity[913] = 0.91350086; 	selectivity[914] = 0.91450085; 	
			selectivity[915] = 0.91550084; 	selectivity[916] = 0.91650083; 	selectivity[917] = 0.91750082; 	selectivity[918] = 0.91850081; 	selectivity[919] = 0.91950080; 	
			selectivity[920] = 0.92050079; 	selectivity[921] = 0.92150078; 	selectivity[922] = 0.92250077; 	selectivity[923] = 0.92350076; 	selectivity[924] = 0.92450075; 	
			selectivity[925] = 0.92550074; 	selectivity[926] = 0.92650073; 	selectivity[927] = 0.92750072; 	selectivity[928] = 0.92850071; 	selectivity[929] = 0.92950070; 	
			selectivity[930] = 0.93050069; 	selectivity[931] = 0.93150068; 	selectivity[932] = 0.93250067; 	selectivity[933] = 0.93350066; 	selectivity[934] = 0.93450065; 	
			selectivity[935] = 0.93550064; 	selectivity[936] = 0.93650063; 	selectivity[937] = 0.93750062; 	selectivity[938] = 0.93850061; 	selectivity[939] = 0.93950060; 	
			selectivity[940] = 0.94050059; 	selectivity[941] = 0.94150058; 	selectivity[942] = 0.94250057; 	selectivity[943] = 0.94350056; 	selectivity[944] = 0.94450055; 	
			selectivity[945] = 0.94550054; 	selectivity[946] = 0.94650053; 	selectivity[947] = 0.94750052; 	selectivity[948] = 0.94850051; 	selectivity[949] = 0.94950050; 	
			selectivity[950] = 0.95050049; 	selectivity[951] = 0.95150048; 	selectivity[952] = 0.95250047; 	selectivity[953] = 0.95350046; 	selectivity[954] = 0.95450045; 	
			selectivity[955] = 0.95550044; 	selectivity[956] = 0.95650043; 	selectivity[957] = 0.95750042; 	selectivity[958] = 0.95850041; 	selectivity[959] = 0.95950040; 	
			selectivity[960] = 0.96050039; 	selectivity[961] = 0.96150038; 	selectivity[962] = 0.96250037; 	selectivity[963] = 0.96350036; 	selectivity[964] = 0.96450035; 	
			selectivity[965] = 0.96550034; 	selectivity[966] = 0.96650033; 	selectivity[967] = 0.96750032; 	selectivity[968] = 0.96850031; 	selectivity[969] = 0.96950030; 	
			selectivity[970] = 0.97050029; 	selectivity[971] = 0.97150028; 	selectivity[972] = 0.97250027; 	selectivity[973] = 0.97350026; 	selectivity[974] = 0.97450025; 	
			selectivity[975] = 0.97550024; 	selectivity[976] = 0.97650023; 	selectivity[977] = 0.97750022; 	selectivity[978] = 0.97850021; 	selectivity[979] = 0.97950020; 	
			selectivity[980] = 0.98050019; 	selectivity[981] = 0.98150018; 	selectivity[982] = 0.98250017; 	selectivity[983] = 0.98350016; 	selectivity[984] = 0.98450015; 	
			selectivity[985] = 0.98550014; 	selectivity[986] = 0.98650013; 	selectivity[987] = 0.98750012; 	selectivity[988] = 0.98850011; 	selectivity[989] = 0.98950010; 	
			selectivity[990] = 0.99050009; 	selectivity[991] = 0.99150008; 	selectivity[992] = 0.99250007; 	selectivity[993] = 0.99350006; 	selectivity[994] = 0.99450005; 	
			selectivity[995] = 0.99550004; 	selectivity[996] = 0.99650003; 	selectivity[997] = 0.99750002; 	selectivity[998] = 0.99850001; 	selectivity[999] = 0.99950000; 	

		}

		//the selectivity distribution
		//System.out.println("The selectivity distribution using is ");
		//			for(int i=0;i<resolution;i++)
		//			System.out.println("\t"+selectivity[i]);
	}
	
	
	
	void readpkt(ADiagramPacket gdp, boolean allPlanCost) throws IOException, SQLException
	{
		String funName="readpkt";
		//ADiagramPacket gdp = getGDP(new File(pktPath));
		totalPlans = gdp.getMaxPlanNumber();
		dimension = gdp.getDimension();
		resolution = gdp.getMaxResolution();
		data = gdp.getData();
		totalPoints = (int) Math.pow(resolution, dimension);
		//System.out.println("\nthe total plans are "+totalPlans+" with dimensions "+dimension+" and resolution "+resolution);
		int plan_no;
		

		assert (totalPoints==data.length) : "Data length and the resolution didn't match !";

		this.plans = new int [data.length];
		this.OptimalCost = new double [data.length]; 
		//	this.points_list = new point[resolution][resolution];
		int [] index = new int[dimension];


		

		for (int i = 0;i < data.length;i++)
		{
			index=getCoordinates(dimension,resolution,i);
			//			points_list[index[0]][index[1]] = new point(index[0],index[1],data[i].getPlanNumber(),remainingDim);
			//			points_list[index[0]][index[1]].putopt_cost(data[i].getCost());
			this.OptimalCost[i]= data[i].getCost();
			this.plans[i] = data[i].getPlanNumber();
			plan_no = data[i].getPlanNumber();
			//plans_list[plan_no].addPoint(i);

			//cat = plans_list[plan_no].getcategory(remainingDim);
		}
		
	




		//			minIndex = new double[dimension];
		//			maxIndex = new double[dimension];

		// ------------------------------------- Read pcst files
		
		
		nPlans = totalPlans;
		if(allPlanCost){
			AllPlanCosts = new double[nPlans][totalPoints];
			//costBouquet = new double[total_points];

			int x,y;
			for (int i = 0; i < nPlans; i++) {
				try {

					ObjectInputStream ip = new ObjectInputStream(new FileInputStream(new File(apktPath + "pcstFiles/"+i + ".pcst")));
					double[] cost = (double[]) ip.readObject();
					for (int j = 0; j < totalPoints; j++)
					{

						AllPlanCosts[i][j] = cost[j];
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}



		}
		
		
//		writeFPCCostFile(0);
//		writeFPCCostFile(1);
//		writeFPCCostFile(7);
//		writeFPCCostFile(8);
		//System.exit(0);


	}
	
	private void writeCostToFile() throws SQLException {
		
		double[] sel_arr = new double [1000];

		sel_arr[0] = 0.00051000; 	sel_arr[1] = 0.00150999; 	sel_arr[2] = 0.00250998; 	sel_arr[3] = 0.00350997; 	sel_arr[4] = 0.00450996; 	
		sel_arr[5] = 0.00550995; 	sel_arr[6] = 0.00650994; 	sel_arr[7] = 0.00750993; 	sel_arr[8] = 0.00850992; 	sel_arr[9] = 0.00950990; 	
		sel_arr[10] = 0.01050989; 	sel_arr[11] = 0.01150988; 	sel_arr[12] = 0.01250987; 	sel_arr[13] = 0.01350986; 	sel_arr[14] = 0.01450985; 	
		sel_arr[15] = 0.01550984; 	sel_arr[16] = 0.01650983; 	sel_arr[17] = 0.01750982; 	sel_arr[18] = 0.01850981; 	sel_arr[19] = 0.01950980; 	
		sel_arr[20] = 0.02050979; 	sel_arr[21] = 0.02150978; 	sel_arr[22] = 0.02250977; 	sel_arr[23] = 0.02350976; 	sel_arr[24] = 0.02450975; 	
		sel_arr[25] = 0.02550974; 	sel_arr[26] = 0.02650973; 	sel_arr[27] = 0.02750972; 	sel_arr[28] = 0.02850971; 	sel_arr[29] = 0.02950970; 	
		sel_arr[30] = 0.03050969; 	sel_arr[31] = 0.03150968; 	sel_arr[32] = 0.03250967; 	sel_arr[33] = 0.03350966; 	sel_arr[34] = 0.03450965; 	
		sel_arr[35] = 0.03550964; 	sel_arr[36] = 0.03650963; 	sel_arr[37] = 0.03750962; 	sel_arr[38] = 0.03850961; 	sel_arr[39] = 0.03950960; 	
		sel_arr[40] = 0.04050959; 	sel_arr[41] = 0.04150958; 	sel_arr[42] = 0.04250957; 	sel_arr[43] = 0.04350956; 	sel_arr[44] = 0.04450955; 	
		sel_arr[45] = 0.04550954; 	sel_arr[46] = 0.04650953; 	sel_arr[47] = 0.04750952; 	sel_arr[48] = 0.04850951; 	sel_arr[49] = 0.04950950; 	
		sel_arr[50] = 0.05050949; 	sel_arr[51] = 0.05150948; 	sel_arr[52] = 0.05250947; 	sel_arr[53] = 0.05350946; 	sel_arr[54] = 0.05450945; 	
		sel_arr[55] = 0.05550944; 	sel_arr[56] = 0.05650943; 	sel_arr[57] = 0.05750942; 	sel_arr[58] = 0.05850941; 	sel_arr[59] = 0.05950940; 	
		sel_arr[60] = 0.06050939; 	sel_arr[61] = 0.06150938; 	sel_arr[62] = 0.06250937; 	sel_arr[63] = 0.06350936; 	sel_arr[64] = 0.06450935; 	
		sel_arr[65] = 0.06550934; 	sel_arr[66] = 0.06650933; 	sel_arr[67] = 0.06750933; 	sel_arr[68] = 0.06850932; 	sel_arr[69] = 0.06950931; 	
		sel_arr[70] = 0.07050930; 	sel_arr[71] = 0.07150929; 	sel_arr[72] = 0.07250928; 	sel_arr[73] = 0.07350927; 	sel_arr[74] = 0.07450926; 	
		sel_arr[75] = 0.07550925; 	sel_arr[76] = 0.07650924; 	sel_arr[77] = 0.07750923; 	sel_arr[78] = 0.07850922; 	sel_arr[79] = 0.07950921; 	
		sel_arr[80] = 0.08050920; 	sel_arr[81] = 0.08150919; 	sel_arr[82] = 0.08250918; 	sel_arr[83] = 0.08350917; 	sel_arr[84] = 0.08450916; 	
		sel_arr[85] = 0.08550915; 	sel_arr[86] = 0.08650914; 	sel_arr[87] = 0.08750913; 	sel_arr[88] = 0.08850912; 	sel_arr[89] = 0.08950911; 	
		sel_arr[90] = 0.09050910; 	sel_arr[91] = 0.09150909; 	sel_arr[92] = 0.09250908; 	sel_arr[93] = 0.09350907; 	sel_arr[94] = 0.09450906; 	
		sel_arr[95] = 0.09550905; 	sel_arr[96] = 0.09650904; 	sel_arr[97] = 0.09750903; 	sel_arr[98] = 0.09850902; 	sel_arr[99] = 0.09950901; 	
		sel_arr[100] = 0.10050900; 	sel_arr[101] = 0.10150899; 	sel_arr[102] = 0.10250898; 	sel_arr[103] = 0.10350897; 	sel_arr[104] = 0.10450896; 	
		sel_arr[105] = 0.10550895; 	sel_arr[106] = 0.10650894; 	sel_arr[107] = 0.10750893; 	sel_arr[108] = 0.10850892; 	sel_arr[109] = 0.10950891; 	
		sel_arr[110] = 0.11050890; 	sel_arr[111] = 0.11150889; 	sel_arr[112] = 0.11250888; 	sel_arr[113] = 0.11350887; 	sel_arr[114] = 0.11450886; 	
		sel_arr[115] = 0.11550885; 	sel_arr[116] = 0.11650884; 	sel_arr[117] = 0.11750883; 	sel_arr[118] = 0.11850882; 	sel_arr[119] = 0.11950881; 	
		sel_arr[120] = 0.12050880; 	sel_arr[121] = 0.12150879; 	sel_arr[122] = 0.12250878; 	sel_arr[123] = 0.12350877; 	sel_arr[124] = 0.12450876; 	
		sel_arr[125] = 0.12550875; 	sel_arr[126] = 0.12650874; 	sel_arr[127] = 0.12750873; 	sel_arr[128] = 0.12850872; 	sel_arr[129] = 0.12950871; 	
		sel_arr[130] = 0.13050870; 	sel_arr[131] = 0.13150869; 	sel_arr[132] = 0.13250868; 	sel_arr[133] = 0.13350867; 	sel_arr[134] = 0.13450866; 	
		sel_arr[135] = 0.13550865; 	sel_arr[136] = 0.13650864; 	sel_arr[137] = 0.13750863; 	sel_arr[138] = 0.13850862; 	sel_arr[139] = 0.13950861; 	
		sel_arr[140] = 0.14050860; 	sel_arr[141] = 0.14150859; 	sel_arr[142] = 0.14250858; 	sel_arr[143] = 0.14350857; 	sel_arr[144] = 0.14450856; 	
		sel_arr[145] = 0.14550855; 	sel_arr[146] = 0.14650854; 	sel_arr[147] = 0.14750853; 	sel_arr[148] = 0.14850852; 	sel_arr[149] = 0.14950851; 	
		sel_arr[150] = 0.15050850; 	sel_arr[151] = 0.15150849; 	sel_arr[152] = 0.15250848; 	sel_arr[153] = 0.15350847; 	sel_arr[154] = 0.15450846; 	
		sel_arr[155] = 0.15550845; 	sel_arr[156] = 0.15650844; 	sel_arr[157] = 0.15750843; 	sel_arr[158] = 0.15850842; 	sel_arr[159] = 0.15950841; 	
		sel_arr[160] = 0.16050840; 	sel_arr[161] = 0.16150839; 	sel_arr[162] = 0.16250838; 	sel_arr[163] = 0.16350837; 	sel_arr[164] = 0.16450836; 	
		sel_arr[165] = 0.16550835; 	sel_arr[166] = 0.16650834; 	sel_arr[167] = 0.16750833; 	sel_arr[168] = 0.16850832; 	sel_arr[169] = 0.16950831; 	
		sel_arr[170] = 0.17050830; 	sel_arr[171] = 0.17150829; 	sel_arr[172] = 0.17250828; 	sel_arr[173] = 0.17350827; 	sel_arr[174] = 0.17450826; 	
		sel_arr[175] = 0.17550825; 	sel_arr[176] = 0.17650824; 	sel_arr[177] = 0.17750823; 	sel_arr[178] = 0.17850822; 	sel_arr[179] = 0.17950821; 	
		sel_arr[180] = 0.18050820; 	sel_arr[181] = 0.18150819; 	sel_arr[182] = 0.18250818; 	sel_arr[183] = 0.18350817; 	sel_arr[184] = 0.18450816; 	
		sel_arr[185] = 0.18550815; 	sel_arr[186] = 0.18650814; 	sel_arr[187] = 0.18750813; 	sel_arr[188] = 0.18850812; 	sel_arr[189] = 0.18950811; 	
		sel_arr[190] = 0.19050810; 	sel_arr[191] = 0.19150809; 	sel_arr[192] = 0.19250808; 	sel_arr[193] = 0.19350807; 	sel_arr[194] = 0.19450806; 	
		sel_arr[195] = 0.19550805; 	sel_arr[196] = 0.19650804; 	sel_arr[197] = 0.19750803; 	sel_arr[198] = 0.19850802; 	sel_arr[199] = 0.19950801; 	
		sel_arr[200] = 0.20050800; 	sel_arr[201] = 0.20150799; 	sel_arr[202] = 0.20250798; 	sel_arr[203] = 0.20350797; 	sel_arr[204] = 0.20450796; 	
		sel_arr[205] = 0.20550795; 	sel_arr[206] = 0.20650794; 	sel_arr[207] = 0.20750793; 	sel_arr[208] = 0.20850792; 	sel_arr[209] = 0.20950791; 	
		sel_arr[210] = 0.21050790; 	sel_arr[211] = 0.21150789; 	sel_arr[212] = 0.21250788; 	sel_arr[213] = 0.21350787; 	sel_arr[214] = 0.21450786; 	
		sel_arr[215] = 0.21550785; 	sel_arr[216] = 0.21650784; 	sel_arr[217] = 0.21750783; 	sel_arr[218] = 0.21850782; 	sel_arr[219] = 0.21950781; 	
		sel_arr[220] = 0.22050780; 	sel_arr[221] = 0.22150779; 	sel_arr[222] = 0.22250778; 	sel_arr[223] = 0.22350777; 	sel_arr[224] = 0.22450776; 	
		sel_arr[225] = 0.22550775; 	sel_arr[226] = 0.22650774; 	sel_arr[227] = 0.22750773; 	sel_arr[228] = 0.22850772; 	sel_arr[229] = 0.22950771; 	
		sel_arr[230] = 0.23050770; 	sel_arr[231] = 0.23150769; 	sel_arr[232] = 0.23250768; 	sel_arr[233] = 0.23350767; 	sel_arr[234] = 0.23450766; 	
		sel_arr[235] = 0.23550765; 	sel_arr[236] = 0.23650764; 	sel_arr[237] = 0.23750763; 	sel_arr[238] = 0.23850762; 	sel_arr[239] = 0.23950761; 	
		sel_arr[240] = 0.24050760; 	sel_arr[241] = 0.24150759; 	sel_arr[242] = 0.24250758; 	sel_arr[243] = 0.24350757; 	sel_arr[244] = 0.24450756; 	
		sel_arr[245] = 0.24550755; 	sel_arr[246] = 0.24650754; 	sel_arr[247] = 0.24750753; 	sel_arr[248] = 0.24850752; 	sel_arr[249] = 0.24950751; 	
		sel_arr[250] = 0.25050750; 	sel_arr[251] = 0.25150749; 	sel_arr[252] = 0.25250748; 	sel_arr[253] = 0.25350747; 	sel_arr[254] = 0.25450746; 	
		sel_arr[255] = 0.25550745; 	sel_arr[256] = 0.25650744; 	sel_arr[257] = 0.25750743; 	sel_arr[258] = 0.25850742; 	sel_arr[259] = 0.25950741; 	
		sel_arr[260] = 0.26050740; 	sel_arr[261] = 0.26150739; 	sel_arr[262] = 0.26250738; 	sel_arr[263] = 0.26350737; 	sel_arr[264] = 0.26450736; 	
		sel_arr[265] = 0.26550735; 	sel_arr[266] = 0.26650734; 	sel_arr[267] = 0.26750733; 	sel_arr[268] = 0.26850732; 	sel_arr[269] = 0.26950731; 	
		sel_arr[270] = 0.27050730; 	sel_arr[271] = 0.27150729; 	sel_arr[272] = 0.27250728; 	sel_arr[273] = 0.27350727; 	sel_arr[274] = 0.27450726; 	
		sel_arr[275] = 0.27550725; 	sel_arr[276] = 0.27650724; 	sel_arr[277] = 0.27750723; 	sel_arr[278] = 0.27850722; 	sel_arr[279] = 0.27950721; 	
		sel_arr[280] = 0.28050720; 	sel_arr[281] = 0.28150719; 	sel_arr[282] = 0.28250718; 	sel_arr[283] = 0.28350717; 	sel_arr[284] = 0.28450716; 	
		sel_arr[285] = 0.28550715; 	sel_arr[286] = 0.28650714; 	sel_arr[287] = 0.28750713; 	sel_arr[288] = 0.28850712; 	sel_arr[289] = 0.28950711; 	
		sel_arr[290] = 0.29050710; 	sel_arr[291] = 0.29150709; 	sel_arr[292] = 0.29250708; 	sel_arr[293] = 0.29350707; 	sel_arr[294] = 0.29450706; 	
		sel_arr[295] = 0.29550705; 	sel_arr[296] = 0.29650704; 	sel_arr[297] = 0.29750703; 	sel_arr[298] = 0.29850702; 	sel_arr[299] = 0.29950701; 	
		sel_arr[300] = 0.30050700; 	sel_arr[301] = 0.30150699; 	sel_arr[302] = 0.30250698; 	sel_arr[303] = 0.30350697; 	sel_arr[304] = 0.30450696; 	
		sel_arr[305] = 0.30550695; 	sel_arr[306] = 0.30650694; 	sel_arr[307] = 0.30750693; 	sel_arr[308] = 0.30850692; 	sel_arr[309] = 0.30950691; 	
		sel_arr[310] = 0.31050690; 	sel_arr[311] = 0.31150689; 	sel_arr[312] = 0.31250688; 	sel_arr[313] = 0.31350687; 	sel_arr[314] = 0.31450686; 	
		sel_arr[315] = 0.31550685; 	sel_arr[316] = 0.31650684; 	sel_arr[317] = 0.31750683; 	sel_arr[318] = 0.31850682; 	sel_arr[319] = 0.31950681; 	
		sel_arr[320] = 0.32050680; 	sel_arr[321] = 0.32150679; 	sel_arr[322] = 0.32250678; 	sel_arr[323] = 0.32350677; 	sel_arr[324] = 0.32450676; 	
		sel_arr[325] = 0.32550675; 	sel_arr[326] = 0.32650674; 	sel_arr[327] = 0.32750673; 	sel_arr[328] = 0.32850672; 	sel_arr[329] = 0.32950671; 	
		sel_arr[330] = 0.33050670; 	sel_arr[331] = 0.33150669; 	sel_arr[332] = 0.33250668; 	sel_arr[333] = 0.33350667; 	sel_arr[334] = 0.33450666; 	
		sel_arr[335] = 0.33550665; 	sel_arr[336] = 0.33650664; 	sel_arr[337] = 0.33750663; 	sel_arr[338] = 0.33850662; 	sel_arr[339] = 0.33950661; 	
		sel_arr[340] = 0.34050660; 	sel_arr[341] = 0.34150659; 	sel_arr[342] = 0.34250658; 	sel_arr[343] = 0.34350657; 	sel_arr[344] = 0.34450656; 	
		sel_arr[345] = 0.34550655; 	sel_arr[346] = 0.34650654; 	sel_arr[347] = 0.34750653; 	sel_arr[348] = 0.34850652; 	sel_arr[349] = 0.34950651; 	
		sel_arr[350] = 0.35050650; 	sel_arr[351] = 0.35150649; 	sel_arr[352] = 0.35250648; 	sel_arr[353] = 0.35350647; 	sel_arr[354] = 0.35450646; 	
		sel_arr[355] = 0.35550645; 	sel_arr[356] = 0.35650644; 	sel_arr[357] = 0.35750643; 	sel_arr[358] = 0.35850642; 	sel_arr[359] = 0.35950641; 	
		sel_arr[360] = 0.36050640; 	sel_arr[361] = 0.36150639; 	sel_arr[362] = 0.36250638; 	sel_arr[363] = 0.36350637; 	sel_arr[364] = 0.36450636; 	
		sel_arr[365] = 0.36550635; 	sel_arr[366] = 0.36650634; 	sel_arr[367] = 0.36750633; 	sel_arr[368] = 0.36850632; 	sel_arr[369] = 0.36950631; 	
		sel_arr[370] = 0.37050630; 	sel_arr[371] = 0.37150629; 	sel_arr[372] = 0.37250628; 	sel_arr[373] = 0.37350627; 	sel_arr[374] = 0.37450626; 	
		sel_arr[375] = 0.37550625; 	sel_arr[376] = 0.37650624; 	sel_arr[377] = 0.37750623; 	sel_arr[378] = 0.37850622; 	sel_arr[379] = 0.37950621; 	
		sel_arr[380] = 0.38050620; 	sel_arr[381] = 0.38150619; 	sel_arr[382] = 0.38250618; 	sel_arr[383] = 0.38350617; 	sel_arr[384] = 0.38450616; 	
		sel_arr[385] = 0.38550615; 	sel_arr[386] = 0.38650614; 	sel_arr[387] = 0.38750613; 	sel_arr[388] = 0.38850612; 	sel_arr[389] = 0.38950611; 	
		sel_arr[390] = 0.39050610; 	sel_arr[391] = 0.39150609; 	sel_arr[392] = 0.39250608; 	sel_arr[393] = 0.39350607; 	sel_arr[394] = 0.39450606; 	
		sel_arr[395] = 0.39550605; 	sel_arr[396] = 0.39650604; 	sel_arr[397] = 0.39750603; 	sel_arr[398] = 0.39850602; 	sel_arr[399] = 0.39950601; 	
		sel_arr[400] = 0.40050600; 	sel_arr[401] = 0.40150599; 	sel_arr[402] = 0.40250598; 	sel_arr[403] = 0.40350597; 	sel_arr[404] = 0.40450596; 	
		sel_arr[405] = 0.40550595; 	sel_arr[406] = 0.40650594; 	sel_arr[407] = 0.40750593; 	sel_arr[408] = 0.40850592; 	sel_arr[409] = 0.40950591; 	
		sel_arr[410] = 0.41050590; 	sel_arr[411] = 0.41150589; 	sel_arr[412] = 0.41250588; 	sel_arr[413] = 0.41350587; 	sel_arr[414] = 0.41450586; 	
		sel_arr[415] = 0.41550585; 	sel_arr[416] = 0.41650584; 	sel_arr[417] = 0.41750583; 	sel_arr[418] = 0.41850582; 	sel_arr[419] = 0.41950581; 	
		sel_arr[420] = 0.42050580; 	sel_arr[421] = 0.42150579; 	sel_arr[422] = 0.42250578; 	sel_arr[423] = 0.42350577; 	sel_arr[424] = 0.42450576; 	
		sel_arr[425] = 0.42550575; 	sel_arr[426] = 0.42650574; 	sel_arr[427] = 0.42750573; 	sel_arr[428] = 0.42850572; 	sel_arr[429] = 0.42950571; 	
		sel_arr[430] = 0.43050570; 	sel_arr[431] = 0.43150569; 	sel_arr[432] = 0.43250568; 	sel_arr[433] = 0.43350567; 	sel_arr[434] = 0.43450566; 	
		sel_arr[435] = 0.43550565; 	sel_arr[436] = 0.43650564; 	sel_arr[437] = 0.43750563; 	sel_arr[438] = 0.43850562; 	sel_arr[439] = 0.43950561; 	
		sel_arr[440] = 0.44050560; 	sel_arr[441] = 0.44150559; 	sel_arr[442] = 0.44250558; 	sel_arr[443] = 0.44350557; 	sel_arr[444] = 0.44450556; 	
		sel_arr[445] = 0.44550555; 	sel_arr[446] = 0.44650554; 	sel_arr[447] = 0.44750553; 	sel_arr[448] = 0.44850552; 	sel_arr[449] = 0.44950551; 	
		sel_arr[450] = 0.45050550; 	sel_arr[451] = 0.45150549; 	sel_arr[452] = 0.45250548; 	sel_arr[453] = 0.45350547; 	sel_arr[454] = 0.45450546; 	
		sel_arr[455] = 0.45550545; 	sel_arr[456] = 0.45650544; 	sel_arr[457] = 0.45750543; 	sel_arr[458] = 0.45850542; 	sel_arr[459] = 0.45950541; 	
		sel_arr[460] = 0.46050540; 	sel_arr[461] = 0.46150539; 	sel_arr[462] = 0.46250538; 	sel_arr[463] = 0.46350537; 	sel_arr[464] = 0.46450536; 	
		sel_arr[465] = 0.46550535; 	sel_arr[466] = 0.46650534; 	sel_arr[467] = 0.46750533; 	sel_arr[468] = 0.46850532; 	sel_arr[469] = 0.46950531; 	
		sel_arr[470] = 0.47050530; 	sel_arr[471] = 0.47150529; 	sel_arr[472] = 0.47250528; 	sel_arr[473] = 0.47350527; 	sel_arr[474] = 0.47450526; 	
		sel_arr[475] = 0.47550525; 	sel_arr[476] = 0.47650524; 	sel_arr[477] = 0.47750523; 	sel_arr[478] = 0.47850522; 	sel_arr[479] = 0.47950521; 	
		sel_arr[480] = 0.48050520; 	sel_arr[481] = 0.48150519; 	sel_arr[482] = 0.48250518; 	sel_arr[483] = 0.48350517; 	sel_arr[484] = 0.48450516; 	
		sel_arr[485] = 0.48550515; 	sel_arr[486] = 0.48650514; 	sel_arr[487] = 0.48750513; 	sel_arr[488] = 0.48850512; 	sel_arr[489] = 0.48950511; 	
		sel_arr[490] = 0.49050510; 	sel_arr[491] = 0.49150509; 	sel_arr[492] = 0.49250508; 	sel_arr[493] = 0.49350507; 	sel_arr[494] = 0.49450506; 	
		sel_arr[495] = 0.49550505; 	sel_arr[496] = 0.49650504; 	sel_arr[497] = 0.49750503; 	sel_arr[498] = 0.49850502; 	sel_arr[499] = 0.49950501; 	
		sel_arr[500] = 0.50050500; 	sel_arr[501] = 0.50150499; 	sel_arr[502] = 0.50250498; 	sel_arr[503] = 0.50350497; 	sel_arr[504] = 0.50450496; 	
		sel_arr[505] = 0.50550495; 	sel_arr[506] = 0.50650494; 	sel_arr[507] = 0.50750493; 	sel_arr[508] = 0.50850492; 	sel_arr[509] = 0.50950491; 	
		sel_arr[510] = 0.51050490; 	sel_arr[511] = 0.51150489; 	sel_arr[512] = 0.51250488; 	sel_arr[513] = 0.51350487; 	sel_arr[514] = 0.51450486; 	
		sel_arr[515] = 0.51550485; 	sel_arr[516] = 0.51650484; 	sel_arr[517] = 0.51750483; 	sel_arr[518] = 0.51850482; 	sel_arr[519] = 0.51950481; 	
		sel_arr[520] = 0.52050480; 	sel_arr[521] = 0.52150479; 	sel_arr[522] = 0.52250478; 	sel_arr[523] = 0.52350477; 	sel_arr[524] = 0.52450476; 	
		sel_arr[525] = 0.52550475; 	sel_arr[526] = 0.52650474; 	sel_arr[527] = 0.52750473; 	sel_arr[528] = 0.52850472; 	sel_arr[529] = 0.52950471; 	
		sel_arr[530] = 0.53050470; 	sel_arr[531] = 0.53150469; 	sel_arr[532] = 0.53250468; 	sel_arr[533] = 0.53350467; 	sel_arr[534] = 0.53450466; 	
		sel_arr[535] = 0.53550465; 	sel_arr[536] = 0.53650464; 	sel_arr[537] = 0.53750463; 	sel_arr[538] = 0.53850462; 	sel_arr[539] = 0.53950461; 	
		sel_arr[540] = 0.54050460; 	sel_arr[541] = 0.54150459; 	sel_arr[542] = 0.54250458; 	sel_arr[543] = 0.54350457; 	sel_arr[544] = 0.54450456; 	
		sel_arr[545] = 0.54550455; 	sel_arr[546] = 0.54650454; 	sel_arr[547] = 0.54750453; 	sel_arr[548] = 0.54850452; 	sel_arr[549] = 0.54950451; 	
		sel_arr[550] = 0.55050450; 	sel_arr[551] = 0.55150449; 	sel_arr[552] = 0.55250448; 	sel_arr[553] = 0.55350447; 	sel_arr[554] = 0.55450445; 	
		sel_arr[555] = 0.55550444; 	sel_arr[556] = 0.55650443; 	sel_arr[557] = 0.55750442; 	sel_arr[558] = 0.55850441; 	sel_arr[559] = 0.55950440; 	
		sel_arr[560] = 0.56050439; 	sel_arr[561] = 0.56150438; 	sel_arr[562] = 0.56250437; 	sel_arr[563] = 0.56350436; 	sel_arr[564] = 0.56450435; 	
		sel_arr[565] = 0.56550434; 	sel_arr[566] = 0.56650433; 	sel_arr[567] = 0.56750432; 	sel_arr[568] = 0.56850431; 	sel_arr[569] = 0.56950430; 	
		sel_arr[570] = 0.57050429; 	sel_arr[571] = 0.57150428; 	sel_arr[572] = 0.57250427; 	sel_arr[573] = 0.57350426; 	sel_arr[574] = 0.57450425; 	
		sel_arr[575] = 0.57550424; 	sel_arr[576] = 0.57650423; 	sel_arr[577] = 0.57750422; 	sel_arr[578] = 0.57850421; 	sel_arr[579] = 0.57950420; 	
		sel_arr[580] = 0.58050419; 	sel_arr[581] = 0.58150418; 	sel_arr[582] = 0.58250417; 	sel_arr[583] = 0.58350416; 	sel_arr[584] = 0.58450415; 	
		sel_arr[585] = 0.58550414; 	sel_arr[586] = 0.58650413; 	sel_arr[587] = 0.58750412; 	sel_arr[588] = 0.58850411; 	sel_arr[589] = 0.58950410; 	
		sel_arr[590] = 0.59050409; 	sel_arr[591] = 0.59150408; 	sel_arr[592] = 0.59250407; 	sel_arr[593] = 0.59350406; 	sel_arr[594] = 0.59450405; 	
		sel_arr[595] = 0.59550404; 	sel_arr[596] = 0.59650403; 	sel_arr[597] = 0.59750402; 	sel_arr[598] = 0.59850401; 	sel_arr[599] = 0.59950400; 	
		sel_arr[600] = 0.60050399; 	sel_arr[601] = 0.60150398; 	sel_arr[602] = 0.60250397; 	sel_arr[603] = 0.60350396; 	sel_arr[604] = 0.60450395; 	
		sel_arr[605] = 0.60550394; 	sel_arr[606] = 0.60650393; 	sel_arr[607] = 0.60750392; 	sel_arr[608] = 0.60850391; 	sel_arr[609] = 0.60950390; 	
		sel_arr[610] = 0.61050389; 	sel_arr[611] = 0.61150388; 	sel_arr[612] = 0.61250387; 	sel_arr[613] = 0.61350386; 	sel_arr[614] = 0.61450385; 	
		sel_arr[615] = 0.61550384; 	sel_arr[616] = 0.61650383; 	sel_arr[617] = 0.61750382; 	sel_arr[618] = 0.61850381; 	sel_arr[619] = 0.61950380; 	
		sel_arr[620] = 0.62050379; 	sel_arr[621] = 0.62150378; 	sel_arr[622] = 0.62250377; 	sel_arr[623] = 0.62350376; 	sel_arr[624] = 0.62450375; 	
		sel_arr[625] = 0.62550374; 	sel_arr[626] = 0.62650373; 	sel_arr[627] = 0.62750372; 	sel_arr[628] = 0.62850371; 	sel_arr[629] = 0.62950370; 	
		sel_arr[630] = 0.63050369; 	sel_arr[631] = 0.63150368; 	sel_arr[632] = 0.63250367; 	sel_arr[633] = 0.63350366; 	sel_arr[634] = 0.63450365; 	
		sel_arr[635] = 0.63550364; 	sel_arr[636] = 0.63650363; 	sel_arr[637] = 0.63750362; 	sel_arr[638] = 0.63850361; 	sel_arr[639] = 0.63950360; 	
		sel_arr[640] = 0.64050359; 	sel_arr[641] = 0.64150358; 	sel_arr[642] = 0.64250357; 	sel_arr[643] = 0.64350356; 	sel_arr[644] = 0.64450355; 	
		sel_arr[645] = 0.64550354; 	sel_arr[646] = 0.64650353; 	sel_arr[647] = 0.64750352; 	sel_arr[648] = 0.64850351; 	sel_arr[649] = 0.64950350; 	
		sel_arr[650] = 0.65050349; 	sel_arr[651] = 0.65150348; 	sel_arr[652] = 0.65250347; 	sel_arr[653] = 0.65350346; 	sel_arr[654] = 0.65450345; 	
		sel_arr[655] = 0.65550344; 	sel_arr[656] = 0.65650343; 	sel_arr[657] = 0.65750342; 	sel_arr[658] = 0.65850341; 	sel_arr[659] = 0.65950340; 	
		sel_arr[660] = 0.66050339; 	sel_arr[661] = 0.66150338; 	sel_arr[662] = 0.66250337; 	sel_arr[663] = 0.66350336; 	sel_arr[664] = 0.66450335; 	
		sel_arr[665] = 0.66550334; 	sel_arr[666] = 0.66650333; 	sel_arr[667] = 0.66750332; 	sel_arr[668] = 0.66850331; 	sel_arr[669] = 0.66950330; 	
		sel_arr[670] = 0.67050329; 	sel_arr[671] = 0.67150328; 	sel_arr[672] = 0.67250327; 	sel_arr[673] = 0.67350326; 	sel_arr[674] = 0.67450325; 	
		sel_arr[675] = 0.67550324; 	sel_arr[676] = 0.67650323; 	sel_arr[677] = 0.67750322; 	sel_arr[678] = 0.67850321; 	sel_arr[679] = 0.67950320; 	
		sel_arr[680] = 0.68050319; 	sel_arr[681] = 0.68150318; 	sel_arr[682] = 0.68250317; 	sel_arr[683] = 0.68350316; 	sel_arr[684] = 0.68450315; 	
		sel_arr[685] = 0.68550314; 	sel_arr[686] = 0.68650313; 	sel_arr[687] = 0.68750312; 	sel_arr[688] = 0.68850311; 	sel_arr[689] = 0.68950310; 	
		sel_arr[690] = 0.69050309; 	sel_arr[691] = 0.69150308; 	sel_arr[692] = 0.69250307; 	sel_arr[693] = 0.69350306; 	sel_arr[694] = 0.69450305; 	
		sel_arr[695] = 0.69550304; 	sel_arr[696] = 0.69650303; 	sel_arr[697] = 0.69750302; 	sel_arr[698] = 0.69850301; 	sel_arr[699] = 0.69950300; 	
		sel_arr[700] = 0.70050299; 	sel_arr[701] = 0.70150298; 	sel_arr[702] = 0.70250297; 	sel_arr[703] = 0.70350296; 	sel_arr[704] = 0.70450295; 	
		sel_arr[705] = 0.70550294; 	sel_arr[706] = 0.70650293; 	sel_arr[707] = 0.70750292; 	sel_arr[708] = 0.70850291; 	sel_arr[709] = 0.70950290; 	
		sel_arr[710] = 0.71050289; 	sel_arr[711] = 0.71150288; 	sel_arr[712] = 0.71250287; 	sel_arr[713] = 0.71350286; 	sel_arr[714] = 0.71450285; 	
		sel_arr[715] = 0.71550284; 	sel_arr[716] = 0.71650283; 	sel_arr[717] = 0.71750282; 	sel_arr[718] = 0.71850281; 	sel_arr[719] = 0.71950280; 	
		sel_arr[720] = 0.72050279; 	sel_arr[721] = 0.72150278; 	sel_arr[722] = 0.72250277; 	sel_arr[723] = 0.72350276; 	sel_arr[724] = 0.72450275; 	
		sel_arr[725] = 0.72550274; 	sel_arr[726] = 0.72650273; 	sel_arr[727] = 0.72750272; 	sel_arr[728] = 0.72850271; 	sel_arr[729] = 0.72950270; 	
		sel_arr[730] = 0.73050269; 	sel_arr[731] = 0.73150268; 	sel_arr[732] = 0.73250267; 	sel_arr[733] = 0.73350266; 	sel_arr[734] = 0.73450265; 	
		sel_arr[735] = 0.73550264; 	sel_arr[736] = 0.73650263; 	sel_arr[737] = 0.73750262; 	sel_arr[738] = 0.73850261; 	sel_arr[739] = 0.73950260; 	
		sel_arr[740] = 0.74050259; 	sel_arr[741] = 0.74150258; 	sel_arr[742] = 0.74250257; 	sel_arr[743] = 0.74350256; 	sel_arr[744] = 0.74450255; 	
		sel_arr[745] = 0.74550254; 	sel_arr[746] = 0.74650253; 	sel_arr[747] = 0.74750252; 	sel_arr[748] = 0.74850251; 	sel_arr[749] = 0.74950250; 	
		sel_arr[750] = 0.75050249; 	sel_arr[751] = 0.75150248; 	sel_arr[752] = 0.75250247; 	sel_arr[753] = 0.75350246; 	sel_arr[754] = 0.75450245; 	
		sel_arr[755] = 0.75550244; 	sel_arr[756] = 0.75650243; 	sel_arr[757] = 0.75750242; 	sel_arr[758] = 0.75850241; 	sel_arr[759] = 0.75950240; 	
		sel_arr[760] = 0.76050239; 	sel_arr[761] = 0.76150238; 	sel_arr[762] = 0.76250237; 	sel_arr[763] = 0.76350236; 	sel_arr[764] = 0.76450235; 	
		sel_arr[765] = 0.76550234; 	sel_arr[766] = 0.76650233; 	sel_arr[767] = 0.76750232; 	sel_arr[768] = 0.76850231; 	sel_arr[769] = 0.76950230; 	
		sel_arr[770] = 0.77050229; 	sel_arr[771] = 0.77150228; 	sel_arr[772] = 0.77250227; 	sel_arr[773] = 0.77350226; 	sel_arr[774] = 0.77450225; 	
		sel_arr[775] = 0.77550224; 	sel_arr[776] = 0.77650223; 	sel_arr[777] = 0.77750222; 	sel_arr[778] = 0.77850221; 	sel_arr[779] = 0.77950220; 	
		sel_arr[780] = 0.78050219; 	sel_arr[781] = 0.78150218; 	sel_arr[782] = 0.78250217; 	sel_arr[783] = 0.78350216; 	sel_arr[784] = 0.78450215; 	
		sel_arr[785] = 0.78550214; 	sel_arr[786] = 0.78650213; 	sel_arr[787] = 0.78750212; 	sel_arr[788] = 0.78850211; 	sel_arr[789] = 0.78950210; 	
		sel_arr[790] = 0.79050209; 	sel_arr[791] = 0.79150208; 	sel_arr[792] = 0.79250207; 	sel_arr[793] = 0.79350206; 	sel_arr[794] = 0.79450205; 	
		sel_arr[795] = 0.79550204; 	sel_arr[796] = 0.79650203; 	sel_arr[797] = 0.79750202; 	sel_arr[798] = 0.79850201; 	sel_arr[799] = 0.79950200; 	
		sel_arr[800] = 0.80050199; 	sel_arr[801] = 0.80150198; 	sel_arr[802] = 0.80250197; 	sel_arr[803] = 0.80350196; 	sel_arr[804] = 0.80450195; 	
		sel_arr[805] = 0.80550194; 	sel_arr[806] = 0.80650193; 	sel_arr[807] = 0.80750192; 	sel_arr[808] = 0.80850191; 	sel_arr[809] = 0.80950190; 	
		sel_arr[810] = 0.81050189; 	sel_arr[811] = 0.81150188; 	sel_arr[812] = 0.81250187; 	sel_arr[813] = 0.81350186; 	sel_arr[814] = 0.81450185; 	
		sel_arr[815] = 0.81550184; 	sel_arr[816] = 0.81650183; 	sel_arr[817] = 0.81750182; 	sel_arr[818] = 0.81850181; 	sel_arr[819] = 0.81950180; 	
		sel_arr[820] = 0.82050179; 	sel_arr[821] = 0.82150178; 	sel_arr[822] = 0.82250177; 	sel_arr[823] = 0.82350176; 	sel_arr[824] = 0.82450175; 	
		sel_arr[825] = 0.82550174; 	sel_arr[826] = 0.82650173; 	sel_arr[827] = 0.82750172; 	sel_arr[828] = 0.82850171; 	sel_arr[829] = 0.82950170; 	
		sel_arr[830] = 0.83050169; 	sel_arr[831] = 0.83150168; 	sel_arr[832] = 0.83250167; 	sel_arr[833] = 0.83350166; 	sel_arr[834] = 0.83450165; 	
		sel_arr[835] = 0.83550164; 	sel_arr[836] = 0.83650163; 	sel_arr[837] = 0.83750162; 	sel_arr[838] = 0.83850161; 	sel_arr[839] = 0.83950160; 	
		sel_arr[840] = 0.84050159; 	sel_arr[841] = 0.84150158; 	sel_arr[842] = 0.84250157; 	sel_arr[843] = 0.84350156; 	sel_arr[844] = 0.84450155; 	
		sel_arr[845] = 0.84550154; 	sel_arr[846] = 0.84650153; 	sel_arr[847] = 0.84750152; 	sel_arr[848] = 0.84850151; 	sel_arr[849] = 0.84950150; 	
		sel_arr[850] = 0.85050149; 	sel_arr[851] = 0.85150148; 	sel_arr[852] = 0.85250147; 	sel_arr[853] = 0.85350146; 	sel_arr[854] = 0.85450145; 	
		sel_arr[855] = 0.85550144; 	sel_arr[856] = 0.85650143; 	sel_arr[857] = 0.85750142; 	sel_arr[858] = 0.85850141; 	sel_arr[859] = 0.85950140; 	
		sel_arr[860] = 0.86050139; 	sel_arr[861] = 0.86150138; 	sel_arr[862] = 0.86250137; 	sel_arr[863] = 0.86350136; 	sel_arr[864] = 0.86450135; 	
		sel_arr[865] = 0.86550134; 	sel_arr[866] = 0.86650133; 	sel_arr[867] = 0.86750132; 	sel_arr[868] = 0.86850131; 	sel_arr[869] = 0.86950130; 	
		sel_arr[870] = 0.87050129; 	sel_arr[871] = 0.87150128; 	sel_arr[872] = 0.87250127; 	sel_arr[873] = 0.87350126; 	sel_arr[874] = 0.87450125; 	
		sel_arr[875] = 0.87550124; 	sel_arr[876] = 0.87650123; 	sel_arr[877] = 0.87750122; 	sel_arr[878] = 0.87850121; 	sel_arr[879] = 0.87950120; 	
		sel_arr[880] = 0.88050119; 	sel_arr[881] = 0.88150118; 	sel_arr[882] = 0.88250117; 	sel_arr[883] = 0.88350116; 	sel_arr[884] = 0.88450115; 	
		sel_arr[885] = 0.88550114; 	sel_arr[886] = 0.88650113; 	sel_arr[887] = 0.88750112; 	sel_arr[888] = 0.88850111; 	sel_arr[889] = 0.88950110; 	
		sel_arr[890] = 0.89050109; 	sel_arr[891] = 0.89150108; 	sel_arr[892] = 0.89250107; 	sel_arr[893] = 0.89350106; 	sel_arr[894] = 0.89450105; 	
		sel_arr[895] = 0.89550104; 	sel_arr[896] = 0.89650103; 	sel_arr[897] = 0.89750102; 	sel_arr[898] = 0.89850101; 	sel_arr[899] = 0.89950100; 	
		sel_arr[900] = 0.90050099; 	sel_arr[901] = 0.90150098; 	sel_arr[902] = 0.90250097; 	sel_arr[903] = 0.90350096; 	sel_arr[904] = 0.90450095; 	
		sel_arr[905] = 0.90550094; 	sel_arr[906] = 0.90650093; 	sel_arr[907] = 0.90750092; 	sel_arr[908] = 0.90850091; 	sel_arr[909] = 0.90950090; 	
		sel_arr[910] = 0.91050089; 	sel_arr[911] = 0.91150088; 	sel_arr[912] = 0.91250087; 	sel_arr[913] = 0.91350086; 	sel_arr[914] = 0.91450085; 	
		sel_arr[915] = 0.91550084; 	sel_arr[916] = 0.91650083; 	sel_arr[917] = 0.91750082; 	sel_arr[918] = 0.91850081; 	sel_arr[919] = 0.91950080; 	
		sel_arr[920] = 0.92050079; 	sel_arr[921] = 0.92150078; 	sel_arr[922] = 0.92250077; 	sel_arr[923] = 0.92350076; 	sel_arr[924] = 0.92450075; 	
		sel_arr[925] = 0.92550074; 	sel_arr[926] = 0.92650073; 	sel_arr[927] = 0.92750072; 	sel_arr[928] = 0.92850071; 	sel_arr[929] = 0.92950070; 	
		sel_arr[930] = 0.93050069; 	sel_arr[931] = 0.93150068; 	sel_arr[932] = 0.93250067; 	sel_arr[933] = 0.93350066; 	sel_arr[934] = 0.93450065; 	
		sel_arr[935] = 0.93550064; 	sel_arr[936] = 0.93650063; 	sel_arr[937] = 0.93750062; 	sel_arr[938] = 0.93850061; 	sel_arr[939] = 0.93950060; 	
		sel_arr[940] = 0.94050059; 	sel_arr[941] = 0.94150058; 	sel_arr[942] = 0.94250057; 	sel_arr[943] = 0.94350056; 	sel_arr[944] = 0.94450055; 	
		sel_arr[945] = 0.94550054; 	sel_arr[946] = 0.94650053; 	sel_arr[947] = 0.94750052; 	sel_arr[948] = 0.94850051; 	sel_arr[949] = 0.94950050; 	
		sel_arr[950] = 0.95050049; 	sel_arr[951] = 0.95150048; 	sel_arr[952] = 0.95250047; 	sel_arr[953] = 0.95350046; 	sel_arr[954] = 0.95450045; 	
		sel_arr[955] = 0.95550044; 	sel_arr[956] = 0.95650043; 	sel_arr[957] = 0.95750042; 	sel_arr[958] = 0.95850041; 	sel_arr[959] = 0.95950040; 	
		sel_arr[960] = 0.96050039; 	sel_arr[961] = 0.96150038; 	sel_arr[962] = 0.96250037; 	sel_arr[963] = 0.96350036; 	sel_arr[964] = 0.96450035; 	
		sel_arr[965] = 0.96550034; 	sel_arr[966] = 0.96650033; 	sel_arr[967] = 0.96750032; 	sel_arr[968] = 0.96850031; 	sel_arr[969] = 0.96950030; 	
		sel_arr[970] = 0.97050029; 	sel_arr[971] = 0.97150028; 	sel_arr[972] = 0.97250027; 	sel_arr[973] = 0.97350026; 	sel_arr[974] = 0.97450025; 	
		sel_arr[975] = 0.97550024; 	sel_arr[976] = 0.97650023; 	sel_arr[977] = 0.97750022; 	sel_arr[978] = 0.97850021; 	sel_arr[979] = 0.97950020; 	
		sel_arr[980] = 0.98050019; 	sel_arr[981] = 0.98150018; 	sel_arr[982] = 0.98250017; 	sel_arr[983] = 0.98350016; 	sel_arr[984] = 0.98450015; 	
		sel_arr[985] = 0.98550014; 	sel_arr[986] = 0.98650013; 	sel_arr[987] = 0.98750012; 	sel_arr[988] = 0.98850011; 	sel_arr[989] = 0.98950010; 	
		sel_arr[990] = 0.99050009; 	sel_arr[991] = 0.99150008; 	sel_arr[992] = 0.99250007; 	sel_arr[993] = 0.99350006; 	sel_arr[994] = 0.99450005; 	
		sel_arr[995] = 0.99550004; 	sel_arr[996] = 0.99650003; 	sel_arr[997] = 0.99750002; 	sel_arr[998] = 0.99850001; 	sel_arr[999] = 0.99950000; 	

		
		//PrintWriter writer = new PrintWriter(apktPath+"cost.txt", "UTF-8");
		double costarray[] = new double[1000];
		for(int i=0;i<1000;i++){
			double sel [] = new double [dimension];
			sel[0] = sel_arr[i];
			sel[1] = 0.9;
			System.out.println(i);
			//writer.println(i+","+getFPCCost(sel, -1)+","+getFPCCost(sel, 16)+","+getFPCCost(sel, 17)+","+getFPCCost(sel, 18)+","+getFPCCost(sel, 14));
			//writer.println(i+","+OptimalCost[loc]);
			costarray[i] = getFPCCost(sel, 16);
			
		}
		double prevratio = 1;
		double violatioCnt =0;
		for(int i=1; i < 1000; i++){
			double ratio = (costarray[i]-costarray[i-1]);
			if((prevratio*1.0001 < ratio) && (i >1)){
				
				System.out.println("i = "+i+": cost[i-2] = "+costarray[i-2]+" cost[i-1] = "+costarray[i-1]+"cost[i] = "+costarray[i]+" prevratio = "+prevratio+" ratio = "+ratio);
				
				violatioCnt ++;
			}
			prevratio = ratio;
		}
			
		System.out.println("violation cnt is "+violatioCnt);
		//writer.close();
		
	}

	private void writeCostToFileOld() {
		
		
		
		try{
		    PrintWriter writer = new PrintWriter(apktPath+"cost.txt", "UTF-8");
		    for(int i=0;i<resolution;i++){
		    	int arr [] = new int [dimension];
		    	arr[0] = i;
		    	arr[1] = 99;
		    	int loc = getIndex(arr, resolution);
		    	writer.println(i+","+OptimalCost[loc]+","+fpc_cost_generic(arr, 16)+","+fpc_cost_generic(arr, 17)+","+fpc_cost_generic(arr, 18)+","+fpc_cost_generic(arr, 14));
		    	//writer.println(i+","+OptimalCost[loc]);
		    }
		    writer.close();
		} catch (IOException e) {
		   // do something
		}
		
	}

private void writeFPCCostFile(int plan) {
		
		
		
		try{
		    PrintWriter writer = new PrintWriter(apktPath+"cost"+plan+".txt", "UTF-8");
		    for(int i=0;i<resolution;i++){
		    	int arr [] = new int [dimension];
		    	arr[0] = i;
		    	arr[1] = 15;
		    	int loc = getIndex(arr, resolution);
		    	writer.println(i+","+fpc_cost_generic(arr, plan));
		    }
		    writer.close();
		} catch (IOException e) {
		   // do something
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
