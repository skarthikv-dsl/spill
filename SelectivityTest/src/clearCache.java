import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;


public class clearCache {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		clearCache obj = new clearCache();
		//obj.run();
		obj.selectivityGen();
	}
	private void selectivityGen() {
		// TODO Auto-generated method stub
		//	System.out.println("\n **********************************************************Resolution = "+resolution);
			double sel;

			
			double r = 1.0;
			double diff =0;
			
			double QDIST_SKEW_10 = 2.0;
			double QDIST_SKEW_30 = 1.33;
			double QDIST_SKEW_100 = 1.083;
			double QDIST_SKEW_300 = 1.027;
			double QDIST_SKEW_1000 = 1.00808;
			
			/*parameters to change*/
			int option=1;
			int resolution = 100;
			double startpoint = 0.00005;
			double endpoint = 1.0;
			/*---------------------*/
			
			double[] selectivity = new double[resolution];
			
			//assert(option == UNI || option == EXP): "Wrong input to loadSelectivity";
			if(option == 0)
			{
				
				
				sel= startpoint + ((endpoint - startpoint)/(2*resolution));
				for(int i=0;i<resolution;i++){
					selectivity[i] = sel;	
					//System.out.println("\nSelectivity["+i+"] = "+selectivity[i]+"\t");
					sel += ((endpoint - startpoint)/resolution);
				}
			}
			else if (option == 1)
			{
				switch(resolution)
				{
					case 10:
						r=QDIST_SKEW_10;
						break;
					case 30:
						r=QDIST_SKEW_30;
						break;
					case 100:
						r=QDIST_SKEW_100;
						break;
					case 300:
						r=QDIST_SKEW_300;
						break;
					case 1000:
						r=QDIST_SKEW_1000;
						break;
				}
				int i,j=0;
				
				int popu=resolution;
				double a=1; //startval
				double curval=a,sum=a/2;
				
				for(i=1;i<=popu;i++)
				{
					curval*=r;
					if(i!=popu)
					sum+=curval;
					else
						sum+=curval/2;
				}
				a=1/sum;
				curval=a;
				sum=a/2;
				
				for(i=1;i<=popu;i++)
				{
					
					selectivity[i-1] = startpoint + sum;
					//System.out.println("\n"+Math.abs(diff - selectivity[i-1]));
					diff = selectivity[i-1];
				//	System.out.println("\n Sel["+(i-1)+"]="+selectivity[i-1]);
					curval*=r;
					if(i!=popu)
						sum+=(curval * (endpoint - startpoint));
					else
						sum+=(curval * (endpoint - startpoint))/2;
				}
			}
			
			for(int i=0;i<resolution;i++){
				if(i%5==0)
					System.out.println();
				//System.out.print("selecE100["+i+"] = "+selectivity[i]+";"+"\t");
				System.out.format("selecE100[%d] = %.6f; \t",i, selectivity[i]);
			}
			System.out.println();
	}
	void run(){
	boolean success = false;
//String start = "/home/dsladmin/Srinivas/AnshPG/bin/pg_ctl -D /home/dsladmin/Srinivas/AnshPG/tpch/ -w start";
	//String stop = "/home/dsladmin/Srinivas/AnshPG/bin/pg_ctl -D /home/dsladmin/Srinivas/AnshPG/tpch/ -w stop";
	String stop1 = "netstat -tulpn   > /home/dsladmin/Srinivas/data/others/processKill";
	String stop2 = "kill -9 ";//| grep '5431' | rev |cut -d' ' -f3 | rev | cut -d'/' -f1 | head -n1
	//String stop = "/home/dsladmin/Srinivas/AnshPG/bin/pg_ctl -D /home/dsladmin/Srinivas/AnshPG/tpch/ -w stop";

		String[] cmd = {
				"/bin/sh",
				"-c",
				"echo 3 | sudo tee /proc/sys/vm/drop_caches"
		};
		Process p;
		try 
		{
			while(!success){
			Runtime r = Runtime.getRuntime();
//			p = r.exec(stop1);
//			p.waitFor();
//		       File file = new File("/home/dsladmin/Srinivas/data/others/processKill");
//		        FileReader fr = new FileReader(file);
//		        BufferedReader br = new BufferedReader(fr);
//		    	Integer pid = Integer.parseInt(br.readLine());
//		    	br.close();
//		    	fr.close();
//		    	System.out.println("The process id is "+pid);
			p = r.exec(cmd);
			p.waitFor();
			BufferedReader reader = 
					new BufferedReader(new InputStreamReader(p.getInputStream()));

			String line = "";			
			while ((line = reader.readLine())!= null) 
			{
				if(line.equals("3"))
				{
					success = true;
				}
				System.out.println(line);
			}
			}
//			p = r.exec(start);
//			p.waitFor();

		} catch (Exception e) 
		{
			e.printStackTrace();
		}
		//start execution
	      Connection c = null;
	       Statement stmt = null;
	       Double val_l,val_r,val;
	       try {
	         Class.forName("org.postgresql.Driver");
	         c = DriverManager
	            .getConnection("jdbc:postgresql://localhost:5431/tpch",
	            "sa", "database");
	         System.out.println("Opened database successfully");
	         stmt = c.createStatement();

        
        stmt.execute("set work_mem='100MB'");
        stmt.execute("set effective_cache_size='1GB'");
        stmt.execute("set random_page_cost=4.0");
        stmt.execute("set cpu_operator_cost=0.0025");
        stmt.execute("set cpu_index_tuple_cost=0.005");
        stmt.execute("set cpu_tuple_cost=0.01");	
        //stmt.execute("set limit_cost = 10000");
//        stmt.execute("set full_robustness = on");
//        stmt.execute("set FPCfull_robustness = on");
//        stmt.execute("set varyingJoins = 1223");
//        stmt.execute("set JS_multiplier1 = 1");
//        stmt.execute("set JS_multiplier2 = 11");
//        stmt.execute("set robust_eqjoin_selec1 = 0.001");
//        stmt.execute("set robust_eqjoin_selec2 = 0.001");
        //stmt.execute("set spill_node = 12");
        //stmt.executeQuery("explain analyze select c_custkey, c_name,	sum(l_extendedprice * (1 - l_discount)) as revenue, c_acctbal, n_name, c_address, c_phone, c_comment from customer, orders, lineitem,	nation where c_custkey = o_custkey and l_orderkey = o_orderkey and o_orderdate >= '1993-06-01' and o_orderdate < '1994-01-01' and l_shipdate between '1993-01-01' and '1995-12-31'	and c_nationkey = n_nationkey group by	c_custkey, c_name, c_acctbal, c_phone, n_name, c_address, c_comment order by	revenue desc ");
        
        File file = new File("/home/dsladmin/Srinivas/data/PostgresCardinality/"+"spill_cardinality");
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);
    	val_r = Double.parseDouble(br.readLine());
    	br.close();
    	fr.close();
    	System.out.println("The no. of rows spilled is "+val_r);

	       }
	       catch ( Exception e ) {
	           System.err.println( e.getClass().getName()+": "+ e.getMessage() );
	           System.exit(0);
	         }
	       
}
}
