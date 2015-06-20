import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Iterator;
import java.util.Set;

import iisc.dsl.picasso.common.PicassoConstants;


public class test {
	private static double selectivity[];
	static String newLine = System.getProperty("line.separator");
	static double matrix_one [][];
	double matrix_two [][];
	static double array1 [];
	static HashMap<Integer,Integer> uniquePointsMap = new HashMap<Integer,Integer>();
    public static void main(String[] args) {
    	
    	double threshold = 20;
    	double cost =455; 
    	
    	double lt = cost * (1 + threshold / 100);
    	double[] mem_alloc = new double[100000000];
    	Set<String> hashStrings = new HashSet<String>();
    	hashStrings.add("one");
    	hashStrings.add("two");
    	hashStrings.add("three");
    	
    	System.out.println("uniquePoints Map "+uniquePointsMap);
    	System.out.println("Before: hashstring is "+hashStrings);
        test obj = new test();
        obj.test_copy_by_reference(hashStrings);
        System.out.println("After: hashstring is "+hashStrings);
        double x = obj.calculate_x(1.3,30);
        System.out.println("\n x is "+x+"\n");
        // ****************************************************************
        double r=1.0;
		int resolution = 30;
		selectivity = new double[resolution];
		double startpoint = 0.0, endpoint = 1.0;
		
		switch(resolution)
		{
			case 10:
				r=PicassoConstants.QDIST_SKEW_10;
				break;
			case 30:
				r=PicassoConstants.QDIST_SKEW_30;
				break;
			case 100:
				r=PicassoConstants.QDIST_SKEW_100;
				break;
			case 300:
				r=PicassoConstants.QDIST_SKEW_300;
				break;
			case 1000:
				r=PicassoConstants.QDIST_SKEW_1000;
				break;
		}
		int i;
		
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
		//	constants[i-1] = getConstant(selectivity[i-1]);
			//System.out.println("Sel="+df.format(100*selectivity[i-1])+"\tConstant="+constants[i-1]);
			curval*=r;
			if(i!=popu)
			sum+=(curval * (endpoint - startpoint));
			else
				sum+=(curval * (endpoint - startpoint))/2;
		}
	//	for(i = 0 ; i < 10 ; i ++)
	//	System.out.println("\nSelectivity["+i+"] =" +selectivity[i]+"\n");
		// $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
		List<Integer> list = new ArrayList<Integer>();
		list.add(1);
		list.add(30);
		list.add(89);
		System.out.println(list);
		
		System.out.println(list.indexOf(30));
		System.out.println(list.indexOf(1));
		System.out.println(list.indexOf(89));
		System.out.println(list.indexOf(320));
		//############################################## Queue Implementation Starts
		
		System.out.println(newLine + "Queue in Java" + newLine);
	    System.out.println("-----------------------" + newLine);
	    System.out.println("Adding items to the Queue" + newLine);
	    //Creating queue would require you to create instannce of LinkedList and assign 
	    //it to Queue
	    //Object. You cannot create an instance of Queue as it is abstract
	    Queue<Integer> queue = new LinkedList<Integer>();
	    
	    //you add elements to queue using add method
	    queue.add(102);
	    queue.add(2);

	    queue.add(3);
	    queue.add(4);
	    queue.add(5);
	    
	    
	    
	    System.out.println(newLine + "Items in the queue..." + queue + newLine);

	    //You remove element from the queue using .remove method
	    //This would remove the first element added to the queue, here Java
	    System.out.println("remove element: " + queue.remove() + newLine);
	    int cur = queue.peek();
	    int temp = cur + 1;
	    System.out.println("temp = "+temp+"\n");
	    
	    //.element() returns the current element in the queue, here when "java" is removed
	    //the next most top element is .NET, so .NET would be printed.
	    System.out.println("retrieve element: " + queue.element() + newLine);
	    
	    //.poll() method retrieves and removes the head of this queue
	    //or return null if this queue is empty. Here .NET would be printed and then would 
	    //be removed
	    //from the queue
	    System.out.println("remove and retrieve element, null if empty: " + queue.poll() + 
	    newLine);
	    
	    //.peek() just returns the current element in the queue, null if empty
	    //Here it will print Javascript as .NET is removed above
	    System.out.println("retrieve element, null is empty " + queue.peek() + newLine);
		// ############################################# Queue implementation ends
		 array1 = new double[3];
		array1[0] = 100;
		array1[1] = 200;
		array1[2]= 303;
	//	matrix_one = new double[4][4];
		test obj1 = new test();
		obj1.testfun();
		if(2!=2)
			System.out.println("\n not equals works !!\n");

/*		PrintStream ps = new PrintStream(C:\Lohit\work_1\test_file.txt, true); // true for auto-flush
		int test = 0;
		int count = 0;
		while(count < temps.length)
		{
		    test = temps[count];  
		    ps.println(test);
		    count++;
		}
		ps.close();*/
		
		
	}
    void testfun()
    {
    	array1[1] = 400;
    	return;
    }
        
        // ****************************************************************
        
    //}
    double calculate_x (double r, double res)
    {
    	double answer = (r -1 )/ (Math.pow(r,res)*(2*r -1) - 1);
    	return answer;
    }
    
    void test_copy_by_reference(Set<String> hashStrings){
    	hashStrings.add("four");
    	System.out.println("Inside copy_reference "+hashStrings);
    }
}
