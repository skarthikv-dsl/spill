package misc;



import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;

import iisc.dsl.picasso.common.ds.DataValues;
import iisc.dsl.picasso.server.ADiagramPacket;


public class basicCode
{
	static int UNI = 1;
	static int EXP = 2;
	static int selConf;
	
	double OptimalCost[];
	int totalPlans;
	int dimension;
	int resolution;
	DataValues[] data;
	int totalPoints;
	double selectivity[];
	
	public static void main(String args[])
	{
		String folderPath = "/home/dsladmin/Lohit/PG_APKT/UNI/2D/";
		String qtName = "POSTGRES_H2DQT8_300";
		selConf = UNI;
		
		String pktPath = folderPath + qtName + ".apkt" ;
		
		basicCode obj = new basicCode();
		
		//Populate the OptimalCost Matrix.
		obj.readpkt(pktPath);
		
		//Populate the selectivity Matrix.
		obj.loadSelectivity(UNI);
		
		System.out.println("\nMainFinished\n");
	}
	

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
		
		OptimalCost = new double [data.length]; 
		for (int i = 0;i < data.length;i++)
		{
			this.OptimalCost[i]= data[i].getCost();
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