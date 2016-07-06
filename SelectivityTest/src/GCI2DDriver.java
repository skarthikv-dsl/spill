/*
 *  This Program generates the Virtual Contours using the gradient assumption.
 *  It also gives the number of optimizations required for getting all the contours.
 *  
 *  -- Pseudocode --
 *  Input : QT's apkt file
 *  		Cost of the contour to be find out.
 *  
 *  Output : Set of points in the plan diagram(and Plans at those points) which covers a specified contour.
 *  
 *  Algorithm :
 *  
 *  Parse the apkt file to get the resolution, selectivities and cost at each point.
 *  Fix Alpha, e -> Error due to discretization..
 *  x_min = minimum Selectivity of the dimension
 *  While x_act >=1
 *  	x_act = x_min
 *  	do binary search to find y_act, s.t Cost(x_act, y_act) = [C-e, C+e]
 *  	if(alpha*x_min is not greater than 1)
 *  		Put the point (alpha*x_min, y_act) to the output list.
 *  	else
 *  		put(1,y_act) to output list.
 *  	x_act = alpha*x_act;
 *  	loop
 *  	
 *  -- Pseudocode Ends --
 *  
 *  Keep track of number of optimizations been called.
 *  Keep track of the total investment benifit
 *  	Call a function which actually gets the contours and gets the total investment.(Brute force case)
 *  
 * */
// ---------------------------------------------------------------------
/*
 * Doubts :
 *  1. Whether the apkt and pkt files are same ??
 *  
 * */



import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import iisc.dsl.picasso.server.db.Database;
import iisc.dsl.picasso.server.db.postgres.PostgresDatabase;

import javax.swing.plaf.metal.MetalIconFactory.FolderIcon16;
import javax.swing.text.html.MinimalHTMLWriter;

import org.omg.CORBA.portable.RemarshalException;

import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import iisc.dsl.picasso.common.ds.DataValues;
import iisc.dsl.picasso.server.ADiagramPacket;


public class GCI2DDriver
{
	
	int plans[];
	double OptimalCost[];
	int totalPlans;
	int dimension;
	int resolution;
	DataValues[] data;
	static int totalPoints;
	double selectivity[];

	//The following parameters has to be set manually for each query
	static String apktPath = "/home/dsladmin/Srinivas/data/DSQT962DR100_E/";
	static String plansPath = "/home/dsladmin/Srinivas/data/DSQT962DR100_E/";
	static String qtName = "DSQT962DR100_E" ;
	static Connection c = null;

	//This is for giving the order of predicates
 
	
	public static void main(String args[]) throws IOException, SQLException
	{
		GCI2DDriver obj = new GCI2DDriver();
		String pktPath = apktPath + qtName + ".apkt" ;
		System.out.println("Query Template: "+qtName);
	//	obj.clearCache();	/* to clear the OS/DB cache*/
	
		ADiagramPacket gdp = obj.getGDP(new File(pktPath));

		
		//Populate the OptimalCost Matrix.
		obj.readpkt(gdp); 			
		
		//Populate the selectivity Matrix.
		obj.loadSelectivity(); 	
		

	}





	
	public int getPlanNumber(int x, int y)
	{
		 int arr[] = {x,y};
		int index = getIndex(arr,resolution);
		return plans[index];
	}
	

	/*-------------------------------------------------------------------------------------------------------------------------
	 * Populates -->
	 * 	dimension
	 * 	resolution
	 * 	totalPoints
	 * 	OptimalCost[][]
	 * 
	 * */
	void readpkt(ADiagramPacket gdp) throws IOException
	{
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
			//System.out.println("Plan Number ="+plans[i]+"\n");
		//	System.out.println("\nOptimal_cost:["+i+"] ="+OptimalCost[i]+"\n");
		}
	
		//TO get the number of points for each plan
		int  [] plan_count = new int[totalPlans];
		for(int p=0;p<data.length;p++){
			plan_count[plans[p]]++;
		}
		//printing the above
		for(int p=0;p<plan_count.length;p++){
			System.out.println("Plan "+p+" has "+plan_count[p]+" points");
		}

	}
	
	double cost(int x, int y)
	{
		int [] arr = new int [2];
		arr[0] = x;
		arr[1] = y;
		int index = getIndex(arr,resolution);

	
		return OptimalCost[index];
	}
	double cost_matrix(int x, int y, double[] cost_matrix)
	{
		int [] arr = new int [2];
		arr[0] = x;
		arr[1] = y;
		int index = getIndex(arr,resolution);

		
		return cost_matrix[index];
	}
	//-------------------------------------------------------------------------------------------------------------------
	/*
	 * Populates the selectivity Matrix according to the input given
	 * */
	void loadSelectivity()
	{
		String funName = "loadSelectivity: ";
		System.out.println(funName+" Resolution = "+resolution);
		double sel;
		this.selectivity = new double [resolution];
		
		if(resolution == 10){
		selectivity[0] = 0.00005;	selectivity[1] = 0.0005;selectivity[2] = 0.005;	selectivity[3] = 0.02;
		selectivity[4] = 0.05;		selectivity[5] = 0.10;	selectivity[6] = 0.15;	selectivity[7] = 0.25;
		selectivity[8] = 0.50;		selectivity[9] = 0.99;                                 // oct - 2012
		}


		if(resolution == 20){
		selectivity[0] = 0.000005;		selectivity[1] = 0.00005;		selectivity[2] = 0.0005;	selectivity[3] = 0.002;
		selectivity[4] = 0.005;		selectivity[5] = 0.008;		selectivity[6] = 0.01;		selectivity[7] = 0.02;
		selectivity[8] = 0.03;			selectivity[9] = 0.04;			selectivity[10] = 0.05;	selectivity[11] = 0.08;
		selectivity[12] = 0.10; 		selectivity[13] = 0.15;		selectivity[14] = 0.20;	selectivity[15] = 0.30;
		selectivity[16] = 0.40;		selectivity[17] = 0.60;		selectivity[18]=0.80;		selectivity[19] = 0.99;
		}

		if(resolution == 30){
		selectivity[0] = 0.00002;  selectivity[1] = 0.00009;	selectivity[2] = 0.0002;	selectivity[3] = 0.0005;
		selectivity[4] = 0.0007;   selectivity[5] = 0.0010;	selectivity[6] = 0.0014;	selectivity[7] = 0.0019;
		selectivity[8] = 0.0026;	selectivity[9] = 0.0036;	selectivity[10] = 0.0048;	selectivity[11] = 0.0065;
		selectivity[12] = 0.0087;	selectivity[13] = 0.0117;	selectivity[14] = 0.0156;	selectivity[15] = 0.0208;
		selectivity[16] = 0.0278;	selectivity[17] = 0.0370;	selectivity[18] = 0.0493;	selectivity[19] = 0.0657;
		selectivity[20] = 0.0874;	selectivity[21] = 0.1164;	selectivity[22] = 0.1549;	selectivity[23] = 0.2061;
		selectivity[24] = 0.2741;	selectivity[25] = 0.3647;	selectivity[26] = 0.48515;	selectivity[27] = 0.6453;
		selectivity[28] = 0.8583;	selectivity[29] = 0.9950;		
		}
		
		if(resolution==100){
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
		//the selectivity distribution
		//System.out.println("The selectivity distribution using is ");
//		for(int i=0;i<resolution;i++)
//		System.out.println("\t"+selectivity[i]);
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
	double getOptimalCost(int index)
	{
		return this.OptimalCost[index];
	}
	

	public static boolean clearCache() 
	{
		boolean success = false;
//		String start = "/home/dsladmin/Srinivas/AnshPG/bin/pg_ctl -D /home/dsladmin/Srinivas/AnshPG/tpch/ -w start";
//		String stop = "/home/dsladmin/Srinivas/AnshPG/bin/pg_ctl -D /home/dsladmin/Srinivas/AnshPG/tpch/ -w stop";

			String[] cmd = {
					"/bin/sh",
					"-c",
					"echo 3 | sudo tee /proc/sys/vm/drop_caches"
			};
			Process p;
			try 
			{
				
				Runtime r = Runtime.getRuntime();
//				p = r.exec(stop);
//				p.waitFor();
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
				
//				p = r.exec(start);
//				p.waitFor();

			} catch (Exception e) 
			{
				e.printStackTrace();
			}
			
		
		return(success);
	}


}