/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.nucleusweb.server.configuration;

import biz.szydlowski.nucleusweb.server.PrepareCmdApi;
import static biz.szydlowski.nucleusweb.server.NucleusServer.prepareCmdApi;
import biz.szydlowski.utils.OSValidator;
import java.io.File;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Dominik
 */
public class ReadPrepareCmd {
   
    
   private String _setting="setting/prepareCmd.xml";
   
   static final Logger logger = LogManager.getLogger(ReadPrepareCmd.class);

       
     /** Konstruktor pobierający parametry z pliku konfiguracyjnego "config.xml"
     */
     public  ReadPrepareCmd(){
         
         if (OSValidator.isUnix()){
              String absolutePath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
                   absolutePath = absolutePath.substring(0, absolutePath.lastIndexOf("/"));
              _setting = absolutePath + "/" + _setting;
         }
           
          
         try {
                        
		File fXmlFile = new File(_setting);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(fXmlFile);
		doc.getDocumentElement().normalize();
              
                NodeList  nList = doc.getElementsByTagName("cmd"); 
                
		for (int temp = 0; temp < nList.getLength(); temp++) {
                
		   Node nNode = nList.item(temp);
		   if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                          Element eElement = (Element) nNode;
                                                
                          PrepareCmdApi _prepareCmdApi = new PrepareCmdApi();
                          
                          _prepareCmdApi.setAlias(getTagValue("alias", eElement));
                          _prepareCmdApi.setCommand(getTagValue("command", eElement));
                          _prepareCmdApi.setDescription(getTagValue("description", eElement));
                          
                          prepareCmdApi.add(_prepareCmdApi);
		   }
		}
                
                logger.debug("Read _prepareCmdApi done");
                                
         }  catch (ParserConfigurationException | SAXException | IOException e) {         
                logger.fatal("prepareCmd.xml::XML Exception/Error:", e);
                System.exit(-1);
				
	 }
    }
    

  
      private static String getTagValue(String sTag, Element eElement) {
            try {
                NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
                Node nValue = (Node) nlList.item(0);
                return nValue.getNodeValue();
            } catch (Exception e){
                logger.error("getTagValue error " + sTag + " "+ e);
                return "ERROR";
            }

      }

}
