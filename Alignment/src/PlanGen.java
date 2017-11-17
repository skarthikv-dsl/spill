import iisc.dsl.picasso.common.PicassoConstants;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.postgresql.jdbc3.Jdbc3PoolingDataSource;


	public class PlanGen {
	
	
	static double AllPlanCosts[][];
	static Vector<Plan> plans_vector = new Vector<Plan>();
	static int nPlans;
	static int plans[];
	static double OptimalCost[];
	static int totalPlans;
	static int dimension;
	static int resolution;
	static DataValues[] data;
	static DataValues[] data_new;
	static int totalPoints;
	static double selectivity[];
	static String apktPath;
	static String qtName ;
	static Jdbc3PoolingDataSource source;
	static String varyingJoins;
	static double JS_multiplier [];
	static String query;
	static String query_opt_spill;
	static String select_query;
	static String predicates;
	static String cardinalityPath;
	static int sel_distribution; 
	static boolean FROM_CLAUSE;
	Connection conn = null;
	static int database_conn=1;
	static double h_cost;
	int leafid=0;
	static plan[] plans_list;
	static boolean planstructure_format = true;
	static boolean single_thread = false;
	static int num_of_usable_threads;
	//static DataValues [] data = new DataValues[totalPoints];
	
	public static void main(String[] args) throws IOException, PicassoException, SQLException {
		// TODO Auto-generated method stub

		//NativeSubOptimality obj = new NativeSubOptimality();
		PlanGen obj = new PlanGen();
		
		
		obj.loadPropertiesFile();
		String pktPath = apktPath + qtName + "_new9.4_megh.apkt" ;
		String pktPath_new = apktPath + qtName + "_new9.4_parallel.apkt";
		//obj.validateApktFiles(pktPath, pktPath_new);
		//System.exit(1);
		//System.out.println("Query Template: "+QTName);


		ADiagramPacket gdp = obj.getGDP(new File(pktPath));
		dimension = gdp.getDimension();
		resolution = gdp.getMaxResolution();
		totalPoints = (int) Math.pow(resolution, dimension);
		obj.readpkt(gdp, false);
		obj.loadPropertiesFile();
		obj.loadSelectivity();
		
		num_of_usable_threads = (int) ( Runtime.getRuntime().availableProcessors()*1.0);
		try{
			System.out.println("entered DB conn1");
			source = new Jdbc3PoolingDataSource();
			source.setDataSourceName("A Data Source");
			File f_marwa = new File("/home/dsladmin/marwa");
			
			//Settings
			//System.out.println("entered DB conn2");
			if(database_conn==0){
//				conn = DriverManager
//						.getConnection("jdbc:postgresql://localhost:5431/tpch-ai",
//								"sa", "database");
			}
			else{
			
				if(f_marwa.exists() && !f_marwa.isDirectory()) { 
					System.out.println("entered DB tpcds");
//					conn = DriverManager
//							.getConnection("jdbc:postgresql://localhost:5431/tpcds-ai",
//									"sa", "database");
					source.setServerName("localhost:5431");
					source.setDatabaseName("tpcds-ai");
				}
				else{
				System.out.println("entered DB tpcds");
//				conn = DriverManager
//						.getConnection("jdbc:postgresql://localhost:5432/tpcds-ai",
//								"sa", "database");
				source.setServerName("localhost:5432");
				source.setDatabaseName("tpcds-ai");
				}
			}
			source.setUser("sa");
			source.setPassword("database");
			
			if(single_thread)
				source.setMaxConnections(1);
			else
				source.setMaxConnections(num_of_usable_threads);
			
			System.out.println("Opened database successfully");
		}
		catch ( Exception e ) {
			System.out.println("entered DB err");
			System.err.println( e.getClass().getName()+": "+ e.getMessage() );
		}
//		for(int p=0;p<totalPlans;p++)
//			if(p==0 || p == 2 || p==7 || p ==29 || p == 30 || p==78 || p ==154 || p==169)
//				obj.getOptimalLocationsforPlan(p);
		data_new = new DataValues[totalPoints];
		
		if(single_thread){
			obj.conn = source.getConnection();
			
			for ( int i=0; i< totalPoints;i++){
				//Plan p = obj.getNativePlan_84(i);
				Plan p = obj.getNativePlan(i);
				double cost = p.getCost();
				int p_no = p.getPlanNo();
				//Put into the apkt packet.
				data_new[i] = new DataValues();
				data_new[i].setCost(cost);
				data_new[i].setPlanNumber(p_no);
				System.out.println(i+","+(int)cost+","+p.getHash());
			}
			
			if (obj.conn != null) {
				try { obj.conn.close(); } catch (SQLException e) {}
			}
		}
		else{
			obj.getPlanGenParallel();
		}
		gdp.setMaxPlanNumber(totalPlans);
		gdp.setDataPoints(data_new);
		//Write the new apkt file
		//ADP.setPlans(plans);
		//ADP.setMaxPlanNumber(plans.size());
		try
		{
//			String fName = PicassoConstants.SAVE_PATH + "packets"+ System.getProperty("file.separator")	+ sqp.getQueryName() + ".apkt";
			String fName = apktPath + qtName  + "_new9.4_parallel.apkt";		
			FileOutputStream fis = new FileOutputStream (fName);
			ObjectOutputStream ois;				
			ois = new ObjectOutputStream (fis);			
			ois.writeObject(gdp);			
			ois.flush();
			ois.close();
		}
		catch(Exception ex)
		{
			System.out.println(ex.getMessage());
			ex.printStackTrace();
		}		
		
		ADiagramPacket gdp_new = obj.getGDP(new File(pktPath_new));
		obj.readpkt(gdp_new, false);
		/*
		int nativeplan = obj.getNativePlan();
		obj.clearCache();
		long t1 = System.currentTimeMillis();
		//obj.executePlan(nativeplan);
		long t2 = System.currentTimeMillis();
		System.out.println("Time taken (in secs) by native optimizer chosen plan is "+(t2-t1)/1000);
		long min_time = (t2-t1)/1000;
		for(int p=100;p<101;p++){
			if(p!=nativeplan){
				t1 = System.currentTimeMillis();
				//obj.executePlan(p);
				t2 = System.currentTimeMillis();
				System.out.println("Time taken (in secs) by plan "+p+" is "+(t2-t1)/1000);
				if(min_time < (t2-t1)/1000){
					min_time = (t2-t1)/1000;
					System.out.println("Current minimum Time taken (in secs) by plan "+p+"  is "+min_time);
				}
				obj.clearCache();
			}
		}
			*/

	}

	public void getPlanGenParallel() throws SQLException {

		
		System.out.println("Number of Usable threads are : "+num_of_usable_threads + " with contour locs size "+totalPoints);
		
		// 1. Divide the contour_locs into usable threads-1
		
		int step_size = totalPoints/num_of_usable_threads;
		int cur_min_val = 0;
		int cur_max_val =  0;
		
		ArrayList<PlanGeninputParamStruct> inputs = new ArrayList<PlanGeninputParamStruct>();
		for (int j = 0; j < num_of_usable_threads ; ++j) {

			cur_min_val = cur_max_val;
			cur_max_val = cur_min_val + step_size ;
			
			if(j==num_of_usable_threads-1 || (totalPoints < num_of_usable_threads))
				cur_max_val = totalPoints;

			PlanGeninputParamStruct input = new PlanGeninputParamStruct(source, cur_min_val, cur_max_val - 1,this);
			inputs.add(input);
			
			System.out.println(cur_min_val+"-"+(cur_max_val-1));
			
			if(totalPoints < num_of_usable_threads)
				break;
		}
		
		//System.out.println("after spltting");
		// 2. execute them in parallel
		ExecutorService service = Executors.newFixedThreadPool(num_of_usable_threads);
	    ArrayList<Future<PlanGenOutputParamStruct>> futures = new ArrayList<Future<PlanGenOutputParamStruct>>();
	    		
	     for (final PlanGeninputParamStruct input : inputs) {
	        Callable<PlanGenOutputParamStruct> callable = new Callable<PlanGenOutputParamStruct>() {
	            public PlanGenOutputParamStruct call() throws Exception {
	            	PlanGenOutputParamStruct output = new PlanGenOutputParamStruct();
	            	//System.out.println("before getting the connection");
	            	
	            	input.getLocationDetails(apktPath, qtName, select_query, predicates, dimension, database_conn);
	            	
	            	//System.out.println("after getting the connection");
	            	output.hm = input.hm;
            		return output;
	            }
	        };
		       futures.add(service.submit(callable));			   
			    
		}
	     service.shutdown();
	     //System.out.println("after shutdown of service"); 
	     //3. aggregating the results back
	     ArrayList<location> returning_locs = new ArrayList<location>();
	     HashMap<Long, Integer> planHashMap = new HashMap<Long, Integer>();
	     for (Future<PlanGenOutputParamStruct> future : futures) {
	    	 
		    	try {
		    		PlanGenOutputParamStruct output = future.get();
		    		
		    		for(int key : output.hm.keySet()){
		    			long hash_val = output.hm.get(key).hash_val;
		    			int plan_number = -1;
						if(!planHashMap.containsKey(hash_val)){
//							if(true){
								plan_number = planHashMap.size();
								planHashMap.put(hash_val,plan_number);
						}
						else{
								plan_number = planHashMap.get(hash_val);
							}
						data_new[key] = new DataValues();
						data_new[key].setCost(output.hm.get(key).cost);
						data_new[key].setPlanNumber(plan_number);
						//System.out.println("Location = "+key+" with cost = "+output.hm.get(key).cost+" plan no = "+plan_number);
		    		}

				} catch (InterruptedException | ExecutionException e) {
					
					e.printStackTrace();
				}
		    	
		    	
	     }
	     
	     totalPlans = planHashMap.size();
	     //System.out.println("after aggregation");
	     
	     if (conn != null) {
				try { conn.close(); } catch (SQLException e) {}
			}
	     
	  
	     return ;//for safety check
	} 

	
	public  void getOptimalLocationsforPlan(int p) {
	
		int loc = plans_list[p].plan_locs.get(0);
		System.out.print("\nCoordinates for plan "+p+" are ");
		int coordinates[] = getCoordinates(dimension, resolution, loc); 
		for(int i=0;i<dimension;i++)
			System.out.print(selectivity[coordinates[i]]+",");
		
	}

	public void validateApktFiles(String old_path, String new_path) throws IOException{
		ADiagramPacket old_gdp = getGDP(new File(old_path));
		ADiagramPacket new_gdp = getGDP(new File(new_path));
		DataValues[] old_data = old_gdp.getData();
		DataValues[] new_data = new_gdp.getData();
		PlanGen obj = new PlanGen();
		obj.readpkt(new_gdp, false);
		System.exit(1);
		double diff=0;
		int cnt_1pc =0;
		int cnt_10pc =0;
		int cnt_50pc =0;
		int cnt_100pc =0;
		double pc = 0.0;
		for(int i = 0;i<old_data.length;i++){
			diff = Math.abs(old_data[i].getCost() - new_data[i].getCost());
			pc = diff*100/old_data[i].getCost();
			System.out.println("percent error = "+pc);
			if(pc > 10){
				cnt_10pc++;
			}
			if(pc > 1){
				cnt_1pc++;
			}
			if(pc > 100){
				cnt_100pc++;
			}
			if(pc > 50){
				cnt_50pc++;
			}
		}
		System.out.println("Number of locations with error more than 1% = "+cnt_1pc);
		System.out.println("Number of locations with error more than 10% = "+cnt_10pc);
		System.out.println("Number of locations with error more than 50% = "+cnt_50pc);
		System.out.println("Number of locations with error more than 100% = "+cnt_100pc);
	}
	
	double executePlan(int plan) throws SQLException{
		
		int loc = plans_list[plan].plan_locs.get(0);
		int coordinates[] = getCoordinates(dimension, resolution, loc); 
		
		//conn = source.getConnection();
		Statement stmt = conn.createStatement();
		stmt.execute("set work_mem = '100MB'");
		//NOTE	,Settings: 4GB for DS and 1GB for H
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
		stmt.execute("set full_robustness = on");
		stmt.execute("set time_limit = 200538");
		//stmt.execute("set spill_optimization = off");
		stmt.execute("set varyingJoins = "+varyingJoins);
		//stmt.execute("set spill_node = -1");
		for(int d=0;d<dimension;d++){
			stmt.execute("set JS_multiplier"+(d+1)+ "= "+ JS_multiplier[d]);
			stmt.execute("set robust_eqjoin_selec"+(d+1)+ "= "+ selectivity[coordinates[d]]);
			// essentially forcing the  plan optimal at (x,y) location to the query having (x_a,y_a) 
			// as selectivities been injected 
		}
		String exp_ana_query = new String("explain analyze "+"select cc_call_center_id , cc_name , cc_manager , sum(cr_net_loss) from call_center,catalog_returns, date_dim, customer, customer_address, customer_demographics, household_demographics where cr_call_center_sk = cc_call_center_sk and cr_returned_date_sk     = d_date_sk and cr_returning_customer_sk=c_customer_sk and cd_demo_sk = c_current_cdemo_sk and hd_demo_sk = c_current_hdemo_sk and ca_address_sk = c_current_addr_sk and d_year = 2000  and d_moy = 12 and ( (cd_marital_status = 'M' and cd_education_status     = 'Unknown') or(cd_marital_status = 'W' and cd_education_status = 'Advanced Degree')) and hd_buy_potential like '5001-10000%' and ca_gmt_offset = -7 group by cc_call_center_id,cc_name,cc_manager,cd_marital_status,cd_education_status order by sum(cr_net_loss) desc"); 
		ResultSet rs = stmt.executeQuery(exp_ana_query);
		while(rs.next()){
        	
			System.out.println(rs.getString(1));
		}
		return 0.0;
		
	}
	
public void clearCache(){

	
			String[] cmd = {
		
				"/bin/bash",
				"-c",
				"echo 3 | sudo tee /proc/sys/vm/drop_caches"
		};
		
			
		boolean success = false;
		boolean freed = false; //freed enough buffers
		Process p;
		int iterations_required = 0;
		int no_of_buffers = Integer.MAX_VALUE;
		try 
		{
			while(!success || !freed){
			Runtime r = Runtime.getRuntime();

			p = r.exec(cmd);
			iterations_required ++;
			p.waitFor();
			BufferedReader reader = 
					new BufferedReader(new InputStreamReader(p.getInputStream()));

			String line = "";
			line = reader.readLine();
			if ((line)!= null) 
			{
				if(line.equals("3"))
				{
					success = true;
				}
				//System.out.println(line);
			}
		
//			p = r.exec(start);
//			p.waitFor();

			if(!success)
				continue;
			 r = Runtime.getRuntime();
			String freeCmd = new String("free");
			p = r.exec(freeCmd);
			 reader = 
					new BufferedReader(new InputStreamReader(p.getInputStream()));
			if(reader.readLine() != null){
				line = "";
				if((line = reader.readLine())!= null){
					String [] strSplit = line.split(" ");
					int[] free_command_results = new int[6];
					//the fifth element in this gives the no of buffers
					int it =0;
					for(String st : strSplit){
						if(st.trim().matches(".*\\d+.*"))
							free_command_results[it++] = Integer.parseInt(st);
					}
					//System.out.println("the strSplit is "+strSplit);
					no_of_buffers = free_command_results[4];
					System.out.println("the no of buffers "+no_of_buffers);
					if(no_of_buffers<350){
						freed = true;
					}
				}
					
			}
			}	
			System.out.println("no of iterations required to finally achieve  "+no_of_buffers+" is "+iterations_required);
			
		} catch (Exception e) 
		{
			e.printStackTrace();
		}
		
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

public Plan getNativePlan_84(int loc) throws SQLException {
	int coordinates[] = getCoordinates(dimension, resolution, loc); 
	Vector textualPlan = new Vector();
	Plan plan = new Plan();
	//conn = source.getConnection();
	Statement stmt = conn.createStatement();
	stmt.execute("set work_mem = '100MB'");
	//NOTE	,Settings: 4GB for DS and 1GB for H
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
	stmt.execute("set full_robustness = on");
	stmt.execute("set time_limit = 200538");
	//stmt.execute("set spill_optimization = off");
	stmt.execute("set varyingJoins = "+varyingJoins);
	//stmt.execute("set spill_node = -1");
	for(int d=0;d<dimension;d++){
		stmt.execute("set JS_multiplier"+(d+1)+ "= "+ JS_multiplier[d]);
		stmt.execute("set robust_eqjoin_selec"+(d+1)+ "= "+ selectivity[coordinates[d]]);
		// essentially forcing the  plan optimal at (x,y) location to the query having (x_a,y_a) 
		// as selectivities been injected 
	}
	String exp_ana_query = new String("explain analyze "+"select cc_call_center_id , cc_name , cc_manager , sum(cr_net_loss) from call_center,catalog_returns, date_dim, customer, customer_address, customer_demographics, household_demographics where cr_call_center_sk = cc_call_center_sk and cr_returned_date_sk     = d_date_sk and cr_returning_customer_sk=c_customer_sk and cd_demo_sk = c_current_cdemo_sk and hd_demo_sk = c_current_hdemo_sk and ca_address_sk = c_current_addr_sk and d_year = 2000  and d_moy = 12 and ( (cd_marital_status = 'M' and cd_education_status     = 'Unknown') or(cd_marital_status = 'W' and cd_education_status = 'Advanced Degree')) and hd_buy_potential like '5001-10000%' and ca_gmt_offset = -7 group by cc_call_center_id,cc_name,cc_manager,cd_marital_status,cd_education_status order by sum(cr_net_loss) desc"); 
	ResultSet rs = stmt.executeQuery(exp_ana_query);
	while(rs.next())  {
		textualPlan.add(rs.getString(1)); 
	}
	rs.close();
	stmt.close();
	if(textualPlan.size()<=0)
		return null;
	String str = (String)textualPlan.remove(0);
	CreateNode(plan, str, 0, -1);
	//plan.isOptimal = true;
	FindChilds(plan, 0, 1, textualPlan, 2);
	//if(PicassoConstants.saveExtraPlans == false ||  PicassoConstants.topkquery == false)
	SwapSORTChilds(plan);
	
	//plan = database.getPlan(newQuery,query);
	String planDiffLevel = "1";
	plan.computeHash(planDiffLevel);
	int planNumber;
	planNumber = plan.getIndexInVector(plans_vector);                  // see if the plan is new or already seen	

	if(planNumber == -1) {
		plans_vector.add(plan);
		planNumber=plans_vector.size() - 1;
		plan.setPlanNo(planNumber);
	
		//Store the result in path
	
	String path = apktPath+"planStructure_new/"+plan.getPlanNo()+".txt";
	File fnative=new File(path);
	try {
		
		FileWriter fw = new FileWriter(fnative, false);   //overwrites if the file already exists
		for(int n=0;n<plan.getSize();n++){
			Node node = plan.getNode(n);
			if(node!=null && node.getId()>=0)
				fw.write(node.getId()+","+node.getParentId()+","+node.getName()+","+node.getPredicate()+"\n");
		}


		fw.close();
		

	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} 
	}

	return plan;
}
	
	
	public Plan getNativePlan(int loc) throws PicassoException, IOException, SQLException {

		

		Vector textualPlan = new Vector();
		StringBuilder XML_Plan = new StringBuilder();
		Plan plan = new Plan();
		String xml_query = null;
		int  index [] = new int[dimension] ;
		index = getCoordinates(dimension, resolution, loc);
		try{      	
			Statement stmt = conn.createStatement();
			String exp_query = new String("Selectivity ( "+predicates+ ") ( ");
			for(int i=0;i<dimension;i++){
				if(i !=dimension-1){
					exp_query = exp_query + (selectivity[index[i]])+ ", ";
				}
				else{
					exp_query = exp_query + (selectivity[index[i]]) + " ) ";
				}
			}
			exp_query = exp_query + select_query;
			xml_query = "explain (format xml) "+ exp_query ;
			exp_query = "explain " + exp_query ;
			//String exp_query = new String(query_opt_spill);
			//System.out.println(exp_query);
//			stmt.execute("set work_mem = '100MB'");
//			//NOTE,Settings: 4GB for DS and 1GB for H
//			if(database_conn==0){
//				stmt.execute("set effective_cache_size='1GB'");
//			}
//			else{
//				stmt.execute("set effective_cache_size='4GB'");
//			}

			//NOTE,Settings: need not set the page cost's
//			stmt.execute("set  enable_hashjoin = off");
//			//stmt.execute("set  enable_mergejoin = off");
//			stmt.execute("set  enable_nestloop = off");
//			stmt.execute("set  enable_indexscan = off");
//			stmt.execute("set  enable_bitmapscan = off");
//			//stmt.execute("set  enable_seqscan = off");
//			stmt.execute("set  seq_page_cost = 1");
//			stmt.execute("set  random_page_cost=4");
//			stmt.execute("set cpu_operator_cost=0.0025");
//			stmt.execute("set cpu_index_tuple_cost=0.005");
//			stmt.execute("set cpu_tuple_cost=0.01");
			
			ResultSet rs = stmt.executeQuery(exp_query);
			//System.out.println("coming here");
			while(rs.next())  {
				textualPlan.add(rs.getString(1)); 
			}
			
			if(textualPlan.size()<=0)
				return null;
			
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
		
		
		//Write temp_plan
		String path = apktPath+"planStructure_new/tempPlan.txt";
		File fnative=new File(path);
		try {
			
			FileWriter fw = new FileWriter(fnative, false);   //overwrites if the file already exists
			for(int n=0;n<plan.getSize();n++){
				Node node = plan.getNode(n);
				if(node!=null && node.getId()>=0)
					fw.write(node.getId()+","+node.getParentId()+","+node.getName()+","+node.getPredicate()+"\n");
			}


			fw.close();
			

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
//		if(loc==17478)
//			System.out.println("interesting");
File plansFile = new File(apktPath+"planStructure_new");
		
		String[] myFiles;  
		int nativePlan = -1;
		boolean directoryComparison = false;
		if(plansFile.isDirectory() && directoryComparison){
			myFiles = plansFile.list();
			for (int hi=0; hi<myFiles.length; hi++) {
				//File myFile = new File(plansFile, myFiles[hi]);
				if(!myFiles[hi].contains("tempPlan")){
					Path nativePlanpath = Paths.get(apktPath+"planStructure_new/tempPlan.txt");
					Path otherPlanspath = Paths.get(apktPath+"planStructure_new/"+myFiles[hi]);
					byte[] f1 = Files.readAllBytes(nativePlanpath);
					byte[] f2 = Files.readAllBytes(otherPlanspath);
					boolean flag =Arrays.equals(f1, f2);
					if(flag){
						System.out.println("file "+myFiles[hi]);
						StringTokenizer st = new StringTokenizer(myFiles[hi],".");  
					     if (st.hasMoreTokens()) {  
					         nativePlan = Integer.parseInt(st.nextToken().trim());  
					         System.out.println("Native Plan = "+nativePlan);
					     }  
					     break;
					}
				}
			}
		}
		
		int planNumber = -1;
		//plan = database.getPlan(newQuery,query);
		String planDiffLevel = "SUB-OPERATOR";
		if(!directoryComparison){
			plan.computeHash(planDiffLevel);
			planNumber = plan.getIndexInVector(plans_vector);                  // see if the plan is new or already seen
		}
		else{
			 planNumber = nativePlan;
		}
			
		plan.setPlanNo(planNumber);
		if(planNumber == -1) {
			plans_vector.add(plan);
			planNumber=plans_vector.size() - 1;
			plan.setPlanNo(planNumber);
			System.out.println("Loc = "+loc+", Plan = "+planNumber);
			//Dump xml plan
			String xml_path = apktPath+"planStructureXML/"+plan.getPlanNo()+".xml";
			File xml_file =new File(xml_path);
			//Execute the xml query
			
			try{
				FileWriter fw_xml = new FileWriter(xml_file, false); 
				//Statement stmt_xml = conn.createStatement();
				//ResultSet rs_xml = stmt_xml.executeQuery(xml_query);
				//while(rs_xml.next()){
					//fw_xml.write(rs_xml.getString(1));
				//}
				fw_xml.write(XML_Plan.toString());
				fw_xml.close();
			}
			catch(Exception e){
				e.printStackTrace();
				ServerMessageUtil.SPrintToConsole("Cannot get plan from postgres: "+e);
				throw new PicassoException("Error getting plan: "+e);
			}
			//Store the result in path
			
			
		
		path = apktPath+"planStructure_new/"+plan.getPlanNo()+".txt";
		fnative=new File(path);
		try {
			
			FileWriter fw = new FileWriter(fnative, false);   //overwrites if the file already exists
			for(int n=0;n<plan.getSize();n++){
				Node node = plan.getNode(n);
				if(node!=null && node.getId()>=0)
					fw.write(node.getId()+","+node.getParentId()+","+node.getName()+","+node.getPredicate()+"\n");
			}


			fw.close();
			

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		}
	
	

		return plan;
		
	}
	
	public Plan getNativePlanOperators(int loc, Operators op) throws PicassoException, IOException {



		Vector textualPlan = new Vector();
		StringBuilder XML_Plan = new StringBuilder();
		Plan plan = new Plan();
		String xml_query = null;
		int  index [] = new int[dimension] ;
		index = getCoordinates(dimension, resolution, loc);
		try{      	
			Statement stmt = conn.createStatement();
			String exp_query = new String("Selectivity ( "+predicates+ ") ( ");
			for(int i=0;i<dimension;i++){
				if(i !=dimension-1){
					exp_query = exp_query + (selectivity[index[i]])+ ", ";
				}
				else{
					exp_query = exp_query + (selectivity[index[i]]) + " ) ";
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
			
			switch (op){
				case  BitmapScan: 	
					stmt.execute("set  enable_indexscan = off");
					stmt.execute("set  enable_seqscan = off");
					break;
				case  SeqScan: 
					stmt.execute("set  enable_indexscan = off");
					stmt.execute("set  enable_bitmapscan = off");
					break;
				case  IndexScan: 
					stmt.execute("set  enable_bitmapscan = off");
					stmt.execute("set  enable_seqscan = off");
					break;
				case  Sort: break;
				case  HashJoinBitmapIndexScan: 
					stmt.execute("set  enable_mergejoin = off");
					stmt.execute("set  enable_nestloop = off");
					stmt.execute("set  enable_seqscan = off");
					break;
				case  HashJoinSeqScan: 
					stmt.execute("set  enable_mergejoin = off");
					stmt.execute("set  enable_nestloop = off");
					stmt.execute("set  enable_indexscan = off");
					stmt.execute("set  enable_bitmapscan = off");
					break;
				case  MergeJoinSeqScan: 
					stmt.execute("set  enable_hashjoin = off");
					stmt.execute("set  enable_nestloop = off");
					stmt.execute("set  enable_indexscan = off");
					stmt.execute("set  enable_bitmapscan = off");
					break;
				case  MergeJoinIndexScan: 
					stmt.execute("set  enable_hashjoin = off");
					stmt.execute("set  enable_nestloop = off");
					stmt.execute("set  enable_seqscan = off");
					stmt.execute("set  enable_bitmapscan = off");
					break;
				case  MergeJoinBitmapScan: 
					stmt.execute("set  enable_hashjoin = off");
					stmt.execute("set  enable_nestloop = off");
					stmt.execute("set  enable_indexscan = off");
					stmt.execute("set  enable_bitmapscan = off");
					break;
				case  NestedLoopSeqScan:
					stmt.execute("set  enable_hashjoin = off");
					stmt.execute("set  enable_mergejoin = off");
					stmt.execute("set  enable_indexscan = off");
					stmt.execute("set  enable_bitmapscan = off");
					break;
				case  NestedLoopIndexScan: 
					stmt.execute("set  enable_hashjoin = off");
					stmt.execute("set  enable_mergejoin = off");
					stmt.execute("set  enable_seqscan = off");
					stmt.execute("set  enable_bitmapscan = off");
					break;
				case  NestedLoopBitmapScan: 
					stmt.execute("set  enable_hashjoin = off");
					stmt.execute("set  enable_mergejoin = off");
					stmt.execute("set  enable_seqscan = off");
					stmt.execute("set  enable_indexscan = off");
					break;
				default: break;	
				
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
			
			ResultSet rs_xml = stmt.executeQuery(xml_query);
			
			while(rs_xml.next())  {
				XML_Plan.append(rs_xml.getString(1)); 
			}
	
			
			
			rs.close();
			rs_xml.close();
			
		//reenabling the operators
			
			switch (op){
			case  BitmapScan: 	
				stmt.execute("set  enable_indexscan = on");
				stmt.execute("set  enable_seqscan = on");
				break;
			case  SeqScan: 
				stmt.execute("set  enable_indexscan = on");
				stmt.execute("set  enable_bitmapscan = on");
				break;
			case  IndexScan: 
				stmt.execute("set  enable_bitmapscan = on");
				stmt.execute("set  enable_seqscan = on");
				break;
			case  Sort: break;
			case  HashJoinBitmapIndexScan: 
				stmt.execute("set  enable_mergejoin = on");
				stmt.execute("set  enable_nestloop = on");
				stmt.execute("set  enable_seqscan = on");
				break;
			case  HashJoinSeqScan: 
				stmt.execute("set  enable_mergejoin = on");
				stmt.execute("set  enable_nestloop = on");
				stmt.execute("set  enable_indexscan = on");
				stmt.execute("set  enable_bitmapscan = on");
				break;
			case  MergeJoinSeqScan: 
				stmt.execute("set  enable_hashjoin = on");
				stmt.execute("set  enable_nestloop = on");
				stmt.execute("set  enable_indexscan = on");
				stmt.execute("set  enable_bitmapscan = on");
				break;
			case  MergeJoinIndexScan: 
				stmt.execute("set  enable_hashjoin = on");
				stmt.execute("set  enable_nestloop = on");
				stmt.execute("set  enable_seqscan = on");
				stmt.execute("set  enable_bitmapscan = on");
				break;
			case  MergeJoinBitmapScan: 
				stmt.execute("set  enable_hashjoin = on");
				stmt.execute("set  enable_nestloop = on");
				stmt.execute("set  enable_indexscan = on");
				stmt.execute("set  enable_bitmapscan = on");
				break;
			case  NestedLoopSeqScan:
				stmt.execute("set  enable_hashjoin = on");
				stmt.execute("set  enable_mergejoin = on");
				stmt.execute("set  enable_indexscan = on");
				stmt.execute("set  enable_bitmapscan = on");
				break;
			case  NestedLoopIndexScan: 
				stmt.execute("set  enable_hashjoin = on");
				stmt.execute("set  enable_mergejoin = on");
				stmt.execute("set  enable_seqscan = on");
				stmt.execute("set  enable_bitmapscan = on");
				break;
			case  NestedLoopBitmapScan: 
				stmt.execute("set  enable_hashjoin = on");
				stmt.execute("set  enable_mergejoin = on");
				stmt.execute("set  enable_seqscan = on");
				stmt.execute("set  enable_indexscan = on");
				break;
			default: break;	
			
		}
			
			stmt.close();
			if(textualPlan.size()<=0)
				return null;
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
		
		
		//Write temp_plan
		String path = apktPath+"planStructure_new/tempPlan.txt";
		File fnative=new File(path);
		try {
			
			FileWriter fw = new FileWriter(fnative, false);   //overwrites if the file already exists
			for(int n=0;n<plan.getSize();n++){
				Node node = plan.getNode(n);
				if(node!=null && node.getId()>=0)
					fw.write(node.getId()+","+node.getParentId()+","+node.getName()+","+node.getPredicate()+"\n");
			}


			fw.close();
			

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

		
		File plansFile = new File(apktPath+"planStructure_new");
		
		String[] myFiles;  
		int nativePlan = -1;
		boolean directoryComparison = true;
		if(plansFile.isDirectory() && directoryComparison){
			myFiles = plansFile.list();
			for (int hi=0; hi<myFiles.length; hi++) {
				//File myFile = new File(plansFile, myFiles[hi]);
				if(!myFiles[hi].contains("tempPlan")){
					Path nativePlanpath = Paths.get(apktPath+"planStructure_new/tempPlan.txt");
					Path otherPlanspath = Paths.get(apktPath+"planStructure_new/"+myFiles[hi]);
					byte[] f1 = Files.readAllBytes(nativePlanpath);
					byte[] f2 = Files.readAllBytes(otherPlanspath);
					boolean flag =Arrays.equals(f1, f2);
					if(flag){
						System.out.println("file "+myFiles[hi]);
						StringTokenizer st = new StringTokenizer(myFiles[hi],".");  
					     if (st.hasMoreTokens()) {  
					         nativePlan = Integer.parseInt(st.nextToken().trim());  
					         System.out.println("Native Plan = "+nativePlan);
					     }  
					     break;
					}
				}
			}
		}
		
		int planNumber = -1;
		//plan = database.getPlan(newQuery,query);
		String planDiffLevel = "SUB-OPERATOR";
		if(!directoryComparison){
			plan.computeHash(planDiffLevel);
			planNumber = plan.getIndexInVector(plans_vector);                  // see if the plan is new or already seen
		}
		else{
			 planNumber = nativePlan;
		}
		
			
		plan.setPlanNo(planNumber);
		if(planNumber == -1) {
			plans_vector.add(plan);
			planNumber=plans_vector.size() - 1;
			plan.setPlanNo(planNumber);
			System.out.println("Loc = "+loc+", Plan = "+planNumber);
			//Dump xml plan
			String xml_path = apktPath+"planStructureXML/"+plan.getPlanNo()+".xml";
			File xml_file =new File(xml_path);
			//Execute the xml query
			try{
				FileWriter fw_xml = new FileWriter(xml_file, false); 
				//Statement stmt_xml = conn.createStatement();
				//ResultSet rs_xml = stmt_xml.executeQuery(xml_query);
				//while(rs_xml.next()){
					//fw_xml.write(rs_xml.getString(1));
				//}
				fw_xml.write(XML_Plan.toString());
				fw_xml.close();
			}
			
			catch(Exception e){
				e.printStackTrace();
				ServerMessageUtil.SPrintToConsole("Cannot get plan from postgres: "+e);
				throw new PicassoException("Error getting plan: "+e);
			}
			//Store the result in path
			
			
		if(planstructure_format){
		path = apktPath+"planStructure_new/"+plan.getPlanNo()+".txt";
		fnative=new File(path);
		try {
			
			FileWriter fw = new FileWriter(fnative, false);   //overwrites if the file already exists
			for(int n=0;n<plan.getSize();n++){
				Node node = plan.getNode(n);
				if(node!=null && node.getId()>=0)
					fw.write(node.getId()+","+node.getParentId()+","+node.getName()+","+node.getPredicate()+"\n");
			}


			fw.close();
			

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		}
		}
	

		return plan;
		
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

	void loadPlans() throws NumberFormatException, IOException
	{

		plans_list = new plan[totalPlans];
		for(int i=0;i<totalPlans;i++)
		{
			plans_list[i]=new plan(i);
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
		System.out.println("\nthe total plans are "+totalPlans+" with dimensions "+dimension+" and resolution "+resolution);

		
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
//				this was used for spillbound				
//				selectivity[0] = 0.000064; 	selectivity[1] = 0.000093; 	selectivity[2] = 0.000126; 	selectivity[3] = 0.000161; 	selectivity[4] = 0.000198;
//				selectivity[5] = 0.000239; 	selectivity[6] = 0.000284; 	selectivity[7] = 0.000332; 	selectivity[8] = 0.000384; 	selectivity[9] = 0.000440;
//				selectivity[10] = 0.000501; 	selectivity[11] = 0.000567; 	selectivity[12] = 0.000638; 	selectivity[13] = 0.000716; 	selectivity[14] = 0.000800;
//				selectivity[15] = 0.000890; 	selectivity[16] = 0.000989; 	selectivity[17] = 0.001095; 	selectivity[18] = 0.001211; 	selectivity[19] = 0.001335;
//				selectivity[20] = 0.001471; 	selectivity[21] = 0.001617; 	selectivity[22] = 0.001776; 	selectivity[23] = 0.001948; 	selectivity[24] = 0.002134;
//				selectivity[25] = 0.002335; 	selectivity[26] = 0.002554; 	selectivity[27] = 0.002790; 	selectivity[28] = 0.003046; 	selectivity[29] = 0.003323;
//				selectivity[30] = 0.003624; 	selectivity[31] = 0.003949; 	selectivity[32] = 0.004301; 	selectivity[33] = 0.004683; 	selectivity[34] = 0.005096;
//				selectivity[35] = 0.005543; 	selectivity[36] = 0.006028; 	selectivity[37] = 0.006552; 	selectivity[38] = 0.007121; 	selectivity[39] = 0.007736;
//				selectivity[40] = 0.008403; 	selectivity[41] = 0.009125; 	selectivity[42] = 0.009907; 	selectivity[43] = 0.010753; 	selectivity[44] = 0.011670;
//				selectivity[45] = 0.012663; 	selectivity[46] = 0.013739; 	selectivity[47] = 0.014904; 	selectivity[48] = 0.016165; 	selectivity[49] = 0.017531;
//				selectivity[50] = 0.019011; 	selectivity[51] = 0.020613; 	selectivity[52] = 0.022348; 	selectivity[53] = 0.024228; 	selectivity[54] = 0.026263;
//				selectivity[55] = 0.028467; 	selectivity[56] = 0.030854; 	selectivity[57] = 0.033440; 	selectivity[58] = 0.036240; 	selectivity[59] = 0.039272;
//				selectivity[60] = 0.042556; 	selectivity[61] = 0.046113; 	selectivity[62] = 0.049965; 	selectivity[63] = 0.054136; 	selectivity[64] = 0.058654;
//				selectivity[65] = 0.063547; 	selectivity[66] = 0.068845; 	selectivity[67] = 0.074584; 	selectivity[68] = 0.080799; 	selectivity[69] = 0.087530;
//				selectivity[70] = 0.094819; 	selectivity[71] = 0.102714; 	selectivity[72] = 0.111263; 	selectivity[73] = 0.120523; 	selectivity[74] = 0.130550;
//				selectivity[75] = 0.141411; 	selectivity[76] = 0.153172; 	selectivity[77] = 0.165910; 	selectivity[78] = 0.179705; 	selectivity[79] = 0.194645;
//				selectivity[80] = 0.210825; 	selectivity[81] = 0.228348; 	selectivity[82] = 0.247325; 	selectivity[83] = 0.267877; 	selectivity[84] = 0.290136;
//				selectivity[85] = 0.314241; 	selectivity[86] = 0.340348; 	selectivity[87] = 0.368621; 	selectivity[88] = 0.399241; 	selectivity[89] = 0.432403;
//				selectivity[90] = 0.468316; 	selectivity[91] = 0.507211; 	selectivity[92] = 0.549334; 	selectivity[93] = 0.594953; 	selectivity[94] = 0.644359;
//				selectivity[95] = 0.697865; 	selectivity[96] = 0.755812; 	selectivity[97] = 0.818569; 	selectivity[98] = 0.886535; 	selectivity[99] = 0.990142;

				//this is used for onlinePB
				selectivity[0] = 0.0001035f; 	selectivity[1] = 0.0001111f; 	selectivity[2] = 0.0001194f; 	selectivity[3] = 0.0001286f; 	selectivity[4] = 0.0001387f; 	
				selectivity[5] = 0.0001499f; 	selectivity[6] = 0.0001621f; 	selectivity[7] = 0.0001756f; 	selectivity[8] = 0.0001904f; 	selectivity[9] = 0.0002067f; 	
				selectivity[10] = 0.0002246f; 	selectivity[11] = 0.0002443f; 	selectivity[12] = 0.0002660f; 	selectivity[13] = 0.0002899f; 	selectivity[14] = 0.0003161f; 	
				selectivity[15] = 0.0003450f; 	selectivity[16] = 0.0003767f; 	selectivity[17] = 0.0004117f; 	selectivity[18] = 0.0004501f; 	selectivity[19] = 0.0004924f; 	
				selectivity[20] = 0.0005389f; 	selectivity[21] = 0.0005900f; 	selectivity[22] = 0.0006463f; 	selectivity[23] = 0.0007081f; 	selectivity[24] = 0.0007762f; 	
				selectivity[25] = 0.0008511f; 	selectivity[26] = 0.0009334f; 	selectivity[27] = 0.0010240f; 	selectivity[28] = 0.0011237f; 	selectivity[29] = 0.0012333f; 	
				selectivity[30] = 0.0013539f; 	selectivity[31] = 0.0014866f; 	selectivity[32] = 0.0016325f; 	selectivity[33] = 0.0017930f; 	selectivity[34] = 0.0019695f; 	
				selectivity[35] = 0.0021638f; 	selectivity[36] = 0.0023774f; 	selectivity[37] = 0.0026124f; 	selectivity[38] = 0.0028709f; 	selectivity[39] = 0.0031552f; 	
				selectivity[40] = 0.0034680f; 	selectivity[41] = 0.0038121f; 	selectivity[42] = 0.0041905f; 	selectivity[43] = 0.0046068f; 	selectivity[44] = 0.0050648f; 	
				selectivity[45] = 0.0055685f; 	selectivity[46] = 0.0061226f; 	selectivity[47] = 0.0067321f; 	selectivity[48] = 0.0074026f; 	selectivity[49] = 0.0081401f; 	
				selectivity[50] = 0.0089514f; 	selectivity[51] = 0.0098438f; 	selectivity[52] = 0.0108254f; 	selectivity[53] = 0.0119052f; 	selectivity[54] = 0.0130930f; 	
				selectivity[55] = 0.0143996f; 	selectivity[56] = 0.0158368f; 	selectivity[57] = 0.0174177f; 	selectivity[58] = 0.0191567f; 	selectivity[59] = 0.0210696f; 	
				selectivity[60] = 0.0231739f; 	selectivity[61] = 0.0254885f; 	selectivity[62] = 0.0280346f; 	selectivity[63] = 0.0308353f; 	selectivity[64] = 0.0339161f; 	
				selectivity[65] = 0.0373050f; 	selectivity[66] = 0.0410328f; 	selectivity[67] = 0.0451333f; 	selectivity[68] = 0.0496439f; 	selectivity[69] = 0.0546055f; 	
				selectivity[70] = 0.0600633f; 	selectivity[71] = 0.0660669f; 	selectivity[72] = 0.0726709f; 	selectivity[73] = 0.0799352f; 	selectivity[74] = 0.0879260f; 	
				selectivity[75] = 0.0967158f; 	selectivity[76] = 0.1063847f; 	selectivity[77] = 0.1170204f; 	selectivity[78] = 0.1287197f; 	selectivity[79] = 0.1415889f; 	
				selectivity[80] = 0.1557451f; 	selectivity[81] = 0.1713168f; 	selectivity[82] = 0.1884458f; 	selectivity[83] = 0.2072876f; 	selectivity[84] = 0.2280136f; 	
				selectivity[85] = 0.2508122f; 	selectivity[86] = 0.2758907f; 	selectivity[87] = 0.3034770f; 	selectivity[88] = 0.3338220f; 	selectivity[89] = 0.3672014f; 	
				selectivity[90] = 0.4039188f; 	selectivity[91] = 0.4443080f; 	selectivity[92] = 0.4887360f; 	selectivity[93] = 0.5376069f; 	selectivity[94] = 0.5913649f; 	
				selectivity[95] = 0.6504986f; 	selectivity[96] = 0.7155457f; 	selectivity[97] = 0.7870975f; 	selectivity[98] = 0.8658045f; 	selectivity[99] = 0.9523823f; 	

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


	class plan{
		ArrayList<Integer> order;
		int category;
		int value;
		ArrayList<Integer> plan_locs;

		plan(int p_no) throws NumberFormatException, IOException{
			plan_locs = new ArrayList <Integer>();
			order =  new ArrayList<Integer>();

			FileReader file = new FileReader(PlanGen.apktPath+"predicateOrder/"+p_no+".txt");

			BufferedReader br = new BufferedReader(file);
			String s;
			while((s = br.readLine()) != null) {
				//System.out.println(Integer.parseInt(s));
				// If the number is the remaining dimensions then only add it to the order

				value = Integer.parseInt(s);
				//System.out.println("\nvalue="+value+"\n");
				order.add(value);
			}
			category = order.get(0);
			br.close();
			file.close();
		}
		void addPoint(int loc)
		{
			this.plan_locs.add(loc);
			return;
		}
		ArrayList<Integer> getPoints()
		{
			return this.plan_locs;
		}
		int getcategory(ArrayList<Integer> remainingDim){
			int len = order.size();
			int cur_dim;
			for(int i=0;i<len;i++)
			{
				cur_dim = order.get(i);
				if(remainingDim.contains(cur_dim))
				{
					//category=cur_dim;
					//return category;
					return(cur_dim);
				}
			}
			System.out.println("\nError in getcategory\n");
			return -1;

		}
	}
	

}

class CostHashValue{
		double cost;
		long hash_val;
		
		CostHashValue(double cost, long hv){
			this.cost = cost;
			this.hash_val = hv;
		}
}
	
	
	class PlanGeninputParamStruct {
		
		Jdbc3PoolingDataSource source;
		String apktPath, qtName, select_query, predicates;
		HashMap<Integer,CostHashValue> hm = new HashMap<Integer,CostHashValue>();
		int dimension, database_conn;
		int min_val, max_val;
		 Connection conn;
		 PlanGen pg;
		 
		public PlanGeninputParamStruct( Jdbc3PoolingDataSource source, int min_val, int max_val, PlanGen obj) throws SQLException {
			this.source = source;
			this.min_val = min_val;
			this.max_val = max_val;
			pg = obj;
		}
		
		public void getLocationDetails(String apktPath, String qtName, String select_query, String predicates, int dimension, int database_conn) throws SQLException, IOException, PicassoException{
			if(apktPath!= null && qtName!= null && predicates!= null && select_query!=null){ 
				this.apktPath = apktPath;
				this.qtName = qtName;
				this.select_query = select_query;
				this.predicates = predicates;
				this.dimension = dimension;
				this.database_conn = database_conn;
			}
				
			conn = source.getConnection();
			
			for(int i =min_val; i<= max_val; i++) {
				
				int sel_int_arr [] =  pg.getCoordinates(dimension, pg.resolution, i);
				double sel_arr [] = new double[dimension];
				for(int j=0; j < dimension; j++)
					sel_arr[j] = pg.selectivity[sel_int_arr[j]];
				location loc = new location(sel_arr, pg, conn);
				CostHashValue chv = new CostHashValue(loc.opt_cost,loc.plan.getHash());
				System.out.println(i+","+(int)chv.cost+","+chv.hash_val);
				hm.put(i, chv);
			}
			
			if (conn != null) {
				try { conn.close(); } catch (SQLException e) {}
			}
		}
		
		
	}
	
	class PlanGenOutputParamStruct {
		HashMap<Integer,CostHashValue> hm;
	}


