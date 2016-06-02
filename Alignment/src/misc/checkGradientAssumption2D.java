package misc;



import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import iisc.dsl.picasso.common.ds.DataValues;
import iisc.dsl.picasso.server.ADiagramPacket;


public class checkGradientAssumption2D
{
	static int UNI = 1;
	static int EXP = 2;
	static int selConf;
	static double alpha = 1.6;
	
	int plans[];
	static double alpha_values [] = {2.0, 1.2, 1.3,1.4,1.5,1.6,1.7,1.8,1.9};
	double OptimalCost[];
	int totalPlans;
	int dimension;
	int resolution;
	DataValues[] data;
	int totalPoints;
	double selectivity[];
	List<Integer> PCMViolationList = new ArrayList<Integer>();
	
	List<point> ViolationListX = new ArrayList<point> ();
	List<point> ViolationListY = new ArrayList<point> ();
	
	public static void main(String args[])
	{
//		String folderPath = "/home/dsladmin/Lohit/PG_APKT/UNI/2D/";
//		String qtName = "POSTGRES_H2DQT21_300";
		
		
		
		String folderPath = "/home/dsladmin/Documents/Project/post_lab_pres_2/packets/PG/2D_PG_EXPONENTIAL/";
		String qtName = "POSTGRES_H2DQT7_300_EXP";
		
		System.out.println("=======================================\n"+qtName+"\n =====================================");
		selConf = EXP;
		
		String pktPath = folderPath + qtName + ".apkt" ;
		
		checkGradientAssumption2D obj = new checkGradientAssumption2D();
		
		//Populate the OptimalCost Matrix.
		obj.readpkt(pktPath);
		
		//Populate the selectivity Matrix.
		obj.loadSelectivity(EXP);
		
		obj.checkGradientViolation();
		System.out.println("\n========MainFinished ============================================\n");
	}
	/*
	 * Get PCM Violation List.
	 * Check for both x and y !!
	 * */
	
	void checkGradientViolation()
	{
		
		int j;
		for(j = 0; j < 9;j++)
		{
			alpha = alpha_values[j];
			
			getPCMViolationList(totalPoints, OptimalCost);
			System.out.println("\n alpha ="+alpha);
			System.out.println("PCM Violation list size="+PCMViolationList.size()+"\n");
			int i;
			
			int [] x1_index = new int [2];
			int [] x2_index = new int [2];
			
			double [] x1_sel = new double [2];
			double [] x2_sel = new double [2];
			
			for(i = 0;i < totalPoints;i++)
			{
				if(PCMViolationList.indexOf(i) != -1)
				{
					continue;
				}
				x1_index = getCoordinates(dimension,resolution,i);
				x1_sel = getSelectivity(x1_index);
				
				// -------- Check for x Violation ------
				x2_sel[0] = alpha*x1_sel[0];
				x2_sel[1] = x1_sel[1];
				
				x2_index [0] = findNearestIndex(x2_sel[0]);
				x2_index [1] = x1_index[1];
				if(cost(x2_index) > alpha*cost(x1_index))
				{
					
					point p1 = new point(x1_sel[0],x1_sel[1],getPlanNumber(x1_index[0],x1_index[1]));
					ViolationListX.add(p1);
					
					//There is x-violation
				}	
				//---------- X violation check ends -----
				
				//----------- y violation check --------
				
				x2_sel[0] = x1_sel[0];
				x2_sel[1] = alpha*x1_sel[1];
				
				x2_index[0] = x1_index[0];
				x2_index[1] = findNearestIndex(x2_sel[1]);
				
				if(cost(x2_index) > alpha*cost(x1_index))
				{
					// There is y-violation
					point p1 = new point(x1_sel[0],x1_sel[1],getPlanNumber(x1_index[0],x1_index[1]));
					ViolationListY.add(p1);
				}
				// -----------y violation check ends ----
			}
			//for ends
			double xViolations = (double) ViolationListX.size();
			double yViolations = (double) ViolationListY.size();
			
			int pviolate=PCMViolationList.size();
			totalPoints = totalPoints - pviolate;
			double percentXViolation = xViolations*100/totalPoints;
			double percentYViolation = yViolations*100/totalPoints;
			
			System.out.println("% X Violations ="+percentXViolation);
			System.out.println("% Y Violations ="+percentYViolation);
			
			ViolationListX.clear();
			ViolationListY.clear();
			PCMViolationList.clear();
		}
		
	}
	
	//=======================================
	double cost(int [] index)
	{
		int i = getIndex(index,resolution);
		return OptimalCost[i];
		
	}
	
	//================================
	//============================
	/*
	 * Find the least index where seletivity[index] >= sel
	 * */
	int findNearestIndex(double sel)
	{
		int i;
		for(i = 0;i < resolution;i++)
		{
			if(selectivity[i] > sel || i == (resolution - 1))
			{
				break;
			}
		}
		return i;
	}
	//============================
	int getPlanNumber(int x, int y)
	{
		 int arr[] = {x,y};
		int index = getIndex(arr,resolution);
		return plans[index];
	}
	
//=========================================
	void getPCMViolationList(int total_points, double [] Cost)
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
			if(Cost[right_pos] < Cost[i] || Cost[top_pos] < Cost[i])
			{
			//	System.out.println("\n ["+index[0]+","+index[1]+"] added\t cost ="+Cost[i]+"\t R="+Cost[right_pos]+"\t T="+Cost[top_pos]);
				PCMViolationList.add(i);
			}
		}
		return;
	}
	//=======================================
	double [] getSelectivity(int [] index)
	{
		
		double [] sel = new double[dimension];
		assert(dimension == index.length):  "Index length doesnt match";
		for(int i=0; i<dimension; i++){
			sel[i] = selectivity[index[i]];
		}
		return sel;
	
	}
	
	//============================================
	/*-------------------------------------------------------------------------------------------------------------------------
	 * Populates -->
	 * 	dimension
	 * 	resolution
	 * 	totalPoints
	 * 	OptimalCost[][]
	 * 
	 * */
	void readpkt(String pktPath)
	{
		ADiagramPacket gdp = getGDP(new File(pktPath));
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
		//	System.out.println("\nOptimal_cost:["+i+"] ="+OptimalCost[i]+"\n");
		}
	}
	//-------------------------------------------------------------------------------------------------------------------
	/*
	 * Populates the selectivity Matrix according to the input given
	 * */
	void loadSelectivity(int option)
	{
//		System.out.println("\n Resolution = "+resolution);
		double sel;
		this.selectivity = new double [resolution];
		double startpoint = 0.0;
		double endpoint = 1.0;
		
		double r = 1.0;
//		double diff;
		
		double QDIST_SKEW_10 = 2.0;
		double QDIST_SKEW_30 = 1.33;
		double QDIST_SKEW_100 = 1.083;
		double QDIST_SKEW_300 = 1.027;
		double QDIST_SKEW_1000 = 1.00808;
		
		
		assert(option == UNI || option == EXP): "Wrong input to loadSelectivity";
		if(option == UNI)
		{
			
			sel= startpoint + ((endpoint - startpoint)/(2*resolution));
			for(int i=0;i<resolution;i++){
				this.selectivity[i] = sel;
				//System.out.println("\nSelectivity["+i+"] = "+selectivity[i]+"\t");
				sel += ((endpoint - startpoint)/resolution);
			}
		}
		else if (option == EXP)
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
				//System.out.println("\n"+Math.abs(diff - selectivity[i-1]));
//				diff = selectivity[i-1];
			//	System.out.println("\n Sel["+(i-1)+"]="+selectivity[i-1]);
				curval*=r;
				if(i!=popu)
					sum+=(curval * (endpoint - startpoint));
				else
					sum+=(curval * (endpoint - startpoint))/2;
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
}