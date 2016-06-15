/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */ 

/**
 * This example, like all Swing examples, exists in a package:
 * in this case, the "start" package.
 * If you are using an IDE, such as NetBeans, this should work 
 * seamlessly.  If you are compiling and running the examples
 * from the command-line, this may be confusing if you aren't
 * used to using named packages.  In most cases,
 * the quick and dirty solution is to delete or comment out
 * the "package" line from all the source files and the code
 * should work as expected.  For an explanation of how to
 * use the Swing examples as-is from the command line, see
 * http://docs.oracle.com/javase/javatutorials/tutorial/uiswing/start/compile.html#package
 */


/*
 * HelloWorldSwing.java requires no other files. 
 */
import iisc.dsl.picasso.server.ADiagramPacket;

import javax.swing.*;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
//import iisc.dsl.picasso.client.util.Draw2DDiagram;
public class JavaSwing extends JPanel {
	GCI3D obj;
    public static final Color RED = new Color(255,0,0);
    public static final Color YELLOW = new Color(255,255,0);
    public static final Color BLUE = new Color(0,0,255);
    
    public static final HashMap<Integer, Integer> missedPredicates = new HashMap<Integer, Integer>();
    static {
        //missedPredicates.put(1, 10);
        missedPredicates.put(2, 10);
    }

    
    public static final HashMap<Integer, Color> Colors = new HashMap<Integer, Color>();
    static {    
    	Colors.put(0, RED);
        Colors.put(1, YELLOW);
        Colors.put(2, BLUE);
    }

    public static final int PREFERRED_GRID_SIZE_PIXELS = 1;
    private Color[][] spillDiagram;

    public JavaSwing() throws IOException{
    	obj = new GCI3D();

    	loadGCI3DObject();
				
		loadSpillDiagram();
        
        int preferredWidth = obj.resolution * PREFERRED_GRID_SIZE_PIXELS;
        int preferredHeight = obj.resolution * PREFERRED_GRID_SIZE_PIXELS;
        this.spillDiagram[0][0] = Colors.get(2);
        setPreferredSize(new Dimension(preferredWidth, preferredHeight));
    }

    private void loadSpillDiagram() throws NumberFormatException, IOException {

    	boolean flag = false;
    	int X,Y;
    	this.spillDiagram = new Color[obj.resolution][obj.resolution];

    	for(int i=0; i<obj.data.length; i++){
    		int arr[] = obj.getCoordinates(obj.dimension, obj.resolution, i);
    		flag = true; 
    		X= -1;Y = -1;
    		for(int j=0; j<obj.dimension; j++){
    			if(missedPredicates.containsKey(j) && arr[j] != missedPredicates.get(j)){
    				flag = false;
    				break;
    			}
    			else if(!missedPredicates.containsKey(j)){
    				if(X==-1)
    					X = arr[j];
    				else
    					Y = arr[j];
    			}
    		}
    		if(flag){
    			int planno = obj.getPlanNumber_generic(arr);
    			try{
    				String plansPath = obj.apktPath+"predicateOrder/";
    				FileReader file = new FileReader(plansPath+planno+".txt");

    				BufferedReader br = new BufferedReader(file);
    				String s;
    				while((s = br.readLine()) != null) {
    					//System.out.println(Integer.parseInt(s));
    					int value = Integer.parseInt(s);
    					if(!missedPredicates.containsKey(value)){
    						this.spillDiagram[X][Y] = Colors.get(value);
    						break;
    					}
    				}
    				br.close();
    				file.close();
    			}
    			catch(FileNotFoundException e){
    				e.printStackTrace();
    			}	
    		}

    	}
    }

	private void loadGCI3DObject() throws IOException {
		
    	obj.loadPropertiesFile();
		String pktPath = obj.apktPath + obj.qtName + ".apkt" ;
		System.out.println("Query Template: "+obj.qtName);

		ADiagramPacket gdp = obj.getGDP(new File(pktPath));

		//Populate the OptimalCost Matrix.
		obj.readpkt(gdp, false);

		//Populate the selectivity Matrix.
		obj.loadSelectivity();

		
	}

	@Override
    public void paintComponent(Graphics g) {
        // Important to call super class method
        super.paintComponent(g);
        // Clear the board
        
        g.clearRect(0, 0, getWidth(), getHeight());
        Graphics2D g2d = (Graphics2D) g;
        g2d.translate(0, getHeight());
        g2d.scale(1.0, -1.0);

        g2d.drawLine(20, 20, 300, 200);
        // Draw the grid
        int rectWidth = getWidth() / obj.resolution;
        int rectHeight = getHeight() / obj.resolution;

        for (int i = 0; i < obj.resolution; i++) {
            for (int j = 0; j < obj.resolution; j++) {
                // Upper left corner of this terrain rect
                int x = i * rectWidth;
                int y = j * rectHeight;
                Color terrainColor = spillDiagram[i][j];
                g2d.setColor(terrainColor);
                g2d.fillRect(x, y, rectWidth, rectHeight);
            }
        }
    }

    public static void main(String[] args) {
        // http://docs.oracle.com/javase/tutorial/uiswing/concurrency/initial.html
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new JFrame("Game");
                JavaSwing map = null;
				try {
					map = new JavaSwing();
				} catch (IOException e) {
					
					e.printStackTrace();
				}
                frame.add(map);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.pack();
                frame.setVisible(true);
            }
        });
    }
}