
//import dsl.picasso.common.ds.DiagramPacket;
package iisc.dsl.picasso.common;
import iisc.dsl.picasso.common.PicassoConstants;
import iisc.dsl.picasso.common.ds.DataValues;
import iisc.dsl.picasso.common.ds.DiagramPacket;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.HashSet;
import java.text.DecimalFormat;




public class SERF_codeN {

	//GLOBAL variables
	
	static class Set {
		HashSet elements = new HashSet();
	}
	
	static String serfOutFile = new String(HardCoded.serfCalPath+HardCoded.queryTemp+"\\Output\\");
//	static String serfOutFile = new String(path);
	static String outputType = "";
		
	static String query = "SQL SERVER_Default_U_"+HardCoded.queryTemp+"_";
	static String qNameOrig = query + "P.pkt";
	static String qNameEX = query + "R_UnionExpanded"+".pkt";




	static String path = HardCoded.serfCalPath+HardCoded.queryTemp+"\\";
//	static String path = "/media/Dataless DB/DSL-Data/Data/Seer-Pkts/default/TPCH/Uniform/";

	static String pktPath =  path;

	static String pcstPath = path;
	
	
	static int numPlansSeer = 0;
	static int numPlansLiteseer = 0;
	static int numPlansPrefacto = 0;
	static int numPlans = 0;
	static int numPlansCG = 0;
	
	static boolean both = false;
	static boolean prefactoOnly = false;

	static boolean writeFile = true;
	static boolean onScreenDisplay = true;

	static boolean oldserf = false;
	static boolean doOriginal = true;
	static boolean doCG = true;
	static boolean doSeer = true;
	static boolean doLiteSeer = true;
	static boolean doPrefacto = false;
	static boolean doRqep = true;  //Expanded 2,3,4,5,6 or 2
	
	static int ANOREXIC = 0;
	static int BENEFIT = 1;
	static int NEW_BENEFIT = 2;

	static int choiceGreedy = ANOREXIC;                // whether or not countGreedy i.e. anorexia-greedy
//	static int choiceGreedy = NEW_BENEFIT;                // whether or not countGreedy i.e. anorexia-greedy

	


	static boolean Mapping = false;
//	static boolean Mapping = true;
	

//	static int SANITY_CONSTANT = 10000;
	static int SANITY_CONSTANT = 0;
	static int minSerfSanity = 10000;

	static double[][] coeff;
	static double[][][] new_coeff;
	static boolean isDelightFul = false;
	static boolean isStability = false;
	static long   ptCnt = -1;

	
	static int plansPrefacto[];
	static int plansOriginal[];
	static boolean CCseer_allowed[]; 

	public static double[][] AllPlanCosts;
	static int[] offset;
	static int[] base;
	static double th = 20;
	static float[] sel;
	//GLOBAL variables
	static PrintWriter log;
	
	
	public static void setAllPlanCosts( double[][] allPlanCosts)
	{
		AllPlanCosts = allPlanCosts;
	}
	public static void setBase( int[] b)
	{
		base=b;
	}
	public static void setOffset( int[] o)
	{
		offset = o;
	}
	public static void setSel( float[] s)
	{
		sel = s;
	}
	
	public static HashMap<Integer,double[]> AdditionalPlansCost = null;
	
	public static void setAdditionalPlansCost(HashMap addPlanCosts)
	{
		AdditionalPlansCost = addPlanCosts;
	}
	
	public static int gdpPlans;
	
	public static void setGDP(DiagramPacket ogdp)
	{
		gdp = ogdp;
		gdpPlans = gdp.getMaxPlanNumber();
		try {
		setupFiles();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	public static void start(DiagramPacket ogdp, DiagramPacket egdp) throws FileNotFoundException {
			
		try {
			
			gdp = ogdp;
			qNameOrig = query + "P.pkt";
			serfOutFile = new String(HardCoded.serfCalPath+HardCoded.queryTemp+"\\Output\\sanity_const_"+HardCoded.SanityConstant+"\\");
			log = new PrintWriter(new File(serfOutFile+"log.txt"));
			calculateSerf(egdp);
			
			log.println("Finished");
			log.close();
			
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}	

	static int Original = 0;
	static int CG = 1;
	static int SEER = 2;
	static int LSEER = 3;
	static int PREFACTO = 9;  // Dont-Do
	static int RQEP = 4;
	//static int[] RQEP = {0};  // numReductionAlgos = 1

	static int NUM_PLANS = 0;
	static int MINSERF = 1;
	static int NETSERF = 2;
	static int REP = 3;
	static int HELP = 4;
	static int HARM = 5;
	static int NEED = 6;
	static int DYNAMIC_MINSERF_THRESHOLD = 7;


	static int numPackets = 1;
	static int numPerformanceParameters = 8;
	static int numReductionAlgos = 5;

	static double performance[][][];
	static	DecimalFormat twoDForm;
	static	DecimalFormat oneDForm;

	static String[] templates;
	static int[] totalPlans;

	static DiagramPacket gdp =null; 	
	static DataValues [] data;

	public static void calculateSerf(DiagramPacket egdp){
		
		templates = new String[numPackets];
		totalPlans = new int[numPackets];

//		templates[0] = "sqlserver_tpch_U_30_Q2";
//		templates[1] = "sqlserver_tpch_U_30_Q5";			
//		templates[2] = "sqlserver_tpch_U_30_Q8";			
//		templates[3] = "sqlserver_tpch_U_30_Q9";			
//		templates[4] = "sqlserver_tpch_U_30_Q10";			
//		templates[5] = "sqlserver_tpch_U_30_Q16";						

		templates[0] = "SQL SERVER_Default_U_"+HardCoded.queryTemp;
//		templates[1] = "sqlserver_tpch_U_10_Q8_3d";			
//		templates[2] = "sqlserver_tpch_U_10_Q10_3d";			



		twoDForm = new DecimalFormat("#.##");
		oneDForm = new DecimalFormat("#.#");

		performance = new double[numPerformanceParameters][numReductionAlgos][numPackets];

	
		for(int i = 0; i< numPackets; i++)
		{
			/* - Read Early and passed as argument. Do only the initialization. */
			try{
			setupFiles();
			}
			catch(Exception e){
				e.printStackTrace();
			}
			
			qNameOrig = templates[i];
			log.println(qNameOrig);		
			
			DiagramPacket newGdp1 = null;

			if(gdp.getDimension() == 2)
			{
				if(doOriginal){	
					System.out.println(" SERFCalculation - Original");
					outputType = "-Original";
					newGdp1 = gdp;
					SerfCal(gdp, newGdp1, th, Original, i);
				}
				if(doCG){	
					System.out.println(" SERFCalculation - CG");
					outputType = "-CG";
					newGdp1 = reduce2D(th, gdp);
					SerfCal(gdp, newGdp1, th, CG, i);
				}
	
				if(doSeer){
					System.out.println(" SERFCalculation - SEER");
					outputType = "-seer";
					newGdp1 = hcSeer(th, gdp);      // to compare old and new seer
				//	newGdp1 = reduceND(gdp, (float) th , (float) 1.0 , dim);
				//	newGdp1 = seer(th, gdp);
					SerfCal(gdp, newGdp1, th , SEER, i );
				}
	
				if(doLiteSeer){
					System.out.println(" SERFCalculation - LiteSEER");
					outputType = "-liteseer";
					if(choiceGreedy == NEW_BENEFIT)
						newGdp1 = liteSeerBenefit(th, gdp);
					else
						newGdp1 = liteSeer(th, gdp);

					SerfCal(gdp, newGdp1, th , LSEER, i);
				}
				if(doPrefacto){
					outputType = "-prefacto";
					newGdp1 = getGDP(new File(pktPath + qNameEX+".apkt"));
					SerfCal(gdp, newGdp1, th, PREFACTO, i);					
				}
				if(doRqep){
					System.out.println("SERFCalculation - RQEP");
					//qNameOrig = query + "P.pkt";
					qNameEX = query + "R_UnionExpanded"+".pkt";
					outputType = "-rqep-";
					//newGdp1 = getGDP(new File(pktPath + qNameEX));
					newGdp1 = egdp;
					log.println(" pkt - "+pktPath+qNameEX+"   : "+RQEP+" planNo - "+newGdp1.getMaxPlanNumber());
					SerfCal(gdp, newGdp1, th, RQEP, i);		
				}
				
				
			}

			if(gdp.getDimension() == 3)
			{
				if(doCG){
					outputType = "-CG";
				//	newGdp1 = fastReduce3D(gdp,th);
					newGdp1 = reduce3D(th, gdp);   // to compare old and new seer		
					SerfCal(gdp, newGdp1, th , CG, i);
				}

				if(doSeer){
					outputType = "-seer";
					newGdp1 = hcSeer(th, gdp);
					
					//newGdp1 = reduceND(gdp, (float) th , (float) 1.0 , dim);
					SerfCal(gdp, newGdp1, th , SEER, i);
				}

				if(doLiteSeer){
					outputType = "-liteseer";

					if(choiceGreedy == NEW_BENEFIT)
						newGdp1 = liteSeerBenefit(th, gdp);
					else
						newGdp1 = liteSeer3D(th, gdp);

					SerfCal(gdp, newGdp1, th , LSEER, i);
				}
			}

		}


		try{
			PrintWriter pw = new PrintWriter(new File(serfOutFile+"performace.txt"));
			//pw.println("PERFORMANCE\t\t\t\t\t\tCOSTGREEDY\t\t\t\t\t\t\t\tSEER\t\t\t\t\t\t\t\tLSEER\t\t\t\t\t\t\t\tEXPAND" );
			pw.println("PERFORMANCE\t\t\t\t\t\tORIGINAL\t\t\t\t\t\t\t\tCOSTGREEDY\t\t\t\t\t\t\t\tSEER\t\t\t\t\t\t\t\tLSEER\t\t\t\t\t\t\t\tRQEP\t\t\t\t\t\t\t\t" );
			pw.println("----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
			pw.println("TEMPLATE (TOTAL PLANS)\t\t\t\tPLANS\tMIN(DMST)\tAGG\tREP\tHELP\tHARM\tNEED\t\tPLANS\tMIN(DMST)\tAGG\tREP\tHELP\tHARM\tNEED\t\tPLANS\tMIN(DMST)\tAGG\tREP\tHELP\tHARM\tNEED\t\tPLANS\tMIN(DMST)\tAGG\tREP\tHELP\tHARM\tNEED\t\tPLANS\tMIN(DMST)\tAGG\tREP\tHELP\tHARM\tNEED");
			pw.println("----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");

		
			for(int i=0;i<numPackets;i++)
			{
				pw.print(templates[i] + " (" + totalPlans[i] + ")" + "\t");				
				for(int j=0; j <  numReductionAlgos; j++)
				{
					for(int k=0; k < numPerformanceParameters-1; k++)
					{					 
						if(k==0)	
						{
							int plans = (int) performance[k][j][i];						
							pw.print("\t\t" + plans); 
						}
						else if(k==1)
							pw.print("\t" + Double.valueOf(twoDForm.format(performance[k][j][i]))+"("+Double.valueOf(twoDForm.format(performance[numPerformanceParameters-1][j][i]))+")");
						else if ( k > 0 && k < 7)
							pw.print("\t" + Double.valueOf(twoDForm.format(performance[k][j][i])));
						else 
							//pw.print("\t" + Math.round(performance[k][j][i]));
							pw.print("\t" + performance[k][j][i]);
					}
				}
				pw.print("\n");	
			}


			pw.close();
		}catch(Exception e){
			log.println("Error while saving performance file");
			e.printStackTrace();
		}



	}



	static int nPlans;



	public static void setupFiles() throws IOException {
		
		int n;
		
		//log.println(pktPath + qNameOrig);
		
		//gdp = getGDP(new File(pktPath + qNameOrig));

		n = gdp.getMaxPlanNumber();
		
		nPlans = n;		
		
		data = gdp.getData();
		sel = gdp.getPicassoSelectivity();
		dim = gdp.getDimension();    // we needed a global var dim
	   /*
		int d = dim;

		AllPlanCosts = new double[n][data.length];
	
		
		for (int i = 0; i < n; i++) {
			try {
				ObjectInputStream ip = new ObjectInputStream(new FileInputStream(new File(pcstPath + i + ".pcst")));
				double[] cost = (double[])ip.readObject();
				
				
				for(int j = 0; j < data.length; j++) 
					AllPlanCosts[i][j] = cost[j];
			} 
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		*/
	}
	
	


	static int SANITY_1SEC = HardCoded.SanityConstant;
	

	static void SerfCal(DiagramPacket gdp, DiagramPacket ngdp, double th, int rednAlgo, int template) {

		th = 1 + th / 100;
		double minSerf = Double.MAX_VALUE, maxSerf = Double.MIN_VALUE;
		double dynamicSerfHigherBound = -1.2;
		double MindynamicSerfHigherBound = -1.2;
		double serf = 0.0, pos_serf = 0.0, neg_serf = 0.0,netSerf = 0.0;
		double totalSerf = 0.0;
		double help = 0.0;
		double harm = 0.0;
		

		int replacedPts = 0;
		DataValues[] data = gdp.getData();
		DataValues[] newData = ngdp.getData();
		int dim = gdp.getDimension();

		long wrongPts = 0, endopts = 0, boringpts = 0, interestPts = 0, positive_pts=0, neg_pts = 0, zero_pts = 0, needful_loc = 0;
		long unsafePts = 0;
		if(rednAlgo == PREFACTO){
			int area[] = new int [200];
			for(int i = 0; i < newData.length; i++)
				area[newData[i].getPlanNumber()]++;
		
			int count = 0;
			for(int i = 0; i < area.length; i++)
				if(area[i] > 0)			count++;
			
			ngdp.setMaxPlanNumber(count);
		}
		
		
		
		
		totalPlans[template] = gdp.getMaxPlanNumber();
		performance[NUM_PLANS][rednAlgo][template] = ngdp.getMaxPlanNumber();
		
		for (int i = 0; i < data.length; i++) {
				
			int poe = data[i].getPlanNumber();
			int pre = newData[i].getPlanNumber();
			
			if(poe != pre)				{replacedPts++;}
			else if(oldserf == true)		{continue;}

			
			//if((i+1)%100 == 0) 
				//log.print("rednAlgo : "+rednAlgo+"/"+numReductionAlgos+" - "+new Double((double)i/data.length * 100).intValue() + "% " + "Aggserf = " + totalSerf +" Goodpoints = " + interestPts + "  netserf = " + totalSerf/interestPts + " plans = (" + poe + ", " + pre +  ")\n");
			
			
			for (int j = 0; j < data.length; j++){
				double coe = AllPlanCosts[poe][j];
				double cre;
				if(pre >= gdpPlans)
				{
					double cost[] = AdditionalPlansCost.get(new Integer(pre));
					cre = cost[j];
				}
				else
					cre = AllPlanCosts[pre][j];
				int    poa = data[j].getPlanNumber();
				double coa;
				if(poa >= gdpPlans)
				{
					double cost[] = AdditionalPlansCost.get(new Integer(poa));
					coa = cost[j];
				}
				else
					coa = AllPlanCosts[poa][j];
				
				double BOUND = -10.0;

				if (coe < coa - 0.1) {	wrongPts++;	coe = coa + 0.01; }    // make costs logically ordered
				if (cre < coa - 0.1) {			cre = coa + 0.01; }
			

				BOUND = Math.max(BOUND, th * coa - coa);
				BOUND = Math.max(BOUND, SANITY_1SEC);				// find the correct BOUND - which is not too small


				double num = -10.0, den = -10.0;

				if(oldserf == false) 		{den = BOUND;		den = Math.max(den,  coe - coa);}
				if(oldserf == true)			den = th * coe - coa;

				num = cre - coa;
				num = Math.max(num, SANITY_1SEC);

				serf = 1.0 - num/den;		

			//	if( i == 875 && (j >= 0 && j <= 29 || j >= 870 && j <= 899)){
			//		DecimalFormat twoDForm = new DecimalFormat("#.##");
			//		log.println("(" + i + ", " + j + ")  is a interesting point " + BOUND + "::" + Double.valueOf(twoDForm.format(serf)) + " coe=" + coe + " cre=" + cre + " coa=" + coa + " poe=" + poe + " pre=" + pre );	
			//		}
				dynamicSerfHigherBound = Math.min(-0.2, 1.0 - ( Math.max((th*coe - coa), SANITY_1SEC) / den) );

				if(poe == poa)
				{
					endopts++;
					continue;
				}
				
				if(Double.compare(th*coe,cre) < 0)
				{
					unsafePts++;
					/*
					if(rednAlgo != CG)
						log.println("(" + i + ", " + j + ")  unsafe point ::  coe=" + coe + " cre=" + cre + " coa=" + coa + " poe=" + poe + " pre=" + pre );
						*/
				}
				if(serf < minSerf){
					MindynamicSerfHigherBound = dynamicSerfHigherBound;
					log.println("(" + i + ", " + j + ")  minserf point ::" + minSerf + " to " + serf + "( " +dynamicSerfHigherBound+" )" +" coe=" + coe + " cre=" + cre + " coa=" + coa + " poe=" + poe + " pre=" + pre );
				}
				minSerf = Math.min(minSerf, serf);
				
				
				if(coe - coa <= BOUND && cre - coa <= BOUND)
				{
					boringpts++;
					DecimalFormat twoDForm = new DecimalFormat("#.##");
				//	log.println("(" + i + ", " + j + ")  is a boring point " + BOUND + "::" + Double.valueOf(twoDForm.format(serf)) + " coe=" + coe + " cre=" + cre + " coa=" + coa );//+ " poe=" + poe + " pre=" + pre );	
					if	(serf < dynamicSerfHigherBound)	{harm = harm + 1.0;}
					continue;	
				}
	
				
				maxSerf = Math.max(maxSerf, serf);

				if      (serf > 0.0)	{positive_pts++;	pos_serf += serf;}
				else if (serf < 0.0)	{neg_pts++;		neg_serf += serf;}
				else			{zero_pts++;					 }	

				if	(serf > 0.66)	{help = help + 1.0;}
				if	(serf < dynamicSerfHigherBound)	{harm = harm + 1.0;}

				totalSerf += serf;
				interestPts++;

			}
		}


		help = help * 100 /interestPts;
		harm = harm * 100 /(interestPts + boringpts);

		if(minSerf == Double.MAX_VALUE)			minSerf = 0;

		netSerf = (totalSerf/(double)interestPts);


		performance[MINSERF][rednAlgo][template] = minSerf;
		performance[NETSERF][rednAlgo][template] = netSerf;
		performance[REP][rednAlgo][template] = replacedPts * 100.0/data.length;
		performance[HELP][rednAlgo][template] = help;
		performance[HARM][rednAlgo][template] = harm;
		performance[NEED][rednAlgo][template] = interestPts * 100.0 / (data.length * data.length);
		performance[DYNAMIC_MINSERF_THRESHOLD][rednAlgo][template] = MindynamicSerfHigherBound;

		if(onScreenDisplay == true){
		log.println("Replaced Points: " + replacedPts);
		log.println("Minserf value is  : " + minSerf + "("+neg_pts+")"+"DynamicThreshold : "+MindynamicSerfHigherBound);
		log.println("% of negative pts : " + (neg_pts * 100 / (data.length * data.length)));
		log.println("Total serf value is  : " + totalSerf);
		log.println("Aggserf value is  : " + netSerf);
		log.println("The maxserf value is  : " + maxSerf);
		log.println("Wrong points : "+wrongPts);
		log.println("Interesting points: "+interestPts+", Endo-optimal points: "+endopts+", Boring points: "+boringpts +  " Positive pts = " + positive_pts + "   negative points = " + neg_pts + "   zero points = " + zero_pts);
		log.println("Help% : "+help);
		log.println("Harm% : "+harm);
		log.println("+ve = "+pos_serf + "   -ve ="+neg_serf);
		log.println("Unsafe Points : "+unsafePts);
		log.println();
		}

		if(writeFile){
		
		try{
			PrintWriter pw = new PrintWriter(new File(serfOutFile+qNameOrig+outputType+".txt"));
			pw.println("SERF DISTRIBUTION");
			pw.println("------------------------------------------");
			
//			pw.println("prefacto plans = " + numPlansPrefacto);

			pw.println("costgreedy plans = " + numPlansCG);
			pw.println("liteseer plans = " + numPlansLiteseer);
			pw.println("Seer plans = " + numPlansSeer);
			pw.println("plans = " + numPlans);
			
			pw.println("Replacements : "+replacedPts);
			pw.println("The minserf value is  : " + minSerf + "("+neg_pts+")"+"DynamicThreshold : "+MindynamicSerfHigherBound);
			pw.println("The total serf value is  : " + totalSerf);
			pw.println("The Aggserf value is  : " + netSerf);
			pw.println("% of negative pts : " + (neg_pts * 100 / interestPts));
			pw.println("The maxserf value is  : " + maxSerf);
			pw.println("Wrong Points : "+wrongPts);
			pw.println("Interesting points: "+interestPts+", Endo-optimal points: "+endopts+", Boring points: "+boringpts +  " Positive pts = " + positive_pts + " Non-negative points = " + neg_pts + "zero_pts = " + zero_pts);
			pw.println("Help% : "+ help);
			pw.println("Harm% : "+ harm);
			pw.println("+ve = "+pos_serf + "   -ve ="+neg_serf);
			pw.println("Unsafe Points : "+unsafePts);
			pw.close();
		}catch(Exception e){
			log.println("Error while saving serf to file");
			e.printStackTrace();
		}
		}

	}

	
	

	
	
	static int[] plan = null;

	public static DiagramPacket liteSeer(double threshold, DiagramPacket gdp) {

		long start = System.currentTimeMillis();
		DiagramPacket ngdp = new DiagramPacket(gdp);

		int n;
		
		n = gdp.getMaxPlanNumber();
				
		numPlans = n;
		
		HashSet[] s = new HashSet[n];
		Integer[] x = new Integer[n];
		boolean[] notSwallowed = new boolean[n];
		DataValues[] data = gdp.getData();
		DataValues[] newData = new DataValues[data.length];
		boolean[][] swallow = new boolean[n][n];
		int res = gdp.getMaxResolution();

		sel = gdp.getPicassoSelectivity();
		int d = gdp.getDimension();

		int[] r	= new int[d];
		for (int i = 0; i < d; i++)
			r[i] = res;


		offset = new int[d];
		base = new int[d];

		// initialize offset
		offset[0] = 0;
		for (int i = 1; i < d; i++)
			offset[i] = offset[i - 1] + r[i - 1];

		// initialize base
		base[0] = 1;
		for (int i = 1; i < d; i++)
			base[i] = base[i - 1] * r[i - 1];

		ArrayList points = new ArrayList();
		ArrayList virtualPts = new ArrayList();
		int[] selPt = new int[d];

		for (int i = 0; i < n; i++) {
			s[i] = new HashSet();
			x[i] = new Integer(i);
			s[i].add(x[i]);
		}

		ArrayList slopeDim = new ArrayList();
		ArrayList slopeDimVal = new ArrayList();
		int[] dim = new int[d];
		for (int i = 0; i < d; i++)
			dim[i] = i;

		int[][] selpt = new int[4][2];

		selpt[0][0] = 0;
		selpt[0][1] = 0;
		selpt[1][0] = 0;
		selpt[1][1] = r[1] - 1;
		selpt[2][0] = r[0] - 1;
		selpt[2][1] = r[1] - 1;
		selpt[3][0] = r[0] - 1;
		selpt[3][1] = 0;

		int ctr = 0;
		
		int[] area = new int[n];
		for(int i = 0;i < data.length;i++)
			area[data[i].getPlanNumber()]++;
		
		double[] stability = new double[n];
		for(int i = 0;i < n;i++){
			stability[i] = 0.0;
			for(int j = 0;j < 4;j++){
				stability[i] += Cost(i, PtIndex(selpt[j], r, 2));
			}
		}
		
		for (int i = 0; i < n; i++) {
			boolean flag = false;
			for (int j = 0; j < n; j++) {
				// j -> swallower & i -> swallowee
				if (i != j) {
					ctr = 0;
					if(isDelightFul)
					{
						if (Cost(j, PtIndex(selpt[2], r, 2)) <= (1 + th / 100) * Cost(i, PtIndex(selpt[2], r, 2))){
							if (Cost(j, PtIndex(selpt[0], r, 2)) <= (1 + th / 100) * Cost(i, PtIndex(selpt[0], r, 2)))
								ctr = 4;
							
						}
					}
					else{
						for (int k = 0; k < 4; k++) {
							if (Cost(j, PtIndex(selpt[k], r, 2)) > (1 + th / 100) * Cost(i, PtIndex(selpt[k], r, 2))){
								break;
							}else{
								ctr++;
							}
						}
					}
					if (ctr == 4) {
						s[j].add(x[i]);
						flag = true;
						swallow[j][i] = true;
					}
				}
			}
			if (!flag) {
				notSwallowed[i] = true;
			}
		}

		ArrayList soln = new ArrayList();
		HashSet temp = new HashSet();


		
		log.print("\n\nIn liteseer\n\n");
		for(int i=0;i<n;i++){
			log.print("\n" + i + " (" + area[i] + " swallows = ");
			for(int j=0;j<n;j++){
				if(s[i].contains(x[j]))					log.print(" " + j + " ");
			}
		}
		
		
		
		
		if(choiceGreedy == ANOREXIC) 
		{	

			for (int i = 0; i < n; i++) {
				if (notSwallowed[i] && s[i] != null) {
					//i can't be swallowed but i can swallow someone
					temp.addAll(s[i]);
					s[i].clear();
					s[i] = null;
					soln.add(new Integer(i));
				}
			}
			//temp contains all plans which can be swallowed by plans which can't be swallowed
			for (int i = 0; i < n; i++) {
				//i can swallow someone
				if (s[i] != null) {
					s[i].removeAll(temp);//remove plans which are already swallowed
				}
			}
			//s[i] contain plans not swallowed till now
			while (true) {
				int max = 0;
				int p = -1;
				for (int i = 0; i < n; i++) {
					if (s[i] != null) {
						int size = s[i].size();
						if (size > max) {
							max = size;
							p = i;
						}/*else if(size == max){
							if(isStability && stability[p] < stability[i])
							{
								p = i;
							}
							else if(!isStability && area[p] < area[i])
							{
								p = i;							
							}
						}*/
					}
				}
				//p is the plan which can swallow max number of plans
				if (p == -1) {
					break;
				}
				soln.add(new Integer(p));
				for (int i = 0; i < n; i++) {
					if (s[i] != null && i != p) {
						s[i].removeAll(s[p]);
						if (s[i].size() == 0) {
							s[i] = null;
						}
					}
				}
				s[p] = null;
			}
	
		}
	
		else if(choiceGreedy == BENEFIT)
		{
			for (int i = 0; i < n; i++) {
//				if (notSwallowed[i] ){ 			  // those which are non-eatable
				if (notSwallowed[i] && s[i] != null) {   //i can't be swallowed but i can swallow someone
					
					temp.addAll(s[i]);          // remove all which can be eaten by non-eatables
					//temp.add(i);		      // remove only non-eatables
	
					s[i].clear();		      // in benefit greedy - they have to still compete in the eating process - if commented
					s[i] = null;
					soln.add(new Integer(i));
				}
			}
				//temp contains all plans which can be swallowed by plans which can't be swallowed - old

				//temp contains all plans which can't be swallowed - new   --> so no need to remove them from others 
			for (int i = 0; i < n; i++) {
				//i can swallow someone
				if (s[i] != null) {
					s[i].removeAll(temp);//remove plans which are already swallowed
				}
			}



			//s[i] contain plans not swallowed till now

			while (true) {
				double maxBenefit = Double.MAX_VALUE;
				int p = -1;
				for (int i = 0; i < n; i++) {
					if (s[i] != null) {
						double benefit = stability[i];
						if (benefit < maxBenefit) {		// maxBenefit corresponds to minSumCorners
							maxBenefit = benefit;
							p = i;
						}
					}
				}
				//p is the plan which can swallow max number of plans
				if (p == -1) {
					break;
				}
				soln.add(new Integer(p));
				for (int i = 0; i < n; i++) {
					if (s[i] != null && i != p) {
						s[i].removeAll(s[p]);
						if (s[i].size() == 0) {
							s[i] = null;
						}
					}
				}
				s[p] = null;
			}
		}

		
		log.print("\n");
		for(int i=0; i<data.length; i++) {
			if(soln.contains(new Integer(data[i].getPlanNumber())))
				log.print("(" + i % res + ", " + i/res + ")-" + data[i].getPlanNumber()+ "  ");
				
		}
		
		
		// soln has the required soln

		log.println("\n# plans in Reduced Diagram : " + soln.size());
		log.print("Plans : ");
		for(Iterator it = soln.iterator();it.hasNext();) {
			Integer pl = (Integer) it.next();
			log.print(pl + ",");
		}
		log.println();

//		log.println("Number of plans in liteseer reduced diagram : "				+ soln.size());
		
		numPlansLiteseer = soln.size();
		
		plan = new int[n];
		for (int i = 0; i < n; i++) {
			if (soln.contains(x[i])) {
				plan[i] = i;
			} else {
				for (Iterator it = soln.iterator(); it.hasNext();) {
					Integer xx = (Integer) it.next();
					if (swallow[xx.intValue()][i]) {
						plan[i] = xx.intValue();
						break;
					}
				}
			}
		}




		for (int i = 0; i < data.length; i++) {
			int pi = data[i].getPlanNumber();
			newData[i] = new DataValues();
			newData[i].setCard(data[i].getCard());
			newData[i].setPlanNumber(plan[pi]);
		}
		ngdp.setDataPoints(newData);
		
		int area_actual[] = new int[n];
		int area_reduced[] = new int[n];
		int tot_points = data.length;
		int rep_points = 0;
		for(int i=0;i<n;i++)
		{
			area_actual[i]=0;
			area_reduced[i]=0;
		}		
		for(int i=0;i<tot_points;i++)
		{
			area_actual[data[i].getPlanNumber()]++;
			area_reduced[newData[i].getPlanNumber()]++;
			if(data[i].getPlanNumber() != newData[i].getPlanNumber())
				rep_points++;
		}		
		
		/*for(int i=0;i<gdp.getMaxPlanNumber();i++)
		{
			log.println("Plan " + i +" : Actual Area=" + area_actual[i] + ",Reduced Area="+area_reduced[i]);
		}*/
	//	log.println("Replaced Points:"+rep_points);
		/*double maxBI = Double.MIN_VALUE;
		
		
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				
				if (i != j && plan[i] == j) {
					double BI = ComputeBenefit(j, i);
					maxBI = Math.max(BI, maxBI);
					log.println("Benefit Index : " + BI);
				}
			}
		}
		log.println("Max benefit index : " + maxBI);*/
		ngdp.setMaxPlanNumber(soln.size());
		return ngdp;

	}
	
	


//works for 2D and 3D for now
	public static DiagramPacket liteSeerBenefit(double threshold, DiagramPacket gdp) {


		long start = System.currentTimeMillis();
		DiagramPacket ngdp = new DiagramPacket(gdp);

		int n;
		
		n = gdp.getMaxPlanNumber();
				
		numPlans = n;
		
		HashSet[] s = new HashSet[n];
		Integer[] x = new Integer[n];
		boolean[] notSwallowed = new boolean[n];
		DataValues[] data = gdp.getData();
		DataValues[] newData = new DataValues[data.length];
		boolean[][] swallow = new boolean[n][n];
		int res = gdp.getMaxResolution();

		sel = gdp.getPicassoSelectivity();
		int d = gdp.getDimension();


		int[] r	= new int[d];
		for (int i = 0; i < d; i++)
			r[i] = res;




		offset = new int[d];
		base = new int[d];

		// initialize offset
		offset[0] = 0;
		for (int i = 1; i < d; i++)
			offset[i] = offset[i - 1] + r[i - 1];

		// initialize base
		base[0] = 1;
		for (int i = 1; i < d; i++)
			base[i] = base[i - 1] * r[i - 1];

		ArrayList points = new ArrayList();
		ArrayList virtualPts = new ArrayList();
		int[] selPt = new int[d];

		for (int i = 0; i < n; i++) {
			s[i] = new HashSet();
			x[i] = new Integer(i);
			s[i].add(x[i]);
		}

		ArrayList slopeDim = new ArrayList();
		ArrayList slopeDimVal = new ArrayList();
		int[] dim = new int[d];
		for (int i = 0; i < d; i++)
			dim[i] = i;


		int numPoints = (int) Math.pow(2,d);           // to make 2D to generic


		int[][] selpt = new int[numPoints][d];

		if(d == 2) {		
			selpt[0][0] = 0;
			selpt[0][1] = 0;
			selpt[1][0] = 0;
			selpt[1][1] = r[1] - 1;
			selpt[2][0] = r[0] - 1;
			selpt[2][1] = r[1] - 1;
			selpt[3][0] = r[0] - 1;
			selpt[3][1] = 0;
		}
		else if ( d == 3) {

			selpt[0][0] = 0;
			selpt[0][1] = 0;
			selpt[0][2] = 0;
		
			selpt[1][0] = r[0] - 1;
			selpt[1][1] = 0;
			selpt[1][2] = 0;
		

			selpt[2][0] = r[0] - 1;
			selpt[2][1] = r[1] - 1;
			selpt[2][2] = 0;
		
			selpt[3][0] = 0;
			selpt[3][1] = r[1] - 1;
			selpt[3][2] = 0;
		
			selpt[4][0] = 0;
			selpt[4][1] = 0;
			selpt[4][2] = r[2] - 1;
		
			selpt[5][0] = r[0] - 1;
			selpt[5][1] = 0;
			selpt[5][2] = r[2] - 1;
		
			selpt[6][0] = r[0] - 1;
			selpt[6][1] = r[1] - 1;
			selpt[6][2] = r[2] - 1;
		
			selpt[7][0] = 0;
			selpt[7][1] = r[1] - 1;
			selpt[7][2] = r[2] - 1;
		
		}
////////////////////////////////////////////////////


		int ctr = 0;
		
		int[] area = new int[n];
		for(int i = 0;i < data.length;i++)
			area[data[i].getPlanNumber()]++;
		
		double[] stability = new double[n];
		for(int i = 0;i < n;i++){
			stability[i] = 0.0;
			for(int j = 0;j < numPoints;j++){
				stability[i] += Cost(i, PtIndex(selpt[j], r, d));
			}
		}
		
		for (int i = 0; i < n; i++) {
			boolean flag = false;
			for (int j = 0; j < n; j++) {
				// j -> swallower & i -> swallowee
				if (i != j) {
					ctr = 0;
					if(isDelightFul)
					{
						if (Cost(j, PtIndex(selpt[numPoints-2], r, d)) <= (1 + th / 100) * Cost(i, PtIndex(selpt[numPoints-2], r, d))){
							if (Cost(j, PtIndex(selpt[0], r, d)) <= (1 + th / 100) * Cost(i, PtIndex(selpt[0], r, d)))
								ctr = numPoints;
							
						}
					}
					else{
						for (int k = 0; k < numPoints; k++) {
							if (Cost(j, PtIndex(selpt[k], r, d)) > (1 + th / 100) * Cost(i, PtIndex(selpt[k], r, d))){
								break;
							}else{
								ctr++;
							}
						}
					}
					if (ctr == numPoints) {
						s[j].add(x[i]);
						flag = true;
						swallow[j][i] = true;
					}
				}
			}
			if (!flag) {
				notSwallowed[i] = true;
			}
		}


	
		plan = new int[n];         // fill the replacement plan in this array
		double[] benefit = new double[n];
		int[] soln = new int[n];
		int previousReplacer = -1;





		// filling according to benefit

		for(int i=0; i<n ; i++)			soln[i] = 0;    // it will be soln if it replaces more than 0


		for(int i=0; i<n; i++) {
			if(notSwallowed[i] == true) {
				plan[i] = i;
				benefit[i] = 1.0;
				soln[i] += 1;
				continue;
			}
			else {
				benefit[i] = Double.MIN_VALUE;	
			}


			previousReplacer = -1;

			for(int j=0; j<n; j++) {
				if(swallow[j][i] == true || j==i) {
					double num = 0.0, denom = 0.0, curBenefit = 0.0;

					for(int k=0; k<numPoints; k++) {
						num += Cost(i, PtIndex(selpt[k], r, d));
						//denom += Math.min( Cost(i, PtIndex(selpt[k], r, d)), Cost(j, PtIndex(selpt[k], r, d)) );
						denom += Cost(j, PtIndex(selpt[k], r, d)) ;
					} 
					curBenefit = num/denom;
			
					if(curBenefit > benefit[i]) {
						benefit[i] = curBenefit;
						plan[i] = j;
						
						if(previousReplacer == -1) {
							previousReplacer = j;
							soln[j] += 1;	
						}
						else {
							soln[previousReplacer] -= 1;
							previousReplacer = j;
							soln[j] += 1;

						}
					}
				}
			}
		}



		log.print("\n The Solution is - \n");
		int count = 0;	
		for(int i=0; i<n; i++){
			if(soln[i] >= 1) {
			 	//log.print("\t" + i + "\t" + plan[i] + "\t" + soln[i] + "\t" + stability[i] + "\t" + stability[plan[i]] + "\n");
				log.print("\t" + i + "\t" + soln[i] + "\n");
				count++;
			}	
		}
		log.print("\n Total plans - " + count + "\n");



		ngdp.setMaxPlanNumber(count);

		for (int i = 0; i < data.length; i++) {
			int pi = data[i].getPlanNumber();
			newData[i] = new DataValues();
			newData[i].setCard(data[i].getCard());
			newData[i].setPlanNumber(plan[pi]);
		}
		ngdp.setDataPoints(newData);
		
		int area_actual[] = new int[n];
		int area_reduced[] = new int[n];
		int tot_points = data.length;
		int rep_points = 0;
		for(int i=0;i<n;i++)
		{
			area_actual[i]=0;
			area_reduced[i]=0;
		}		
		for(int i=0;i<tot_points;i++)
		{
			area_actual[data[i].getPlanNumber()]++;
			area_reduced[newData[i].getPlanNumber()]++;
			if(data[i].getPlanNumber() != newData[i].getPlanNumber())
				rep_points++;
		}		
		
		/*for(int i=0;i<gdp.getMaxPlanNumber();i++)
		{
			log.println("Plan " + i +" : Actual Area=" + area_actual[i] + ",Reduced Area="+area_reduced[i]);
		}*/
	//	log.println("Replaced Points:"+rep_points);
		/*double maxBI = Double.MIN_VALUE;
		
		
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {

				
				if (i != j && plan[i] == j) {
					double BI = ComputeBenefit(j, i);
					maxBI = Math.max(BI, maxBI);
					log.println("Benefit Index : " + BI);
				}
			}
		}
		log.println("Max benefit index : " + maxBI);*/
//		ngdp.setMaxPlanNumber(soln.size());
		return ngdp;

	}

	
	
	public static DiagramPacket liteSeer3D(double threshold, DiagramPacket gdp) {

		long start = System.currentTimeMillis();
		DiagramPacket ngdp = new DiagramPacket(gdp);

		int n;

		n = gdp.getMaxPlanNumber();
		
//		log.println("Number of plans in original diagram : " + n);
		HashSet[] s = new HashSet[n];
		Integer[] x = new Integer[n];
		boolean[] notSwallowed = new boolean[n];
		DataValues[] data = gdp.getData();
		DataValues[] newData = new DataValues[data.length];
		boolean[][] swallow = new boolean[n][n];
		int res = gdp.getMaxResolution();

		sel = gdp.getPicassoSelectivity();
		int d = gdp.getDimension();

		int[] r	= new int[d];
		for (int i = 0; i < d; i++)
			r[i] = res;


		offset = new int[d];
		base = new int[d];

		// initialize offset
		offset[0] = 0;
		for (int i = 1; i < d; i++)
			offset[i] = offset[i - 1] + r[i - 1];

		// initialize base
		base[0] = 1;
		for (int i = 1; i < d; i++)
			base[i] = base[i - 1] * r[i - 1];

		ArrayList points = new ArrayList();
		ArrayList virtualPts = new ArrayList();
		int[] selPt = new int[d];

		for (int i = 0; i < n; i++) {
			s[i] = new HashSet();
			x[i] = new Integer(i);
			s[i].add(x[i]);
		}

		ArrayList slopeDim = new ArrayList();
		ArrayList slopeDimVal = new ArrayList();
		int[] dim = new int[d];
		for (int i = 0; i < d; i++)
			dim[i] = i;


		int[][] selpt = new int[8][3];

		selpt[0][0] = 0;
		selpt[0][1] = 0;

		selpt[0][2] = 0;
		
		selpt[1][0] = r[0] - 1;
		selpt[1][1] = 0;
		selpt[1][2] = 0;
		

		selpt[2][0] = r[0] - 1;
		selpt[2][1] = r[1] - 1;
		selpt[2][2] = 0;
		
		selpt[3][0] = 0;
		selpt[3][1] = r[1] - 1;
		selpt[3][2] = 0;
		
		selpt[4][0] = 0;
		selpt[4][1] = 0;
		selpt[4][2] = r[2] - 1;
		
		selpt[5][0] = r[0] - 1;
		selpt[5][1] = 0;
		selpt[5][2] = r[2] - 1;
		
		selpt[6][0] = r[0] - 1;
		selpt[6][1] = r[1] - 1;
		selpt[6][2] = r[2] - 1;
		
		selpt[7][0] = 0;
		selpt[7][1] = r[1] - 1;
		selpt[7][2] = r[2] - 1;
		
		int ctr = 0;
		
		int[] area = new int[n];
		for(int i = 0;i < data.length;i++)
			area[data[i].getPlanNumber()]++;
		
		for (int i = 0; i < n; i++) {
			boolean flag = false;
			for (int j = 0; j < n; j++) {
				// j -> swallower & i -> swallowee
				if (i != j) {
					ctr = 0;
					if(isDelightFul)
					{
						if (Cost(j, PtIndex(selpt[6], r, 3)) <= (1 + th / 100) * Cost(i, PtIndex(selpt[6], r, 3))){
							if (Cost(j, PtIndex(selpt[0], r, 3)) <= (1 + th / 100) * Cost(i, PtIndex(selpt[0], r, 3)))
								ctr = 8;
							
						}
					}
					else{
						for (int k = 0; k < 8; k++) {
							if (Cost(j, PtIndex(selpt[k], r, 3)) > (1 + th / 100) * Cost(i, PtIndex(selpt[k], r, 3))){
								break;
							}else{
								ctr++;
							}
						}
					}
					if (ctr == 8) {
						s[j].add(x[i]);
						flag = true;
						swallow[j][i] = true;
					}
				}
			}
			if (!flag) {
				notSwallowed[i] = true;
			}
		}
		
		
//		for(int i=0;i<n;i++){
//			
//			//log.println(notSwallowed[i]);
//			
//			log.println( " \n" + i + "  can be swallowed by :");
//			
//			for(int j=0;j<n;j++){
//				if(swallow[j][i] == true)
//					log.print(j + "   ");				
//			}
//		}
		
		
		
		log.print("\n\nIn liteseer\n\n");
		for(int i=0;i<n;i++){
			log.print("\n" + i + " (" + area[i] + " swallows = ");
			for(int j=0;j<n;j++){
				if(s[i].contains(x[j]))					log.print(" " + j + " ");
			}
		}

		
		
		
	//	System.exit(0);              // to just see possible swallower plans

		
		
		
		
		

		ArrayList soln = new ArrayList();
		HashSet temp = new HashSet();
		for (int i = 0; i < n; i++) {
			if (notSwallowed[i] && s[i] != null) {
				//i can't be swallowed but i can swallow someone
				temp.addAll(s[i]);
				s[i].clear();
				s[i] = null;
				soln.add(new Integer(i));
			}
		}
		//temp contains all plans which can be swallowed by plans which can't be swallowed
		for (int i = 0; i < n; i++) {
			//i can swallow someone
			if (s[i] != null) {
				s[i].removeAll(temp);//remove plans which are already swallowed
			}
		}
		//s[i] contain plans not swallowed till now
		while (true) {
			int max = 0;
			int p = -1;
			for (int i = 0; i < n; i++) {
				if (s[i] != null) {
					int size = s[i].size();
					if (size > max) {
						max = size;
						p = i;
					}/*else if(size == max){
						if(area[p] < area[i])
						{
							p = i;							
						}
					}*/
				}
			}
			//p is the plan which can swallow max number of plans
			if (p == -1) {
				break;
			}
			soln.add(new Integer(p));
			for (int i = 0; i < n; i++) {
				if (s[i] != null && i != p) {
					s[i].removeAll(s[p]);
					if (s[i].size() == 0) {
						s[i] = null;
					}
				}
			}
			s[p] = null;
		}

		// soln has the required soln
//		log.println("Number of plans in liteseer reduced diagram : "	+ soln.size());
		
		
//		for(int i=0;i<soln.size();i++)
//			log.println("Plan number : " + soln.get(i));
//		log.println("is the solution");
//		
//		System.exit(0);
		
	
		// soln has the required soln
		log.println("\n# plans in Reduced Diagram : " + soln.size());
		log.print("Plans : ");
		for(Iterator it = soln.iterator();it.hasNext();) {
			Integer pl = (Integer) it.next();
			log.print(pl + ",");
		}
		log.println();

		
		plan = new int[n];
		for (int i = 0; i < n; i++) {
			if (soln.contains(x[i])) {
				plan[i] = i;
			} else {
				for (Iterator it = soln.iterator(); it.hasNext();) {
					Integer xx = (Integer) it.next();
					if (swallow[xx.intValue()][i]) {
						plan[i] = xx.intValue();
						break;
					}
				}
			}
		}
		for (int i = 0; i < data.length; i++) {
			int pi = data[i].getPlanNumber();
			newData[i] = new DataValues();
			newData[i].setCard(data[i].getCard());
			newData[i].setPlanNumber(plan[pi]);
		}
		ngdp.setDataPoints(newData);
		
		int area_actual[] = new int[n];
		int area_reduced[] = new int[n];
		int tot_points = data.length;
		int rep_points = 0;
		for(int i=0;i<n;i++)
		{
			area_actual[i]=0;
			area_reduced[i]=0;
		}		
		for(int i=0;i<tot_points;i++)
		{
			area_actual[data[i].getPlanNumber()]++;
			area_reduced[newData[i].getPlanNumber()]++;
			if(data[i].getPlanNumber() != newData[i].getPlanNumber())
				rep_points++;
		}		
		
		/*for(int i=0;i<gdp.getMaxPlanNumber();i++)
		{
			log.println("Plan " + i +" : Actual Area=" + area_actual[i] + ",Reduced Area="+area_reduced[i]);
		}*/
		//log.println("Replaced Points:"+rep_points);
		/*double maxBI = Double.MIN_VALUE;
		
		
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				
				if (i != j && plan[i] == j) {
					double BI = ComputeBenefit(j, i);
					maxBI = Math.max(BI, maxBI);
					log.println("Benefit Index : " + BI);
				}
			}
		}
		log.println("Max benefit index : " + maxBI);*/
		ngdp.setMaxPlanNumber(soln.size());
		return ngdp;

	}


	
	
	static double efficiency, maxInc, minInc;

	static double resultantIncrease = 0.0;

	static double maxOldCost, minOldCost;

	static double maxNewCost, minNewCost;

	static public DiagramPacket reduce2D(double threshold, DiagramPacket gdp) {
		DiagramPacket ngdp = new DiagramPacket();

	/*	ngdp.setMaxCard(gdp.getMaxCard());
		ngdp.setResolution(gdp.getMaxResolution());
		ngdp.setMaxConditions(gdp.getDimension());
		ngdp.setMaxCost(gdp.getMaxCost());
		ngdp.setRelationNames(gdp.getRelationNames());
		ngdp.setMaxPlanNumber(gdp.getMaxPlanNumber());
		ngdp.setPicassoSelectivity(gdp.getPicassoSelectivity());
		ngdp.setPlanSelectivity(gdp.getPlanSelectivity());
		ngdp.setPredicateSelectivity(gdp.getPredicateSelectivity());
		ngdp.setRelationNames(gdp.getRelationNames());
		ngdp.setAttributeNames(gdp.getAttributeNames());
		ngdp.setConstants(gdp.getConstants());
		ngdp.setQueryPacket(gdp.getQueryPacket());*/

		int n; 

		n = gdp.getMaxPlanNumber();
		
//		log.println("# plans : " + n);
		int r = gdp.getMaxResolution();
		DataValues[] data = gdp.getData();
		DataValues[] newData = new DataValues[data.length];
		double maxx = 0;
		double minx = Double.MAX_VALUE;
		double[][] xcost = new double[r][n];
		boolean[] notSwallowed = new boolean[n];
		Set[] s = new Set[n];
		double maxc = 0;
		double minc = Double.MAX_VALUE;
		for(int i = 0;i < data.length;i ++) {
			maxc = Math.max(maxc, data[i].getCost());
			minc = Math.min(minc, data[i].getCost());
		}
//		log.println(maxc + " " + minc);
//		maxc *= 0.03;
		maxc = 0;
		for (int i = r - 1; i >= 0; i--) {
			for (int j = r - 1; j >= 0; j--) {
				int x = (i * r + j);
				Integer xI = new Integer(x);
				int p = data[x].getPlanNumber();

				if (s[p] == null) {
					s[p] = new Set();
				}
				s[p].elements.add(xI);
				
				double cost = data[x].getCost();
				maxx = Math.max(maxx, cost);
				minx = Math.min(minx, cost);
				double lt = cost * (1 + threshold / 100);
				if (xcost[j][p] != 0) {
					xcost[j][p] = Math.min(cost, xcost[j][p]);
				} else {
					xcost[j][p] = cost;
				}
				for (int k = 0; k < j; k++) {
					if (xcost[k][p] != 0) {
						xcost[k][p] = Math.min(cost, xcost[k][p]);
					} else {
						xcost[k][p] = cost;
					}
				}
				if (notSwallowed[p]) {
					continue;
				}
				//108.36555480957033
				boolean flag = false;
				for (int xx = 0; xx < n; xx++) {
					double abs = Math.abs(xcost[j][xx] - cost); 
					if (xx != p && xcost[j][xx] != 0
							/*&& xcost[j][xx] >= cost */&& (xcost[j][xx] <= lt || abs <= maxc)) {
						if(s[xx] == null) {
							s[xx] = new Set();
						}
						s[xx].elements.add(xI);
						flag = true;
					}
				}
				if (!flag) {
					notSwallowed[p] = true;
				}
			}
		}

		ArrayList soln = new ArrayList();
		Set temp = new Set();
		for (int i = 0; i < n; i++) {
			if (notSwallowed[i] && s[i] != null) {
				temp.elements.addAll(s[i].elements);
				s[i].elements.clear();
				s[i] = null;
				soln.add(new Integer(i));
			}
		}
		int cct = 0;
		for (int i = 0; i < n; i++) {
			if (s[i] != null) {
				s[i].elements.removeAll(temp.elements);
				if(s[i].elements.size() != 0) {
					cct ++;
				}
			}
		}

		while (true) {
			int max = 0;
			int p = -1;
			for (int i = 0; i < n; i++) {
				if (s[i] != null) {
					int size = s[i].elements.size();
					if (size > max) {
						max = size;
						p = i;
					}
				}
			}
			if (p == -1) {
				break;
			}
			soln.add(new Integer(p));
			for (int i = 0; i < n; i++) {
				if (s[i] != null && i != p) {
					s[i].elements.removeAll(s[p].elements);
					if (s[i].elements.size() == 0) {
						s[i] = null;
					}
				}
			}
			s[p] = null;
		}

//		log.println("CG Soln : " + soln.size());
		xcost = new double[r][n];
		// repeat the whole process to get the values for the new data
		for (int i = r - 1; i >= 0; i--) {
			for (int j = r - 1; j >= 0; j--) {
				int x = (i * r + j);
				
				int p = data[x].getPlanNumber();
				Integer pI = new Integer(p);
				newData[x] = new DataValues();
				newData[x].setCard(data[x].getCard());
				
				double cost = data[x].getCost();
				maxx = Math.max(maxx, cost);
				minx = Math.min(minx, cost);
				double lt = cost * (1 + threshold / 100);
				if (xcost[j][p] != 0) {
					xcost[j][p] = Math.min(cost, xcost[j][p]);
				} else {
					xcost[j][p] = cost;
				}
				for (int k = 0; k < j; k++) {
					if (xcost[k][p] != 0) {
						xcost[k][p] = Math.min(cost, xcost[k][p]);
					} else {
						xcost[k][p] = cost;
					}
				}
				
				if(soln.contains(pI)) {
					newData[x].setPlanNumber(p);
					newData[x].setCost(data[x].getCost());
				} else {
					int plan = -1;
					double newcost = Double.MAX_VALUE; 
					for (int xx = 0; xx < n; xx++) {
						double abs = Math.abs(xcost[j][xx] - cost);
						if (soln.contains(new Integer(xx)) && xx != p
								&& xcost[j][xx] != 0 && (xcost[j][xx] <= lt  || abs <= maxc)) {
								//&& xcost[j][xx] >= cost) {
							// another redundant check for xx != p
							if (xcost[j][xx] <= newcost) {
								// what r the chances of being equal ??
								newcost = xcost[j][xx];
								plan = xx;
							}
						}
					}
					newData[x].setPlanNumber(plan);
					newData[x].setCost(newcost);
				}
			}
		}
		
		ngdp.setDataPoints(newData);
		numPlansCG = soln.size();

		ngdp.setMaxPlanNumber(soln.size());
		return ngdp;
	}
	




		public static DiagramPacket reduce3D(double th, DiagramPacket gdp) {
			DiagramPacket ngdp = new DiagramPacket(gdp);

			/*ngdp.setMaxCard(gdp.getMaxCard());
			ngdp.setResolution(gdp.getMaxResolution());
			ngdp.setMaxConditions(gdp.getDimension());
			ngdp.setMaxCost(gdp.getMaxCost());
			ngdp.setRelationNames(gdp.getRelationNames());
			ngdp.setMaxPlanNumber(gdp.getMaxPlanNumberNumber());
			ngdp.setPicassoSelectivity(gdp.getPicassoSelectivity());
			ngdp.setPlanSelectivity(gdp.getPlanSelectivity());
			ngdp.setPredicateSelectivity(gdp.getPredicateSelectivity());
			ngdp.setRelationNames(gdp.getRelationNames());
			ngdp.setAttributeNames(gdp.getAttributeNames());
			ngdp.setConstants(gdp.getConstants());
			ngdp.setQueryPacket(gdp.getQueryPacket());*/

			int n;

			n = gdp.getMaxPlanNumber();
			
			int res = gdp.getMaxResolution();
			int r[] = new int[3];
			r[0] = res; r[1] = res; r[2] = res;
			DataValues[] data = gdp.getData();
			DataValues[] newData = new DataValues[data.length];
			Set[] s = new Set[n];

	
			
			//String[] forplan = new String[n];          //anshuman
			//for(int a=0;a<n;a++)
			//	forplan[a] = "";                          //anshuman
		
			
			
			boolean[] notSwallowed = new boolean[n];

			th /= 100;
			double[][][] xcost = new double[gdp.getMaxResolution()][gdp.getMaxResolution()][n];
			for (int i = r[0] - 1; i >= 0; i--) {
				for (int j = r[1] - 1; j >= 0; j--) {
					for (int k = r[2] - 1; k >= 0; k--) {
						int jj = j + 1;
						if (jj < r[1]) {
							for (int l = 0; l < n; l++) {
								if (xcost[j][k][l] != 0) {
									if (xcost[jj][k][l] != 0) {
										xcost[j][k][l] = Math.min(xcost[j][k][l], xcost[jj][k][l]);
									}
								} else {
									xcost[j][k][l] = xcost[jj][k][l];
								}
							}
						}
					}
					for (int k = r[2] - 1; k >= 0; k--) {
						int x = k*r[0]*r[1] + j*r[0] + i;

						Integer xI = new Integer(x);

						double c = data[x].getCost();
						int p = data[x].getPlanNumber();
						double t = c * (1 + th);

						if (s[p] == null) {
							s[p] = new Set();
						}
						s[p].elements.add(xI);

						if (xcost[j][k][p] != 0) {
							xcost[j][k][p] = Math.min(xcost[j][k][p], c);
						} else {
							xcost[j][k][p] = c;
						}
						for (int b = 0; b <= k; b++) {
							if (xcost[j][b][p] == 0) {
								xcost[j][b][p] = c;
							} else {
								xcost[j][b][p] = Math.min(xcost[j][b][p], c);
							}
						}
						if (notSwallowed[p]) {
							continue;
						}
						
						
			//			forplan[p] = forplan[p] + "(";                          //anshuman
						
						boolean flag = false;
						for (int xx = 0; xx < n; xx++) {
							if (xx != p && xcost[j][k][xx] != 0
									&& /* xcost[j][k][xx] >= c && */xcost[j][k][xx] <= t) {
								
		//						forplan[p] = forplan[p] + "" +   xx + ",";            //anshuman
								
								if (s[xx] == null) {
									s[xx] = new Set();
								}
								s[xx].elements.add(xI);
								flag = true;
							}
						}
						
					//	forplan[p] = forplan[p] + ") ";            //anshuman
						
						if (!flag) {
							notSwallowed[p] = true;
						}
					}
				}
			}

			
//			for(int ii=0;ii<1;ii++)                              //anshuman
	//			log.println(ii + " options  : "+ forplan[ii]);
			
			
			
			// Now we have a reduced Universal set. apply the log n
			// approximation
			// univ is the Universal set and s is the subsets
			ArrayList soln = new ArrayList();
			Set temp = new Set();
			for (int i = 0; i < n; i++) {
				if (notSwallowed[i] && s[i] != null) {
					temp.elements.addAll(s[i].elements);
					s[i] = null;
					soln.add(new Integer(i));
				}
			}
			for (int i = 0; i < n; i++) {
				if (s[i] != null) {
					s[i].elements.removeAll(temp.elements);
				}
			}

			while (true) {
				int max = 0;
				int p = -1;
				for (int i = 0; i < n; i++) {
					if (s[i] != null) {
						int size = s[i].elements.size();
						if (size > max) {
							max = size;
							p = i;
						}
					}
				}
				if (p == -1) {
					break;
				}
				soln.add(new Integer(p));
				for (int i = 0; i < n; i++) {
					if (s[i] != null && i != p) {
						s[i].elements.removeAll(s[p].elements);
						if (s[i].elements.size() == 0) {
							s[i] = null;
						}
					}
				}
				s[p] = null;
			}

			xcost = new double[gdp.getMaxResolution()][gdp.getMaxResolution()][n];
			for (int i = r[0] - 1; i >= 0; i--) {
				for (int j = r[1] - 1; j >= 0; j--) {
					for (int k = r[2] - 1; k >= 0; k--) {
						int jj = j + 1;
						if (jj < r[1]) {
							for (int l = 0; l < n; l++) {
								if (xcost[j][k][l] != 0) {
									if (xcost[jj][k][l] != 0) {
										xcost[j][k][l] = Math.min(xcost[j][k][l], xcost[jj][k][l]);
									}
								} else {
									xcost[j][k][l] = xcost[jj][k][l];
								}
							}
						}
					}
					for (int k = r[2] - 1; k >= 0; k--) {
						// int x = (j * r[0] + i) * r[1] + k;
						int x = k*r[0]*r[1] + j*r[0] + i;

						double c = data[x].getCost();
						int p = data[x].getPlanNumber();
						double t = c * (1 + th);

						if (xcost[j][k][p] != 0) {
							xcost[j][k][p] = Math.min(xcost[j][k][p], c);
						} else {
							xcost[j][k][p] = c;
						}
						for (int b = 0; b <= k; b++) {
							if (xcost[j][b][p] == 0) {
								xcost[j][b][p] = c;
							} else {
								xcost[j][b][p] = Math.min(xcost[j][b][p], c);
							}
						}
						double cheap = Double.MAX_VALUE;
						int plan = -1;
						for (int xx = 0; xx < n; xx++) {
							if (soln.contains(new Integer(xx))) {
								if (xcost[j][k][xx] != 0 && xcost[j][k][xx] <= t
										&& xcost[j][k][xx] <= cheap) {
									plan = xx;
									cheap = xcost[j][k][xx];
								}
							}
						}
						newData[x] = new DataValues();
						newData[x].setCard(data[x].getCard());
						newData[x].setPlanNumber(plan);
						newData[x].setCost(cheap);
					}
				}
			}
			for (Iterator it = soln.iterator(); it.hasNext();) {
				Integer ii = (Integer) it.next();
				// log.println(ii);
			}
			ngdp.setDataPoints(newData);
			numPlansCG = soln.size();
			ngdp.setMaxPlanNumber(soln.size());
//			setInfoValues(data, newData);
			return ngdp;
		}





	static public DiagramPacket reduce2D_abhirama(double threshold, DiagramPacket gdp) {

			DiagramPacket ngdp = new DiagramPacket(gdp);

		int n;

		n = gdp.getMaxPlanNumber();
		
		numPlans = n;


		int r = gdp.getMaxResolution();//rss
		int c = gdp.getMaxResolution();//rss
			DataValues[] data = gdp.getData();
			DataValues[] newData = new DataValues[data.length];
			// double maxx = 0;
			// double minx = Double.MAX_VALUE;
		double[][] xcost = new double[gdp.getMaxResolution()][n]; //-ma
			boolean[] notSwallowed = new boolean[n];
			HashSet[] s = new HashSet[n];
			
			
			String[] forplan = new String[n];          //anshuman
			for(int a=0;a<n;a++)
				forplan[a] = "";                          //anshuman
			
			
			
			int pb = 0;
			for (int i = r - 1; i >= 0; i--) // for each row
			{
			for (int j = c - 1; j >= 0; j--)	//for each column //rss
				{
				int x = (i * c + j);			//for each point, starting from topmost, rightmos
					Integer xI = new Integer(x);

					// belong[x] = new Belong(n);
					int p = data[x].getPlanNumber();

					if (s[p] == null) {
						s[p] = new HashSet(); // create a set for each plan
					}
					s[p].add(xI); // add the point to the set

					double cost = data[x].getCost(); // orig cost of x
					// maxx = Math.max(maxx, cost);
					// minx = Math.min(minx, cost);
					double lt = cost * (1 + threshold / 100);
					
					
					
					if (xcost[j][p] != 0) {
						xcost[j][p] = Math.min(cost, xcost[j][p]);
					} else {
						xcost[j][p] = cost;
					}
					
					
					
					
					for (int k = 0; k < j; k++) {
						if (xcost[k][p] != 0) {
							xcost[k][p] = Math.min(cost, xcost[k][p]);
						} else {
							xcost[k][p] = cost;
						}
					}
					
					
					
					
					//log.println("Plan: " + p + " replace options ");            //anshuman
					 
				 
					
					
					
					if (notSwallowed[p]) {
						continue;
					}
					// int jj = j + 1;
					boolean flag = false;
					
				//	forplan[p] = forplan[p] + "(";                          //anshuman
					
					for (int xx = 0; xx < n; xx++) {
						if (xx != p && xcost[j][xx] != 0
								&& /* xcost[j][k][xx] >= c && */xcost[j][xx] <= lt
								&& xcost[j][xx] >= cost) {
							// belong[x].a[xx] = xcost[j][xx];
					
					//		log.println(xx + "\t");            //anshuman
							
											
						//	forplan[p] = forplan[p] + "" +   xx + ",";            //anshuman
							
							
							if (s[xx] == null) {
								s[xx] = new HashSet();
							}
							s[xx].add(xI);
							flag = true;
						}
					}
					
					
					//forplan[p] = forplan[p] + ") ";            //anshuman
					
					
					// dont think this is required
					// if (jj < r) {
					// for (int xx = 0; xx < n; xx++) {
					// if (xx != p && xcost[jj][xx] != 0
					// && /* xcost[j][k][xx] >= c && */xcost[jj][xx] <= lt) {
					// // belong[x].a[xx] = xcost[jj][xx];
					// if (s[xx] == null) {
					// s[xx] = new Set();
					// }
					// s[xx].elements.add(xI);
					// flag = true;
					// }
					// }
					// }
					if (!flag) {
						notSwallowed[p] = true;
					}

					//parent.setProgressBar(pb);
					if (i % (0.2 * r) == 0 && pb <= 30) {
						pb++;
					}
				}
			}

				
		//	for(int ii=0;ii<n;ii++)                              //anshuman
		//		log.println(ii + " options  : "+ forplan[ii]);
			
			
		
			ArrayList soln = new ArrayList();
			HashSet temp = new HashSet();
			for (int i = 0; i < n; i++) {
				if (notSwallowed[i] && s[i] != null) {
					temp.addAll(s[i]);
					s[i].clear();
					s[i] = null;
					soln.add(new Integer(i));
				}
			}
			for (int i = 0; i < n; i++) {
				if (s[i] != null) {
					s[i].removeAll(temp);
				}
			}

			while (true) {
				int max = 0;
				int p = -1;
				for (int i = 0; i < n; i++) {
					if (s[i] != null) {
						int size = s[i].size();
						if (size > max) {
							max = size;
							p = i;
						}
					}
				}
				if (p == -1) {
					break;
				}
				soln.add(new Integer(p));
				for (int i = 0; i < n; i++) {
					if (s[i] != null && i != p) {
						s[i].removeAll(s[p]);
						if (s[i].size() == 0) {
							s[i] = null;
						}
					}
					//parent.setProgressBar(pb);
					if (i % (n / 3) == 0 && pb <= 70) {
						pb++;
					}
				}
				s[p] = null;
			}

			
			
	/*		
			Object[] arr; 
			
			for(int a=0;a<2;a++)                                  // anshuman : finding set of big sharks
			{
				arr = s[a].elements.toArray();
				int size = s[a].elements.size();
				for(int ii = 0; ii<size; ii++)
					log.println ( data[ Integer.parseInt(arr[ii].toString()) ].getPlanNumber());
			}
			log.println("Plan: " + pI + " replace options ");            //anshuman
			log.println(xx + "\t");            //anshuman 
						
		*/	
			
			
			double costinc = 0;
			minInc = Double.MAX_VALUE;
			maxInc = 0;
			resultantIncrease = 0;
			
						
			
			// repeat the whole process to get the values for the new data
		xcost = new double[gdp.getMaxResolution()][n];
		for (int i = r - 1; i >= 0; i--)
		{
			for (int j = c - 1; j >= 0; j--)
			{

				int x = (i * c + j);

					newData[x] = new DataValues();

					int p = data[x].getPlanNumber();
					Integer pI = new Integer(p);
					newData[x].setCard(data[x].getCard());

					double cost = data[x].getCost();
					// maxx = Math.max(maxx, cost);
					// minx = Math.min(minx, cost);
					double lt = cost * (1 + threshold / 100);
					if (xcost[j][p] != 0) {
						xcost[j][p] = Math.min(cost, xcost[j][p]);
					} else {
						xcost[j][p] = cost;
					}
					for (int k = 0; k < j; k++) {
						if (xcost[k][p] != 0) {
							xcost[k][p] = Math.min(cost, xcost[k][p]);
						} else {
							xcost[k][p] = cost;
						}
					}

					if (soln.contains(pI)) {
						newData[x].setPlanNumber(p);
						newData[x].setCost(data[x].getCost());
					} else {
						
						
						
						int plan = -1;
						double newcost = Double.MAX_VALUE;
						for (int xx = 0; xx < n; xx++) {
							if (soln.contains(new Integer(xx)) && xx != p && xcost[j][xx] != 0
									&& xcost[j][xx] <= lt && xcost[j][xx] >= cost) {
								// another redundant check for xx != p
								
						 
								
								if (xcost[j][xx] <= newcost) {
									costinc = ((xcost[j][xx] - cost) / cost);
									// what r the chances of being equal ??
									if (minInc > costinc && costinc > 0)
										minInc = costinc;
									if (maxInc < costinc && costinc > 0)
										maxInc = costinc;
									// resultantIncrease += costinc;
									newcost = xcost[j][xx];
									plan = xx;
								}
							}
						}
						costinc = ((xcost[j][plan] - cost) / cost);
						resultantIncrease += costinc;
						newData[x].setPlanNumber(plan);
						newData[x].setCost(newcost);
					}

					//parent.setProgressBar(pb);
					if (i % (0.2 * r) == 0 && pb <= 100) {
						pb++;
					}

				}
			}
			if (minInc == Double.MAX_VALUE)
				minInc = 0;
			resultantIncrease = (resultantIncrease / (r * r)) * 100;
			ngdp.setMaxPlanNumber(soln.size());

			numPlansCG = soln.size();

			ngdp.setDataPoints(newData);
			// log.println("Exiting CG method");
			return ngdp;
		}







	
	
	static double ComputeBenefit(int pEr, int pEe) {
		
		int[] selPt = {0, 9900, 9999, 99};
		
		double nr = 0, dr = 0;
		
		for (int i = 0; i < 4; i++) {
			nr += AllPlanCosts[pEe][selPt[i]];
		}
		
		for(int i = 0; i < 4; i++)
			dr += AllPlanCosts[pEr][selPt[i]];
		
		return (nr/ dr);
		
	}

	public static DiagramPacket hcSeer(double threshold, DiagramPacket gdp) {

		long start = System.currentTimeMillis();
		DiagramPacket ngdp = new DiagramPacket(gdp);

		
		
	
		
		int n; 

		
		n = gdp.getMaxPlanNumber();
		
//		log.println("Number of plans in original diagram : " + n);
		
		
		HashSet[] s = new HashSet[n];
		Integer[] x = new Integer[n];
		boolean[] notSwallowed = new boolean[n];
		DataValues[] data = gdp.getData();
		DataValues[] newData = new DataValues[data.length];
		boolean[][] swallow = new boolean[n][n];
		int res = gdp.getMaxResolution();

		sel = gdp.getPicassoSelectivity();
		int d = gdp.getDimension();

		int[] r	= new int[d];
		for (int i = 0; i < d; i++)
			r[i] = res;

		offset = new int[d];
		base = new int[d];

		// initialize offset
		offset[0] = 0;
		for (int i = 1; i < d; i++)
			offset[i] = offset[i - 1] + r[i - 1];

		// initialize base
		base[0] = 1;
		for (int i = 1; i < d; i++)
			base[i] = base[i - 1] * r[i - 1];

		ArrayList points = new ArrayList();
		ArrayList virtualPts = new ArrayList();
		int[] selPt = new int[d];

		for (int i = 0; i < n; i++) {
			s[i] = new HashSet();
			x[i] = new Integer(i);
			s[i].add(x[i]);
		}

		ArrayList slopeDim = new ArrayList();
		ArrayList slopeDimVal = new ArrayList();
		int[] dim = new int[d];
		for (int i = 0; i < d; i++)
			dim[i] = i;

		for (int i = 0; i < n; i++) {
			boolean flag = false;
			for (int j = 0; j < n; j++) {
				// j -> swallower & i -> swallowee
				if (i != j) {
					/*
					{
						log.print("j: "+j+" i: "+i+" d: "+d+" selPt: (");
						for(int td=0;td<d;td++)
							log.print(" "+selPt[td]);
						log.println(" )"+" d: "+d+" th: "+threshold);
					}
					*/
					if (hctest(j, i, d, selPt, d, dim, r, threshold, slopeDim,
							slopeDimVal)) {
						s[j].add(x[i]);
						flag = true;
						swallow[j][i] = true;
					}
				}
			}
			if (!flag) {
				notSwallowed[i] = true;
			}
		}

		
		log.print("\n\nIn HC_SEER\n\n");
		for(int i=0;i<n;i++){
			log.print("\n" + i + " swallows = ");
			for(int j=0;j<n;j++){
				if(s[i].contains(x[j]) && i!=j)					log.print(" " + j + " ");
			}
		}


		int[] plan = new int[n];
		
		if (choiceGreedy == ANOREXIC) {
		
			ArrayList soln = new ArrayList();
			HashSet temp = new HashSet();
			for (int i = 0; i < n; i++) {
				if (notSwallowed[i] && s[i] != null) {
					temp.addAll(s[i]);
					s[i].clear();
					s[i] = null;
					soln.add(new Integer(i));
				}
				
			}

			for (int i = 0; i < n; i++) {
				if (s[i] != null) {
					s[i].removeAll(temp);
				}
			}
			while (true) {
				int max = 0;
				int p = -1;
				for (int i = 0; i < n; i++) {
					if (s[i] != null) {
						int size = s[i].size();
						if (size > max) {
							max = size;
							p = i;
						}
					}
				}
				if (p == -1) {
					break;
				}
				soln.add(new Integer(p));
				for (int i = 0; i < n; i++) {
					if (s[i] != null && i != p) {
						s[i].removeAll(s[p]);
						if (s[i].size() == 0) {
							s[i] = null;
						}
					}
				}
				s[p] = null;
			}

		
			//		for(int i=0;i<soln.size();i++)
			//			log.println("Plan number : " + soln.get(i));
//			log.println("is the solution");
			
		//	System.exit(0);
		// 	soln has the required soln
			log.println("\n# plans in HC_SEER-Reduced Diagram : " + soln.size());
			log.print("Plans : ");
			for(Iterator it = soln.iterator();it.hasNext();) {
				Integer pl = (Integer) it.next();
				log.print(pl + ",");
			}
			log.println();
					
			
			

			for (int i = 0; i < n; i++) {
				if (soln.contains(x[i])) {
					plan[i] = i;
				} else {
					for (Iterator it = soln.iterator(); it.hasNext();) {
						Integer xx = (Integer) it.next();
						if (swallow[xx.intValue()][i]) {
							plan[i] = xx.intValue();
							break;
						}
					}
				}
			}
			
			ngdp.setMaxPlanNumber(soln.size());		
	} // end if (ANOREXIC)

	else if (choiceGreedy == NEW_BENEFIT) {

		///////////////////creating selpts - used to find corner costs - to calculate benefit estimate	

		int numPoints = (int) Math.pow(2,d);            // needed for NEW_BENEFIT


		int[][] selpt = new int[numPoints][d];

		if(d == 2) {		
			selpt[0][0] = 0;
			selpt[0][1] = 0;
			selpt[1][0] = 0;
			selpt[1][1] = r[1] - 1;
			selpt[2][0] = r[0] - 1;
			selpt[2][1] = r[1] - 1;
			selpt[3][0] = r[0] - 1;
			selpt[3][1] = 0;
		}
		else if ( d == 3) {

			selpt[0][0] = 0;
			selpt[0][1] = 0;
			selpt[0][2] = 0;
		
			selpt[1][0] = r[0] - 1;
			selpt[1][1] = 0;
			selpt[1][2] = 0;
		

			selpt[2][0] = r[0] - 1;
			selpt[2][1] = r[1] - 1;
			selpt[2][2] = 0;
		
			selpt[3][0] = 0;
			selpt[3][1] = r[1] - 1;
			selpt[3][2] = 0;
		
			selpt[4][0] = 0;
			selpt[4][1] = 0;
			selpt[4][2] = r[2] - 1;
		
			selpt[5][0] = r[0] - 1;
			selpt[5][1] = 0;
			selpt[5][2] = r[2] - 1;
		
			selpt[6][0] = r[0] - 1;
			selpt[6][1] = r[1] - 1;
			selpt[6][2] = r[2] - 1;
		
			selpt[7][0] = 0;
			selpt[7][1] = r[1] - 1;
			selpt[7][2] = r[2] - 1;
		
		}
		///////////////////creating selpts - used to find corner costs - to calculate benefit estimate


		      // fill the replacement plan in this array
		double[] benefit = new double[n];
		int[] soln = new int[n];
		int previousReplacer = -1;


		// filling according to benefit

		for(int i=0; i<n ; i++)			soln[i] = 0;    // it will be soln if it replaces more than 0

		for(int i=0; i<n; i++) {
			if(notSwallowed[i] == true) {
				plan[i] = i;
				benefit[i] = 1.0;
				soln[i] += 1;
				continue;
			}
			else {
				benefit[i] = Double.MIN_VALUE;	
			}


			previousReplacer = -1;

			for(int j=0; j<n; j++) {
				if(swallow[j][i] == true || j==i) {
					double num = 0.0, denom = 0.0, curBenefit = 0.0;

					for(int k=0; k<numPoints; k++) {
						num += Cost(i, PtIndex(selpt[k], r, d));
						denom += Cost(j, PtIndex(selpt[k], r, d)) ;
					} 
					curBenefit = num/denom;
			
					if(curBenefit > benefit[i]) {
						benefit[i] = curBenefit;
						plan[i] = j;
						
						if(previousReplacer == -1) {
							previousReplacer = j;
							soln[j] += 1;	
						}
						else {
							soln[previousReplacer] -= 1;
							previousReplacer = j;
							soln[j] += 1;

						}
					}
				}
			}
		}	




		log.print("\n The Solution is - \n");
		int count = 0;	
		for(int i=0; i<n; i++){
			if(soln[i] >= 1) {
			 	//log.print("\t" + i + "\t" + plan[i] + "\t" + soln[i] + "\t" + stability[i] + "\t" + stability[plan[i]] + "\n");
				log.print("\t" + i + "\t" + soln[i] + "\n");
				count++;
			}	
		}
		log.print("\n Total plans - " + count + "\n");

		ngdp.setMaxPlanNumber(count);

	}  // end NEW - BENEFIT


		
		
		
		
		for (int i = 0; i < data.length; i++) {
			int pi = data[i].getPlanNumber();
			newData[i] = new DataValues();
			newData[i].setCard(data[i].getCard());
			newData[i].setPlanNumber(plan[pi]);
		}
		ngdp.setDataPoints(newData);


		double ctr = 0;
		for (int i = 0; i < data.length; i++) {
			if (data[i].getPlanNumber() != newData[i].getPlanNumber())
				ctr++;
		}
//		log.println("% of pts replaced : "+ (ctr * 100 / ((double) data.length)));

		return ngdp;

	}

	/*
	 * diagDim : dimensionality of the plan diagram
	 * 
	 * nDim : describes the dimensionality of the hyperplane (which is a slice
	 * of the original plan diagram). For eg.: its value would be 2 if the
	 * concerned hyperplane is a 2 dimensonal slice of the original plan
	 * diagram.
	 * 
	 * dim : it is an array of size $numOfDim$ decribing the orientation of the
	 * hyperplane of the original diagram. For eg.: say the original diagram is
	 * 3 dimensional with X, Y, Z being the dimensions 1, 2 and 3 respectively.
	 * Then a XZ hyperplane of the original diagram would be described by
	 * sending {1, 3} as the value of the array $dim$.
	 * 
	 * selPt : it is an array containing the $d$ coordinates of a selectivity
	 * point in the original diagram space.
	 */

	// Determines the index of a selectivity point
	public static int PtIndex(int[] selPt, int[] r, int diagDim) {
		int ptIndex = 0;
		for (int i = 0; i < diagDim; i++)
			ptIndex += selPt[i] * base[i];
		return ptIndex;
	}

	final static int SEC_DER_POSITIVE = 0;
	final static int SEC_DER_NEG = 1;
	final static int SEC_DER_INCONCLUSIVE = 2;
	final static int SANITY_CONST = HardCoded.SanityConstant;

	public static boolean hctest(int pEr, int pEe, int diagDim, int[] selPt,
			int nDim, int[] dim, int[] r, double th, ArrayList slopeDim,
			ArrayList slopeDimVal) {

		
		//log.println(" threshold is = " + th);
		
		if (nDim == 0) {
			ArrayList slopeDimClone = new ArrayList();
			ArrayList slopeDimValClone = new ArrayList();

			slopeDimClone = (ArrayList) slopeDim.clone();
			slopeDimValClone = (ArrayList) slopeDimVal.clone();
			if (ComputeVal(pEr, pEe, selPt, slopeDimClone, slopeDimValClone, r,
					diagDim, th) <= 0)
				return true;
			else
				return false;
		}

		int[] newDim = new int[nDim - 1];

		for (int i = 0; i < nDim; i++) {
			ConstructArrayDim(nDim, dim, newDim, dim[i]);

			ArrayList slopeDimClone = new ArrayList();
			ArrayList slopeDimValClone = new ArrayList();

			slopeDimClone = (ArrayList) slopeDim.clone();
			slopeDimValClone = (ArrayList) slopeDimVal.clone();
			selPt[dim[i]] = 0;
			boolean frontSafety = hctest(pEr, pEe, diagDim, selPt, nDim - 1,
					newDim, r, th, slopeDimClone, slopeDimValClone);

			slopeDimClone = (ArrayList) slopeDim.clone();
			slopeDimValClone = (ArrayList) slopeDimVal.clone();
			selPt[dim[i]] = r[dim[i]] - 1;
			boolean backSafety = hctest(pEr, pEe, diagDim, selPt, nDim - 1,
					newDim, r, th, slopeDimClone, slopeDimValClone);

			if (!(frontSafety && backSafety))
				continue;

			// compute second derivative
			ArrayList hPlaneDim = new ArrayList();
			for (int j = 0; j < nDim - 1; j++)
				hPlaneDim.add(new Integer(newDim[j]));
			slopeDimClone = (ArrayList) slopeDim.clone();
			slopeDimValClone = (ArrayList) slopeDimVal.clone();
			int secDerSign = ComputeSecDer(pEr, pEe, hPlaneDim, selPt, dim[i],
					slopeDimClone, slopeDimValClone, r, diagDim, th);

			if (secDerSign == SEC_DER_POSITIVE)
				return true;
			else if (secDerSign == SEC_DER_INCONCLUSIVE)
				continue;

			ArrayList newSlopeDim = new ArrayList();
			ArrayList newSlopeDimVal = new ArrayList();

			newSlopeDim = (ArrayList) slopeDim.clone();
			newSlopeDimVal = (ArrayList) slopeDimVal.clone();
			newSlopeDim.add(new Integer(dim[i]));
			newSlopeDimVal.add(new Integer(1));

			if (hctest(pEr, pEe, diagDim, selPt, nDim - 1, newDim, r, th,
					newSlopeDim, newSlopeDimVal))
				return true;

			newSlopeDim = (ArrayList) slopeDim.clone();
			newSlopeDimVal = (ArrayList) slopeDimVal.clone();
			newSlopeDim.add(new Integer(dim[i]));
			newSlopeDimVal.add(new Integer(r[dim[i]] - 1));

			if (hctest(pEr, pEe, diagDim, selPt, nDim - 1, newDim, r, th,
					newSlopeDim, newSlopeDimVal))
				return true;

		}

		return false;
	}

	public static double ComputeVal(int pEr, int pEe, int[] selPt,
			ArrayList slopeDim, ArrayList slopeDimVal, int[] r, int diagDim,
			double th) {

		if (slopeDim.isEmpty()) {
			return SFVal(pEr, pEe, selPt, r, diagDim, th);
		}
		Integer curDim = (Integer) slopeDim.get(0);
		Integer curDimVal = (Integer) slopeDimVal.get(0);

		ArrayList slopeDimClone = new ArrayList();
		ArrayList slopeDimValClone = new ArrayList();


		slopeDimClone = (ArrayList) slopeDim.clone();
		slopeDimValClone = (ArrayList) slopeDimVal.clone();
		slopeDimClone.remove(0);
		slopeDimValClone.remove(0);

		selPt[curDim.intValue()] = curDimVal.intValue();
		double val1 = ComputeVal(pEr, pEe, selPt, slopeDimClone,
				slopeDimValClone, r, diagDim, th);

		slopeDimClone = (ArrayList) slopeDim.clone();
		slopeDimValClone = (ArrayList) slopeDimVal.clone();
		slopeDimClone.remove(0);
		slopeDimValClone.remove(0);
		selPt[curDim.intValue()]--;
		double val2 = ComputeVal(pEr, pEe, selPt, slopeDimClone,
				slopeDimValClone, r, diagDim, th);

		double finalVal = val2 - val1;

		if (curDimVal.intValue() == r[curDim.intValue()] - 1)
			finalVal /= (sel[offset[curDim.intValue()] + r[curDim.intValue()]
					- 1] - sel[offset[curDim.intValue()] + r[curDim.intValue()]
					- 2]);
		else
			finalVal /= (sel[offset[curDim.intValue()] + 0] - sel[offset[curDim
					.intValue()] + 1]);

		return finalVal;
	}

	// compute the value of the safety function at the given selectivity point.
	static double SFVal(int pEr, int pEe, int[] selPt, int[] r, int diagDim,
			double th) {
		th = 1 + th / 100;
		double sfVal = Cost(pEr, PtIndex(selPt, r, diagDim))
				- (th * Cost(pEe, PtIndex(selPt, r, diagDim))) - SANITY_CONST;
		return sfVal;
	}

	public static double Cost(int planNum, int ptIndex) {
		/**
		 * Ordering the costs.
		 */
		int pOptimal = data[ptIndex].getPlanNumber();
		double cOptimal;
		if(pOptimal >= gdpPlans)
		{
			double cost[] = AdditionalPlansCost.get(new Integer(pOptimal));
			cOptimal = cost[ptIndex];
		}
		else
			cOptimal= AllPlanCosts[pOptimal][ptIndex];
		double cPlan;
		if(planNum >= gdpPlans)
		{
			double cost[] = AdditionalPlansCost.get(new Integer(planNum));
			cPlan = cost[ptIndex];
		}
		else
			cPlan= AllPlanCosts[planNum][ptIndex];
		
		double underEstimate = cOptimal - cPlan;  //Should be Negative. cPlan > cOptimal
		HardCoded.logEstimation.println("Cost : PtIndex: "+ptIndex+" : "+" (pOpt: "+pOptimal+" ["+cOptimal+"]  pre: "+planNum+" ["+cPlan+"] )");
		if(Double.compare(underEstimate, 0.0) > 0)
		{
			HardCoded.logEstimation.println(" UnderEstimate : PtIndex: "+ptIndex+" : underEstimate"+" (pOpt: "+pOptimal+" ["+cOptimal+"]  pre: "+planNum+" ["+cPlan+"] )");
			if(Double.compare(underEstimate, HardCoded.maxUnderEstimate) > 0)
			{
				HardCoded.maxUnderEstimate = underEstimate;
				HardCoded.logEstimation.println("[MAX] UnderEstimate : PtIndex: "+ptIndex+" : underEstimate"+" (pOpt: "+pOptimal+" ["+cOptimal+"]  pre: "+planNum+" ["+cPlan+"] )");
			}
			if(Double.compare(underEstimate, HardCoded.minUnderEstimate) < 0)
			{
				HardCoded.minUnderEstimate = underEstimate;
				HardCoded.logEstimation.println("[MIN] UnderEstimate : PtIndex: "+ptIndex+" : underEstimate"+" (pOpt: "+pOptimal+" ["+cOptimal+"]  pre: "+planNum+" ["+cPlan+"] )");
			}
			HardCoded.sumUnderEstimate = HardCoded.sumUnderEstimate + underEstimate;
		}
		
		if(cPlan < cOptimal - 0.1 )
		{
			//System.out.println(" ## - planNum "+planNum+" cPlan: "+cPlan+" ptIndex: "+ptIndex+" PlanOpt: "+pOptimal+"  cOpt: "+cOptimal);
			cPlan = cOptimal + 0.01;
		}
		return cPlan;
		
		//return AllPlanCosts[planNum][ptIndex];
	}

	public static void ConstructArrayDim(int nDim, int[] dim, int[] newDim,
			int currDim) {
		int k = 0;
		for (int i = 0; i < nDim; i++) {
			if (dim[i] != currDim)
				newDim[k++] = dim[i];

		}
	}

	public static int ComputeSecDer(int pEr, int pEe, ArrayList hPlaneDim,
			int[] selPt, int secDerDim, ArrayList slopeDim,
			ArrayList slopeDimVal, int[] r, int diagDim, double th) {
		if (hPlaneDim.isEmpty()) {
			ArrayList slopeDimClone = new ArrayList();
			ArrayList slopeDimValClone = new ArrayList();

			slopeDimValClone = (ArrayList) slopeDimVal.clone();
			slopeDimClone = (ArrayList) slopeDim.clone();

			selPt[secDerDim] = 0;
			double val1 = ComputeVal(pEr, pEe, selPt, slopeDimClone,
					slopeDimValClone, r, diagDim, th);

			slopeDimValClone = (ArrayList) slopeDimVal.clone();
			slopeDimClone = (ArrayList) slopeDim.clone();

			selPt[secDerDim] = 1;
			double val2 = ComputeVal(pEr, pEe, selPt, slopeDimClone,
					slopeDimValClone, r, diagDim, th);

			slopeDimValClone = (ArrayList) slopeDimVal.clone();
			slopeDimClone = (ArrayList) slopeDim.clone();

			selPt[secDerDim] = r[secDerDim] - 2;
			double val3 = ComputeVal(pEr, pEe, selPt, slopeDimClone,
					slopeDimValClone, r, diagDim, th);

			slopeDimValClone = (ArrayList) slopeDimVal.clone();
			slopeDimClone = (ArrayList) slopeDim.clone();

			selPt[secDerDim] = r[secDerDim] - 1;
			double val4 = ComputeVal(pEr, pEe, selPt, slopeDimClone,
					slopeDimValClone, r, diagDim, th);

			double firstDer1 = (val1 - val2)
					/ (sel[offset[secDerDim] + 0] - sel[offset[secDerDim] + 1]);
			double firstDer2 = (val3 - val4)
					/ (sel[offset[secDerDim] + r[secDerDim] - 2] - sel[offset[secDerDim]
							+ r[secDerDim] - 1]);

			if (firstDer1 <= firstDer2)
				return SEC_DER_POSITIVE;
			else
				return SEC_DER_NEG;
		} else {
			ArrayList hPlaneDimClone = new ArrayList();
			ArrayList slopeDimClone = new ArrayList();
			ArrayList slopeDimValClone = new ArrayList();

			hPlaneDimClone = (ArrayList) hPlaneDim.clone();
			slopeDimValClone = (ArrayList) slopeDimVal.clone();
			slopeDimClone = (ArrayList) slopeDim.clone();

			Integer curDim = (Integer) hPlaneDimClone
					.get(hPlaneDimClone.size() - 1);
			hPlaneDimClone.remove(hPlaneDimClone.size() - 1);

			selPt[curDim.intValue()] = 0;
			int secDerSign1 = ComputeSecDer(pEr, pEe, hPlaneDimClone, selPt,
					secDerDim, slopeDimClone, slopeDimValClone, r, diagDim, th);

			hPlaneDimClone = (ArrayList) hPlaneDim.clone();
			hPlaneDimClone.remove(hPlaneDimClone.size() - 1);
			slopeDimValClone = (ArrayList) slopeDimVal.clone();
			slopeDimClone = (ArrayList) slopeDim.clone();

			selPt[curDim.intValue()] = r[curDim.intValue()] - 1;
			int secDerSign2 = ComputeSecDer(pEr, pEe, hPlaneDimClone, selPt,
					secDerDim, slopeDimClone, slopeDimValClone, r, diagDim, th);

			if (secDerSign1 == SEC_DER_POSITIVE
					&& secDerSign2 == SEC_DER_POSITIVE)
				return SEC_DER_POSITIVE;
			else if (secDerSign1 == SEC_DER_NEG && secDerSign2 == SEC_DER_NEG)
				return SEC_DER_NEG;
			else

				return SEC_DER_INCONCLUSIVE;
		}

	}

	/************************** HC-SEER ********************************/

	public static DiagramPacket seer(double threshold, DiagramPacket gdp) {
		DiagramPacket ngdp = new DiagramPacket();

		int n; 


		n = gdp.getMaxPlanNumber();
		
//		log.println(n);
		HashSet[] s = new HashSet[n];
		Integer[] x = new Integer[n];
		boolean[] notSwallowed = new boolean[n];
		DataValues[] data = gdp.getData();
		DataValues[] newData = new DataValues[data.length];
		boolean[][] swallow = new boolean[n][n];
		int res = gdp.getMaxResolution();
		sel = gdp.getPicassoSelectivity();
		int d = gdp.getDimension();

		int[] r	= new int[d];
		for (int i = 0; i < d; i++)
			r[i] = res;

		for (int i = 0; i < n; i++) {
			s[i] = new HashSet();
			x[i] = new Integer(i);
			s[i].add(x[i]);
		}

		for (int i = 0; i < n; i++) {
			boolean flag = false;
			for (int j = 0; j < n; j++) {
				// j -> swallower & i -> swallowee
				if (i != j) {
					if (ndCheck(j, i, th, gdp.getDimension(), r)) {
						s[j].add(x[i]);
						flag = true;
						swallow[j][i] = true;
					}
				}
			}
			if (!flag) {
				notSwallowed[i] = true;
			}
		}

		
		
		log.print("\n\nIn SEER\n\n");
		for(int i=0;i<n;i++){
			log.print("\n" + i + " swallows = ");
			for(int j=0;j<n;j++){
				if(s[i].contains(x[j]))					log.print(" " + j + " ");
			}
		}

		
		ArrayList soln = new ArrayList();
		HashSet temp = new HashSet();
		for (int i = 0; i < n; i++) {
			if (notSwallowed[i] && s[i] != null) {
				temp.addAll(s[i]);
				s[i].clear();
				s[i] = null;
				soln.add(new Integer(i));
			}
		}

		for (int i = 0; i < n; i++) {
			if (s[i] != null) {
				s[i].removeAll(temp);
			}
		}
		while (true) {
			int max = 0;
			int p = -1;
			for (int i = 0; i < n; i++) {
				if (s[i] != null) {
					int size = s[i].size();
					if (size > max) {
						max = size;
						p = i;
					}
				}
			}
			if (p == -1) {
				break;
			}
			soln.add(new Integer(p));
			for (int i = 0; i < n; i++) {
				if (s[i] != null && i != p) {
					s[i].removeAll(s[p]);
					if (s[i].size() == 0) {
						s[i] = null;
					}
				}
			}
			s[p] = null;
		}

		// soln has the required soln
//		log.println("Number of plans in seer reduced plan diagram is: "	+ (soln.size()));
		// soln has the required soln
		log.println("\n# plans in SEER-Reduced Diagram : " + soln.size());
		log.print("Plans : ");
		for(Iterator it = soln.iterator();it.hasNext();) {
			Integer pl = (Integer) it.next();
			log.print(pl + ",");
		}
		log.println();
		
		
		
		numPlansSeer = soln.size();

		int[] plan = new int[n];
		for (int i = 0; i < n; i++) {
			if (soln.contains(x[i])) {
				plan[i] = i;
			} else {
				for (Iterator it = soln.iterator(); it.hasNext();) {
					Integer xx = (Integer) it.next();
					if (swallow[xx.intValue()][i]) {
						plan[i] = xx.intValue();
						break;
					}
				}
			}
		}
		for (int i = 0; i < data.length; i++) {
			int pi = data[i].getPlanNumber();
			newData[i] = new DataValues();
			newData[i].setCard(data[i].getCard());
			newData[i].setPlanNumber(plan[pi]);
		}
		ngdp.setDataPoints(newData);
		ngdp.setMaxPlanNumber(soln.size());

		double ctr = 0;
		for (int i = 0; i < data.length; i++) {
			if (data[i].getPlanNumber() != newData[i].getPlanNumber())
				ctr++;
		}
//		log.println("No of plans in reduced diagram : " + soln.size());
//		log.println("No of pts replaced : " + ctr);
//		log.println("% of points replaced : " + (ctr / ((double) data.length) * 100) + " %");

		return ngdp;

	}

		
	private static boolean ndCheck(int pEr, int pEe, double th, int d, int[] r) {
		return check(pEr, pEe, th, d, r);
	}

	private static boolean check(int er, int ee, double th, int d, int[] r) {

		if(d == 2)
			return wptest(er, ee, th, r);
		else
		{	
		boolean ret = true;
		for (int i = 0; i < r[d-1]; i++) {
			ret = ret & check(er, ee, th, d - 1, r);
		}
		return ret;
		}					
	}


	// Wedge followed by perimeter test

	public static boolean wptest(int pEr, int pEe, double th, int[] r) {

		th = 1 + th / 100;

		// wedge test
		double lb = SFVal(getPlanCost(pEr, 0, 0, r), getPlanCost(pEe, 0, 0, r),	th);
		double lt = SFVal(getPlanCost(pEr, 0, r[1] - 1, r), getPlanCost(pEe, 0,	r[1] - 1, r), th);
		double rb = SFVal(getPlanCost(pEr, r[0] - 1, 0, r), getPlanCost(pEe,r[0] - 1, 0, r), th);
		double rt = SFVal(getPlanCost(pEr, r[0] - 1, r[1] - 1, r), getPlanCost(	pEe, r[0] - 1, r[1] - 1, r), th);

		short ct = 0;
		if (lb <= 0) {
			ct++;
		}
		if (lt <= 0) {
			ct++;
		}
		if (rb <= 0) {
			ct++;
		}
		if (rt <= 0) {
			ct++;
		}
		if (ct != 4) {
			return false;
		}

		double ml1 = SFVal(getPlanCost(pEr, 0, 1, r),
				getPlanCost(pEe, 0, 1, r), th);
		double mr1 = SFVal(getPlanCost(pEr, r[0] - 1, 1, r), getPlanCost(pEe,
				r[0] - 1, 1, r), th);
		double mb1 = SFVal(getPlanCost(pEr, 1, 0, r),
				getPlanCost(pEe, 1, 0, r), th);
		double mt1 = SFVal(getPlanCost(pEr, 1, r[1] - 1, r), getPlanCost(pEe,
				1, r[1] - 1, r), th);

		double ml2 = SFVal(getPlanCost(pEr, 0, r[1] - 2, r), getPlanCost(pEe,
				0, r[1] - 2, r), th);
		double mr2 = SFVal(getPlanCost(pEr, r[0] - 1, r[1] - 2, r),
				getPlanCost(pEe, r[0] - 1, r[1] - 2, r), th);
		double mb2 = SFVal(getPlanCost(pEr, r[0] - 2, 0, r), getPlanCost(pEe,
				r[0] - 2, 0, r), th);
		double mt2 = SFVal(getPlanCost(pEr, r[0] - 2, r[1] - 1, r),
				getPlanCost(pEe, r[0] - 2, r[1] - 1, r), th);

		double sl1 = (ml1 - lb) / (sel[r[0] + 1] - sel[r[0] + 0]);
		double sl2 = (lt - ml2) / (sel[r[0] + r[1] - 1] - sel[r[0] + r[1] - 2]);

		double sr1 = (mr1 - rb) / (sel[r[0] + 1] - sel[r[0] + 0]);
		double sr2 = (rt - mr2) / (sel[r[0] + r[1] - 1] - sel[r[0] + r[1] - 2]);

		double st1 = (mt1 - lt) / (sel[1] - sel[0]);
		double st2 = (rt - mt2) / (sel[r[0] - 1] - sel[r[0] - 2]);

		double sb1 = (mb1 - lb) / (sel[1] - sel[0]);
		double sb2 = (rb - mb2) / (sel[r[0] - 1] - sel[r[0] - 2]);

		boolean incL = false;
		boolean incR = false;
		boolean incT = false;
		boolean incB = false;

		if (sl2 >= sl1) {
			incL = true;
		}
		if (sr2 >= sr1) {
			incR = true;
		}
		if (st2 >= st1) {
			incT = true;
		}
		if (sb2 >= sb1) {
			incB = true;
		}

		boolean lr = false;
		if (incL && incR) {
			lr = true;
		}

		boolean tb = false;
		if (incT && incB) {
			tb = true;
		}

		boolean top = false;
		boolean bottom = false;
		boolean left = false;
		boolean right = false;

		if (lb <= 0 && lt <= 0) {
			if (incL) {
				left = true;
			} else {
				if (sl1 <= 0 || sl2 >= 0) {
					left = true;
				}
			}
		}

		if (rb <= 0 && rt <= 0) {
			if (incR) {
				right = true;
			} else {
				if (sr1 <= 0 || sr2 >= 0) {
					right = true;
				}
			}
		}

		if (lb <= 0 && rt <= 0) {
			if (incB) {
				bottom = true;
			} else {
				if (sb1 <= 0 || sb2 >= 0) {
					bottom = true;
				}
			}
		}

		if (lt <= 0 && rt <= 0) {
			if (incT) {
				top = true;
			} else {
				if (st1 <= 0 || st2 >= 0) {
					top = true;
				}
			}
		}

		if (lr && top && bottom) {
			return true;
		}

		if (tb && left && right) {
			return true;
		}

		// Perimeter test
		if (!left) {
			for (int i = 0; i < r[1]; i++) {
				double c = SFVal(getPlanCost(pEr, 0, i, r), getPlanCost(pEe, 0,i, r), th);
				if (c > 0) {
					return false;
				}
			}
		}
		if (!right) {
			for (int i = 0; i < r[1]; i++) {
				double c = SFVal(getPlanCost(pEr, r[0] - 1, i, r), getPlanCost(pEe, r[0] - 1, i, r), th);
				if (c > 0) {
					return false;
				}
			}
		}
		if (!bottom) {
			for (int i = 0; i < r[0]; i++) {
				double c = SFVal(getPlanCost(pEr, i, 0, r), getPlanCost(pEe, i,	0, r), th);
				if (c > 0) {
					return false;
				}
			}
		}
		if (!top) {
			for (int i = 0; i < r[0]; i++) {
				double c = SFVal(getPlanCost(pEr, i, r[1] - 1, r), getPlanCost(	pEe, i, r[1] - 1, r), th);
				if (c > 0) {
					return false;
				}
			}
		}
		if (lr || tb) {
			return true;
		}
		// lr
		boolean flag = true;
		if (!incL && !incR) {
			for (int i = 0; i < r[0]; i++) {
				double c1 = getPlanCost(pEr, i, 0, r) - th * getPlanCost(pEe, i, 0, r);
				double c2 = getPlanCost(pEr, i, 1, r) - th * getPlanCost(pEe, i, 1, r);
				double c3 = getPlanCost(pEr, i, r[1] - 2, r) - th * getPlanCost(pEe, i, r[1] - 2, r);
				double c4 = getPlanCost(pEr, i, r[1] - 1, r) - th * getPlanCost(pEe, i, r[1] - 1, r);

				double s1 = (c2 - c1) / (sel[r[0] + 1] - sel[r[0] + 0]);
				double s2 = (c4 - c3) / (sel[r[0] + r[1] - 1] - sel[r[0] + r[1] - 2]);
				if (!(s1 <= 0 || s2 >= 0)) {
					flag = false;
					break;
				}
			}
		}
		if (flag) {
			return true;
		}

		if (!incT && !incB) {
			for (int i = 0; i < r[1]; i++) {
				double c1 = getPlanCost(pEr, 0, i, r) - th * getPlanCost(pEe, 0, i, r);
				double c2 = getPlanCost(pEr, 1, i, r) - th * getPlanCost(pEe, 1, i, r);
				double c3 = getPlanCost(pEr, r[0] - 2, i, r) - th * getPlanCost(pEe, r[0] - 2, i, r);
				double c4 = getPlanCost(pEr, r[0] - 1, i, r) - th * getPlanCost(pEe, r[0] - 1, i, r);

				double s1 = (c2 - c1) / (sel[1] - sel[0]);
				double s2 = (c4 - c3) / (sel[r[0] - 1] - sel[r[0] - 2]);
				if (!(s1 <= 0 || s2 >= 0)) {
					return false;
				}
			}
		}
		return true;
	}

	public static double SFVal(double pErCost, double pEeCost, double th) {
		return (pErCost - th * pEeCost  );
	}

	public static double getPlanCost(int plan, int i, int j, int[] r) {
		double Cost;
		if(plan >= gdpPlans)
		{
			double cost[] = AdditionalPlansCost.get(new Integer(plan));
			Cost = cost[PtIndex(i, j, r)];
		}
		else
			Cost= AllPlanCosts[plan][PtIndex(i, j, r)];
		return Cost;
	}

	public static int PtIndex(int i, int j, int[] r) {
		int ptIndex = 0;
		int[] selPt = new int[r.length];

		selPt[0] = i;
		selPt[1] = j;
		ptIndex = selPt[0] + (selPt[1] * r[0]);
		return ptIndex;
	}

	private static DiagramPacket getGDP(File file) {
		DiagramPacket gdp = null;
		FileInputStream fis;
		try {
			fis = new FileInputStream(file);
			ObjectInputStream ois = new ObjectInputStream(fis);
			Object obj = ois.readObject();
			gdp = (DiagramPacket) obj;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return gdp;
	}

























































	static class Data {
		double min = Double.MAX_VALUE;
		double max = 0;
		double tot = 0;
		int ct = 0;
	}

	static File pkd;
	static double maxcst = 0;
	static double[] planCostEr;
	static double[] planCostEe;
	static int per, pee;


	// computes the euclidean distance between two selectivity point $i$ and $j$
	public static double getDist(int i, int j, int r, float[] sel, int dim) {
		int[] p1 = new int[dim];
		int[] p2 = new int[dim];
		for (int x = 0; x < dim; x++) {
			p1[x] = i % r;
			i /= r;
			p2[x] = j % r;
			j /= r;
		}

		double d = 0;
		for (int x = 0; x < dim; x++) {
			d += (sel[p2[x]] - sel[p1[x]]) * (sel[p2[x]] - sel[p1[x]]);
		}
		d = Math.sqrt(d);
		return d;
	}

	public static double getMetric(double coe, double cre, double cao,
			double i, double lam) {
		if (cre < cao) {
			cao = cre;
		}
		double sanity_constant = 0;
		double l = cao * (1 + i) + sanity_constant;
		maxcst = 1;
		if (cre > l || coe > l) {
			double num = Math.max(cre - cao, maxcst);
			double den = Math.max(lam * coe  - cao,	maxcst);
			double metric = 1.0 - num / den;
			return metric;
		}
		return 2;
	}

	public static void init(int pEr, int pEe) {
		per = pEr;
		pee = pEe;
		try {
			planCostEr = AllPlanCosts[pEr];
			planCostEe = AllPlanCosts[pEe];
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	


	static void getReplacementMapping(DiagramPacket gdp, DiagramPacket ngdp)
	{
		DataValues[] data = gdp.getData();
		DataValues[] newData = ngdp.getData();
		try{
			PrintStream ps = new PrintStream(new File("/home/abhirama/Desktop/blah/liteseermapping.txt"));
			for(int i = 0;i < data.length;i++)
			{
				ps.println(data[i].getPlanNumber()+" "+newData[i].getPlanNumber());
			}
			ps.close();
			log.println("Replacement mapping saved");
		}catch(Exception e){
			log.print("Error: ");
			e.printStackTrace();
		}
	}








	static int dim;
	static int index[];
	static DataValues [] gdata;
	static int costCt;
	static boolean [][] costing;

	static double [][] costs;

	public static DiagramPacket reduceND(DiagramPacket gdp,float th,float minAr,int d) {
		DiagramPacket ngdp = new DiagramPacket();

	/*	ngdp.setMaxCard(gdp.getMaxCard());
		ngdp.setResolution(gdp.getMaxResolution());
		ngdp.setMaxConditions(gdp.getDimension());
		ngdp.setMaxCost(gdp.getMaxCost());
		ngdp.setRelationNames(gdp.getRelationNames());
		ngdp.setMaxPlanNumber(gdp.getMaxPlanNumber());
		ngdp.setPicassoSelectivity(gdp.getPicassoSelectivity());
		ngdp.setPlanSelectivity(gdp.getPlanSelectivity());
		ngdp.setPredicateSelectivity(gdp.getPredicateSelectivity());
		ngdp.setRelationNames(gdp.getRelationNames());
		ngdp.setAttributeNames(gdp.getAttributeNames());
		ngdp.setConstants(gdp.getConstants());
		ngdp.setQueryPacket(gdp.getQueryPacket());*/
		
		int n;
		n = gdp.getMaxPlanNumber();
		
//		log.println(n);
		Set[] s = new Set[n];
		Integer [] x = new Integer[n];
		boolean [] notSwallowed = new boolean[n];
		DataValues [] data = gdp.getData();
		gdata = data;
		DataValues[] newData = new DataValues[data.length];
		boolean [][] swallow = new boolean[n][n];
		int r = gdp.getMaxResolution();
		sel = gdp.getPicassoSelectivity();
		costing = new boolean [n][data.length];
		
		
// for NEW_BENEFIT
		int [] res = new int[d];
		for( int i = 0; i<d ; i++)  			
			res[i] = r;

		offset = new int[d];
		base = new int[d];
		// initialize offset
		offset[0] = 0;
		for (int i = 1; i < d; i++)
			offset[i] = offset[i - 1] + res[i - 1];
		// initialize base
		base[0] = 1;
		for (int i = 1; i < d; i++)
			base[i] = base[i - 1] * res[i - 1];
// for NEW_BENEFIT
		
		
		float rem = (1 - minAr) * data.length;
		index = new int[d];
		for(int i = 0;i < n;i ++) {
			s[i] = new Set();
			x[i] = new Integer(i);
			s[i].elements.add(x[i]);
		}
		init();
		for(int i = 0;i < n;i ++) {
			boolean flag = false;
//			if(i == 6) {
//				log.println("Seer.reduceND()");
//			}
			for(int j = 0;j < n;j ++) {
				// j -> swallower & i -> swallowee
				if(i != j) {
//					if(pointCheck(j,i,th,rem,r,0) >= 0) {
//					if(cornerCheck(j,i,th,minAr,r,d) != -1) {
					if(ndCheckND(j,i,th,rem,d,r,0) != -1) {
						s[j].elements.add(x[i]);
						flag = true;
						swallow[j][i] = true;
					}
				}
			}
			if(!flag) {
				notSwallowed[i] = true;
			}
		}

		int [] plan = new int [n];

		
		log.print("\n\nIn OLD-SEER\n\n");

		for(int i=0;i<n;i++){
			log.print("\n" + i + " swallows = ");
			for(int j=0;j<n;j++){
				if(s[i].elements.contains(x[j]))					log.print(" " + j + " ");
			}
		}
		
		
		
		if(choiceGreedy == ANOREXIC) {

			ArrayList soln = new ArrayList();
			Set temp = new Set();
			for (int i = 0; i < n; i++) {
				if (notSwallowed[i] && s[i] != null) {
					temp.elements.addAll(s[i].elements);
					s[i].elements.clear();
					s[i] = null;
					soln.add(new Integer(i));
				}
			}
	
			for (int i = 0; i < n; i++) {
				if (s[i] != null) {
					s[i].elements.removeAll(temp.elements);
				}
			}
			while (true) {
				int max = 0;
				int p = -1;
				for (int i = 0; i < n; i++) {
					if (s[i] != null) {
						int size = s[i].elements.size();
						if (size > max) {
							max = size;
							p = i;
						}
					}
				}
				if (p == -1) {
					break;
				}
				soln.add(new Integer(p));
				for (int i = 0; i < n; i++) {
					if (s[i] != null && i != p) {
						s[i].elements.removeAll(s[p].elements);
						if (s[i].elements.size() == 0) {
							s[i] = null;
						}
					}
				}
				s[p] = null;
			}
	
	
			// soln has the required soln
			log.println("\n# plans in OLD_SEER Reduced Diagram : " + soln.size());
			log.print("Plans : ");
			for(Iterator it = soln.iterator();it.hasNext();) {
				Integer pl = (Integer) it.next();
				log.print(pl + ",");
			}
			log.println();
			
			for(int i = 0;i < n;i ++) {
				if(soln.contains(x[i])) {
					plan[i] = i;
				} else {
					for(Iterator it = soln.iterator();it.hasNext();) {
						Integer xx = (Integer) it.next();
						if(swallow[xx.intValue()][i]) {
							plan[i] = xx.intValue();
							break;
						}
					}
				}
			}
			ngdp.setMaxPlanNumber(soln.size());
		} // end if (ANOREXIC)

		else if (choiceGreedy == NEW_BENEFIT) {

			///////////////////creating selpts - used to find corner costs - to calculate benefit estimate	

			int numPoints = (int) Math.pow(2,d);            // needed for NEW_BENEFIT


			int[][] selpt = new int[numPoints][d];

			if(d == 2) {		
				selpt[0][0] = 0;
				selpt[0][1] = 0;
				selpt[1][0] = 0;
				selpt[1][1] = res[1] - 1;
				selpt[2][0] = res[0] - 1;
				selpt[2][1] = res[1] - 1;
				selpt[3][0] = res[0] - 1;
				selpt[3][1] = 0;
			}
			else if ( d == 3) {
	
				selpt[0][0] = 0;
				selpt[0][1] = 0;
				selpt[0][2] = 0;
			
				selpt[1][0] = res[0] - 1;
				selpt[1][1] = 0;
				selpt[1][2] = 0;
			
	
				selpt[2][0] = res[0] - 1;
				selpt[2][1] = res[1] - 1;
				selpt[2][2] = 0;
			
				selpt[3][0] = 0;
				selpt[3][1] = res[1] - 1;
				selpt[3][2] = 0;
			
				selpt[4][0] = 0;
				selpt[4][1] = 0;
				selpt[4][2] = res[2] - 1;
			
				selpt[5][0] = res[0] - 1;
				selpt[5][1] = 0;
				selpt[5][2] = res[2] - 1;
			
				selpt[6][0] = res[0] - 1;
				selpt[6][1] = res[1] - 1;
				selpt[6][2] = res[2] - 1;
			
				selpt[7][0] = 0;
				selpt[7][1] = res[1] - 1;
				selpt[7][2] = res[2] - 1;
			
			}
			///////////////////creating selpts - used to find corner costs - to calculate benefit estimate


			      // fill the replacement plan in this array
			double[] benefit = new double[n];
			int[] soln = new int[n];
			int previousReplacer = -1;


			// filling according to benefit

			for(int i=0; i<n ; i++)			soln[i] = 0;    // it will be soln if it replaces more than 0

			for(int i=0; i<n; i++) {
				if(notSwallowed[i] == true) {
					plan[i] = i;
					benefit[i] = 1.0;
					soln[i] += 1;
					continue;
				}
				else {
					benefit[i] = Double.MIN_VALUE;	
				}


				previousReplacer = -1;
	
				for(int j=0; j<n; j++) {
					if(swallow[j][i] == true || j==i) {
						double num = 0.0, denom = 0.0, curBenefit = 0.0;
	
						for(int k=0; k<numPoints; k++) {
							num += Cost(i, PtIndex(selpt[k], res, d));
							denom += Cost(j, PtIndex(selpt[k], res, d)) ;
						} 
						curBenefit = num/denom;
				
						if(curBenefit > benefit[i]) {
							benefit[i] = curBenefit;
							plan[i] = j;
							
							if(previousReplacer == -1) {
								previousReplacer = j;
								soln[j] += 1;	
							}
							else {
								soln[previousReplacer] -= 1;
								previousReplacer = j;
								soln[j] += 1;
	
							}
						}
					}
				}
			}	




			log.print("\n The Solution is - \n");
			int count = 0;	
			for(int i=0; i<n; i++){
				if(soln[i] >= 1) {
				 	//log.print("\t" + i + "\t" + plan[i] + "\t" + soln[i] + "\t" + stability[i] + "\t" + stability[plan[i]] + "\n");
					log.print("\t" + i + "\t" + soln[i] + "\n");
					count++;
				}	
			}
			log.print("\n Total plans - " + count + "\n");

			ngdp.setMaxPlanNumber(count);

		}  // end NEW - BENEFIT


		
		



		for(int i = 0;i < data.length;i ++) {
			int pi = data[i].getPlanNumber();
			newData[i] = new DataValues();
			newData[i].setCard(data[i].getCard());
			newData[i].setPlanNumber(plan[pi]);
			newData[i].setCost(getCost(plan[pi], i));
		}
		ngdp.setDataPoints(newData);
		
		return ngdp;
	}

	
	private static float ndCheckND(int pEr,int pEe,float th, float rem, int d, int r,float curAr) {
		dim = d;
		index = new int[d];
		return checkND(pEr,pEe,th,rem,d,curAr,r);
	}


	private static float checkND(int er, int ee, float th, float rem, int d,float curAr,int r) {
		if(d == 2) {
//			return pointCheck(er, ee, th,rem,r,curAr);
//			if(er == 3 && ee == 8) {
//				log.println("Seer.check()");
//			}
//			float wc = wedgeCheck(er, ee, th,rem,r,curAr);
			float pc = pointCheckND(er, ee, th,rem,r,curAr);
//			if(wc != pc) {
//				log.println("Seer.check()");
//			}
			return pc;
		} else {
			for(int i = 0;i < r;i ++) {
				index[d-1] = i;
				curAr = checkND(er,ee,th,rem,d-1,curAr,r);
				if(curAr == -1) {
					return -1;
				}
			}
		}
		return curAr;
	}





	public static float pointCheckND(int pEr,int pEe,float th, float rem,int r,float cur) {
		// IMP: x,y as row and col, not axis
		
		// To add Diagram pkt when secessary
		// For now using global variables and loading the costs into it
		th = 1 + th/100;
		init(pEr,pEe);
		
		// and get the resolution from it
//		int r = 30;
//		if(pEr == 6 && pEe == 15) {
//			log.println("Seer.pointCheck()");
//		}
		double maxc = 0;
		
		double lb = cost(pEr, 0,0,r) - th * cost(pEe,0,0,r);
		double lt = cost(pEr, 0,r-1,r) - th * cost(pEe,0,r-1,r);
		double rb = cost(pEr, r-1,0,r) - th * cost(pEe,r-1,0,r);
		double rt = cost(pEr, r-1,r-1,r) - th * cost(pEe,r-1,r-1,r);
		
		short ct = 0;
		if(lb <= maxc) {
			ct ++;
		}
		if(lt <= maxc) {
			ct ++;
		}
		if(rb <= maxc) {
			ct ++;
		}
		if(rt <= maxc) {
			ct ++;
		}
		if(ct != 4 && rem == 0) {
			return -1;
		}

		double ml1 = cost(pEr, 0,1,r) - th * cost(pEe,0,1,r);
		double mr1 = cost(pEr, r-1,1,r) - th * cost(pEe,r-1,1,r);
		double mb1 = cost(pEr, 1,0,r) - th * cost(pEe,1,0,r);
		double mt1 = cost(pEr, 1,r-1,r) - th * cost(pEe,1,r-1,r);
		
		double ml2 = cost(pEr, 0,r-2,r) - th * cost(pEe,0,r-2,r);
		double mr2 = cost(pEr, r-1,r-2,r) - th * cost(pEe,r-1,r-2,r);
		double mb2 = cost(pEr, r-2,0,r) - th * cost(pEe,r-2,0,r);
		double mt2 = cost(pEr, r-2,r-1,r) - th * cost(pEe,r-2,r-1,r);
		
		if(ml1 > maxc || mr1 > maxc || mb1 > maxc || mt1 > maxc || ml2 > maxc || mr2 > maxc || mb2 > maxc || mt2 > maxc) {
			return -1;
		}
		
		double sl1 = (ml1 - lb) / (sel[1] - sel[0]);
		double sl2 = (lt - ml2) / (sel[r-1] - sel[r-2]);
		
		double sr1 = (mr1 - rb) / (sel[1] - sel[0]);
		double sr2 = (rt - mr2) / (sel[r-1] - sel[r-2]);
		
		double st1 = (mt1 - lt) / (sel[1] - sel[0]);
		double st2 = (rt - mt2) / (sel[r-1] - sel[r-2]);
		
		double sb1 = (mb1 - lb) / (sel[1] - sel[0]);;
		double sb2 = (rb - mb2) / (sel[r-1] - sel[r-2]);
		
		boolean incL = false;
		boolean incR = false;
		boolean incT = false;
		boolean incB = false;
		
		if(sl2 >= sl1) {
			incL = true; 
		}
		if(sr2 >= sr1) {
			incR = true; 
		}
		if(st2 >= st1) {
			incT = true; 
		}
		if(sb2 >= sb1) {
			incB = true; 
		}

		boolean lr = false;
		if(incL && incR ) {
			lr = true;
		}

		boolean tb = false;
		if(incT && incB) {
			tb = true;
		}
		
		boolean top = false;
		boolean bottom = false;
		boolean left = false;
		boolean right = false;
		
		if(lb <= 0 && lt <= 0) {
			if(incL) {
				left = true;
			} else {
				if(sl1 <= 0 || sl2 >= 0) {
					left = true;
				}
			}
		}

		if(rb <= 0 && rt <= 0) {
			if(incR) {
				right = true;
			} else {
				if(sr1 <= 0 || sr2 >= 0) {
					right = true;
				}
			}
		}

		if(lb <= 0 && rt <= 0) {
			if(incB) {
				bottom = true;
			} else {
				if(sb1 <= 0 || sb2 >= 0) {
					bottom = true;
				}
			}
		}

		if(lt <= 0 && rt <= 0) {
			if(incT) {
				top = true;
			} else {
				if(st1 <= 0 || st2 >= 0) {
					top = true;
				}
			}
		}
		
		// Corollary 1
		if(lr && top && bottom) {
			return cur;
		}
		
		if(tb && left && right) {
			return cur;
		}
		
		// Theorem 1
		if(!lr && !tb) {
			boolean sl = false;
			if(!incL && !incR) {
				if(sl1 <= 0 && sr1 <= 0) {
					for(int i = 1;i < r - 1;i ++) {
						double cc = cost(pEr, i,1,r) - th * cost(pEe,i,1,r);
						if(cc > maxc) {
							return -1;
						}
						
						double cc1 = (cost(pEr, i,0,r) - th * cost(pEe,i,0,r)); 
						cc = cc - cc1;
						if(cc1 > maxc) {
							return -1;
						}
						
						if(cc > 0) {
							sl = true;
							break;
						}
					}
				} else if(sl2 >= 0 && sr2 >= 0) {
					for(int i = 1;i < r - 1;i ++) {
						double cc = cost(pEr, i,r-1,r) - th * cost(pEe,i,r-1,r);
						if(cc > maxc) {
							return -1;
						}
						double cc1 = (cost(pEr, i,r-2,r) - th * cost(pEe,i,r-2,r));
						cc = cc - cc1;
						if(cc1 > maxc) {
							return -1;
						}

						if(cc < 0) {
							sl = true;
							break;
						}
					}
				}
			}
//			if(!incL && !incR && !sl) {
//			if(!sl) {
//				lr = true;
//				if(!top) {
//					top = true;
//					for(int i = 1;i < r - 1;i ++) {
//						double cc = cost(pEr, i,r-1,r) - th * cost(pEe,i,r-1,r);
//						if(cc > 0) {
//							top = false;
//							break;
//						}
//					}
//				}
//				if(!bottom) {
//					bottom = true;
//					for(int i = 1;i < r - 1;i ++) {
//						double cc = cost(pEr, i,0,r) - th * cost(pEe,i,0,r);
//						if(cc > 0) {
//							bottom = false;
//							break;
//						}
//					}
//				}
//				if(top && bottom) {
//					return cur;
//				}
//			}
//			sl = false;
//			if(!incT && !incB) {
//				if(st1 <= 0 && sb1 <= 0) {
//					for(int i = 1;i < r - 1;i ++) {
//						double cc = cost(pEr, 1,i,r) - th * cost(pEe,1,i,r);
//						cc = cc - (cost(pEr,0,i,r) - th * cost(pEe,0,i,r));
//						if(cc > 0) {
//							sl = true;
//							break;
//						}
//					}
//				} else if(st2 >= 0 && sb2 >= 0) {
//					for(int i = 1;i < r - 1;i ++) {
//						double cc = cost(pEr, r-1,i,r) - th * cost(pEe,r-1,i,r);
//						cc = cc - (cost(pEr,r-2,i,r) - th * cost(pEe,r-2,i,r));
//						if(cc < 0) {
//							sl = true;
//							break;
//						}
//					}
//				}
//			}
//			if(!incT && !incB && !sl) {
//				tb = true;
//				if(!left) {
//					left = true;
//					for(int i = 1;i < r - 1;i ++) {
//						double cc = cost(pEr, 0,i,r) - th * cost(pEe,0,i,r);
//						if(cc > 0) {
//							left = false;
//							break;
//						}
//					}
//				}
//				if(!right) {
//					right = true;
//					for(int i = 1;i < r - 1;i ++) {
//						double cc = cost(pEr,r-1,i,r) - th * cost(pEe,r-1,i,r);
//						if(cc > 0) {
//							right = false;
//							break;
//						}
//					}
//				}
//				if(left && right) {
//					return cur;
//				}
//			}
//			if(!lr && !tb) {
//				return -1;
//			}
			if(!sl) {
				lr = true;
			}
			sl = false;
			if(!incT && !incB) {
				if(st1 <= 0 && sb1 <= 0) {
					for(int i = 1;i < r - 1;i ++) {
						double cc = cost(pEr, 1,i,r) - th * cost(pEe,1,i,r);
						if(cc > maxc) {
							return -1;
						}
						double cc1 = (cost(pEr,0,i,r) - th * cost(pEe,0,i,r)); 
						if(cc1 > maxc) {
							return -1;
						}
						cc = cc - cc1;
						if(cc > 0) {
							sl = true;
							break;
						}
					}
				} else if(st2 >= 0 && sb2 >= 0) {
					for(int i = 1;i < r - 1;i ++) {
						double cc = cost(pEr, r-1,i,r) - th * cost(pEe,r-1,i,r);
						if(cc > maxc) {
							return -1;
						}
						double cc1 = (cost(pEr,r-2,i,r) - th * cost(pEe,r-2,i,r));
						cc = cc - cc1;
						if(cc1 > maxc) {
							return -1;
						}
						if(cc < 0) {
							sl = true;
							break;
						}
					}
				}
			}
			if(!sl) {
				tb = true;
			}
			if(!lr && !tb) {
				return -1;
			}

		}
		if(ct == 4 && lr && tb) {
			return cur;
		}
		
		boolean cont = true;
		if(!lr) {
			if(!left) {
				for(int i = 1;i < r - 1;i ++) {
					// l
					double cc = cost(pEr, 0,i,r) - th * cost(pEe,0,i,r);
					if(cc > maxc) {
						cont = false;
						break;
					}
				}
				if(cont) {
					left = true;
				} else if(rem == 0) {
					return -1;
				}
			}
			cont = true;
			if(!right) {
				for(int i = 1;i < r - 1;i ++) {
					// r
					double cc = cost(pEr, r-1,i,r) - th * cost(pEe,r-1,i,r);
					if(cc > maxc) {
						cont = false;
						break;
					}
				}
				if(cont) {
					right = true;
				} else if(rem == 0) {
					return -1;
				}
			}
		} else if(!tb) {
			if(!bottom) {
				for(int i = 1;i < r - 1;i ++) {
					// b
					double cc = cost(pEr, i,0,r) - th * cost(pEe,i,0,r);
					if(cc > maxc) {
						cont = false;
						break;
					}
				}
				if(cont) {
					bottom = true;
				} else if(rem == 0) {
					return -1;
				}
			}
			cont = true;
			if(!top) {
				for(int i = 1;i < r - 1;i ++) {
					// t
					double cc = cost(pEr,i,r-1,r) - th * cost(pEe,i,r-1,r);
					if(cc > maxc) {
						cont = false;
						break;
					}
				}
				if(cont) {
					top = true;
				} else if(rem == 0) {
					return -1;
				}
			}
		} else if(ct == 4) {
			// this is a redundant check
			return cur;
		}
		
		if(lr && top && bottom) {
			return cur;
		}
		
		if(tb && left && right) {
			return cur;
		}
		
		if(rem == 0) {
			return -1;
		}
		
		if(ct >= 3) {
			if(left && bottom) {
				float p = getEffPercent(pEr, pEe, 0, r-1, 1, -1, r, -1, r, th, rem, false,cur);
				return p;
			}
			if(left && top) {
				float p = getEffPercent(pEr, pEe, 0, 0, 1, 1, r, r, r, th,rem, false,cur);
				return p;
			}
			if(right && bottom) {
				float p = getEffPercent(pEr, pEe, r - 1, r - 1, -1, -1, -1, -1, r, th,rem, false,cur);
				return p;
			}
			if(right && top) {
				float p = getEffPercent(pEr, pEe, r-1, 0, -1, 1, -1, r, r, th, rem, false,cur);
				return p;
			}
		}
		return -1;
	}



	static float getEffPercent(int pEr,int pEe,int x,int y,int incR,int incD,int stopX,int stopY,int r, float th,float rem, boolean down,float cur) {
//		float c = 0;
		boolean rt = !down;

		float exc = cur;
		float aexc = 0;
//		float rem = r * r * (1 - ar);
		rem = (float) Math.ceil(rem);
		while(y != stopY && x != stopX) {
			if(exc > rem) {

				return -1;
			}
			double diff = cost(pEr, x,y,r) - th * cost(pEe,x,y,r);
			if(!rt && diff <= 0) {
				rt = true;
			}
			if(rt && diff > 0) {
				rt = false;
			}
			if(rt) {
				x += incR;
//				c += Math.abs((stopY - y));
				aexc += (r - Math.abs((stopY - y)));
			} else {
				y += incD;
			}
			exc = aexc + (Math.abs(stopX - x)) * (r - Math.abs((stopY - y)));
		}
//		if(y == stopY) {
//			c += r * Math.abs(stopX - x);
//		}
//		if(r * r - c != exc) {
//			log.println("Seer.getEffPercent()");
//		}

//		c /= (r * r);
		return exc;
	}


	private static double cost(int p, int i, int j, int r) {
		int ct = 0;
		index[1] = i;
		index[0] = j;
		for(int x = dim-1;x >= 0;x --) {
			ct = ct * r + index[x];
		}
		if(gdata[ct].getPlanNumber() == p) {
			costing[p][ct] = true;
		}
		if (!costing[p][ct]) {
			costing[p][ct] = true;
			costCt++;
		}
		if(p == per) {
			return planCostEr[ct];
		} else {
			return planCostEe[ct];
		}
	}


	private static double getCost(int p, int i) {
//		ObjectInputStream inx;
		try {
			return costs[p][i];
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		return -1;
	}

	public static void init() {
		ObjectInputStream inx;

		pcstPath = path+"3D/"+qNameOrig + "/";

		try {
			inx = new ObjectInputStream(new FileInputStream(new File(pcstPath,"0.pcst")));
			double [] planCost = (double[]) inx.readObject();
			int l = planCost.length;
			inx.close();
			costs = new double[nPlans][l];
			costs[0] = planCost;
			for(int i = 1;i < nPlans;i ++) {
				inx = new ObjectInputStream(new FileInputStream(new File(pcstPath,i + ".pcst")));
				costs[i] = (double[]) inx.readObject();
				inx.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		
	}

}
