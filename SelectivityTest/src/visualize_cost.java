
void export_cost(String bouquetPath) {
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
  
             File file = new File("E:\\SQLServerSetup\\data\\temp\\optCost.txt");  
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

 