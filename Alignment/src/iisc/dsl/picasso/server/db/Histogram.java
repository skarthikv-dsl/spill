
/*
 #
 #
 # PROGRAM INFORMATION
 #
 #
 # Copyright (C) 2006 Indian Institute of Science, Bangalore, India.
 # All rights reserved.
 #
 # This program is part of the Picasso Database Query Optimizer Visualizer
 # software distribution invented at the Database Systems Lab, Indian
 # Institute of Science (PI: Prof. Jayant R. Haritsa). The software is
 # free and its use is governed by the licensing agreement set up between
 # the copyright owner, Indian Institute of Science, and the licensee.
 # The software is distributed without any warranty; without even the
 # implied warranty of merchantability or fitness for a particular purpose.
 # The software includes external code modules, whose use is governed by
 # their own licensing conditions, which can be found in the Licenses file
 # of the Docs directory of the distribution.
 #
 #
 # The official project web-site is
 #     http://dsl.serc.iisc.ernet.in/projects/PICASSO/picasso.html
 # and the email contact address is
 #     picasso@dsl.serc.iisc.ernet.in
 #
 #
*/

package iisc.dsl.picasso.server.db;

import java.text.DecimalFormat;
import java.util.ListIterator;
import java.util.Vector;

import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.server.db.datatype.Datatype;
import iisc.dsl.picasso.server.network.ServerMessageUtil;
import iisc.dsl.picasso.server.plan.Plan;
import iisc.dsl.picasso.common.PicassoConstants;

abstract public class Histogram {
	abstract public String getConstant(double sel);
	// value of type Datatype and freq Integer
	protected Database db;
	protected String tabName, schema, attribName;
	protected int cardinality;
	protected String dType;
	protected Vector value, freq;
	private String constants[];
	private double selectivity[];

	public String getRelationName()
	{
		return tabName;
	}
	public String getSchema()
	{
		return schema;
	}
	public String getAttribName()
	{
		return attribName;
	}
	public String getAttribType()
	{
		return dType;
	}
	public int getCardinality()
	{
		return cardinality;
	}
	public void show()
	{
		ListIterator vit = value.listIterator();
		ListIterator fit = freq.listIterator();
		System.out.println("Table="+tabName+"\tAttrib="+attribName);
		while( vit.hasNext() && fit.hasNext()){
			System.out.println("Value: "+(Datatype)vit.next()+
					"\tFreq: "+(Integer)fit.next());
		}
	}
	public void genConstants(int resolution, String distribution, double startpoint, double endpoint)
	{
		double sel;
		constants = new String[resolution];
		selectivity = new double[resolution];
		sel= startpoint + ((endpoint - startpoint)/(2*resolution));
		//System.out.println("Relation "+tabName+"\tAttribute "+attribName+"\tCard "+cardinality);
		DecimalFormat df = new DecimalFormat("#0.00000");
		df.setMaximumFractionDigits(5);
		//apexp
		if(distribution.equals(PicassoConstants.UNIFORM_DISTRIBUTION))
		{	
			for(int i=0;i<resolution;i++){
				selectivity[i] = sel;
				constants[i] = getConstant(selectivity[i]);
				sel += ((endpoint - startpoint)/resolution);
				//System.out.println("Sel="+df.format(100*selectivity[i])+"\tConstant="+constants[i]);
			}
		}
		
		
		else if(PicassoConstants.newDistribution == 'E' && (resolution == 10 || resolution == 20)) {               
			if(resolution == 10) {
//				selectivity[0] = 0.005;		selectivity[1] = 0.01;	selectivity[2] = 0.02;	selectivity[3] = 0.05;
//				selectivity[4] = 0.1;		selectivity[5] = 0.15;	selectivity[6] = 0.30;	selectivity[7] = 0.50;
//				selectivity[8] = 0.70;		selectivity[9] = 0.90;
				
//				selectivity[0] = 0.0005;	selectivity[1] = 0.005;	selectivity[2] = 0.01;	selectivity[3] = 0.02;
//				selectivity[4] = 0.05;		selectivity[5] = 0.10;	selectivity[6] = 0.20;	selectivity[7] = 0.40;
//				selectivity[8] = 0.60;		selectivity[9] = 0.95;                               

				selectivity[0] = 0.00005;   selectivity[1] = 0.0005;selectivity[2] =0.005;  selectivity[3] = 0.02;
				selectivity[4] = 0.05;      selectivity[5] = 0.10;  selectivity[6] = 0.15;  selectivity[7] = 0.25; 
				selectivity[8] = 0.50;      selectivity[9] = 0.99;
			}
			else if(resolution == 20) {
				selectivity[0] = 0.0050;   selectivity[1] = 0.02;		selectivity[2] = 0.040;		selectivity[3] = 0.060;
				selectivity[4] = 0.080;   	selectivity[5] = 0.100;		selectivity[6] = 0.120;		selectivity[7] = 0.140;
				selectivity[8] = 0.160;		selectivity[9] = 0.180;		selectivity[10] = 0.200;	selectivity[11] = 0.250;
				selectivity[12] = 0.300;	selectivity[13] = 0.400;	selectivity[14] = 0.500;	selectivity[15] = 0.600;
				selectivity[16] = 0.700;	selectivity[17] = 0.800;	selectivity[18] = 0.900;	selectivity[19] = 0.9950;
			}
			
			
			for(int i=0;i<resolution;i++){
				constants[i] = getConstant(selectivity[i]);
			}			
		}

		
		else //if(distribution.startsWith(PicassoConstants.EXPONENTIAL_DISTRIBUTION))
		{
			//initialization will be overridden.
			double r=1.0;
			
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
				constants[i-1] = getConstant(selectivity[i-1]);
				//System.out.println("Sel="+df.format(100*selectivity[i-1])+"\tConstant="+constants[i-1]);
				curval*=r;
				if(i!=popu)
				sum+=(curval * (endpoint - startpoint));
				else
					sum+=(curval * (endpoint - startpoint))/2;
			}
		}
		sel=0; //for debug (breakpointing) purpose
		//end apexp
	}
	public String getConstant(int i)
	{
		if(constants!=null)
			return constants[i];
		return null;

	}
	public double getPicSel(int i)
	{
		if(selectivity!=null)
			return selectivity[i];
		return 0.0;
	}

	public double getPredSel(int i)
	{
		if(constants==null)
			return 0.0;
		try{
			if(cardinality>0){
				db.removeFromPlanTable(3000000);
				Plan plan;
				if(!(/*getDatatype(dType).equals("string") || */getDatatype(dType).equals("date") ))
				{
					plan = db.getPlan("select * from "+tabName+" where "+attribName+" <= "+constants[i],3000000);
				}
				else
				{
					plan = db.getPlan("select * from "+tabName+" where "+attribName+" <= '"+constants[i]+"'",3000000);
				}
				db.removeFromPlanTable(3000000);
				return plan.getCard()/cardinality;
			}
			return 0.0;
		}catch(Exception e){
			e.printStackTrace();
			ServerMessageUtil.SPrintToConsole("Error getting optimizer selectivity:"+e);
			return 0.0;
		}
	}
    public String getDatatype(String type)
    {
        if(PicassoConstants.INT_ALIASES.contains(type.toUpperCase()))
            return "integer";
        else if(PicassoConstants.REAL_ALIASES.contains(type.toUpperCase()))
            return "real";
        else if(PicassoConstants.STRING_ALIASES.contains(type.toUpperCase()))
            return "string";
        else if(PicassoConstants.DATE_ALIASES.contains(type.toUpperCase()))
            return "date";
        else{
            ServerMessageUtil.SPrintToConsole("Unknown datatype="+type+" for table "+tabName+" attribute "+attribName);
            return "invalid";
        }
    }
}
