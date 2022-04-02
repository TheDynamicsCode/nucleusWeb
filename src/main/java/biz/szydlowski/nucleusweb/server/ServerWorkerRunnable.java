/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.nucleusweb.server;

import static biz.szydlowski.nucleusweb.server.HtmlScripts.EXEC;
import static biz.szydlowski.nucleusweb.server.HtmlScripts.SEARCH_IN_TABLE;
import static biz.szydlowski.nucleusweb.server.HtmlStyle.CELL_COMMENT_STYLE;
import static biz.szydlowski.nucleusweb.server.HtmlStyle.ERROR404;
import static biz.szydlowski.nucleusweb.server.HtmlStyle.FIND_INPUT_STYLE;
import static biz.szydlowski.nucleusweb.server.HtmlStyle.LINKSTYLE;
import static biz.szydlowski.nucleusweb.server.HtmlStyle.TABLE_STYLE;
import static biz.szydlowski.nucleusweb.server.HtmlStyle.TOPNAV_STYLE;
import static biz.szydlowski.nucleusweb.server.NucleusServer.absolutePath;
import static biz.szydlowski.nucleusweb.server.NucleusServer.apiKeys;
import static biz.szydlowski.nucleusweb.server.NucleusServer.commandApi;
import static biz.szydlowski.nucleusweb.server.NucleusServer.hmap_cmd;
import static biz.szydlowski.nucleusweb.server.NucleusServer.hmap_time;
import static biz.szydlowski.nucleusweb.server.NucleusServer.modules;
import static biz.szydlowski.nucleusweb.server.NucleusServer.prepareCmdApi;
import static biz.szydlowski.nucleusweb.server.NucleusServer.restartApi;
import static biz.szydlowski.nucleusweb.server.NucleusServer.sdf;
import static biz.szydlowski.nucleusweb.server.NucleusServer.webport;
import biz.szydlowski.utils.OSValidator;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
/**
 *
 * @author Dominik
 */
public class ServerWorkerRunnable implements Runnable {
   
     static final Logger logger = LogManager.getLogger(ServerWorkerRunnable.class);
      protected Socket clientSocket = null;
      protected SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      String userInput = "default";
      boolean isGet=true;
      boolean isPost=false;
       private boolean allowedconn=false;
      private boolean allowedwebhook = false;
      private boolean allowedexc = false;
      BufferedReader br = null;
      private static final ExecutorService THREAD_POOL  = Executors.newCachedThreadPool();
                         
      public ServerWorkerRunnable(Socket clientSocket, boolean allowedconn, boolean allowedwebhook, boolean allowedexc) {
            this.clientSocket = clientSocket;
            this.allowedconn=allowedconn;
            this.allowedexc=allowedexc;
            this.allowedwebhook=allowedwebhook;
      }

    /**
     *
     */
    @Override
    public void run() {
        try {
           
            br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            userInput = br.readLine();
            
            /*while ((userInput = stdIn.readLine()) != null) {
                System.out.println(userInput);
            }*/

          if (userInput == null) userInput = "DEFAULT";

          //System.out.println("user input " + userInput);
          logger.debug("user input " + userInput);
          
          if (userInput.length() == 0){
              return;
           }
         
          isGet = userInput.contains("GET");
          
         if (!allowedconn) {
             logger.debug("Rejected Client : Address - " + clientSocket.getInetAddress().getHostAddress());
           
             try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
                  out.print(ERROR404);
                  out.flush();
             }
             br.close();
             return;
         } 
         
          logger.debug("Accepted Client : Address - " + clientSocket.getInetAddress().getHostName());
          
          String postData = "";
          if ( userInput.contains("POST")){
               isPost=true;
             
               String line;
               int postDataI=0;
                while ((line = br.readLine()) != null && (line.length() != 0)) {
                    logger.debug("HTTP-HEADER: " + line);
                    if (line.contains("Content-Length:")) {
                        postDataI = Integer.parseInt(line.substring(line.indexOf("Content-Length:") + 16, line.length()));
                    }
                }
                postData = "";
                // read the post data
                if (postDataI > 0) {
                    char[] charArray = new char[postDataI];
                    br.read(charArray, 0, postDataI);
                    postData = new String(charArray);
                }
                               
                postData =  replaceURL(postData );                                
                 
                logger.debug("post DATA after replace " + postData); 
             
          } else {
              isPost=false;
          } 
          
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                 
                    if ( isGet || isPost){
                        out.println("HTTP/1.1 200 OK");
                        out.println("Content-Type: text/html");
                        out.println("<html>\n");
                        out.println("<head>");
                        out.println("<title>");
                        out.println(Version.getVersion());
                        out.println("</title>");
                        out.println("<style>\n");
                        out.println(TABLE_STYLE);        
                        out.println(CELL_COMMENT_STYLE);
                        out.println(TOPNAV_STYLE );
                        out.println(FIND_INPUT_STYLE);
                        out.println("</style>");
                        out.println("</head>\n");
                    }

                   if (userInput.length()>0) userInput = userInput.replace("HTTP/1.1", "");
                   if (userInput.length()>0) userInput = userInput.replace("%20", " ");
                   if (userInput.length()>0) userInput = userInput.replaceFirst("GET", "");      
                   if (userInput.length()>0) userInput = userInput.replaceFirst("/", "");  
                   if (userInput.length()>0) userInput = userInput.replaceAll("\\s+", " ");
                   if (userInput.length()>0) userInput = userInput.replaceAll("favicon.ico ", "");

                   if (userInput.length() == 0){
                        return;
                   }   

             if (isPost){

                if ( userInput.contains("exec.api") ){

                        String[] parm = postData.split("&");

                        if (parm.length==2){

                            String[] lockidstr  = parm[1].split("=");
                            int lockid=0;

                            if (lockidstr.length==2){
                                lockid=Integer.parseInt(lockidstr[1]);            
                            }

                            if (lockid==-1){
                                out.append("");
                                out.flush();
                            } else {                       

                                if (!commandApi.get(lockid).isLock()){

                                    String[] _sp = parm[0].split("=");
                                    if (_sp.length==2){
                                        commandApi.get(lockid).setLock(true);
                                        logger.info("Execution start " + _sp[1]);

                                        Process p=null; 
                                        //out.append("<meta http-equiv=\"refresh\" content=\"10;url=/defined_commands\" />");
                                        out.append("</br>"); 
                                        out.flush();
                                        String [] cmds = _sp[1].split("#NEXTCOMMAND#");
                                        for (int h=0; h<cmds.length;h++){
                                           out.append(cmds[h]);
                                           out.append("</br>");
                                        }
                                        out.append("</br>");
                                        out.append("Execution start: ");
                                        out.append(sdf.format(new Date()));
                                        out.append("</br></br>");
                                        out.flush();
                                        
                                        UUID uuid = UUID.randomUUID();
                                        
                                        dumpToFile("response_web", uuid.toString(),"Execution start: " + sdf.format(new Date()) + "\n", true);
                                       
                                        hmap_cmd.put(uuid, _sp[1]);
                                        hmap_time.put(uuid, System.currentTimeMillis());   
                                        
                                        for (String cmd : cmds){
                                                try {
                                                 
                                                    p = Runtime.getRuntime().exec(cmd);
                                                    
                                                    BufferedReader reader =
                                                    new BufferedReader(new InputStreamReader( p.getInputStream()));

                                                    String line = "";
                                                    while ((line = reader.readLine())!= null) {
                                                            logger.trace(line);
                                                            out.append("  ");
                                                            out.append(line);
                                                            out.append("</br>");
                                                            out.flush();
                                                            
                                                            dumpToFile("response_web", uuid.toString(), line+"\n", true);
                                                    }

                                                } catch (Exception e) { 
                                                        out.append("  ");
                                                        out.append(e.toString());
                                                        out.append("</br>");
                                                        logger.error("executeCommand "+e);
                                                } finally {                                                 

                                                    try {
                                                        if (p!=null) {
                                                           p.waitFor(5, TimeUnit.MINUTES);
                                                           if (p.isAlive()){
                                                               p.destroyForcibly();
                                                               out.append("</br>");
                                                               out.append(" Executed stopped forcibly with timeout!!!");
                                                               out.append("</br>");
                                                               
                                                           }
                                                        }
                                                    } catch (InterruptedException o){
                                                        logger.error(o);
                                                    }

                                                }
                                        }
                                        commandApi.get(lockid).setLock(false);
                                        hmap_cmd.remove(uuid);
                                        hmap_time.remove(uuid);
                                       
                                        out.append("</br>");
                                        out.append("Execution finish: ");
                                        out.append(sdf.format(new Date()));
                                        out.append("</br>");
                                        out.append("</br>");
                                        
                                        
                                        dumpToFile("response_web", uuid.toString(), "Execution finish: " + sdf.format(new Date()) + " \n\n", true);
                                        
                                        for (int o=0; o<97; o++){
                                            out.append("#");
                                        }
                                        out.append("</br>");
                                        out.append("</br>");
                                        out.flush();
                                                    
                                        logger.info("Executed " + _sp[1]);
                                        System.gc();

                                  } else {  
                                     out.append("_sp.length!=2</br>");
                                     out.flush();
                                     logger.error("_sp.length!=2");
                                  }
                            }
                            else {
                                  out.append("</br></br>LOCKED!!!</br></br>");
                                  out.flush();
                            }
                        }


                    } else {
                        out.append("</br></br>INTERNAL ERROR</br></br>");
                        out.flush();

                   }
                }  else if ( userInput.contains("restart.api") ){ 
                            logger.info("RESTART API");
                            String[] _sp = postData.split("=");
                             if (_sp.length==2){
                                switch (_sp[1]) {
                                    case "restart":
                                        out.write(new StringBuilder().append("<font color=\"red\">RESTART NucleusWeb</font>").toString());
                                        out.flush();
                                        restartApi.restart();
                                        break;
                                    case "kill":  
                                        out.write(new StringBuilder().append("<font color=\"red\">KILL NucleusWeb</font>").toString());
                                        out.flush();
                                        restartApi.kill();
                                        break;
                                    default:
                                        out.write(new StringBuilder().append("<font color=\"red\">DO NOTHING</font>").toString());
                                        break;
                                }

                            } else {
                                 logger.error("_sp.length!=2");
                            }

               } 
            } //if post end 
         
            else if (userInput.contains("webhook.api")  ) { 
                 out.println("[<br>\n");
                 for (int j = 0; j <  commandApi.size(); j++) {
                            out.println("{<br>\n");
                            out.println("\t\"__ID\":" +   "\""+ j + "\", <br>\n");
                            out.println("\t\"module\":" +   "\""+ commandApi.get(j).getModule() + "\", <br>\n");
                            out.println("\t\"group\":" + "\""+ commandApi.get(j).getGroup() + "\", <br>\n");
                            out.println("\t\"alias\":" + "\""+ commandApi.get(j).getAlias()+ "\", <br>\n");
                            out.println("\t\"__description\":" + "\""+ commandApi.get(j).getDescription()+ "\" <br>\n");
                            //out.println("\t\"__command\":" + "\""+ commandApi.get(j).getCommand()+ "\" <br>\n");
                            if (j<commandApi.size()-1) out.println("},<br>\n");
                            else out.println("}<br>\n");

                }


                     for (int k=0; k<prepareCmdApi.size(); k++) {
                            out.println(",{<br>\n");
                            out.println("\t\"__ID\":" +   "\""+ k + "\", <br>\n");
                            out.println("\t\"module\":" +   "\"PREPARE\", <br>\n");
                            out.println("\t\"alias\":" + "\""+ prepareCmdApi.get(k).getAlias() + "\", <br>\n");
                            out.println("\t\"__description\":" + "\""+ prepareCmdApi.get(k).getDescription()+ "\" <br>\n");
                            if (k<prepareCmdApi.size()-1) out.println("},<br>\n");
                            else out.println("}<br>\n");
                     }


                 out.println("]<br>\n");

            } else if (userInput.contains("webhook")  ) { 

                if (!allowedwebhook) {
                        logger.debug("Rejected Client : Address - " + clientSocket.getInetAddress().getHostAddress());

                        out.print(ERROR404);
                        out.flush();

                        out.close();
                        br.close();

                        return;
               }

                if (userInput.length()>0) userInput = userInput.replace("webhook", "");

                String data[] = userInput.split("/");
                if (data.length != 6 && data.length != 5){
                     out.append("STATUS:ERROR, DESCRIPTION:DATA_LENGTH_SIZE\n<br>");
                }  else {

                    String command="default";
                    boolean valid_module=false;
                    boolean valid_group=false;
                    boolean valid_cmd=false;           
                    boolean valid_prp=true;

                    String key=data[1];
                    String module=data[2];
                    String group=data[3];
                    String alias="____________";
                    if (data.length == 6 ) alias=data[4];               


                    logger.debug("module:"+module+",group:"+group+",alias:"+alias);

                    boolean valid_key=false;
                    for (String key_g : apiKeys){
                        if (key_g.equals(key)) valid_key=true;
                    }
                    if (!valid_key){
                        out.append("STATUS:ERROR, DESCRIPTION:INVALID_KEY\n<br>");
                    } else {


                         for (int j = 0; j <  commandApi.size(); j++) {                       
                               if (commandApi.get(j).getModule().equals(module)
                                       && commandApi.get(j).getGroup().equals(group)
                                       && commandApi.get(j).getAlias().equals(alias)){
                                   valid_cmd=true;
                                   command=commandApi.get(j).getCommand();
                               }
                           }// end for

                           if (!valid_cmd){
                                 for (int j = 0; j <  commandApi.size(); j++) {                       
                                       if (commandApi.get(j).getModule().equals(module)){
                                           valid_module=true;
                                       }
                                   }// end for 
                           }

                           if (valid_module){
                                 for (int j = 0; j <  commandApi.size(); j++) {                       
                                       if (commandApi.get(j).getModule().equals(module) && commandApi.get(j).getGroup().equals(group)){
                                           valid_group=true;
                                       }
                                   }// end for 
                           }

                            if (data.length == 5 && module.equals("PREPARE")){

                                    command="";
                                    valid_cmd=false;
                                    valid_prp=false;

                                    for (int i=0;i<prepareCmdApi.size();i++) {
                                         if (group.startsWith(prepareCmdApi.get(i).getAlias())){
                                             command=group.replaceAll(prepareCmdApi.get(i).getAlias(), prepareCmdApi.get(i).getCommand());
                                             valid_cmd=true;
                                             valid_prp=true;
                                         }

                                    }


                            } else if (data.length == 5) {
                                   valid_cmd=false;                             
                            } 


                       if (!valid_cmd){
                           if (!valid_prp) out.append("STATUS:ERROR, DESCRIPTION:INVALID_PREPARE_CMD\n<br>");
                           else if (valid_group) out.append("STATUS:ERROR, DESCRIPTION:COMMAND_NOT_FOUND\n<br>");
                           else if (valid_module) out.append("STATUS:ERROR, DESCRIPTION:GROUP_NOT_FOUND\n<br>");
                           else out.append("STATUS:ERROR, DESCRIPTION:MODULE_NOT_FOUND\n<br>");
                       } else {
                                if (checkLocking(command)){
                                    out.append("STATUS:ERROR, DESCRIPTION:COMMAND ").append(command).append(" IS LOCKED\n<br>");
                                } else {
                                    
                                       String [] cmds = command.split("#NEXTCOMMAND#");
                                        for (int h=0; h<cmds.length;h++){
                                           out.append(cmds[h]);
                                           out.append("</br>");
                                        }
                                        out.append("</br>");
                                        out.append("Execution start: ");
                                        out.append(sdf.format(new Date()));
                                        out.append("</br></br>");
                                        out.flush();
                                        
                                        UUID uuid = UUID.randomUUID();
                                    
                                        String catalog="response_api";

                                        StringBuilder ret = new StringBuilder();
                                        ret.append("Execution command: ").append(command).append("\n");
                                        ret.append("Execution start: ");
                                        ret.append(sdf.format(new Date()));
                                        ret.append("\n\n");


                                        Process p=null; 
                                        out.append("STATUS:OK, UUID:").append(uuid.toString());
                                        out.append("Execution start: ");
                                        out.append(sdf.format(new Date()));
                                        out.append("\n\n");
                                        out.flush();
                                        
                                        dumpToFile(catalog, uuid.toString(), ret.toString(), false);
                                       
                                        hmap_cmd.put(uuid, command);
                                        hmap_time.put(uuid, System.currentTimeMillis());   
                                        updateJobsProps();
                                         
                                        for (String cmd : cmds){
                                                try {

                                                    p = Runtime.getRuntime().exec(cmd);

                                                    BufferedReader reader =
                                                    new BufferedReader(new InputStreamReader(p.getInputStream()));

                                                    String line = "";
                                                    while ((line = reader.readLine())!= null) {
                                                            logger.trace(line);
                                                             out.append(line);
                                                             out.append("\n");
                                                             ret.append(line);
                                                             ret.append("\n");
                                                             out.flush();
                                                            
                                                            dumpToFile("response_web", uuid.toString(), line+"\n", true);
                                                    }

                                                } catch (Exception e) { 
                                                        out.append("  ");
                                                        out.append(e.toString());
                                                        out.append("</br>");
                                                        logger.error("executeCommand "+e);
                                                } finally {                                                 

                                                    try {
                                                        if (p!=null) {
                                                           p.waitFor(5, TimeUnit.MINUTES);
                                                           if (p.isAlive()) p.destroyForcibly();
                                                        }
                                                    } catch (InterruptedException o){
                                                        logger.error(o);
                                                    }

                                                }
                                        }
                                     
                                        hmap_cmd.remove(uuid);
                                        hmap_time.remove(uuid);
                                  
                                        out.append("\n");
                                        out.append("Execution finish: ");
                                        out.append(sdf.format(new Date()));
                                        out.append("\n");

                                        ret.append("\n");
                                        ret.append("Execution finish: ");
                                        ret.append(sdf.format(new Date()));

                                        dumpToFile(catalog, uuid.toString(), ret.toString(), false);
                                                                                  
                                        logger.info("Executed " + command);
                               

                                }

                       }
                    }//valid key

                } //length

            } else if (userInput.contains("execute")  ) { 

                 if (!allowedexc) {
                        logger.debug("Rejected Client : Address - " + clientSocket.getInetAddress().getHostAddress());
                     
                        out.print(ERROR404);
                        out.flush();

                        out.close();
                        br.close();

                        return;
               }

                if (userInput.length()>0) userInput = userInput.replace("execute=", "");   
                UUID uuid = UUID.randomUUID();

                Thread thread = new Thread(){
                    @Override
                    public void run(){

                            String sCommandString=userInput;

                            if (sCommandString.length() <= 1){
                               logger.warn("sCommandString.length() >= 1");
                               return;
                            } 

                            logger.info("Execution start: " + sCommandString);

                            executeCommand(sCommandString, uuid);

                            logger.info("Executed " + sCommandString);
                            System.gc();

                    }

                     private void executeCommand(String command, UUID uuid) {
                                Process p=null; 
                                StringBuilder ret = new StringBuilder();
                                ret.append("Execution start: ");
                                ret.append(sdf.format(new Date()));
                                ret.append("\n\n");
                                String catalog="response_api";

                                try {
                                        hmap_cmd.put(uuid, command);
                                        hmap_time.put(uuid, System.currentTimeMillis());

                                        dumpToFile(catalog, uuid.toString(), ret.toString(),false);

                                        updateJobsProps();
                                        p = Runtime.getRuntime().exec(command);
                                        //p.waitFor();
                                       // p.waitFor(15, TimeUnit.SECONDS);
                                        BufferedReader reader =
                                        new BufferedReader(new InputStreamReader(p.getInputStream()));

                                        String line = "";
                                        while ((line = reader.readLine())!= null) {
                                                logger.trace(line);
                                                ret.append(line);
                                                ret.append("\n");
                                                dumpToFile(catalog, uuid.toString(), line+"\n", true);
                                        }

                                } catch (Exception e) { 
                                        ret.append(e);
                                        ret.append("\n");
                                        logger.error("executeCommand "+e);
                                } finally {
                                    hmap_cmd.remove(uuid);
                                    hmap_time.remove(uuid);

                                    ret.append("\n");
                                    ret.append("Execution finish: ");
                                    ret.append(sdf.format(new Date()));

                                    try {
                                        if (p!=null) {
                                           p.waitFor(5, TimeUnit.MINUTES);
                                           if (p.isAlive()) p.destroyForcibly();
                                        }
                                    } catch (InterruptedException o){
                                        logger.error(o);
                                    }

                                    dumpToFile(catalog, uuid.toString(), ret.toString(), false);
                                }



                        }                   

                 };

                thread.start();
                out.append("Add command " + userInput + " to queue and will be execute immediately...\n");
                out.append("UUID " + uuid + "\n"); 
                out.append(clientSocket.getLocalAddress().toString().replaceFirst("/", "")+":"+webport+"/response_api/" + uuid + "\n");
                out.append("You can close this window....");


            }  else if (userInput.contains("response_api")  ) {  
                if (userInput.length()>0) {
                    String uuid  = userInput.replace("response_api/", "").replaceAll(" ", "");
                    printResponse(out, "response_api", uuid);
                }  

            } else if (userInput.contains("response_web")  ) {  
                if (userInput.length()>0) {
                    String uuid  = userInput.replace("response_web/", "").replaceAll(" ", "");
                    printResponse(out, "response_web", uuid);
                }  

            }   else if (userInput.contains("defined_command")  ) {  
                    int Imodule = 0;
                    try {
                        Imodule = Integer.parseInt(userInput.replace("defined_command_", "").replaceAll("\\s+", ""));
                    } catch (Exception ee){}

                    printDefinedCommands(out, Imodule);



            }  else if (userInput.contains("all_res_api")  ) {  
                if (userInput.length()>0) {
                    printAllResponse(out, "response_api");
                }  

            }   else if (userInput.contains("all_res_web")  ) {  
                if (userInput.length()>0) {
                    printAllResponse(out, "response_web");
                }  

            } else if (userInput.contains("tasks_count")  ) {  
                out.println(hmap_cmd.size());
            } else if (userInput.contains("tasks_time_max")  ) {  
                long dif=System.currentTimeMillis();
                for (Map.Entry<UUID, Long> entry : hmap_time.entrySet()) {
                    if (entry.getValue()< dif) dif=entry.getValue();                
                }
                dif = System.currentTimeMillis()-dif;
                out.println(dif);
            } else if (userInput.contains("tasks")  ) {  
                if (isGet)  out.println("CURRENT TASKS </br> ==================================== </br>");
                if (hmap_cmd.isEmpty()) out.println("NO TASKS </br>");
                for (Map.Entry<UUID, String> entry : hmap_cmd.entrySet()) {
                  long dif= System.currentTimeMillis() - hmap_time.getOrDefault(entry.getKey(), 0L);
                  if (isGet) {                  
                      out.println(new StringBuilder().append(entry.getKey()).append(" ----> ").append(entry.getValue()).append(" | ").append(dif).append("</br>").toString());
                  }   else {
                      out.println(new StringBuilder().append(entry.getKey()).append(" ----> ").append(entry.getValue()).append(" | ").append(dif).toString());
                  }
                }   

                printHomeAndBack(out,true,false,false); 

            } else if (userInput.replaceAll("\\s+", "").equals("ping")){ 
                 if (isGet) out.println(new StringBuilder().append("PONG").append("<br/>").toString());
                 else out.println(new StringBuilder().append("PONG").append("<br/>").toString());
            }  else { 
                    out.println(new StringBuilder().append("<b>Diagnostic console").append("</b><br/><br/>").toString());                    
                    out.println(new StringBuilder().append(" <a href=\"defined_command_0\">COMMANDS</a><br/><br/>").toString());
                    out.println(new StringBuilder().append(" <a href=\"tasks\"> CURRENT TASKS</a><br/>").toString());
                    out.println(new StringBuilder().append(" <a href=\"all_res_api\"> RESPONSE FOR API</a><br/>").toString());
                    out.println(new StringBuilder().append(" <a href=\"all_res_web\"> RESPONSE FOR WEB</a><br/><br/>").toString());
                    out.println(new StringBuilder().append(LINKSTYLE).toString());
                    printHomeAndBack(out,false,false,true);

            }

            if  ( (isGet || isPost) ) {
                 out.println("</html>");
            }

             out.flush();
             out.close();
           
           
          logger.debug("Request processed/completed...");
        }
        catch (IOException e) {
          logger.error(e);
        } finally {
       
            try { 
                if(br != null) { 
                    br.close(); 
               } 
            } catch(Exception e) {
                logger.error(e);
            }
            try { 
                clientSocket.close(); 
            } catch(Exception e) { 
                logger.error(e);
            }
                  
            
        
        }
    }
      
    private boolean checkLocking(String cmd){
           boolean ret=false;
           for (Map.Entry<UUID, String> entry : hmap_cmd.entrySet()) {
                if (entry.getValue().equals(cmd)) ret=true;
            }   
           return ret;
    }
   
    private void dumpToFile(String catalog, String uuid, String content, boolean append){ 
     
      
                        if (OSValidator.isUnix()){
                                 absolutePath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
                                       absolutePath = "/" + absolutePath.substring(0, absolutePath.lastIndexOf("/"))+"/";
                        }
            
            
                        String dumpfile=catalog+"/"+uuid;
                        String response=catalog;

                        if (OSValidator.isUnix()){
                              dumpfile = absolutePath + "/" + dumpfile;
                              response = absolutePath + "/" +  response ;
                        }  

                        File f = new File(response);

                        if (!f.exists()) {
                            f.mkdirs();
                        }   
                        
                       BufferedWriter bw = null;
		       FileWriter fw = null;

		       try {


                                fw = new FileWriter(dumpfile, append);
                                bw = new BufferedWriter(fw);
                                bw.write(content);

                        } catch (IOException e) {
                                logger.error(e);

                        } finally {

                                try {

                                        if (bw != null)
                                                bw.close();

                                        if (fw != null)
                                                fw.close();

                                } catch (IOException ex) {
                                    logger.error(ex);
                                }

                        }

                        
     }
      
     private void printResponse(PrintWriter out, String catalog, String uuid){
        
           BufferedReader br = null;
           FileReader fr = null;
           
           String file = catalog + "/" + uuid;
           
           if (OSValidator.isUnix()){
                file = absolutePath + "/" + file;
            }  

            try {

                    //br = new BufferedReader(new FileReader(FILENAME));
                    fr = new FileReader(file);
                    br = new BufferedReader(fr);
                    String sCurrentLine;

                    while ((sCurrentLine = br.readLine()) != null) {
                         if (isGet) out.println(sCurrentLine + "</br>");
                         else out.println(sCurrentLine);
                    }

            } catch (IOException e) {
                out.println("Response " + uuid + " not found!!!!");
                logger.error(e);

            } finally {

                    try {

                            if (br != null)
                                    br.close();

                            if (fr != null)
                                    fr.close();

                    } catch (IOException ex) {
                        out.println(ex);
                    }

        }
     
    }
     
    public void printAllResponse(PrintWriter out, String catalog) {
        if (OSValidator.isUnix()){
             absolutePath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
             absolutePath = "/" + absolutePath.substring(0, absolutePath.lastIndexOf("/"))+"/";
        }
               
        File folder = new File(absolutePath+catalog);
        File[] listOfFiles = folder.listFiles();
        out.println("<table border=\"1\" style=\"width:50%\">");
        out.println("<tr> <th>Modification date</th><th>UUID</th>");
        out.println("</tr>");
        if (listOfFiles!=null){
        Arrays.sort(listOfFiles, Comparator.comparingLong(File::lastModified).reversed());
        for (int i = 0; i < listOfFiles.length; i++) {
          out.println("<tr>");
          if (listOfFiles[i].isFile()) {
            out.println("<td>" +  new Date(listOfFiles[i].lastModified()) + "</td><td><a href=\""+catalog+"/" + listOfFiles[i].getName() +"\">"+listOfFiles[i].getName()+"</a></td>");
          } else if (listOfFiles[i].isDirectory()) {
          }
          out.println("</tr>");
        }
        } 
        out.println("</table>");
        printHomeAndBack(out,true,false,false); 
        
    }
    
    public void printDefinedModules(PrintWriter out, int k) {
  
        out.println("<div class=\"topnav\">");
        for (int i=0; i<modules.size(); i++){
             if (i==k) out.println("<a class=\"active\" href=\"defined_command_"+i+"\">"+modules.get(i)+"</a>");
             else out.println("<a href=\"defined_command_"+i+"\">"+modules.get(i)+"</a>");
        }
        
        out.println("</div>");
       
    }
    
    public List<String> getGroupInModule(String module){
             List<String> group = new ArrayList<>();
        
             for (int i=0; i<commandApi.size(); i++){
                if (commandApi.get(i).getModule().equals(module)){
                    boolean exist=false;
                    for (int j=0; j<group.size(); j++){
                        if (group.get(j).equals(commandApi.get(i).getGroup())) exist=true;
                    }
                    if (!exist) group.add(commandApi.get(i).getGroup());
                }
            } 
            return group;
    }
    
     public List<String> getHostsInGroupAndModule(String module, String group){
             List<String> host = new ArrayList<>();
        
             for (int i=0; i<commandApi.size(); i++){
                if (commandApi.get(i).getModule().equals(module) && commandApi.get(i).getGroup().equals(group)){
                    boolean exist=false;
                    for (int j=0; j<host.size(); j++){
                        if (host.get(j).equals(commandApi.get(i).getHost())) exist=true;
                    }
                    if (!exist) host.add(commandApi.get(i).getHost());
                }
            } 
            return host;
    }
     
     public int getItemsCountIndModuleGroupHost(String module, String group, String host){
             int licz=0;
        
             for (int i=0; i<commandApi.size(); i++){
                if (commandApi.get(i).getModule().equals(module) && commandApi.get(i).getGroup().equals(group) && commandApi.get(i).getHost().equals(host)){
                   licz++;  
                }
            } 
            return licz;
    }
    
    public void printDefinedCommands(PrintWriter out, int mdl) {
        
     
               
        printDefinedModules(out, mdl);
        
        
        out.println("<div class=\"tab\">");
        
        if (mdl>=modules.size() || mdl<0) mdl=0;
        
        List<String> group = getGroupInModule(modules.get(mdl));
               
        if (group.isEmpty())  out.println("----");
                  
        for (int i = 0; i <  group.size(); i++) {
             out.println("<button class=\"tablinks\" onclick=\"exec(event, '"+group.get(i)+"')\" id=\"defaultOpen\">"+group.get(i)+"</button>");        
        }               
        out.println("</div>");  
        
        for (int i = 0; i <  group.size(); i++) {
            
           List<String> hosts = getHostsInGroupAndModule(modules.get(mdl), group.get(i));
                       
           out.println("<div id=\""+group.get(i)+"\" class=\"tabcontent\">"); 
           out.println("<h3>"+group.get(i)+"</h3>"); 
           
           out.println("<input class=\"searchInput\" type=\"text\" id=\"searchInput."+i+"\" onkeyup=\"search("+i+")\" placeholder=\"Search for alias..\" title=\"Type in an alias\">");
            
           out.println("<table id=\"commandTab."+i+"\" class=\"scroll\">");
           out.println("<thead><tr>");
           out.println("<th style=\"width:120px\">Host</th>");
           out.println("<th style=\"width:400px\">Alias</th>");
           out.println("<th style=\"width:715px\">Command</th><th style=\"width:96px\"> Executor </th>");
           out.println("</tr></thead>");
           
               
           for (int k=0; k<hosts.size();k++){ 
                for (int j = 0; j <  commandApi.size(); j++) {
               
                  if (commandApi.get(j).getGroup().equals(group.get(i)) && commandApi.get(j).getModule().equals(modules.get(mdl))
                          && commandApi.get(j).getHost().equals(hosts.get(k)))  {
                     
                      out.println("<tr>");
                      if (hosts.get(k).equals("ERROR")){
                         out.println("<td style=\"width:120px\"></td>");
                      } else {
                         out.println("<td style=\"width:120px\">"+hosts.get(k)+"</td>"); 
                      }

                      if (commandApi.get(j).getDescription().equals("ERROR")){
                           out.println("<td style=\"width:400px\">" +  commandApi.get(j).getAlias() + "</td>");
                      } else {
                            out.println("<td style=\"width:400px\" class=\"CellWithComment\">" +  commandApi.get(j).getAlias() + "<span class=\"CellComment\">"+commandApi.get(j).getDescription()+"</span></td>");
                      }

                      out.println("<td style=\"width:715px\">");
                      for (String s : commandApi.get(j).getCommand().split("#NEXTCOMMAND#")){
                           out.println(s+"</br>");
                      }
                      out.println("</td>");

                      out.println("<td style=\"width:80px\"><button class=\"button-exc\"  onclick=\"execute('"+j+"','"+commandApi.get(j).getCommand()+"')\">Execute</button></td>");
                      out.println("</tr>");
                  }
                }
           }

      
            out.println("</table>");            
            out.println("</div>");  
          
        }  
        
        out.println(EXEC); 
        out.println(SEARCH_IN_TABLE);
        
        printApiFunction(out);
        printHomeAndBack(out,true,true,false);
        
    }
   
   
      private void printHomeAndBack(PrintWriter out, boolean printHome, boolean printClear, boolean printRestart){
          
          if (printHome){ 
              out.println("<br/><button onclick=\"goHome()\">HOME</button>\n");
              out.println("  "); 
              out.println("<script>\n");
              out.println("function goHome() {\n");
              out.println("   window.location = '/';\n");
              out.println("}\n" );
              out.println( "</script>");
          }
          
          if (printClear) out.println("<button onclick=\"goBack()\">Go Back</button>\n");
          else out.println("<button onclick=\"goBack()\">Go Back</button><br/>\n");
          out.println("\n");
          out.println("<script>\n");
          out.println("function goBack() {\n");
          out.println("    window.history.back();\n");
          out.println("}\n" );
          out.println( "</script>"); 
          
          if (printClear){
              out.println("<button onclick=\"executeClean(-1, 'clear')\">Clear</button><br/>\n");          
          }
          
          if (printRestart){
               printRestartAndKill(out);          
          }
         // out.println("</br> &copy; 2021 Landevant Research Center <b> <a href=\"mailto:support@szydlowski.biz?subject="+ Version.getVersion()+"\">support@szydlowski.biz</a></b>");
         
          out.println("</br> &copy; 2021 DoSS Research Center <b> <a href=\"mailto:support@szydlowski.biz?subject="+ Version.getVersion()+"\">support@szydlowski.biz</a></b>");
          out.println("</br> " +Version.getVersion());
     }
      
     private void printApiFunction(PrintWriter out){
          out.println("<h3>Command Line Executor</h3><p style=\"color:Lime;background-color:black;width:1400px;\" id=\"info\"></p>");
      
          out.println(new StringBuilder().append("<script>\n").append("function execute(id, command) {\n")
                              .append( "    var txt = \'Action\';\n")
                              .append( "        txt = \"Are you sure you want to execute: \\n\" \n")
                              .append( "        var cmds = \"\";\n")
                              .append( "        cmdTab = command.split('#NEXTCOMMAND#');\n")
                              .append( "        for (i=0; i<cmdTab.length; i++){\n")
                              .append( "         if (i<cmdTab.length-1) cmds = cmds + cmdTab[i] + \"\\n\";\n")
                              .append( "         else cmds = cmds + cmdTab[i]; \n")
                              .append( "        }\n")
                              .append( "        txt = txt + cmds;\n")
                              .append( "        var r = confirm(txt);\n")
                              .append( "        if (r == true) {\n")
                              .append( "        var xhttp = new XMLHttpRequest();\n")
                              .append( "        xhttp.onreadystatechange = function() {\n")
                              .append( "           if (this.status == 200) {")
                              .append( "               document.getElementById(\"info\").innerHTML = this.responseText;\n")
                              .append( "           }\n")
                              .append( "       };\n")
                              .append( "       xhttp.open(\"POST\", \"exec.api\", true);")
                              .append( "       xhttp.setRequestHeader(\"Content-type\", \"application/x-www-form-urlencoded\");\n")
                              .append( "       xhttp.send(\"command=\"+command+\"&id=\"+id);\n ")
                              .append( "    } else {\n")
                              .append( "    }\n")
                              .append(  " }\n")
                              .append( " </script>").toString());
          
        out.println(new StringBuilder().append("<script>\n").append("function executeClean(id, command) {\n")
                              .append( "        var xhttp = new XMLHttpRequest();\n")
                              .append( "        xhttp.onreadystatechange = function() {\n")
                              .append( "           if (this.status == 200) {")
                              .append( "               document.getElementById(\"info\").innerHTML = this.responseText;\n")
                              .append( "           }\n")
                              .append( "       };\n")
                              .append( "       xhttp.open(\"POST\", \"exec.api\", true);")
                              .append( "       xhttp.setRequestHeader(\"Content-type\", \"application/x-www-form-urlencoded\");\n")
                              .append( "       xhttp.send(\"command=\"+command+\"&id=\"+id);\n ")
                              .append(  " }\n")
                              .append( " </script>").toString());
            
    } 
     
     private void printRestartAndKill(PrintWriter out){         
          
          out.println("");
          out.println("<br/><button onclick=\"Restart('restart')\">Restart NucleusWeb</button>");
          out.println("");
          out.println("<button onclick=\"Restart('kill')\">Kill NucleusWeb</button><br/>");
          
          printRestartApiFunction(out);
    }
      
     
    private void printRestartApiFunction(PrintWriter out){
          out.println("<font color=\"red\"><p id=\"info\"></p></font>");
      
          out.println(new StringBuilder().append("<script>\n").append("function Restart(type) {\n")
                              .append( "    var txt = \'Action\';\n")
                              .append( "    if (type === 'restart') {\n")
                              .append( "        txt = \"Are you sure you want to restart daemon?\";\n")
                              .append( "    } else if (type === 'kill') {\n")
                              .append( "        txt = \"Are you sure you want to kill daemon?\";\n")
                              .append( "    } \n")
                              .append( "    var r = confirm(txt);\n")
                              .append( "    if (r == true) {\n")
                              .append( "        var xhttp = new XMLHttpRequest();\n")
                              .append( "        xhttp.onreadystatechange = function() {\n")
                              .append( "           if (this.status == 200) {")
                              .append( "               document.getElementById(\"info\").innerHTML = this.responseText;\n")
                              .append( "           }\n")
                              .append( "       };\n")
                              .append( "       xhttp.open(\"POST\", \"restart.api\", true);")
                              .append( "       xhttp.setRequestHeader(\"Content-type\", \"application/x-www-form-urlencoded\");\n")
                              .append( "       xhttp.send(\"action=\"+type);\n ")
                              .append( "    } else {\n")
                              .append( "    }\n")
                              .append(  " }\n")
                              .append( " </script>").toString());
            
      }
    private static <T> T timedCall(Callable<T> c, long timeout, TimeUnit timeUnit)
            throws InterruptedException, ExecutionException, TimeoutException {
            FutureTask<T> task = new FutureTask<T>(c);
            THREAD_POOL.execute(task);
            return task.get(timeout, timeUnit);
    }
    public void updateJobsProps() {
        
       
	Properties prop = new Properties();
	InputStream input = null;
        OutputStream output = null;
        int jobs=0;
        
	try {

		input = new FileInputStream( absolutePath + "stats.properties");

		// load a properties file
		prop.load(input);
                jobs = Integer.parseInt(prop.getProperty("jobs", "0") ) +  1;
	

	} catch (IOException ex) {
		logger.error(ex);
                //ex.printStackTrace();
	} finally {
		if (input != null) {
			try {
				input.close();
			} catch (IOException e) {
				logger.error(e);
                               // e.printStackTrace();
			}
		}
	}
        
         try {

                    output = new FileOutputStream( absolutePath + "stats.properties");
                    // set the properties value
                    if (jobs==0) jobs=1;
                    prop.setProperty("jobs", ""+jobs);

                    // save properties to project root folder
                    prop.store(output, null);

            } catch (IOException io) {
                   logger.error(io);
                  // io.printStackTrace();
            } finally {
                    if (output != null) {
                            try {
                                    output.close();
                            } catch (IOException e) {
                                   logger.error(e);
                                 // e.printStackTrace();
                            }
                    }

            }
      
  }
    
   private String replaceURL(String postData){
          
         try {
              postData = URLDecoder.decode(postData, "UTF-8");
          } catch (UnsupportedEncodingException ex) {
              logger.error("replaceURL decoder");
          }
              
          postData = postData.replaceAll("%C4%84", "A");
          postData = postData.replaceAll("%C4%85", "a");
          postData = postData.replaceAll("%C4%87", "C");
          postData = postData.replaceAll("%C4%88", "c");
          postData = postData.replaceAll("%C4%98", "E");
          postData = postData.replaceAll("%C4%99", "e");
          postData = postData.replaceAll("%C5%81", "L");
          postData = postData.replaceAll("%C5%82", "l");

          postData = postData.replaceAll("%C5%83", "N");
          postData = postData.replaceAll("%C5%84", "n");

          postData = postData.replaceAll("%C3%B3", "o");
          postData = postData.replaceAll("%C3%93", "O");

          postData = postData.replaceAll("%C5%9A", "S");
          postData = postData.replaceAll("%C5%9B", "s");
          postData = postData.replaceAll("%C5%B9", "Z");
          postData = postData.replaceAll("%C5%BA", "z");
          postData = postData.replaceAll("%C5%BB", "Z");
          postData = postData.replaceAll("%C5%BC", "z");
          postData = postData.replaceAll("%20", " ");

         
          postData = postData.replaceAll("%5B", "[");
          postData = postData.replaceAll("%5C", "\\");
          postData = postData.replaceAll("%5D", "]");
          postData = postData.replaceAll("%21", "!");
          postData = postData.replaceAll("%22", "\"");
          postData = postData.replaceAll("%23", "#");
          postData = postData.replaceAll("%24", "$");
          postData = postData.replaceAll("%28", "(");
          postData = postData.replaceAll("%29", ")");
          postData = postData.replaceAll("%40", "@");
          postData = postData.replaceAll("%3F", "?");
          postData = postData.replaceAll("%25", "%");

          return postData;          
                
    }

     
     protected boolean writeZBXMessage(OutputStream out, byte[] data) {
		int length = data.length;
                boolean ret =false;
              		
		try   {
                     out.write(new byte[] {
				'Z', 'B', 'X', 'D', 
				'\1',
				(byte)(length & 0xFF), 
				(byte)((length >> 8) & 0x00FF), 
				(byte)((length >> 16) & 0x0000FF), 
				(byte)((length >> 24) & 0x000000FF),
				'\0','\0','\0','\0'});
		
		    out.write(data);
                    out.flush();
                    ret =true;
            } catch (Exception e) {                
                logger.error("EX255 " + e);
                 
                 ret =false;
            }
            return ret;
       }
            
}