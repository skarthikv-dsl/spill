import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

public class test {

	public static void main(String[] args) throws InterruptedException {
		// TODO Auto-generated method stub

		
		
		if(args.length > 0)
			System.out.println("there is something in the argument");
		else
			System.out.println("there is NOTHING in the argument");
		
		
		test obj = new test();
		float old_val = obj.roundToDouble(1.5555555f, 3);
		float zero = 1.0f;
		
		
		//obj.arrayListMemoryTest();
		obj.staticMemoryTest();
		//obj.TimeforExecuteStatements();
		//obj.memoryTest();
		//obj.HashMapTest();
		
		//obj.compareIntegers();
		
		//obj.partitionFunction();

		ArrayList<Double> a = new ArrayList<Double>(Collections.nCopies(40, (double)0));
		//a.ensureCapacity(10);
		a.set(10,(double) 200);
		float var1 = (float)1.55;
		float var2 = 7f;
		float new_val = (float) (var1*var2);
//		System.out.println(var1);
//		System.out.println(var2);
		System.out.println(1f*1f);
		System.out.println(new_val);

		if((float)new_val == 1)
			System.out.println("Same");
		else
			System.out.println("Different");
//
//		if(old_val == new_val)
//			System.out.println("Same");

 
	}
	
	public void staticMemoryTest() throws InterruptedException {
		
	String funName = "arrayListMemoryTest";
		
		ArrayList<test_class> l1 = new ArrayList<test_class>();
		ArrayList<test_class> l2 = new ArrayList<test_class>();
		for(int i = 0; i < 2000; i++) {
			test_class l = new test_class(); 
			l1.add(l);
			//in the test class if we make the array as static, then only one instance of the array is allocated for any object of the location
			
		}
		TimeUnit.SECONDS.sleep(10);
		
		
	}

	public void arrayListMemoryTest() throws InterruptedException {
		
		String funName = "arrayListMemoryTest";
		
		ArrayList<location> l1 = new ArrayList<location>();
		ArrayList<location> l2 = new ArrayList<location>();
		for(int i = 0; i < 30000000; i++) {
			location l = new location();
			l1.add(l);
			//l2.add(l); //it will only add the size of pointers, for instance 120 MB for 30000000
		}
		TimeUnit.SECONDS.sleep(10);
		
	}

	private void TimeforExecuteStatements() {
		System.out.println("entered DB tpcds");
		Connection conn = null;

		try{
			Class.forName("org.postgresql.Driver");
			conn = DriverManager
					.getConnection("jdbc:postgresql://localhost:5432/tpcds-ai",
							"sa", "database");
			Statement stmt = conn.createStatement();
			
			long startTime = System.nanoTime();
			for(int i=0; i< 10000; i++) {
				stmt.execute("set  seq_page_cost = 1");
				stmt.execute("set  random_page_cost=4");
				stmt.execute("set cpu_operator_cost=0.0025");
				stmt.execute("set cpu_index_tuple_cost=0.005");
				stmt.execute("set cpu_tuple_cost=0.01");
				stmt.execute("set  seq_page_cost = 1");
				stmt.execute("set  random_page_cost=4");
				stmt.execute("set cpu_operator_cost=0.0025");
				stmt.execute("set cpu_index_tuple_cost=0.005");
				stmt.execute("set cpu_tuple_cost=0.01");

			}
			
			long endTime = System.nanoTime();
			System.out.println("Took "+(endTime - startTime)/1000000000 + " sec");
			
			for(int i=0; i< 10000; i++) {
				stmt.execute("set  seq_page_cost = 1");
			}
			
			long endTime_1 = System.nanoTime();
			System.out.println("Took "+(endTime_1 - endTime)/1000000000 + " sec");
			
			stmt.close();
			
			if (conn != null) {
				try { conn.close(); } catch (SQLException e) {}
			}
			
		}
		catch ( Exception e ) {
			System.out.println("entered DB err");
			System.err.println( e.getClass().getName()+": "+ e.getMessage() );
		}
	}

	private void memoryTest() {
		
		long startTime = System.nanoTime();
		int j=0;
		double test_array [] = new double[1000]; 
		while(j < 10000) {
		for(int i =1 ; i < 1000; i++)
			test_array[i] = i;
			j++;
		}
		//took 0 secs
		long endTime = System.nanoTime();
		System.out.println("Took "+(endTime - startTime)/1000000000 + " sec");
		
		 		j = 0;
		 		while(j < 10000) {
				for(int i =1 ; i < 1000; i++) {
					double test_array_mem [] = new double[1000];
					test_array_mem[i] = i;
				}
				j++;
		 		}
		 // took 8 secs		
		long endTime_1 = System.nanoTime();
				System.out.println("Took "+(endTime_1 - endTime)/1000000000 + " sec");
	}

	private void HashMapTest() {
		
		HashMap<Integer,Integer> local_map = new HashMap<Integer,Integer>();
		
		for(int i=0; i< 1000; i++){
			if(i%2 == 0)
				local_map.put(i, i);
		}
		int j =200;
		Integer k = new Integer(200);
		System.out.println("int key = "+local_map.get(j)+" Integer key = "+local_map.get(k));
	}

	public void compareIntegers() {
		
		System.out.println("Came inside compare integers function");
		Integer i1 = 1000;
		Integer i2 = 1000;
		if(i1 == i2)
			System.out.println("same");
		else
			System.out.println("different");
	}

	public void partitionFunction(){
		
		System.out.println("Came inside partition function");
		int size = 64, num_of_usable_threads= 16;
		int step_size = size/num_of_usable_threads;
		int residue = size % num_of_usable_threads;
		int cur_min_val = 0;
		int cur_max_val =  -1;
		
		for (int j = 0; j < num_of_usable_threads ; ++j) {

			
				cur_min_val = cur_max_val+1;
			cur_max_val = cur_min_val + step_size ;
			
			if(j==num_of_usable_threads-1 || (size < num_of_usable_threads))
				cur_max_val = size;

			if((residue--) > 0 )
				System.out.println("["+cur_min_val+","+cur_max_val+"]");
			else{
				cur_max_val--;
				System.out.println("["+cur_min_val+","+(cur_max_val)+"]");
			}
			
			
			if(size < num_of_usable_threads)
				break;
		}
	}

	public static float roundToDouble(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }


	
}

 class test_class{
	static  double arr[] = new double[1000000];
	test_class() {
		
	}
}