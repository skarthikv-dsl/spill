import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;

import java.io.*;

public class xmlParser {

	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException
	   {
	      //Get Document Builder
		  String path = "/home/dsladmin/Srinivas/data/PG_QT2_100_EXP/";
	      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	      DocumentBuilder builder = factory.newDocumentBuilder();
	       
	      //Build Document
	      Document document = (Document)builder.parse(new File(path+"8.xml"));
	       
	      //Normalize the XML Structure; It's just too important !!
	      document.getDocumentElement().normalize();
	       
	      //Here comes the root node
	      Element root = document.getDocumentElement();
	      System.out.println(root.getNodeName());
	       
	      //Get all employees
	      NodeList nList = document.getElementsByTagName("Plan");
	      System.out.println("============================");
	       
	      for (int temp = 0; temp < nList.getLength(); temp++)
	      {
	       Node node = nList.item(temp);
	       System.out.println("");    //Just a separator
	       /*if (node.getNodeType() == Node.ELEMENT_NODE)
	       {
	          //Print each employee's detail
	          Element eElement = (Element) node;
	          System.out.println("Employee id : "    + eElement.getAttribute("id"));
	          System.out.println("First Name : "  + eElement.getElementsByTagName("firstName").item(0).getTextContent());
	          System.out.println("Last Name : "   + eElement.getElementsByTagName("lastName").item(0).getTextContent());
	          System.out.println("Location : "    + eElement.getElementsByTagName("location").item(0).getTextContent());
	       }*/
	       Element eElement = (Element) node;
	       //System.out.println("Hash Join: "  + eElement.item(0).getTextContent());
	      }
          /*NamedNodeMap nodeMap = root.getAttributes();
          for (int i = 0; i < nodeMap.getLength(); i++)
          {
              Node tempNode = nodeMap.item(i);
              System.out.println("Attr name : " + tempNode.getNodeName()+ "; Value = " + tempNode.getNodeValue());
          }*/
	      //BinaryTree tree = new BinaryTree(root.getAttribute("Hash-Cond"),null,null);
	      
	      visitChildNodes(nList);
	   }
	 
	   //This function is called recursively
	   private static void visitChildNodes(NodeList nList)
	   {
		   if(nList.getLength()>2){
			   System.out.println("BIG PROBLEM "+nList.getLength());
			   
		   }
		   //Node node1 = nList.item(1).getFirstChild().getFirstChild();
		   //System.out.println("Node1 Name = " + node1.getNodeName() + "; Value1 = " + node1.getTextContent());
		   
		   //System.out.println("BIGGEST PROBLEM "+nList.getLength());
	      for (int temp = 0; temp < nList.getLength(); temp++)
	      {

	         Node node = nList.item(temp);
	         //if (node.getNodeType() == Node.ELEMENT_NODE)
	         //{
	            System.out.println("Node Name = " + node.getNodeName() + "; Value = " + node.getTextContent());
	            //Check all attributes
	            if (node.hasAttributes()) {
	               // get attributes names and values
	               NamedNodeMap nodeMap = node.getAttributes();
	               for (int i = 0; i < nodeMap.getLength(); i++)
	               {
	                   Node tempNode = nodeMap.item(i);
	                   System.out.println("Attr name : " + tempNode.getNodeName()+ "; Value = " + tempNode.getNodeValue());
	               }
	               if (node.hasChildNodes()) {
	                  //We got more childs; Let's visit them as well
	                  //visitChildNodes(node.getChildNodes());
	               }
	           }
	         //}
	      }
	   }
}
