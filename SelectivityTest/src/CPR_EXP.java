import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;




import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.ds.DataValues;
import iisc.dsl.picasso.server.ADiagramPacket;
public class CPR_EXP 
{
	int SANITY_CONSTANT = 0;
	int big_cnt =0;
	//private static 	String bouqPath;
	// Skew for the exponential distribution
	public static double QDIST_SKEW_10 = 2.0;
	public static double QDIST_SKEW_30 = 1.33;
	public static double QDIST_SKEW_100 = 1.083;
	public static double QDIST_SKEW_300 = 1.027;
	public static double QDIST_SKEW_1000 = 1.00808;

	int RIGHT = 0;
	int TOP = 1; 
	int DIAGONAL = 2;
	int resolution;
	int dimension;
	int totalPlans;
	int total_points;
	DataValues[] data;
	double AllPlanCosts[][];
	int newOptimalPlan[];
	int allPlanSet[];
	int contourLocation[];
	double threshold = 20.0;
	double OptimalCost [];
	double selectivity[];
	static double alpha = 2;
	double k = 1.0	;
	double multiplier = 1.00;
	double ALPHA_COST = multiplier*Math.pow(alpha,k);
	int  VerticalViolationGrid[];
	int HorizontalViolatoinGrid[];
	int DiagonalViolationGrid[];
	List<Integer> list = new ArrayList<Integer>();
	
	
	public static void main(String args[])
	{
	//	String bouquetPath = "C:\\Lohit\\work_1\\H2DQT21_30";
		//String bouquetPath = "C:\\Lohit\\SQLSERVER_APKT\\H2DQT2_300";
	//	String QT_NAME = "DEMO_new_Q8_2D_100exp";
	//	String QT_NAME = "PB_Q5_2D_100exp";
	//	String bouquetPath = "C:\\Lohit\\SQLSERVER_APKT\\"+QT_NAME;
		
	//	String QT_NAME = "POSTGRES_H2DQT18_300_EXP";
	//		String bouquetPath = "C:\\Lohit\\PG_APKT\\EXP\\"+QT_NAME;
		
		String QT_NAME = "PG_H2DQT7_300_E_FPC";
		String bouquetPath = "/home/dsladmin/Lohit/Output/PostgreSQL/"+QT_NAME+"/"+QT_NAME;
		
		
		
		String QT_NAME = "SQL SERVER_H3DQT5_100";
		String bouquetPath = "/home/dsladmin/Lohit/SQLSERVER_APKT/"+QT_NAME;
			
	
		
	//	String QT_NAME = "DEMO_new_Q8_2D_100exp";
	//	String bouquetPath = "C:\\Lohit\\SQLSERVER_APKT\\"+QT_NAME;
		System.out.print("\n"+QT_NAME+"\n\n");
		
	//	String bouquetPath = "C:\\Lohit\\work_1\\test";
		
		CPR_EXP obj = new CPR_EXP();
		//obj.export_cost(bouquetPath, QT_NAME);
		obj.bouqData(bouquetPath);
		//alpha = 1.2;
	//	obj.bouqData(bouquetPath);
	}

	void bouqData(String bouquetPath)
	{
		System.out.print("Alpha="+alpha+"\nk="+k+"\n");
		double startpoint = 0.0;
		double endpoint = 1.0;
		
	
		
		ADiagramPacket gdp = getGDP(new File( bouquetPath +  ".apkt"));
		totalPlans = gdp.getMaxPlanNumber();
		dimension = gdp.getDimension();
		resolution = gdp.getMaxResolution();
		data = gdp.getData();
		total_points = (int) Math.pow(resolution, dimension);
		//System.out.println("\nthe total plans are "+totalPlans+" with dimensions "+dimension+" and resolution "+resolution);
		
		assert (total_points==data.length) : "Data length and the resolution didn't match !";
		
		double [] OptimalCost = new double [data.length]; 
		for (int i = 0;i < data.length;i++)
		{
			OptimalCost[i]= data[i].getCost();
		//	System.out.println("\nOptimal_cost:["+i+"] ="+OptimalCost[i]+"\n");
		}
		double ratio = OptimalCost[total_points -1]/OptimalCost[0];
//		System.out.println("\n MIN ="+OptimalCost[0]+"\t MAX="+OptimalCost[total_points -1]+"\t cmax_cmin_ration ="+ratio+"\n");
		
		getViolationList(total_points, OptimalCost);
		System.out.println("Violation"	+ " List_size="+list.size());
		
	//	OptimalCost = Smoothen(OptimalCost);
		//System.out.println("\n\n###############################################################\n\n;");
		//OptimalCost = Smoothen(OptimalCost);
		//float[] picsel = new float[resolution];

	//	Smoothen();
	//	System.out.println("\nSmoothen Function ends.\n");
		
		
		
		// $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
		
		double diff =0;
		double r=1.0;
		double [] selectivity = new double [resolution];
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

		//System.exit(1);
		
		// $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
		
		//System.out.println("\nLength of selectivity array ="+selectivity.length+"\n");
		//Checking Violations !
		double [] curr_sel = new double [2];
		int [] curr_index = new int [2];
		int [] new_index = new int [2];
		int [] VerticalViolationGrid = new int [total_points];
		int [] HorizontalViolationGrid = new int [total_points];
		int [] DiagonalViolationGrid = new int [total_points];
		
		int [] input_index = new int [4];
		int [] Endpoint = new int[2];
		int new_pos;
		double c1,c2,s1,s2;
		double approx_cost;
		double CostAtNewPos;
		for(int curr_pos=0;curr_pos<total_points;curr_pos++)
		{
			/*
			 * For each point in the grid, look at the nearest point at alpha*x and alpha*y 
			 * and put it into three 2D array which will have 1 or 0 corresponding to vertical,horizontal or diagonal.
			 * option : RIGHT, TOP, DIAGONAL
			 */
			//indx = findNearest(final_selectivity[2],index[2],option);
			if (list.indexOf(curr_pos) != -1)
				continue;
			curr_index = getCoordinates(dimension, resolution, curr_pos);
			curr_sel[0] = selectivity[curr_index[0]];
			curr_sel[1] = selectivity[curr_index[1]];
			
			HorizontalViolationGrid[curr_pos] = 0;
			VerticalViolationGrid[curr_pos] = 0;
			DiagonalViolationGrid[curr_pos] = 0;
			input_index = FindNearestPoint(curr_sel,curr_index,selectivity,RIGHT);
		//	System.out.println("\n cur_index :["+curr_index[0]+","+curr_index[1]+"]\t min_pt:["+input_index[0]+","+input_index[1]+"]\t max_pt:["+input_index[2]+","+input_index[3]+"]");
		//	new_index = getCoordinates(dimension,resolution,new_pos);
			if(input_index[0] == -1 || input_index[2] == -1 )
			{
		//		HorizontalViolationGrid[curr_pos] = 1;
			}
			else
			{
				if(input_index[2] != resolution - 1 && selectivity[input_index[2]] != alpha*curr_sel[0])
				{
					/*s2 = selectivity[new_index[0]+1];
					s1 = selectivity[(new_index[0])];
					c2 = OptimalCost[new_pos+1];
					c1 = OptimalCost[new_pos];*/
					s1 = selectivity[input_index[0]];
					Endpoint[0] = input_index[0];
					Endpoint[1] = input_index[1];
					
					c1 = OptimalCost[getIndex(Endpoint, resolution)];
					s2 = selectivity[input_index[2]];
					Endpoint[0] = input_index[2];
					Endpoint[1] = input_index[3];
					c2 = OptimalCost[getIndex(Endpoint, resolution)];
					
					assert(input_index[0] <= input_index[2]): "Horizontal Violation check: left point is greater than right point";
					assert(input_index[1] == input_index[3] ): " Horizontal violation check, Y axis is not constant.";
					assert(c2 >= c1) : "Cost of the endpoint is lesser than the initial point !"+c1+","+c2;
					
					if(alpha*curr_sel[0] > s2)
					{
						approx_cost = c2;
					}
					else
					{
						approx_cost = ((c2 - c1)/(s2 - s1))*(alpha*curr_sel[0] - s1)+c1;
					}
					
					
	//				System.out.println("\n c1 :"+c1+"\t c2 :"+c2+"\t curr_point_cost :"+OptimalCost[curr_pos]);
				//	System.out.println("\n alpha_x cost :"+approx_cost+"\t current_cost :"+OptimalCost[curr_pos]);
					if(approx_cost > ALPHA_COST*OptimalCost[curr_pos])
				//	if(OptimalCost[new_pos] > (alpha*OptimalCost[curr_pos]))
					{
					//	System.out.println("\tright violation \n");
					//	HorizontalViolationGrid[curr_pos] = 1;
					}
				}
			}
			input_index = FindNearestPoint(curr_sel,curr_index,selectivity,TOP);
			//new_index = getCoordinates(dimension,resolution,new_pos);
			if(input_index[1] == -1 || input_index[3] == -1 )
			{
		//		VerticalViolationGrid[curr_pos] = 1;
			}
			else
			{
				if(input_index[3] != resolution - 1 && selectivity[input_index[3]] != alpha*curr_sel[1])
				{
					s1 = selectivity[input_index[1]];
					Endpoint[0] = input_index[0];
					Endpoint[1] = input_index[1];
					
					c1 = OptimalCost[getIndex(Endpoint, resolution)];
					s2 = selectivity[input_index[3]];
					Endpoint[0] = input_index[2];
					Endpoint[1] = input_index[3];
					c2 = OptimalCost[getIndex(Endpoint, resolution)];
					
					assert(input_index[1] <= input_index[3]): "Vertical Violation check: bottom point is greater than top point";
					assert(input_index[0] == input_index[2] ): " Vertical violation check, X axis is not constant.";
					assert(c2 >= c1) : "Cost of the endpoint is lesser than the initial point !";
					
					if(alpha*curr_sel[1] > s2)
					{
						approx_cost = c2;
					}
					else
					{
						approx_cost = ((c2 - c1)/(s2 - s1))*(alpha*curr_sel[1] - s1) + c1;
					}
				//	System.out.println("\n c1 :"+c1+"\t c2 :"+c2+"\t curr_point_cost :"+OptimalCost[curr_pos]);
					//System.out.println("\n alpha_y cost :"+approx_cost+"\t current_cost :"+OptimalCost[curr_pos]);
					if(approx_cost > ALPHA_COST*OptimalCost[curr_pos])
				//	if(OptimalCost[new_pos] > (alpha*OptimalCost[curr_pos]))
					{
				//		System.out.println("\nViolating !!!!!!!!\n");
						VerticalViolationGrid[curr_pos] = 1;
					}
				}
			}
			// This is the diagonal case which we are not handling in the 2D case !
			/*new_pos = FindNearestPoint(curr_sel,curr_index,selectivity,DIAGONAL);
			new_index = getCoordinates(dimension,resolution,new_pos);
			s2 = selectivity[new_index[1]];
			s1 = selectivity[(new_index[1]-1)];
			c2 = OptimalCost[new_pos];
			c1 = OptimalCost[new_pos-resolution + 1];
			approx_cost = ((c2 - c1)/(s2 - s1))*(alpha*curr_sel[1]);
			if(OptimalCost[new_pos] > (alpha*OptimalCost[curr_pos]))
			{
	//			System.out.println("\nViolating !!!!!!!!\n");
				DiagonalViolationGrid[curr_pos] = 1;
			}*/
			
		}
		//System.out.println("\nAll violation grid obtained!\n");
		
		int no_of_bad_points =0;
		for(i = 0; i< total_points; i++)
		{
			if (list.indexOf(i) != -1) 
				continue;
			if(HorizontalViolationGrid[i] == 0 &&
					VerticalViolationGrid[i] == 0 ) //&&
					//DiagonalViolationGrid[i] == 1)
			{
				no_of_bad_points ++;
			}
			
		}
		System.out.print("Big Boss cost ="+OptimalCost[(resolution*resolution) - 1]+"\n");
		
		int den = total_points - list.size();
		double percent_error = ((double)(no_of_bad_points)*100)/(total_points - list.size());
		System.out.print(no_of_bad_points+","+den+"\n"+percent_error+ "\n\n");
		
		
	}
	
	
	
	
	
	

//Added by Lohit
/*
 * 1. Get AllPlanCost[r][r]
 * 1.1 Smoothen the PCF's 
 * 2. Construct OptimalCost[resolution], which will contain the optimal cost at each point in the discrete selectivity space. 
 * 3. Write function CheckViolations :
 * 		It uses queue to do a BFS and find out the violations.
 * 		* Get selectivity from the index using uniform or exponential distribution(The pcst files is such)
 * 		three Data Structures which will be having each entry as zero or one (or any number in the second strategy): 
 * 			1. VerticalViolationGrid[resolution]
 * 			2. HorizontalViolatoinGrid[resolution]
 * 			3. DiagonalViolationGrid[resolution]
 * 4. Write three funcitons :
 * 		1. CheckVerticalViolation
 * 		2. CheckHorizontalViolation	
 * 		3. CheckDiagonalViolation
 * 5. Write function GetTotalViolations
 */
// ############################### Written by Lohit ########################################

	/*
	 * Return the new_pos
	 */
	int[] FindNearestPoint(double [] sel,int []base_index,double []selectivity,int option)
	{
		double min_diff = 1000000;
	//	int min_index[];
		double alpha_x = alpha*sel[0];
		double alpha_y = alpha*sel[1];
		double diff,diff1,diff2;
		int [] min_index = new int [2];
		int [] max_index = new int [2]; 
		int [] final_index = new int [4];
		
		min_index[0] = base_index[0];
		min_index[1] = base_index[1];
		if(option == RIGHT)
		{
			for(int i = base_index[0]; i<(resolution-1); i++)
			{
				
				diff = alpha_x - selectivity[i];
				if(diff < 0)
					break;
				if(Math.abs(diff) < min_diff && diff >= 0)
				{
					min_diff = diff;
					min_index[0] = i;
				}
				
			}
			max_index[0] = min_index[0];
			max_index[1] = min_index[1];
			if(min_index[0] != resolution -1)
			{
				max_index[0] = min_index[0] + 1 ;
				max_index[1] = min_index[1];
				while(IsViolating(min_index) == 1 )
				{
					if((min_index[0]) == 0)
						min_index[0] = -1;
					min_index[0] = min_index[0] - 1 ;
				}
				while(IsViolating(max_index) == 1 )
				{
					if((max_index[0]) == resolution - 1)
						max_index[0] = -1;
					max_index[0] = max_index[0] +1;
				}
			}
		}
		if(option == TOP)
		{
			for(int i = base_index[1]; i<(resolution-1); i++)
			{
				
				diff = alpha_y - selectivity[i];
				if(diff < 0)
					break;
				if(Math.abs(diff) < min_diff && diff >= 0)
				{
					min_diff = diff;
					min_index[1] = i;
				}
			}
			max_index[0] = min_index[0];
			max_index[1] = min_index[1];
			if (min_index[1] != resolution -1)
			{
				max_index[0] = min_index[0] ;
				max_index[1] = min_index[1]+1;
				while(IsViolating(min_index) == 1 )
				{
					if((min_index[1]) == 0)
						min_index[1] = -1;
					min_index[1] = min_index[1] - 1 ;
				}
				while(IsViolating(max_index) == 1 )
				{
					if((max_index[1]) == resolution - 1)
						max_index[1] = -1;
					max_index[1] = max_index[1] +1;
				}
			}
		}
		if(option == DIAGONAL)
		{
			int i = base_index[0];
			int j = base_index[1];
			while(i<=(resolution-1) && j<=(resolution-1))
			{
				
				diff1 = alpha_x - selectivity[i];
				diff2 = alpha_y -selectivity[j];
				diff = Math.sqrt(Math.pow(diff1,2) + Math.pow(diff2, 2));
				if(Math.abs(diff) < min_diff && (diff1 >= 0) && (diff2 >= 0) )
				{
					min_diff = diff;
					min_index[0] = i;
					min_index[1] = j;
				}
				i++;
				j++;
			}
			max_index[0] = min_index[0];
			max_index[1] = min_index[1];
			if (min_index[0] + 1 == resolution -1 || max_index[1] + 1 == resolution -1 )
			{
				max_index[0] = min_index[0] + 1 ;
				max_index[1] = min_index[1] + 1;
				while(IsViolating(min_index) == 1 )
				{
					if((min_index[0]) == resolution - 1 || (min_index[1]) == resolution -1 )
						min_index[0] = -1;
					min_index[0] = min_index[0] - 1 ;
					min_index[1] = min_index[1] - 1;
				}
				while(IsViolating(max_index) == 1 )
				{
					if((max_index[0]) == resolution - 1 || (min_index[1]) == resolution - 1)
						max_index[0] = -1;
					max_index[0] = max_index[0] + 1;
					max_index[1] = max_index[1] + 1;
				}
				
			}
			
		}
		final_index[0] = min_index[0];
		final_index[1] = min_index[1];
		final_index[2] = max_index[0];
		final_index[3] = max_index[1];
		
		return final_index;
	}
	
	void getViolationList(int total_points, double [] Cost)
	{
		int [] index = new int [2];
		int [] right_index = new int [2];
		int [] top_index = new int [2];
		int right_pos, top_pos;
		
		for(int i=0; i<total_points;i++)
		{
			right_pos = i;
			top_pos = i;
			index = getCoordinates(dimension,resolution,i);
			if (index[0] != resolution -1 )
			{
				right_index[0] = index[0] + 1 ;
				right_index[1] = index[1];
				right_pos = getIndex(right_index,resolution);
				assert(i <= right_pos);
			}
			if(index[1] != resolution -1)
			{
				top_index[0] = index[0];
				top_index[1] = index[1] + 1;
				top_pos = getIndex(top_index, resolution);
				assert(i <= top_pos);
			}
			if(1.05*Cost[right_pos] < Cost[i] || 1.05*Cost[top_pos] < Cost[i])
			{
			//	System.out.println("\n ["+index[0]+","+index[1]+"] added\t cost ="+Cost[i]+"\t R="+Cost[right_pos]+"\t T="+Cost[top_pos]);
				list.add(i);
			}
		}
		return;
	}
	int IsViolating(int [] index)
	{
		int pos = getIndex(index,resolution);
		if(list.indexOf(pos) != -1)
			return 1;
		return 0;
	}
	
	void export_cost(String bouquetPath,String qt_name) {
        // TODO Auto-generated method stub
         ADiagramPacket gdp = getGDP(new File( bouquetPath +  ".apkt"));
         totalPlans = gdp.getMaxPlanNumber();
         dimension = gdp.getDimension();
         resolution = gdp.getMaxResolution();
         data = gdp.getData();
         float total_points = (int) Math.pow(resolution, dimension);
         //System.out.println("\nthe total plans are "+totalPlans+" with dimensions "+dimension+" and resolution "+resolution);
         
         assert (total_points==data.length) : "Data length and the resolution didn't match !";
         
         //double [][] OptimalCost = new double [resolution][resolution];

         //writing to file
         try {
             
             String content = "This is the content to write into file";
  
             File file = new File("C:\\Lohit\\Output\\"+qt_name);  
             // if file doesn't exists, then create it
             if (!file.exists()) {
                 file.createNewFile();
             }
             FileWriter writer = new FileWriter(file, false);
             PrintWriter pw = new PrintWriter(writer);  
              for (int i = 0;i < data.length;i++)
              {
                 if(i % resolution == 0)
                     pw.format("\n%5.4f",data[i].getCost());
                 else
                     pw.format("\t%5.4f",data[i].getCost());
                         
              }
             
             pw.close();
  
             System.out.println("Done");
  
         } catch (IOException e) {
             e.printStackTrace();
         }
    }

 
	
	
	double [] Smoothen(double [] OptimalCost)
	{
		/*
		 * Get the AllPlanCosts
		 * For (i = 0;i < total_plans ;i++)
		 * 	Put the lowest point in queue.
		 * 	while queue not empty
		 * 		Pull out a item and put it to current_item from queue, and insert point  to its right and top (if any).
		 * 		If cost[current_item] > cost [right of it] || cost [current_item] < cost[left of it]
		 * 			Equalize the cost  to the current_item.
		 * 		 
		 */
		
	//	System.out.println("\n size of array ="+AllPlanCosts.length+"and"+AllPlanCosts[1].length+"\n");
		int loc;
		int current_pos;
		int indx[] = new int[2]; 
		int next_right_pt[] = new int[2];
		int next_top_pt[] = new int[2];
		int right_pos;
		int top_pos;
		
	//	System.out.println("\n total_points = "+total_points+"\t resolution ="+resolution+"\n");
		for (int i=0;i<total_points;i++)
		{
			
			indx = getCoordinates(dimension,resolution,i);
			//System.out.println("\n #"+i+"\t ["+indx[0]+"] \t ["+indx[1]+"]\n");
			if(indx[0] != (resolution-1))
			{
				next_right_pt[0]= indx[0]+1;
				next_right_pt[1] = indx[1];
				right_pos = getIndex(next_right_pt,resolution);
			//	System.out.println("\n right_pos = "+right_pos);
				if (OptimalCost[right_pos] < OptimalCost[i])
				{
					big_cnt++;
					//System.out.println("\n Here");
					OptimalCost[right_pos] = OptimalCost[i];
				}
			}
			if(indx[1]!=(resolution -1 ))
			{
				next_top_pt[0]= indx[0];
				next_top_pt[1] = indx[1]+1;
				top_pos = getIndex(next_top_pt,resolution);
				if (OptimalCost[top_pos] < OptimalCost[i])
				{
					big_cnt++;
					//System.out.println("\n Here2");
					OptimalCost[top_pos] = OptimalCost[i];
				}
			}
		}
		System.out.println("\n>>>>>> Big Count ="+big_cnt+"\n");
		return OptimalCost;
		
	}

	
	

	
	//######################## Get GDP ###############
	void findContours()
	{
		SANITY_CONSTANT = 10000;
		double minCostPacket = Math.max(AllPlanCosts[newOptimalPlan[0]][0],SANITY_CONSTANT);
		double maxPacketCost = AllPlanCosts[newOptimalPlan[data.length-1]][data.length-1];       //assumed to be max
		int steps = 0;				
		double limit = maxPacketCost;
		while(limit > minCostPacket){		limit /= 2;		steps++;}
		double firstCostLimit = limit * 2;
		
		double cost_limit[] = new double[steps];
		for(int s=0; s<steps; s++)					
			cost_limit[s] = firstCostLimit * Math.pow(2, s);
		
		int doneRec[] = new int[data.length];
		for(int loc=0;loc<data.length;loc++){
			int optimalPlan = newOptimalPlan[loc];
			double optCost = Math.max(AllPlanCosts[optimalPlan][loc], SANITY_CONSTANT);
			
			if(optCost > data[data.length-1].getCost())
				optCost = Math.floor(data[data.length-1].getCost());
//			optCost = Math.min(AllPlanCosts[optimalPlan][loc], data[data.length-1].getCost());
			
			
			//find out under which contour the current location lies
			int s=0;
			while(optCost > cost_limit[s]) { 
				
				s++;
				if(s==steps+1)
					System.out.printf("caught");
			}
			doneRec[loc] = s;
		}
		
		doneRec[data.length-1] = steps;
		for(int loc=data.length-2;loc>=0;loc--){
			int d =0, correct = 0;
			int minMark = 99999;
			for(d=0;d<dimension;d++){
				int nloc = loc - (int) Math.pow(resolution, d);
				if(nloc > 0 && nloc < data.length - 1){
					correct++;
					minMark = Math.min(minMark, doneRec[nloc]);
				}
			}
			if(correct == 0)   minMark = 0;
			if(doneRec[loc]==minMark) doneRec[loc] = 0;
		}
		
		
		int contourPlansTotalLocation[][]=new int[steps][totalPlans]; 
		
		int contourPlansMaxLocation[][][]=new int[steps][totalPlans][dimension];
		int contourPlansMinLocation[][][]=new int[steps][totalPlans][dimension];
		
		int allContourPlans[] = new int[totalPlans];
		int allContPlansCount = 0;
		
		for(int i=0;i<steps;i++)
		{
			for(int j=0;j<totalPlans;j++)
			{
				Arrays.fill(contourPlansMaxLocation[i][j], Integer.MIN_VALUE);
				Arrays.fill(contourPlansMinLocation[i][j], Integer.MAX_VALUE);
				Arrays.fill(allContourPlans, -1);
			}
			Arrays.fill(contourPlansTotalLocation[i], 0);
		}
		int optimalPlan;
	
		for(int loc=0;loc<data.length;loc++)
		{
			if(doneRec[loc]>=1)
			{
				optimalPlan = newOptimalPlan[loc];
				contourPlansTotalLocation[doneRec[loc]-1][optimalPlan] = 1;
			}
		}
		int totalPlansOnCountour=0;
		for(int i=0;i<steps;i++)
		{
			for(int j=0;j<totalPlans;j++)
			{
				if(contourPlansTotalLocation[i][j]!=0)
				{
					System.out.print(j+",");
					totalPlansOnCountour++;
					int c=0;
					for(c=0;c<allContPlansCount;c++)
					{
						if(allContourPlans[c]==j)
							break;
					}
					if(c>=allContPlansCount)
						allContourPlans[allContPlansCount++]=j;
				}
			}
			System.out.println();
		}
		
		int[] coordinate = new int[dimension];
		for(int loc=0;loc<data.length;loc++)
		{
			if(doneRec[loc]>=1)
			{
				coordinate = getCoordinates(dimension,resolution,loc);
				optimalPlan = newOptimalPlan[loc];
//				contourPlansTotalLocation[doneRec[loc]-1][optimalPlan]++;
				for(int i=0;i<dimension;i++)
				{
					if(contourPlansMaxLocation[doneRec[loc]-1][optimalPlan][i]<coordinate[i])
					{
						contourPlansMaxLocation[doneRec[loc]-1][optimalPlan][i]=coordinate[i];
					}
					if(contourPlansMinLocation[doneRec[loc]-1][optimalPlan][i]>coordinate[i])
					{
						contourPlansMinLocation[doneRec[loc]-1][optimalPlan][i]=coordinate[i];
					}
				}
			}
		}
		
		int reducedPlans[] = new int[totalPlans];
		int counter=0;
		int temp = totalPlansOnCountour;
//		while(true)
		while(temp>0)
		{
			int eatenPlans[] = new int[totalPlans];
			Arrays.fill(eatenPlans, 0);
			for(int i=0;i<steps;i++)
			{
				for(int j=0;j<totalPlans;j++)
				{
					if(contourPlansTotalLocation[i][j]!=0)
					{
						int c=0;
						for(c=0;c<counter;c++)
						{
							int index = getIndex(contourPlansMaxLocation[i][j], resolution);
//							if(AllPlanCosts[reducedPlans[c]][index] <= cost_limit[i] * (1.2))
							if(reducedPlans[c]==j)
							{
								break;
							}
//							if(Math.abs(AllPlanCosts[reducedPlans[c]][index] -cost_limit[i])/AllPlanCosts[reducedPlans[c]][index]<= threshold/100.0)
							if(AllPlanCosts[reducedPlans[c]][index]<=cost_limit[i]*(1+threshold/100.0))
							{
								break;
							}
						}
						if(c<counter)
							continue;
						
						int index = getIndex(contourPlansMaxLocation[i][j], resolution);
						for(int k=0;k<allContPlansCount;k++)
//						for(int k=0;k<totalPlans;k++)
						{
							if(allContourPlans[k]!=j)
							{
//								currentRelativeDiff = Math.abs(cost_limit[i]-AllPlanCosts[allContourPlans[k]][index]);
//								currentRelativeDiff = currentRelativeDiff * 100.0/AllPlanCosts[allContourPlans[k]][index];
//								if(currentRelativeDiff<=threshold)
								if(AllPlanCosts[allContourPlans[k]][index]<=cost_limit[i]*(1+threshold/100.0))
								{
									eatenPlans[allContourPlans[k]]++;
//									if(k==6)
//									System.out.println("contour="+i+", plan="+j+", eaten="+k+"");
								}
							}
							else
							{
								eatenPlans[allContourPlans[k]]++;
//								if(k==6)
//								System.out.println("contour="+i+", plan="+j+", eaten="+k+"");
							}
						}
					}
				}
			}
			int maxEatenPlan = -1;
			int chosenPlan=-1;
			for(int i=0;i<allContPlansCount;i++)
//			for(int i=0;i<totalPlans;i++)
			{
				if(maxEatenPlan<eatenPlans[allContourPlans[i]])
				{
					maxEatenPlan = eatenPlans[allContourPlans[i]];
					chosenPlan = allContourPlans[i];
				}
			}
			if(maxEatenPlan == -1)
				break;
			reducedPlans[counter++] = chosenPlan;
			temp = temp - maxEatenPlan;
			
			for(int i=0;i<steps;i++)
			{
				for(int j=0;j<totalPlans;j++)
				{
					if(contourPlansTotalLocation[i][j]!=0)
					{
						int c=0;
						for(c=0;c<counter-1;c++)
						{
							int index = getIndex(contourPlansMaxLocation[i][j], resolution);
//							if(AllPlanCosts[reducedPlans[c]][index] <= cost_limit[i] * (1.2))
							if(reducedPlans[c]==j)
							{
								break;
							}
//							if(Math.abs(AllPlanCosts[reducedPlans[c]][index] -cost_limit[i])/AllPlanCosts[reducedPlans[c]][index]<= threshold/100.0)
							if(AllPlanCosts[reducedPlans[c]][index]<=cost_limit[i]*(1+threshold/100.0))
							{
								break;
							}
						}
						if(c<counter-1)
							continue;
						int index = getIndex(contourPlansMaxLocation[i][j], resolution);
						if(chosenPlan!=j)
						{
							if(AllPlanCosts[chosenPlan][index]<=cost_limit[i]*(1+threshold/100.0))
							{
								System.out.println("contour="+i+", plan="+j+", eaten by="+chosenPlan+"");
								contourPlansTotalLocation[i][j] = 0;
								contourPlansTotalLocation[i][chosenPlan] = 1;
							}
						}
						else
						{
							System.out.println("contour="+i+", plan="+j+", eaten by="+chosenPlan+"");
						}
					}
				}
			}
			System.out.println();
		}
		
		for(int i=0;i<counter;i++)
		{
			System.out.print(reducedPlans[i]+",");
		}
		System.out.println();
		System.out.println("Reduced Contour Plans=");
		int a = 0;
		int rho=0;
		for(int i=0;i<steps;i++)
		{ 
			a=0;
			for(int j=0;j<totalPlans;j++)
			{
				if(contourPlansTotalLocation[i][j]!=0)
				{
					a++;
					System.out.print(j+",");
				}
				if(rho<a)
					rho=a;
			}
			System.out.println();
		}
		System.out.println("rho="+rho);
		
//		for(int i=0;i<steps;i++)
//		{
//			for(int j=0;j<totalPlans;j++)
//			{
//				if(contourPlans[i][j]!=0)
//				{
//					for(int k=0;k<dimension;k++)
//					{
//						System.out.print(contourPlansMinLocation[i][j][k]+",");
//					}
//					System.out.print(" :");
//					for(int k=0;k<dimension;k++)
//					{
//						System.out.print(contourPlansMaxLocation[i][j][k]+",");
//					}
//					System.out.print("\t\t");
//				}
//			}
//			System.out.println();
//		}
		
//		for(int i=99;i>=0;i--)
//		{
//			int j=100*i;
//			while(j<(100*i+100))
//			{
//				if(doneRec[j]==0)
//					System.out.print(" ");
//				else
////				System.out.print(newOptimalPlan[j]);
//					System.out.print(getOptimalPlan(j, reducedPlans));
//				j++;
//			}
//			System.out.println();
//		}
	}
	void findContoursAndReduceContourWise()
	{
		SANITY_CONSTANT = 10000;
		double minCostPacket = Math.max(AllPlanCosts[newOptimalPlan[0]][0],SANITY_CONSTANT);
		double maxPacketCost = AllPlanCosts[newOptimalPlan[data.length-1]][data.length-1];       //assumed to be max
		int steps = 0;				
		double limit = maxPacketCost;
		while(limit > minCostPacket){		limit /= 2;		steps++;}
		double firstCostLimit = limit * 2;
		
		double cost_limit[] = new double[steps];
		for(int s=0; s<steps; s++)					
			cost_limit[s] = firstCostLimit * Math.pow(2, s);
		
		int doneRec[] = new int[data.length];
		for(int loc=0;loc<data.length;loc++){
			int optimalPlan = newOptimalPlan[loc];
			double optCost = Math.max(AllPlanCosts[optimalPlan][loc], SANITY_CONSTANT);
			
			if(optCost > data[data.length-1].getCost())
				optCost = Math.floor(data[data.length-1].getCost());
//			optCost = Math.min(AllPlanCosts[optimalPlan][loc], data[data.length-1].getCost());
			
			
			//find out under which contour the current location lies
			int s=0;
			while(optCost > cost_limit[s]) { 
				
				s++;
				if(s==steps+1)
					System.out.printf("caught");
			}
			doneRec[loc] = s;
		}
		
		doneRec[data.length-1] = steps;
		for(int loc=data.length-2;loc>=0;loc--){
			int d =0, correct = 0;
			int minMark = 99999;
			for(d=0;d<dimension;d++){
				int nloc = loc - (int) Math.pow(resolution, d);
				if(nloc > 0 && nloc < data.length - 1){
					correct++;
					minMark = Math.min(minMark, doneRec[nloc]);
				}
			}
			if(correct == 0)   minMark = 0;
			if(doneRec[loc]==minMark) doneRec[loc] = 0;
		}
		
		int contourPlansCount[] = new int[steps];
		Arrays.fill(contourPlansCount, 0);
		
		int contourPlansTotalLocation[][]=new int[steps][totalPlans]; 
		
		int contourPlansMaxLocation[][][]=new int[steps][totalPlans][dimension];
		int contourPlansMinLocation[][][]=new int[steps][totalPlans][dimension];
		
		int allContourPlans[] = new int[totalPlans];
		int allContPlansCount = 0;
		
		for(int i=0;i<steps;i++)
		{
			for(int j=0;j<totalPlans;j++)
			{
				Arrays.fill(contourPlansMaxLocation[i][j], Integer.MIN_VALUE);
				Arrays.fill(contourPlansMinLocation[i][j], Integer.MAX_VALUE);
				Arrays.fill(allContourPlans, -1);
			}
			Arrays.fill(contourPlansTotalLocation[i], 0);
		}
		int optimalPlan;
	
		for(int loc=0;loc<data.length;loc++)
		{
			if(doneRec[loc]>=1)
			{
				optimalPlan = newOptimalPlan[loc];
				contourPlansTotalLocation[doneRec[loc]-1][optimalPlan]++;
			}
		}
		int totalPlansOnCountour=0;
		for(int i=0;i<steps;i++)
		{
			for(int j=0;j<totalPlans;j++)
			{
				if(contourPlansTotalLocation[i][j]!=0)
				{
					contourPlansCount[i]++;
					System.out.print(j+",");
					totalPlansOnCountour++;
					int c=0;
					for(c=0;c<allContPlansCount;c++)
					{
						if(allContourPlans[c]==j)
							break;
					}
					if(c>=allContPlansCount)
						allContourPlans[allContPlansCount++]=j;
				}
			}
			System.out.println();
		}
		
		int[] coordinate = new int[dimension];
		for(int loc=0;loc<data.length;loc++)
		{
			if(doneRec[loc]>=1)
			{
				coordinate = getCoordinates(dimension,resolution,loc);
				optimalPlan = newOptimalPlan[loc];
//				contourPlansTotalLocation[doneRec[loc]-1][optimalPlan]++;
				for(int i=0;i<dimension;i++)
				{
					if(contourPlansMaxLocation[doneRec[loc]-1][optimalPlan][i]<coordinate[i])
					{
						contourPlansMaxLocation[doneRec[loc]-1][optimalPlan][i]=coordinate[i];
					}
					if(contourPlansMinLocation[doneRec[loc]-1][optimalPlan][i]>coordinate[i])
					{
						contourPlansMinLocation[doneRec[loc]-1][optimalPlan][i]=coordinate[i];
					}
				}
			}
		}
		
		int reducedPlans[][]= new int[steps][totalPlans];
		int reducedPlansCount[] = new int[steps];
		Arrays.fill(reducedPlansCount, 0);
		for(int i=0;i<steps;i++)
		{
			while(contourPlansCount[i]>0)
			{
				int eatenPlans[] = new int[totalPlans];
				Arrays.fill(eatenPlans, 0);
				for(int j=0;j<totalPlans;j++)
				{
					if(contourPlansTotalLocation[i][j]!=0)
					{
						int c=0;
						for(c=0;c<reducedPlansCount[i];c++)
						{
							int index = getIndex(contourPlansMaxLocation[i][j], resolution);
							if(reducedPlans[i][c]==j)
							{
								break;
							}
							if(AllPlanCosts[reducedPlans[i][c]][index]<=cost_limit[i]*(1+threshold/100.0))
							{
								break;
							}
						}
						if(c<reducedPlansCount[i])
							continue;
						
						double currentRelativeDiff = 0.0;
						int index = getIndex(contourPlansMaxLocation[i][j], resolution);
						for(int k=0;k<allContPlansCount;k++)
//						for(int k=0;k<totalPlans;k++)
						{
							if(allContourPlans[k]!=j)
							{
//								currentRelativeDiff = Math.abs(cost_limit[i]-AllPlanCosts[allContourPlans[k]][index]);
//								currentRelativeDiff = currentRelativeDiff * 100.0/AllPlanCosts[allContourPlans[k]][index];
//								if(currentRelativeDiff<=threshold)
								if(AllPlanCosts[allContourPlans[k]][index]<=cost_limit[i]*(1+threshold/100.0))
								{
									eatenPlans[allContourPlans[k]]++;
//									System.out.println("contour="+i+", plan="+j+", eaten="+k+"");
								}
							}
							else
							{
								eatenPlans[allContourPlans[k]]++;
//								System.out.println("contour="+i+", plan="+j+", eaten="+k+"");
							}
						}
					}
				}
				int maxEatenPlan = -1;
				int chosenPlan=-1;
				for(int j=0;j<allContPlansCount;j++)
				{
					if(maxEatenPlan<eatenPlans[allContourPlans[j]])
					{
						maxEatenPlan = eatenPlans[allContourPlans[j]];
						chosenPlan = allContourPlans[j];
					}
				}
				if(maxEatenPlan == -1)
					break;
				reducedPlans[i][reducedPlansCount[i]++] = chosenPlan;
				contourPlansCount[i] = contourPlansCount[i] - maxEatenPlan;
			}
//			for(int i=0;i<steps;i++)
//			{
//				for(int j=0;j<totalPlans;j++)
//				{
//					if(contourPlansTotalLocation[i][j]!=0)
//					{
//						double currentRelativeDiff = 0.0;
//						int index = getIndex(contourPlansMaxLocation[i][j], resolution);
//						if(chosenPlan!=j)
//						{
//							if(AllPlanCosts[chosenPlan][index]<=cost_limit[i]*(1+threshold/100.0))
//							{
//								System.out.println("contour="+i+", plan="+j+", eaten by="+chosenPlan+"");
//							}
//						}
//						else
//						{
//							System.out.println("contour="+i+", plan="+j+", eaten by="+chosenPlan+"");
//						}
//					}
//				}
//			}
		}
		
		System.out.println();
		int rho = 0;
		for(int i=0;i<steps;i++)
		{
			if(rho<reducedPlansCount[i])
				rho = reducedPlansCount[i];
			for(int j=0;j<reducedPlansCount[i];j++)
			{
				System.out.print(reducedPlans[i][j]+",");
			}
			System.out.println();
		}
		System.out.println("rho="+rho);
//		for(int i=0;i<steps;i++)
//		{
//			for(int j=0;j<totalPlans;j++)
//			{
//				if(contourPlans[i][j]!=0)
//				{
//					for(int k=0;k<dimension;k++)
//					{
//						System.out.print(contourPlansMinLocation[i][j][k]+",");
//					}
//					System.out.print(" :");
//					for(int k=0;k<dimension;k++)
//					{
//						System.out.print(contourPlansMaxLocation[i][j][k]+",");
//					}
//					System.out.print("\t\t");
//				}
//			}
//			System.out.println();
//		}
		
//		for(int i=99;i>=0;i--)
//		{
//			int j=100*i;
//			while(j<(100*i+100))
//			{
//				if(doneRec[j]==0)
//					System.out.print(" ");
//				else
////				System.out.print(newOptimalPlan[j]);
//					System.out.print(getOptimalPlan(j, reducedPlans));
//				j++;
//			}
//			System.out.println();
//		}
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
	public int getOptimalPlan(int loc, int[] plans) {
		
		double bestCost = Double.MAX_VALUE;
		int opt = -1;
		for(int p=0; p<plans.length; p++){
			if(bestCost > AllPlanCosts[plans[p]][loc]) {
				bestCost = AllPlanCosts[plans[p]][loc];
				opt = p;
			}
		}
		return opt;
	}
	public float Max(float a, float b){
		if(a>b)
			return a;
		else
			return b;
	}
}