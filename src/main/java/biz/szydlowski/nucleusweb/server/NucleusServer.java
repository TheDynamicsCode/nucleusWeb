/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.nucleusweb.server;

import biz.szydlowski.nucleusweb.server.configuration.ReadCommands;
import biz.szydlowski.nucleusweb.server.configuration.ReadPrepareCmd;
import biz.szydlowski.nucleusweb.server.configuration.WebParams;
import biz.szydlowski.utils.OSValidator;
import biz.szydlowski.utils.api.RestartApi;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.security.CodeSource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/**
 *
 * @author szydlowskidom
 */
public class NucleusServer implements Daemon { 
    
    static {
        try {
            System.setProperty("log4j.configurationFile", getJarContainingFolder(NucleusServer.class)+"/setting/log4j2.xml");
        } catch (Exception ex) {
        }
    }
    public static long startTime;  
    public static List<String> allowedConn = new ArrayList<>();
    public static List<String> allowedExc = new ArrayList<>();
    public static List<String> allowedWebhook = new ArrayList<>();
    
    public static List<String> apiKeys = new ArrayList<>();
    public static List<CommandApi> commandApi = new ArrayList<>();
    public static List<PrepareCmdApi> prepareCmdApi = new ArrayList<>();
    public static List<String> modules = new ArrayList<>();
    public static RestartApi restartApi=null;
    
    private static boolean stop = false;
    static final Logger logger = LogManager.getLogger(NucleusServer.class);
    public static SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH-mm-ss"); 
    public static String absolutePath ="";
 
    private Timer Maintenance = null;
    public static HashMap<UUID, String> hmap_cmd = new HashMap<UUID, String>();
    public static HashMap<UUID, Long> hmap_time = new HashMap<UUID, Long>();
    
    private NucleusWebServer WebServer = null;
    private NucleusRemoteControlServer RemoteControlServer = null;
    public static int webport=8080;
    private int remoteport=8081;
  
    public NucleusServer (){		
    } 
    
     public NucleusServer (boolean test, boolean win){
          if (test || win){
            if (!win) System.out.println("****** TESTING MODE  ********"); 
            else System.out.println("****** WINDOWS MODE  ********"); 
            try {
               initialize();
               start();       
             } catch (Exception ex) {
                logger.error(ex);
            }
        }
     }
    
        
            
     public static void main(String[] args) {
       
         if (args.length>0){
             if (args[0].equalsIgnoreCase("testing")){
                 NucleusServer  jobber  = new NucleusServer (true, false);
             }

         }
         
		
     }
   
     
     public void initialize() {
      
            if (OSValidator.isUnix()){
                 absolutePath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
                       absolutePath = "/" + absolutePath.substring(0, absolutePath.lastIndexOf("/"))+"/";
            }
             System.setProperty("log4j.configurationFile", absolutePath+"setting/log4j2.xml");
             startTime=System.currentTimeMillis();
            
            Maintenance = new Timer("Maintenance", true);
            Maintenance.schedule(new MaintenanceTask(), 60000, 30000);
            
            logger.info(new Version().getAllInfo()); 
            
            ReadCommands readCommands = new ReadCommands();
            ReadPrepareCmd _readPrepareCmd = new ReadPrepareCmd();
            
            for (int i=0; i<commandApi.size(); i++){
                boolean exist=false;
                for (int j=0; j<modules.size(); j++){
                    if (modules.get(j).equals(commandApi.get(i).getModule())) exist=true;
                }
                if (!exist) modules.add(commandApi.get(i).getModule());
            }     
           
                  
            WebParams _WebParams = new WebParams();
            webport = _WebParams.getWebConsolePort();
            remoteport = _WebParams.getRemoteControlPort();
            if (webport==remoteport) {
                remoteport=webport+1;
            }
            
            restartApi = new RestartApi(_WebParams.getRestartScriptPath());
            allowedConn = _WebParams.getAllowedConn();
            apiKeys = _WebParams.getApiKeys();
            WebServer = new NucleusWebServer(webport);
            RemoteControlServer = new NucleusRemoteControlServer(remoteport);
     }


    /**
     *
     * @param dc
     * @throws DaemonInitException
     * @throws Exception
     */
    @Override
    public void init(DaemonContext dc) throws DaemonInitException, Exception {
          //String[] args = dc.getArguments();
          initialize();
         
    }

  
    @Override
    public void start() throws Exception {
          logger.info("Starting server");
          WebServer.start();  
          RemoteControlServer.start();
          readProps(webport);
          logger.info("Started server");
    }

   
    @Override
    public void stop() throws Exception {
        logger.info("Stopping daemon");
       
        Properties properties = new Properties();
        for (Map.Entry<UUID, String> entry : hmap_cmd.entrySet()) {
            properties.put(entry.getKey().toString(), entry.getValue());
        }
        saveProps(properties);
        properties.clear();
        
        WebServer.stopSever();   
        RemoteControlServer.stopSever();
        Maintenance.cancel();
             
        logger.info("Stopped daemon");
    }
    
    //for windows
    public static void start(String[] args) {
        System.out.println("start");
        NucleusServer jobber = new NucleusServer(false, true);
              
        while (!stop) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
    }
  
    public static void stop(String[] args) {
        System.out.println("stop");
        stop = true;
       
        logger.info("Stoppping daemon");
     
        Properties properties = new Properties();
        for (Map.Entry<UUID, String> entry : hmap_cmd.entrySet()) {
            properties.put(entry.getKey().toString(), entry.getValue());
        }
        saveProps(properties);
        properties.clear();
           
        int active = Thread.activeCount();
        Thread all[] = new Thread[active];
        Thread.enumerate(all);

        for (int i = 0; i < active; i++) {
            if (!all[i].getName().contains("RESTART")){
               logger.info("Thread to interrupt " + i + ": " + all[i].getName() + " " + all[i].getState());
               all[i].interrupt();
            } else {
                logger.info("Thread alive " + i + ": " + all[i].getName() + " " + all[i].getState());
            }
        }
    
        logger.info("Stopped daemon");  
        
        System.exit(0);
                
    }
 

   
    @Override
    public void destroy() { 
        logger.info("Destroy daemon");
        
        Maintenance = null;
     
        logger.info("*********** Destroyed daemon  ****************");
    }
      
   
          
    public class MaintenanceTask extends TimerTask {
           
            int tick=0;
            int mb = 1024 * 1024; 
            Runtime runtime = Runtime.getRuntime();
                 
            @Override
            public void run() {
                        
                     
                   long maxMemory = runtime.maxMemory();
                   long allocatedMemory = runtime.totalMemory();
                   long freeMemory = runtime.freeMemory();
                   long usedMem = allocatedMemory - freeMemory;
                   long totalMem = runtime.totalMemory();
                   
                    if (tick==50){
                        logger.info("***** Heap utilization statistics [MB] *****");
                        // available memory
                        logger.info("Total Memory: " + totalMem / mb);
                        // free memory
                        logger.info("Free Memory: " + freeMemory / mb);
                        // used memory
                        logger.info("Used Memory: " + usedMem / mb);
                        // Maximum available memory
                        logger.info("Max Memory: " + maxMemory / mb);
                       
                        tick=0;
                        
                        System.gc();
                    }
  
                 
            }
    }
    
    
    private static void saveProps(Properties prop){
	OutputStream output = null;

	try {

	       String file = "jobs.data";
           
               if (OSValidator.isUnix()){
                    file = absolutePath + "/" + file;
                }  
           
                output = new FileOutputStream(file);

		// save properties to project root folder
		prop.store(output, null);

	} catch (IOException io) {
		logger.error(io);
	} finally {
		if (output != null) {
			try {
				output.close();
			} catch (IOException e) {
				logger.error(e);
			}
		}

	}
           
    }
    
    private static void resetProps(Properties prop){
	OutputStream output = null;
	try {

	       String file = "jobs.data";
           
               if (OSValidator.isUnix()){
                    file = absolutePath + "/" + file;
                }  
           
                output = new FileOutputStream(file);

		// save properties to project root folder
		prop.store(output, null);

	} catch (IOException io) {
		logger.error(io);
	} finally {
		if (output != null) {
			try {
				output.close();
			} catch (IOException e) {
				logger.error(e);
			}
		}

	}
           
    }
    
    private static void readProps(int webport){
	InputStream input = null;
        Properties prop = new Properties();
	try {

	       String file = "jobs.data";
           
               if (OSValidator.isUnix()){
                    file = absolutePath + "/" + file;
                }  
           
               input = new FileInputStream(file);

		// load a properties file
		prop.load(input);
                Set<Object> keys = prop.keySet();
                for(Object k:keys){
                    String key = (String)k;
                    logger.info("Read jobs " + key + " --> "+prop.getProperty(key));
                    sendToSocket("localhost", webport, "execute="+prop.getProperty(key));
                }
                prop.clear();
                                
	} catch (IOException io) {
		logger.error(io);
	} finally {
		if (input != null) {
			try {
				input.close();
			} catch (IOException e) {
				logger.error(e);
			}
		}

	}
        
        resetProps(prop);
           
    }
    
     public static void sendToSocket(String hostname, int port, String execute) {
      
        try (Socket socket = new Socket(hostname, port)) {
 
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            writer.println(execute);
 
            InputStream input = socket.getInputStream();
 
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
 
            String line;
 
            while ((line = reader.readLine()) != null) {
                logger.info(line);
                //System.out.println(line);
            }
           

            writer.close();
            reader.close();
            output.close();
 
        } catch (UnknownHostException ex) {
 
            logger.error("Server not found: " + ex.getMessage());
 
        } catch (IOException ex) {
 
            logger.error("I/O error: " + ex.getMessage());
        }
    }
     
    public static String getJarContainingFolder(Class aclass) throws Exception {
          CodeSource codeSource = aclass.getProtectionDomain().getCodeSource();

          File jarFile;

          if (codeSource.getLocation() != null) {
            jarFile = new File(codeSource.getLocation().toURI());
          }
          else {
            String path = aclass.getResource(aclass.getSimpleName() + ".class").getPath();
            String jarFilePath = path.substring(path.indexOf(":") + 1, path.indexOf("!"));
            jarFilePath = URLDecoder.decode(jarFilePath, "UTF-8");
            jarFile = new File(jarFilePath);
          }
          return jarFile.getParentFile().getAbsolutePath();
        }
     
          
}
