/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.nucleusweb.server.configuration;

import biz.szydlowski.nucleusweb.server.CommandApi;
import static biz.szydlowski.nucleusweb.server.NucleusServer.commandApi;
import biz.szydlowski.utils.OSValidator;
import biz.szydlowski.utils.template.TemplateFile;
import java.io.File;
import java.io.IOException;
import java.util.StringJoiner;
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
public class ReadCommands {
   
    
   private String _setting="setting/commands.xml";
   
   static final Logger logger = LogManager.getLogger(ReadCommands.class);

       
     /** Konstruktor pobierający parametry z pliku konfiguracyjnego "config.xml"
     */
     public  ReadCommands(){
         
         if (OSValidator.isUnix()){
              String absolutePath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
                   absolutePath = absolutePath.substring(0, absolutePath.lastIndexOf("/"));
              _setting = absolutePath + "/" + _setting;
         }
         
         readSetting( _setting);
         TemplateFile _Template = new TemplateFile("default");
         
         for (String file : _Template.getFilenames()){
             readSetting(file);
          }
         
     }
         
     private void readSetting(String filename){
             try {
                        
		File fXmlFile = new File(filename);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(fXmlFile);
		doc.getDocumentElement().normalize();
              
                NodeList  nList = doc.getElementsByTagName("cmd"); 
                
		for (int temp = 0; temp < nList.getLength(); temp++) {
                
		   Node nNode = nList.item(temp);
		   if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                          Element eElement = (Element) nNode;
                                                
                          CommandApi _commandApi = new CommandApi();
                          
                          _commandApi.setAlias(getTagValue("alias", eElement));
                          StringJoiner cmd= new StringJoiner("#NEXTCOMMAND#");
                                                    
                           for (int count = 0; count < eElement.getElementsByTagName("command").getLength(); count++) {
                               cmd.add(eElement.getElementsByTagName(("command")).item(count).getTextContent());
                           }
                     
                          _commandApi.setCommand(cmd.toString());
                          _commandApi.setModule(getTagValue("module", eElement));
                          _commandApi.setGroup(getTagValue("group", eElement));  
                          _commandApi.setHost(getTagValue("host", eElement));  
                          _commandApi.setDescription(getTagValue("description", eElement));
                     
                          _commandApi.setLock(false);
                          
                          commandApi.add(_commandApi);
		   }
		}
                
                logger.debug("Read commandApi done ");
                                
         }  catch (ParserConfigurationException | SAXException | IOException e) {         
                logger.fatal("commands.xml::XML Exception/Error:", e);
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
