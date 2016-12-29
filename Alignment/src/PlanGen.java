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
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
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
	static Connection conn = null;
	static int database_conn=1;
	static double h_cost;
	static int leafid=0;
	static plan[] plans_list;
	//static DataValues [] data = new DataValues[totalPoints];
	
	public static void main(String[] args) throws IOException, PicassoException, SQLException {
		// TODO Auto-generated method stub

		//NativeSubOptimality obj = new NativeSubOptimality();
		PlanGen obj = new PlanGen();
		
		
		obj.loadPropertiesFile();
		String pktPath = apktPath + qtName + ".apkt" ;
		String pktPath_new = apktPath + qtName + "_new9.4.apkt";
		//obj.validateApktFiles(pktPath, pktPath_new);
		//System.exit(1);
		//System.out.println("Query Template: "+QTName);


		ADiagramPacket gdp = obj.getGDP(new File(pktPath));
		dimension = gdp.getDimension();
		resolution = gdp.getMaxResolution();
		totalPoints = (int) Math.pow(resolution, dimension);
		//obj.readpkt(gdp, false);
		obj.loadPropertiesFile();
		obj.loadSelectivity();
		
		
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
//		for(int p=0;p<totalPlans;p++)
//			if(p==0 || p == 2 || p==7 || p ==29 || p == 30 || p==78 || p ==154 || p==169)
//				obj.getOptimalLocationsforPlan(p);
		data_new = new DataValues[totalPoints];
		for ( int i=0; i< totalPoints;i++){
			//Plan p = obj.getNativePlan_84(i);
			Plan p = obj.getNativePlan(i);
			double cost = p.getCost();
			int p_no = p.getPlanNo();
			//Put into the apkt packet.
			data_new[i] = new DataValues();
			data_new[i].setCost(cost);
			data_new[i].setPlanNumber(p_no);
		}
		gdp.setMaxPlanNumber(plans_vector.size());
		gdp.setDataPoints(data_new);
		//Write the new apkt file
		//ADP.setPlans(plans);
		//ADP.setMaxPlanNumber(plans.size());
		try
		{
//			String fName = PicassoConstants.SAVE_PATH + "packets"+ System.getProperty("file.separator")	+ sqp.getQueryName() + ".apkt";
			String fName = apktPath + qtName  + "_new9.4.apkt";		
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
			varyingJoins = prop.getProperty("varyingJoins");

			JS_multiplier = new double[dimension];
			for(int d=0;d<dimension;d++){
				String multiplierStr = new String("JS_multiplier"+(d+1));
				JS_multiplier[d] = Double.parseDouble(prop.getProperty(multiplierStr));
			}

			//Settings:Note dont forget to put analyze here
			//query = "explain analyze FPC(\"lineitem\") (\"104949\")  select	supp_nation,	cust_nation,	l_year,	volume from	(select n1.n_name as supp_nation, n2.n_name as cust_nation, 	DATE_PART('YEAR',l_shipdate) as l_year,	l_extendedprice * (1 - l_discount) as volume	from	supplier, lineitem, orders, 	customer, nation n1,	nation n2 where s_suppkey = l_suppkey	and o_orderkey = l_orderkey and c_custkey = o_custkey		and s_nationkey = n1.n_nationkey and c_nationkey = n2.n_nationkey	and  c_acctbal<=10000 and l_extendedprice<=22560 ) as temp";
			//query = "explain analyze FPC(\"catalog_sales\")  (\"150.5\") select ca_zip, cs_sales_price from catalog_sales,customer,customer_address,date_dim where cs_bill_customer_sk = c_customer_sk and c_current_addr_sk = ca_address_sk and cs_sold_date_sk = d_date_sk and ca_gmt_offset <= -7.0   and d_year <= 1900  and cs_list_price <= 150.5";
			query = prop.getProperty("query");
			query_opt_spill = prop.getProperty("query_opt_spill");
			select_query = prop.getProperty("select_query");
			predicates= prop.getProperty("predicates");

			cardinalityPath = prop.getProperty("cardinalityPath");


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
	
	
	public Plan getNativePlan(int loc) throws PicassoException, IOException {


		System.out.println(loc);
//		if(loc==159840)
//			System.out.println("interesting");

		Vector textualPlan = new Vector();
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
			System.out.println(exp_query);
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
			//System.out.println("coming here");
			while(rs.next())  {
				textualPlan.add(rs.getString(1)); 
			}
			rs.close();
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
//		if(loc==17478)
//			System.out.println("interesting");
File plansFile = new File(apktPath+"planStructure_new");
		
		String[] myFiles;  
		int nativePlan = -1;
		if(plansFile.isDirectory() && false){
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
					}
				}
			}
		}
		
		
		//plan = database.getPlan(newQuery,query);
		String planDiffLevel = "SUB-OPERATOR";
		plan.computeHash(planDiffLevel);
		//int planNumber = nativePlan;
		int planNumber = plan.getIndexInVector(plans_vector);                  // see if the plan is new or already seen	
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
				Statement stmt_xml = conn.createStatement();
				ResultSet rs_xml = stmt_xml.executeQuery(xml_query);
				while(rs_xml.next()){
					fw_xml.write(rs_xml.getString(1));
				}
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
		//System.out.println("\nthe total plans are "+totalPlans+" with dimensions "+dimension+" and resolution "+resolution);
		int plan_no;
		this.loadPlans();

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
			plans_list[plan_no].addPoint(i);

			//cat = plans_list[plan_no].getcategory(remainingDim);
		}
		
		int  [] plan_count = new int[totalPlans];
		for(int p=0;p<data.length;p++){
			plan_count[plans[p]]++;
		}
		//printing the above
		for(int p=0;p<plan_count.length;p++){
			System.out.println("Plan "+p+" has "+plan_count[p]+" points");
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

					ObjectInputStream ip = new ObjectInputStream(new FileInputStream(new File(apktPath + i + ".pcst")));
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



