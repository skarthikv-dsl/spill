
public class asoWeightCalculation {
	
	static int UNIFORM_DISTRIBUTION = 0; 							// 0 means exponential distribution
	static int JSP_ExpoDistributionBase = 2;						// for different distribution for location weights
//	static int JSP_ExpoDistributionBase = -1;						// for different distribution for location weights


	// for the new calculation for exponential distribution
	static double planCount[], planRelativeArea[];
	static float picsel[], locationWeight[];

	static double areaSpace =0,totalEstimatedArea = 0;


	// for the new calculation for exponential distribution


// functions for location weight calculations and the checking for validity


	public static void checkValidityofWeights() {
		double areaPlans =0;
		double relativeAreaPlans =0;
		areaSpace = 0.0;

		planRelativeArea = new double[gdp.getMaxPlanNumber()];

		for (int i=0; i< data.length; i++){
			areaSpace += locationWeight[i];
		}
		//	System.out.println(areaSpace);

		for (int i=0; i< gdp.getMaxPlanNumber(); i++){
			planRelativeArea[i] = planCount[i]/areaSpace;
			relativeAreaPlans += planRelativeArea[i];
			areaPlans += planCount[i];
		}
		//	System.out.println(areaPlans);
		//	System.out.println(relativeAreaPlans);

		if(relativeAreaPlans < 0.99) {
			System.out.println("ALERT! The area of plans add up to only " + relativeAreaPlans);
			//System.exit(0);
		}
	}



	public static void getPlanCountArray() {
		planCount = new double[gdp.getMaxPlanNumber()];
		locationWeight = new float[data.length];
		// Set dim depending on whether we are dealing with full packet or slice
		// int dim;

		double StartPoint[] = { 0.0, 0.0, 0.0, 0.0, 0.0 };
		double EndPoint[] = { 1.0, 1.0, 1.0, 1.0, 1.0 };

		int resln = gdp.getMaxResolution();
		int dim = gdp.getDimension();

		int[] r = new int[dim];

		for (int i = 0; i < dim; i++)
			r[i] = resln;

		picsel = new float[5 * resln];
		// picsel = new float[dim*resln];

		//		if (resln == 10) {
		//			picsel[0] = 0.03258f;			picsel[1] = 0.16292f;			picsel[2] = 0.42359f;			picsel[3] = 0.94493f;			picsel[4] = 1.98762f;			picsel[5] = 4.07299f;
		//			picsel[6] = 8.24373f;			picsel[7] = 16.58521f;			picsel[8] = 33.26817f;			picsel[9] = 66.63408f;			picsel[10] = 0.03258f;			picsel[11] = 0.16292f;
		//			picsel[12] = 0.42359f;			picsel[13] = 0.94493f;			picsel[14] = 1.98762f;			picsel[15] = 4.07299f;			picsel[16] = 8.24373f;			picsel[17] = 16.58521f;
		//			picsel[18] = 33.26817f;			picsel[19] = 66.63408f;			picsel[20] = 0.03258f;			picsel[21] = 0.16292f;			picsel[22] = 0.42359f;			picsel[23] = 0.94493f;
		//			picsel[24] = 1.98762f;			picsel[25] = 4.07299f;			picsel[26] = 8.24373f;			picsel[27] = 16.58521f;			picsel[28] = 33.26817f;			picsel[29] = 66.63408f;
		//
		//		}
		//
		//		else if (resln == 30) {
		//			picsel[0] = 0.00273f;			picsel[1] = 0.00998f;			picsel[2] = 0.01963f;			picsel[3] = 0.03246f;			picsel[4] = 0.04953f;			picsel[5] = 0.07222f;
		//			picsel[6] = 0.10241f;			picsel[7] = 0.14256f;			picsel[8] = 0.19596f;			picsel[9] = 0.26698f;			picsel[10] = 0.36144f;			picsel[11] = 0.48707f;
		//			picsel[12] = 0.65416f;			picsel[13] = 0.87638f;			picsel[14] = 1.17194f;			picsel[15] = 1.56504f;			picsel[16] = 2.08785f;			picsel[17] = 2.78320f;
		//			picsel[18] = 3.70800f;			picsel[19] = 4.93800f;			picsel[20] = 6.57389f;			picsel[21] = 8.74963f;			picsel[22] = 11.64337f;			picsel[23] = 15.49203f;
		//			picsel[24] = 20.61076f;			picsel[25] = 27.41866f;			picsel[26] = 36.47317f;			picsel[27] = 48.51568f;			picsel[28] = 64.53220f;			picsel[29] = 85.83418f;
		//
		//			picsel[30] = 0.00273f;			picsel[31] = 0.00998f;			picsel[32] = 0.01963f;			picsel[33] = 0.03246f;			picsel[34] = 0.04953f;			picsel[35] = 0.07222f;
		//			picsel[36] = 0.10241f;			picsel[37] = 0.14256f;			picsel[38] = 0.19596f;			picsel[39] = 0.26698f;			picsel[40] = 0.36144f;			picsel[41] = 0.48707f;
		//			picsel[42] = 0.65416f;			picsel[43] = 0.87638f;			picsel[44] = 1.17194f;			picsel[45] = 1.56504f;			picsel[46] = 2.08785f;			picsel[47] = 2.78320f;
		//			picsel[48] = 3.70800f;			picsel[49] = 4.93800f;			picsel[50] = 6.57389f;			picsel[51] = 8.74963f;			picsel[52] = 11.64337f;			picsel[53] = 15.49203f;
		//			picsel[54] = 20.61076f;			picsel[55] = 27.41866f;			picsel[56] = 36.47317f;			picsel[57] = 48.51568f;			picsel[58] = 64.53220f;			picsel[59] = 85.83418f;
		//
		//			picsel[60] = 0.00273f;			picsel[61] = 0.00998f;			picsel[62] = 0.01963f;			picsel[63] = 0.03246f;			picsel[64] = 0.04953f;			picsel[65] = 0.07222f;
		//			picsel[66] = 0.10241f;			picsel[67] = 0.14256f;			picsel[68] = 0.19596f;			picsel[69] = 0.26698f;			picsel[70] = 0.36144f;			picsel[71] = 0.48707f;
		//			picsel[72] = 0.65416f;			picsel[73] = 0.87638f;			picsel[74] = 1.17194f;			picsel[75] = 1.56504f;			picsel[76] = 2.08785f;			picsel[77] = 2.78320f;
		//			picsel[78] = 3.70800f;			picsel[79] = 4.93800f;			picsel[80] = 6.57389f;			picsel[81] = 8.74963f;			picsel[82] = 11.64337f;			picsel[83] = 15.49203f;
		//			picsel[84] = 20.61076f;			picsel[85] = 27.41866f;			picsel[86] = 36.47317f;			picsel[87] = 48.51568f;			picsel[88] = 64.53220f;			picsel[89] = 85.83418f;
		//
		//		}


		if(resln == 10) {
			if(JSP_ExpoDistributionBase == 2) {
				// april - 2012
				//				picsel[0] = 0.005f;			picsel[1] = 0.01f;			picsel[2] = 0.02f;			picsel[3] = 0.05f;			picsel[4] = 0.1f;			picsel[5] = 0.15f;
				//				picsel[6] = 0.30f;			picsel[7] = 0.50f;			picsel[8] = 0.70f;			picsel[9] = 0.90f;
				//
				//				picsel[10] = 0.005f;		picsel[11] = 0.01f;			picsel[12] = 0.02f;			picsel[13] = 0.05f;			picsel[14] = 0.1f;			picsel[15] = 0.15f;
				//				picsel[16] = 0.30f;			picsel[17] = 0.50f;			picsel[18] = 0.70f;			picsel[19] = 0.90f;
				//
				//				picsel[20] = 0.005f;		picsel[21] = 0.01f;			picsel[22] = 0.02f;			picsel[23] = 0.05f;			picsel[24] = 0.1f;			picsel[25] = 0.15f;
				//				picsel[26] = 0.30f;			picsel[27] = 0.50f;			picsel[28] = 0.70f;			picsel[29] = 0.90f;
				//
				//				picsel[30] = 0.005f;		picsel[31] = 0.01f;			picsel[32] = 0.02f;			picsel[33] = 0.05f;			picsel[34] = 0.1f;			picsel[35] = 0.15f;
				//				picsel[36] = 0.30f;			picsel[37] = 0.50f;			picsel[38] = 0.70f;			picsel[39] = 0.90f;
				//
				//				picsel[40] = 0.005f;		picsel[41] = 0.01f;			picsel[42] = 0.02f;			picsel[43] = 0.05f;			picsel[44] = 0.1f;			picsel[45] = 0.15f;
				//				picsel[46] = 0.30f;			picsel[47] = 0.50f;			picsel[48] = 0.70f;			picsel[49] = 0.90f;
				//oct 2012

				//				picsel[0] = 0.0005f;		picsel[1] = 0.05f;			picsel[2] = 0.01f;			picsel[3] = 0.02f;			picsel[4] = 0.05f;			picsel[5] = 0.10f;
				//				picsel[6] = 0.20f;			picsel[7] = 0.40f;			picsel[8] = 0.60f;			picsel[9] = 0.95f;
				//
				//				picsel[10] = 0.0005f;		picsel[11] = 0.05f;			picsel[12] = 0.01f;			picsel[13] = 0.02f;			picsel[14] = 0.05f;			picsel[15] = 0.10f;
				//				picsel[16] = 0.20f;			picsel[17] = 0.40f;			picsel[18] = 0.60f;			picsel[19] = 0.95f;
				//
				//				picsel[20] = 0.0005f;		picsel[21] = 0.05f;			picsel[22] = 0.01f;			picsel[23] = 0.02f;			picsel[24] = 0.05f;			picsel[25] = 0.10f;
				//				picsel[26] = 0.20f;			picsel[27] = 0.40f;			picsel[28] = 0.60f;			picsel[29] = 0.95f;
				//
				//				picsel[30] = 0.0005f;		picsel[31] = 0.05f;			picsel[32] = 0.01f;			picsel[33] = 0.02f;			picsel[34] = 0.05f;			picsel[35] = 0.10f;
				//				picsel[36] = 0.20f;			picsel[37] = 0.40f;			picsel[38] = 0.60f;			picsel[39] = 0.95f;
				//
				//				picsel[40] = 0.0005f;		picsel[41] = 0.05f;			picsel[42] = 0.01f;			picsel[43] = 0.02f;			picsel[44] = 0.05f;			picsel[45] = 0.10f;
				//				picsel[46] = 0.20f;			picsel[47] = 0.40f;			picsel[48] = 0.60f;			picsel[49] = 0.95f;


				picsel[0] = 0.00005f;		picsel[1] = 0.0005f;		picsel[2] = 0.005f;			picsel[3] = 0.02f;			picsel[4] = 0.05f;			picsel[5] = 0.10f;
				picsel[6] = 0.15f;			picsel[7] = 0.25f;			picsel[8] = 0.50f;			picsel[9] = 0.99f;

				picsel[10] = 0.00005f;		picsel[11] = 0.0005f;		picsel[12] = 0.005f;		picsel[13] = 0.02f;			picsel[14] = 0.05f;			picsel[15] = 0.10f;
				picsel[16] = 0.15f;			picsel[17] = 0.25f;			picsel[18] = 0.50f;			picsel[19] = 0.99f;

				picsel[20] = 0.00005f;		picsel[21] = 0.0005f;		picsel[22] = 0.005f;		picsel[23] = 0.02f;			picsel[24] = 0.05f;			picsel[25] = 0.10f;
				picsel[26] = 0.15f;			picsel[27] = 0.25f;			picsel[28] = 0.50f;			picsel[29] = 0.99f;

				picsel[30] = 0.00005f;		picsel[31] = 0.0005f;		picsel[32] = 0.005f;		picsel[33] = 0.02f;			picsel[34] = 0.05f;			picsel[35] = 0.10f;
				picsel[36] = 0.15f;			picsel[37] = 0.25f;			picsel[38] = 0.50f;			picsel[39] = 0.99f;

				picsel[40] = 0.00005f;		picsel[41] = 0.0005f;		picsel[42] = 0.005f;		picsel[43] = 0.02f;			picsel[44] = 0.05f;			picsel[45] = 0.10f;
				picsel[46] = 0.15f;			picsel[47] = 0.25f;			picsel[48] = 0.50f;			picsel[49] = 0.99f;


				//				picsel[0] = 0.16f;			picsel[1] = 0.42f;			picsel[2] = 0.94f;			picsel[3] = 1.98f;			picsel[4] = 4.07f;			picsel[5] = 8.24f;
				//				picsel[6] = 16.58f;			picsel[7] = 33.26f;			picsel[8] = 66.63f;			picsel[9] = 99.50f;
				//
				//				picsel[10] = 0.16f;			picsel[11] = 0.42f;			picsel[12] = 0.94f;			picsel[13] = 1.98f;			picsel[14] = 4.07f;			picsel[15] = 8.24f;
				//				picsel[16] = 16.58f;		picsel[17] = 33.26f;		picsel[18] = 66.63f;		picsel[19] = 99.50f;
				//
				//				picsel[20] = 0.16f;			picsel[21] = 0.42f;			picsel[22] = 0.94f;			picsel[23] = 1.98f;			picsel[24] = 4.07f;			picsel[25] = 8.24f;
				//				picsel[26] = 16.58f;		picsel[27] = 33.26f;		picsel[28] = 66.63f;		picsel[29] = 99.50f;
				//
				//				picsel[30] = 0.16f;			picsel[31] = 0.42f;			picsel[32] = 0.94f;			picsel[33] = 1.98f;			picsel[34] = 4.07f;			picsel[35] = 8.24f;
				//				picsel[36] = 16.58f;		picsel[37] = 33.26f;		picsel[38] = 66.63f;		picsel[39] = 99.50f;
				//
				//				picsel[40] = 0.16f;			picsel[41] = 0.42f;			picsel[42] = 0.94f;			picsel[43] = 1.98f;			picsel[44] = 4.07f;			picsel[45] = 8.24f;
				//				picsel[46] = 16.58f;		picsel[47] = 33.26f;		picsel[48] = 66.63f;		picsel[49] = 99.50f;
			}
			else if(JSP_ExpoDistributionBase == 5) {
				picsel[0] = 0.0000512f;		picsel[1] = 0.000256f;		picsel[2] = 0.00128f;		picsel[3] = 0.0064f;		picsel[4] = 0.032f;			picsel[5] = 0.16f;
				picsel[6] = 0.8f;			picsel[7] = 4.0f;			picsel[8] = 20.0f;			picsel[9] = 99.50f;

				picsel[10] = 0.0000512f;	picsel[11] = 0.000256f;		picsel[12] = 0.00128f;		picsel[13] = 0.0064f;		picsel[14] = 0.032f;		picsel[15] = 0.16f;
				picsel[16] = 0.8f;			picsel[17] = 4.0f;			picsel[18] = 20.0f;			picsel[19] = 99.50f;

				picsel[20] = 0.0000512f;	picsel[21] = 0.000256f;		picsel[22] = 0.00128f;		picsel[23] = 0.0064f;		picsel[24] = 0.032f;		picsel[25] = 0.16f;
				picsel[26] = 0.8f;			picsel[27] = 4.0f;			picsel[28] = 20.0f;			picsel[29] = 99.50f;

				picsel[30] = 0.0000512f;	picsel[31] = 0.000256f;		picsel[32] = 0.00128f;		picsel[33] = 0.0064f;		picsel[34] = 0.032f;		picsel[35] = 0.16f;
				picsel[36] = 0.8f;			picsel[37] = 4.0f;			picsel[38] = 20.0f;			picsel[39] = 99.50f;

				picsel[40] = 0.0000512f;	picsel[41] = 0.000256f;		picsel[42] = 0.00128f;		picsel[43] = 0.0064f;		picsel[44] = 0.032f;		picsel[45] = 0.16f;
				picsel[46] = 0.8f;			picsel[47] = 4.0f;			picsel[48] = 20.0f;			picsel[49] = 99.50f;
			}
		}
		else if(resln == 20) {
			if(JSP_ExpoDistributionBase == 2) {
				// april - 2012
				picsel[0] = 0.005f;			picsel[1] = 0.02f;			picsel[2] = 0.04f;			picsel[3] = 0.06f;			picsel[4] = 0.08f;			picsel[5] = 0.10f;
				picsel[6] = 0.12f;			picsel[7] = 0.14f;			picsel[8] = 0.16f;			picsel[9] = 0.18f;   		picsel[10] = 0.2f;			picsel[11] = 0.25f;
				picsel[12]= 0.30f;			picsel[13] = 0.40f;			picsel[14] = 0.50f;			picsel[15] = 0.60f;			picsel[16] = 0.70f;			picsel[17] = 0.80f;
				picsel[18] = 0.90f;			picsel[19] = 0.9950f;

				picsel[20] = 0.005f;		picsel[21] = 0.02f;			picsel[22] = 0.04f;			picsel[23] = 0.06f;			picsel[24] = 0.08f;			picsel[25] = 0.10f;
				picsel[26] = 0.12f;			picsel[27] = 0.14f;			picsel[28] = 0.16f;			picsel[29] = 0.18f;   		picsel[30] = 0.2f;			picsel[31] = 0.25f;
				picsel[32]= 0.30f;			picsel[33] = 0.40f;			picsel[34] = 0.50f;			picsel[35] = 0.60f;			picsel[36] = 0.70f;			picsel[37] = 0.80f;
				picsel[38] = 0.90f;			picsel[39] = 0.9950f;

				picsel[40] = 0.005f;		picsel[41] = 0.02f;			picsel[42] = 0.04f;			picsel[43] = 0.06f;			picsel[44] = 0.08f;			picsel[45] = 0.10f;
				picsel[46] = 0.12f;			picsel[47] = 0.14f;			picsel[48] = 0.16f;			picsel[49] = 0.18f;   		picsel[50] = 0.2f;			picsel[51] = 0.25f;
				picsel[52]= 0.30f;			picsel[53] = 0.40f;			picsel[54] = 0.50f;			picsel[55] = 0.60f;			picsel[56] = 0.70f;			picsel[57] = 0.80f;
				picsel[58] = 0.90f;			picsel[59] = 0.9950f;

				picsel[60] = 0.005f;		picsel[61] = 0.02f;			picsel[62] = 0.04f;			picsel[63] = 0.06f;			picsel[64] = 0.08f;			picsel[65] = 0.10f;
				picsel[66] = 0.12f;			picsel[67] = 0.14f;			picsel[68] = 0.16f;			picsel[69] = 0.18f;   		picsel[70] = 0.2f;			picsel[71] = 0.25f;
				picsel[72]= 0.30f;			picsel[73] = 0.40f;			picsel[74] = 0.50f;			picsel[75] = 0.60f;			picsel[76] = 0.70f;			picsel[77] = 0.80f;
				picsel[78] = 0.90f;			picsel[79] = 0.9950f;

				picsel[80] = 0.005f;		picsel[81] = 0.02f;			picsel[82] = 0.04f;			picsel[83] = 0.06f;			picsel[84] = 0.08f;			picsel[85] = 0.10f;
				picsel[86] = 0.12f;			picsel[87] = 0.14f;			picsel[88] = 0.16f;			picsel[89] = 0.18f;   		picsel[90] = 0.2f;			picsel[91] = 0.25f;
				picsel[92]= 0.30f;			picsel[93] = 0.40f;			picsel[94] = 0.50f;			picsel[95] = 0.60f;			picsel[96] = 0.70f;			picsel[97] = 0.80f;
				picsel[98] = 0.90f;			picsel[99] = 0.9950f;




			}
		}
		else if(resln == 30){
			picsel[0] = 0.005f;  				picsel[1] = 0.01f;			picsel[2] = 0.03f;			picsel[3] = 0.05f;			picsel[4] = 0.07f;  		picsel[5] = 0.10f;
			picsel[6] = 0.14f;				picsel[7] = 0.19f;			picsel[8] = 0.26f;			picsel[9] = 0.36f;			picsel[10] = 0.48f;	    picsel[11] = 0.65f;
			picsel[12] = 0.87f;				picsel[13] = 1.17f;			picsel[14] = 1.56f;			picsel[15] = 2.08f;			picsel[16] = 2.78f;		picsel[17] = 3.70f;
			picsel[18] = 4.93f;				picsel[19] = 6.57f;			picsel[20] = 8.74f;			picsel[21] = 11.64f;			picsel[22] = 15.49f;		picsel[23] = 20.61f;
			picsel[24] = 27.41f;				picsel[25] = 36.47f;			picsel[26] = 48.515f;			picsel[27] = 64.53f;			picsel[28] = 85.83f;		picsel[29] = 99.50f;

			picsel[30] = 0.005f;  			picsel[31] = 0.01f;			picsel[32] = 0.03f;			picsel[33] = 0.05f;			picsel[34] = 0.07f;  		picsel[35] = 0.10f;
			picsel[36] = 0.14f;				picsel[37] = 0.19f;			picsel[38] = 0.26f;			picsel[39] = 0.36f;			picsel[40] = 0.48f;	    picsel[41] = 0.65f;
			picsel[42] = 0.87f;				picsel[43] = 1.17f;			picsel[44] = 1.56f;			picsel[45] = 2.08f;			picsel[46] = 2.78f;		picsel[47] = 3.70f;
			picsel[48] = 4.93f;				picsel[49] = 6.57f;			picsel[50] = 8.74f;			picsel[51] = 11.64f;			picsel[52] = 15.49f;		picsel[53] = 20.61f;
			picsel[54] = 27.41f;				picsel[55] = 36.47f;			picsel[56] = 48.515f;			picsel[57] = 64.53f;			picsel[58] = 85.83f;		picsel[59] = 99.50f;

			picsel[60] = 0.005f;  			picsel[61] = 0.01f;			picsel[62] = 0.03f;			picsel[63] = 0.05f;			picsel[64] = 0.07f;  		picsel[65] = 0.10f;
			picsel[66] = 0.14f;				picsel[67] = 0.19f;			picsel[68] = 0.26f;			picsel[69] = 0.36f;			picsel[70] = 0.48f;		picsel[71] = 0.65f;
			picsel[72] = 0.87f;				picsel[73] = 1.17f;			picsel[74] = 1.56f;			picsel[75] = 2.08f;			picsel[76] = 2.78f;		picsel[77] = 3.70f;
			picsel[78] = 4.93f;				picsel[79] = 6.57f;			picsel[80] = 8.74f;			picsel[81] = 11.64f;			picsel[82] = 15.49f;		picsel[83] = 20.61f;
			picsel[84] = 27.41f;				picsel[85] = 36.47f;			picsel[86] = 48.515f;			picsel[87] = 64.53f;			picsel[88] = 85.83f;		picsel[89] = 99.50f;
		}
		else if (resln == 100) {
			picsel[0] = 0.00137f;				picsel[1] = 0.00435f;				picsel[2] = 0.00757f;				picsel[3] = 0.01106f;				picsel[4] = 0.01483f;				picsel[5] = 0.01893f;
			picsel[6] = 0.02336f;				picsel[7] = 0.02816f;				picsel[8] = 0.03335f;				picsel[9] = 0.03898f;				picsel[10] = 0.04508f;			picsel[11] = 0.05168f;
			picsel[12] = 0.05883f;			picsel[13] = 0.06657f;			picsel[14] = 0.07495f;			picsel[15] = 0.08404f;			picsel[16] = 0.09387f;			picsel[17] = 0.10452f;
			picsel[18] = 0.11606f;			picsel[19] = 0.12855f;			picsel[20] = 0.14208f;			picsel[21] = 0.15673f;			picsel[22] = 0.17260f;			picsel[23] = 0.18979f;
			picsel[24] = 0.20840f;			picsel[25] = 0.22856f;			picsel[26] = 0.25039f;			picsel[27] = 0.27403f;			picsel[28] = 0.29964f;			picsel[29] = 0.32737f;
			picsel[30] = 0.35740f;			picsel[31] = 0.38992f;			picsel[32] = 0.42514f;			picsel[33] = 0.46329f;			picsel[34] = 0.50461f;			picsel[35] = 0.54935f;
			picsel[36] = 0.59780f;			picsel[37] = 0.65028f;			picsel[38] = 0.70711f;			picsel[39] = 0.76867f;			picsel[40] = 0.83532f;			picsel[41] = 0.90752f;
			picsel[42] = 0.98570f;			picsel[43] = 1.07037f;			picsel[44] = 1.16208f;			picsel[45] = 1.26139f;			picsel[46] = 1.36894f;			picsel[47] = 1.48543f;
			picsel[48] = 1.61158f;			picsel[49] = 1.74820f;			picsel[50] = 1.89616f;			picsel[51] = 2.05640f;			picsel[52] = 2.22994f;			picsel[53] = 2.41788f;
			picsel[54] = 2.62143f;			picsel[55] = 2.84187f;			picsel[56] = 3.08060f;			picsel[57] = 3.33915f;			picsel[58] = 3.61916f;			picsel[59] = 3.92241f;
			picsel[60] = 4.25083f;			picsel[61] = 4.60651f;			picsel[62] = 4.99171f;			picsel[63] = 5.40889f;			picsel[64] = 5.86068f;			picsel[65] = 6.34998f;
			picsel[66] = 6.87989f;			picsel[67] = 7.45378f;			picsel[68] = 8.07530f;			picsel[69] = 8.74841f;			picsel[70] = 9.47739f;			picsel[71] = 10.26688f;
			picsel[72] = 11.12189f;			picsel[73] = 12.04787f;			picsel[74] = 13.05070f;			picsel[75] = 14.13677f;			picsel[76] = 15.31298f;			picsel[77] = 16.58681f;
			picsel[78] = 17.96638f;			picsel[79] = 19.46045f;			picsel[80] = 21.07853f;			picsel[81] = 22.83091f;			picsel[82] = 24.72873f;			picsel[83] = 26.78408f;
			picsel[84] = 29.01001f;			picsel[85] = 31.42071f;			picsel[86] = 34.03148f;			picsel[87] = 36.85896f;			picsel[88] = 39.92111f;			picsel[89] = 43.23742f;
			picsel[90] = 46.82899f;			picsel[91] = 50.71866f;			picsel[92] = 54.93116f;			picsel[93] = 59.49331f;			picsel[94] = 64.43412f;			picsel[95] = 69.78501f;
			picsel[96] = 75.58002f;			picsel[97] = 81.85603f;			picsel[98] = 88.65294f;			picsel[99] = 96.01399f;

			picsel[100] = 0.00137f;			picsel[101] = 0.00435f;			picsel[102] = 0.00757f;			picsel[103] = 0.01106f;			picsel[104] = 0.01483f;			picsel[105] = 0.01893f;
			picsel[106] = 0.02336f;			picsel[107] = 0.02816f;			picsel[108] = 0.03335f;			picsel[109] = 0.03898f;			picsel[110] = 0.04508f;			picsel[111] = 0.05168f;
			picsel[112] = 0.05883f;			picsel[113] = 0.06657f;			picsel[114] = 0.07495f;			picsel[115] = 0.08404f;			picsel[116] = 0.09387f;			picsel[117] = 0.10452f;
			picsel[118] = 0.11606f;			picsel[119] = 0.12855f;			picsel[120] = 0.14208f;			picsel[121] = 0.15673f;			picsel[122] = 0.17260f;			picsel[123] = 0.18979f;
			picsel[124] = 0.20840f;			picsel[125] = 0.22856f;			picsel[126] = 0.25039f;			picsel[127] = 0.27403f;			picsel[128] = 0.29964f;			picsel[129] = 0.32737f;
			picsel[130] = 0.35740f;			picsel[131] = 0.38992f;			picsel[132] = 0.42514f;			picsel[133] = 0.46329f;			picsel[134] = 0.50461f;			picsel[135] = 0.54935f;
			picsel[136] = 0.59780f;			picsel[137] = 0.65028f;			picsel[138] = 0.70711f;			picsel[139] = 0.76867f;			picsel[140] = 0.83532f;			picsel[141] = 0.90752f;
			picsel[142] = 0.98570f;			picsel[143] = 1.07037f;			picsel[144] = 1.16208f;			picsel[145] = 1.26139f;			picsel[146] = 1.36894f;			picsel[147] = 1.48543f;
			picsel[148] = 1.61158f;			picsel[149] = 1.74820f;			picsel[150] = 1.89616f;			picsel[151] = 2.05640f;			picsel[152] = 2.22994f;			picsel[153] = 2.41788f;
			picsel[154] = 2.62143f;			picsel[155] = 2.84187f;			picsel[156] = 3.08060f;			picsel[157] = 3.33915f;			picsel[158] = 3.61916f;			picsel[159] = 3.92241f;
			picsel[160] = 4.25083f;			picsel[161] = 4.60651f;			picsel[162] = 4.99171f;			picsel[163] = 5.40889f;			picsel[164] = 5.86068f;			picsel[165] = 6.34998f;
			picsel[166] = 6.87989f;			picsel[167] = 7.45378f;			picsel[168] = 8.07530f;			picsel[169] = 8.74841f;			picsel[170] = 9.47739f;			picsel[171] = 10.26688f;
			picsel[172] = 11.12189f;			picsel[173] = 12.04787f;			picsel[174] = 13.05070f;			picsel[175] = 14.13677f;			picsel[176] = 15.31298f;			picsel[177] = 16.58681f;
			picsel[178] = 17.96638f;			picsel[179] = 19.46045f;			picsel[180] = 21.07853f;			picsel[181] = 22.83091f;			picsel[182] = 24.72873f;			picsel[183] = 26.78408f;
			picsel[184] = 29.01001f;			picsel[185] = 31.42071f;			picsel[186] = 34.03148f;			picsel[187] = 36.85896f;			picsel[188] = 39.92111f;			picsel[189] = 43.23742f;
			picsel[190] = 46.82899f;			picsel[191] = 50.71866f;			picsel[192] = 54.93116f;			picsel[193] = 59.49331f;			picsel[194] = 64.43412f;			picsel[195] = 69.78501f;
			picsel[196] = 75.58002f;			picsel[197] = 81.85603f;			picsel[198] = 88.65294f;			picsel[199] = 96.01399f;

			picsel[200] = 0.00137f;			picsel[201] = 0.00435f;			picsel[202] = 0.00757f;			picsel[203] = 0.01106f;			picsel[204] = 0.01483f;			picsel[205] = 0.01893f;
			picsel[206] = 0.02336f;			picsel[207] = 0.02816f;			picsel[208] = 0.03335f;			picsel[209] = 0.03898f;			picsel[210] = 0.04508f;			picsel[211] = 0.05168f;
			picsel[212] = 0.05883f;			picsel[213] = 0.06657f;			picsel[214] = 0.07495f;			picsel[215] = 0.08404f;			picsel[216] = 0.09387f;			picsel[217] = 0.10452f;
			picsel[218] = 0.11606f;			picsel[219] = 0.12855f;			picsel[220] = 0.14208f;			picsel[221] = 0.15673f;			picsel[222] = 0.17260f;			picsel[223] = 0.18979f;
			picsel[224] = 0.20840f;			picsel[225] = 0.22856f;			picsel[226] = 0.25039f;			picsel[227] = 0.27403f;			picsel[228] = 0.29964f;			picsel[229] = 0.32737f;
			picsel[230] = 0.35740f;			picsel[231] = 0.38992f;			picsel[232] = 0.42514f;			picsel[233] = 0.46329f;			picsel[234] = 0.50461f;			picsel[235] = 0.54935f;
			picsel[236] = 0.59780f;			picsel[237] = 0.65028f;			picsel[238] = 0.70711f;			picsel[239] = 0.76867f;			picsel[240] = 0.83532f;			picsel[241] = 0.90752f;
			picsel[242] = 0.98570f;			picsel[243] = 1.07037f;			picsel[244] = 1.16208f;			picsel[245] = 1.26139f;			picsel[246] = 1.36894f;			picsel[247] = 1.48543f;
			picsel[248] = 1.61158f;			picsel[249] = 1.74820f;			picsel[250] = 1.89616f;			picsel[251] = 2.05640f;			picsel[252] = 2.22994f;			picsel[253] = 2.41788f;
			picsel[254] = 2.62143f;			picsel[255] = 2.84187f;			picsel[256] = 3.08060f;			picsel[257] = 3.33915f;			picsel[258] = 3.61916f;			picsel[259] = 3.92241f;
			picsel[260] = 4.25083f;			picsel[261] = 4.60651f;			picsel[262] = 4.99171f;			picsel[263] = 5.40889f;			picsel[264] = 5.86068f;			picsel[265] = 6.34998f;
			picsel[266] = 6.87989f;			picsel[267] = 7.45378f;			picsel[268] = 8.07530f;			picsel[269] = 8.74841f;			picsel[270] = 9.47739f;			picsel[271] = 10.26688f;
			picsel[272] = 11.12189f;			picsel[273] = 12.04787f;			picsel[274] = 13.05070f;			picsel[275] = 14.13677f;			picsel[276] = 15.31298f;			picsel[277] = 16.58681f;
			picsel[278] = 17.96638f;			picsel[279] = 19.46045f;			picsel[280] = 21.07853f;			picsel[281] = 22.83091f;			picsel[282] = 24.72873f;			picsel[283] = 26.78408f;
			picsel[284] = 29.01001f;			picsel[285] = 31.42071f;			picsel[286] = 34.03148f;			picsel[287] = 36.85896f;			picsel[288] = 39.92111f;			picsel[289] = 43.23742f;
			picsel[290] = 46.82899f;			picsel[291] = 50.71866f;			picsel[292] = 54.93116f;			picsel[293] = 59.49331f;			picsel[294] = 64.43412f;			picsel[295] = 69.78501f;
			picsel[296] = 75.58002f;			picsel[297] = 81.85603f;			picsel[298] = 88.65294f;			picsel[299] = 96.01399f;

		}

		/*
		 * if(gdp.getDimension()==1) dim = 1; else if(data.length > r[0] * r[1])
		 * //full packet { dim = dim; //set to actual number of dimensions }
		 * else //slice { if(getDimension()==1) dim = 1; else dim = 2; //if
		 * actual dimension >= 2 }
		 */

		float[] selvals;
		selvals = picsel;
		boolean scaleupflag = false;

		if (UNIFORM_DISTRIBUTION == 1) {
			for (int i = 0; i < data.length; i++) {
				planCount[data[i].getPlanNumber()]++;
				locationWeight[i] = 1;
			}
		}
		else if(JSP_ExpoDistributionBase == 2) {                 // april - 2012

			double locationWeightLocal[] = new double[resln];

			if(resln == 10) {
				//				locationWeightLocal[0] = 1;			locationWeightLocal[1] = 1;				locationWeightLocal[2] = 2;
				//				locationWeightLocal[3] = 4;         locationWeightLocal[4] = 5;				locationWeightLocal[5] = 7;
				//				locationWeightLocal[6] = 20;        locationWeightLocal[7] = 20;			locationWeightLocal[8] = 20;
				//				locationWeightLocal[9] = 20;

				locationWeightLocal[0] = 1;			locationWeightLocal[1] = 1;				locationWeightLocal[2] = 1;
				locationWeightLocal[3] = 2;         locationWeightLocal[4] = 4;				locationWeightLocal[5] = 6;
				locationWeightLocal[6] = 7;        locationWeightLocal[7] = 30;				locationWeightLocal[8] = 30;
				locationWeightLocal[9] = 20;


			}
			else if(resln == 20){
				locationWeightLocal[0] = 1;			locationWeightLocal[1] = 1;				locationWeightLocal[2] = 1;
				locationWeightLocal[3] = 1;			locationWeightLocal[4] = 1;				locationWeightLocal[5] = 1;
				locationWeightLocal[6] = 1;			locationWeightLocal[7] = 1;				locationWeightLocal[8] = 1;
				locationWeightLocal[9] = 1;

				locationWeightLocal[10] = 2;		locationWeightLocal[11] = 2;			locationWeightLocal[12] = 5;
				locationWeightLocal[13] = 5;		locationWeightLocal[14] = 5;			locationWeightLocal[15] = 5;
				locationWeightLocal[16] = 5;		locationWeightLocal[17] = 5;			locationWeightLocal[18] = 5;
				locationWeightLocal[19] = 2.5;
			}
			else if(resln == 30){
				locationWeightLocal[0] = 1;			locationWeightLocal[1] = 1;				locationWeightLocal[2] = 1.6;
				locationWeightLocal[3] = 2;			locationWeightLocal[4] = 4;				locationWeightLocal[5] = 4;
				locationWeightLocal[6] = 4;			locationWeightLocal[7] = 12;			locationWeightLocal[8] = 20;
				locationWeightLocal[9] = 20;

				locationWeightLocal[10] = 20;		locationWeightLocal[11] = 20;			locationWeightLocal[12] = 20;
				locationWeightLocal[13] = 20;		locationWeightLocal[14] = 20;			locationWeightLocal[15] = 20;
				locationWeightLocal[16] = 30;		locationWeightLocal[17] = 40;			locationWeightLocal[18] = 40;
				locationWeightLocal[19] = 40;

				locationWeightLocal[20] = 40;		locationWeightLocal[21] = 70;			locationWeightLocal[22] = 100;
				locationWeightLocal[23] = 150;		locationWeightLocal[24] = 200;			locationWeightLocal[25] = 200;
				locationWeightLocal[26] = 200;		locationWeightLocal[27] = 200;			locationWeightLocal[28] = 200;
				locationWeightLocal[29] = 100;
			}



			for (int loc=0; loc < data.length; loc++)
			{
				double weight = 1.0;
				int tempLoc = loc;
				for(int d=0;d<dim;d++){
					weight *= locationWeightLocal[tempLoc % resln];
					tempLoc = tempLoc/10;
				}

				locationWeight[loc] = (float) weight;
				planCount[data[loc].getPlanNumber()] += weight;
			}
		}


		else // Exponential
		{
			/*
			 * for(int i = 0; i < dim; i++) { if(getQueryPacket().getEndPoint(i)
			 * - getQueryPacket().getStartPoint(i) < 0.05) scaleupflag = true; }
			 */
			int idx[] = new int[dim]; // will be set to all 0's.

			if (PicassoConstants.a[0] == -1 || PicassoConstants.a[1] == -1	|| PicassoConstants.a[2] == -1 || PicassoConstants.a[3] == -1 || PicassoConstants.a[4] == -1)
				// this is necessary because after viewing a 1D diagram, one of the
				// PicassConstats.a elements is -1. This is to remove this.
			{
				PicassoConstants.a[0] = 0;
				PicassoConstants.a[1] = 1;
				PicassoConstants.a[2] = 2;
				PicassoConstants.a[3] = 3;
				PicassoConstants.a[4] = 4;
			}
			for (int i = 0; i < data.length; i++) {
				double fullval = 1.0;
				double curval = 0.0;
				int we;

				int[] ressum = new int[dim];
				for (int p = 1; p < dim; p++)
					ressum[p] += ressum[p - 1] + r[p - 1];
				// find the area represented by this point by multiplying its
				// length in each dimension
				if (dim == 1) {
					if (i != 0 && i != r[0] - 1) {
						curval = (selvals[i + 1] - selvals[i - 1]) / 2;
					} else if (i == 0) {
						curval = selvals[0] + (selvals[1] - selvals[0]) / 2;
					} else // if(k==getResolution()-1)
					{
						curval = EndPoint[0] * 100 - selvals[r[0] - 1] + (selvals[r[0] - 1] - selvals[r[0] - 2]) / 2;
					}
					fullval = curval;
				} else if (dim == 2 || dim == 3 || dim == 4 || dim == 5) {
					for (we = 0; we < dim; we++) {
						if (idx[we] != 0 && idx[we] != r[PicassoConstants.a[we]] - 1) // for in between first and last
						{
							//							 System.out.print("\n(");
							//							 System.out.print(selvals[ressum[PicassoConstants.a[we]]+ idx[we]+1]);
							//							 System.out.print("\t-\t");
							//							 System.out.print(selvals[ressum[PicassoConstants.a[we]]+ idx[we]-1]);
							//							 System.out.print(")/2");
							curval = (selvals[ressum[PicassoConstants.a[we]] + idx[we] + 1] - selvals[ressum[PicassoConstants.a[we]] + idx[we] - 1]) / 2;
						} else if (idx[we] == 0) // for first in row
						{
							//							 System.out.print("\n(");
							//							 System.out.print(selvals[ressum[PicassoConstants.a[we]]+ 1 ]);
							//							 System.out.print("\t-\t");
							//							 System.out.print(selvals[ressum[PicassoConstants.a[we]]]);
							//							 System.out.print(")/2");
							curval = /* getQueryPacket().getStartPoint(we)*100+ */(selvals[ressum[PicassoConstants.a[we]] + 1] - selvals[ressum[PicassoConstants.a[we]]]) / 2;
						} else // if(idx[we]==getResolution()-1) // for last in row
						{
							//							 System.out.print("\n(");
							//							 System.out.print(EndPoint[PicassoConstants.a[we]]*100);
							//							 System.out.print("\t-\t");
							//							 System.out.print(selvals[ressum[PicassoConstants.a[we]]+r[PicassoConstants.a[we]]-1]);
							//							 System.out.print(")");
							//							 System.out.print("\t+\t");
							//							 System.out.print("(");
							//							 System.out.print(selvals[ressum[PicassoConstants.a[we]]+r[PicassoConstants.a[we]]-1]);
							//							 System.out.print("\t-\t");
							//							 System.out.print(selvals[ressum[PicassoConstants.a[we]]+
							//							 r[PicassoConstants.a[we]]-2]);
							//							 System.out.print(")/2");
							curval = EndPoint[PicassoConstants.a[we]] * 100 - selvals[ressum[PicassoConstants.a[we]]+ r[PicassoConstants.a[we]] - 1] + (selvals[ressum[PicassoConstants.a[we]]
									+ r[PicassoConstants.a[we]] - 1] - selvals[ressum[PicassoConstants.a[we]] + r[PicassoConstants.a[we]] - 2]) / 2;
						}
						if (!scaleupflag)
							fullval *= curval;
						else
							fullval *= (curval * 10);
					}

					for (int p = 0; p < dim; p++) {
						idx[p]++;
						if (idx[p] == r[PicassoConstants.a[p]]) {
							idx[p] = 0;
						} else
							break;
					}
				} else {
					// start ma
					// modifying selvals as required

					int dim1 = PicassoConstants.a[0];
					int dim2 = PicassoConstants.a[1];
					int res1 = r[dim1];
					int res2 = r[dim2];

					int res[] = new int[dim];
					double startpt[] = new double[dim];
					double endpt[] = new double[dim];
					for (int k = 0; k < r.length; k++) {
						res[k] = r[k];
						startpt[k] = StartPoint[k];
						endpt[k] = EndPoint[k];
					}

					// swapping resolution and startpoint, endpoint locally for use in the area calculation
					int t = res[dim1];
					res[dim1] = res[dim2];
					res[dim2] = t;

					double x = startpt[dim1];
					startpt[dim1] = startpt[dim2];
					startpt[dim2] = x;

					x = endpt[dim1];
					endpt[dim1] = endpt[dim2];
					endpt[dim2] = x;

					float[] tvals = picsel;
					float temp1[] = new float[res1];
					float temp2[] = new float[res2];
					int index = 0;

					for (int k = 0; k < res1; k++) {
						temp1[k] = selvals[ressum[dim1] + k];
					}
					for (int k = 0; k < res2; k++) {
						temp2[k] = selvals[ressum[dim2] + k];
					}

					for (int k = 0; k < dim; k++) {
						if (inA(k)) {
							if (dim1 == k) {
								for (int j = 0; j < temp1.length; j++)
									tvals[index++] = temp1[j];
							} else if (dim2 == k) {
								for (int j = 0; j < temp2.length; j++)
									tvals[index++] = temp2[j];
							}
						} else {
							for (int j = 0; j < r[k]; j++)
								tvals[index++] = picsel[ressum[k] + j];
						}
					}
					selvals = tvals;

					for (we = 0; we < dim; we++) {
						if (idx[we] != 0 && idx[we] != res[we] - 1) {
							curval = (selvals[idx[we] + 1] - selvals[idx[we] - 1]) / 2;
						} else if (idx[we] == 0) {
							curval = startpt[we] * 100
									+ (selvals[1] - selvals[0]) / 2;
						} else // if(idx[we]==getResolution()-1)
						{
							curval = endpt[we]
									* 100
									- selvals[res[we] - 1]
											+ (selvals[res[we] - 1] - selvals[res[we] - 2])
											/ 2;
						}

						if (scaleupflag)
							fullval *= (curval * 10);
						else
							fullval *= curval;
					}

					for (int p = 0; p < dim; p++) {
						idx[p]++;
						if (idx[p] == res[p]) {
							idx[p] = 0;
						} else
							break;
					}
				}
				// System.out.println("Weight given for " + i + "(" + (idx[0]) + ", " + idx[1] + ")"+ " : " + twoDForm.format(1.0 * fullval));
				planCount[data[i].getPlanNumber()] += (fullval * 100);

				locationWeight[i] = (float) (fullval * 100);
			} // end of for loop (through all points)
		} // end of else (exponential) part

		for (int i = 0; i < data.length; i++) {
			if (planCount[data[i].getPlanNumber()] == 0)
				planCount[data[i].getPlanNumber()] = 1;
		}

		/*
		 * if(scaleupflag) { for(int i = 0; i < planCount.length; i++)
		 * planCount[i] /= 100Math.pow(10, getDimension()); }
		 */


		checkValidityofWeights();

	}


	// functions for location weight calculations and the checking for validity

	//////////////////////////////   LOCATION WEIGHT CALCULATIONS and PCM & POSP VALIDITY CHECKING CODE  //////////////////////////////////////////////////////////////

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}


