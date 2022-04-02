/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.nucleusweb.server;

import static biz.szydlowski.nucleusweb.server.NucleusServer.hmap_cmd;
import static biz.szydlowski.nucleusweb.server.NucleusServer.hmap_time;
import static biz.szydlowski.nucleusweb.server.NucleusServer.startTime;
import static biz.szydlowski.nucleusweb.server.NucleusServer.webport;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Map;
/**
 *
 * @author Dominik
 */
public class RemoteControlRunnable  implements Runnable {
   
      static final Logger log = LogManager.getLogger(RemoteControlRunnable.class);
      protected Socket socket = null;
      String userInput = "default";
      private final boolean allowedconn;
      BufferedReader br = null;
       // Utility items
      StringWriter writer = null;
      short[] tempShort = null;
        
                         
      public RemoteControlRunnable(Socket clientSocket, boolean allowedconn) {
            this.socket = clientSocket;
            this.allowedconn=allowedconn;
            tempShort = new short[4];
      }

    /**
     *
     */
   
      @Override
    public void run()  {
       // final Thread currentThread = Thread.currentThread();
        //currentThread.setName("Processing-" + System.currentTimeMillis());
        
    
        String client = socket.getInetAddress().getHostAddress();
        if(log.isDebugEnabled()) {
               log.info("Accepted Connection From: " + client);
        }
       
          
        if (!allowedconn){
            try {
                 socket.close();
            } catch(Exception e) { 
                 log.error(e);
            }
                  
            return;
        }
        
        try  {
          
           socket.setTcpNoDelay(true); 
          
           int filterOut = 0;
           int dataLength=20000;
           String key="";
          
           br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    
           int indx=-8;
            
            
            while (true) {
              
                if (indx==dataLength) break; 
                indx++;
                
                byte b = (byte) br.read();
                              
                if (filterOut > 0)
                {
                 
                    switch (filterOut) {
                        case 8:
                            tempShort[0] = b;
                            break;
                        case 7:
                            tempShort[1] = b;
                            break;
                        case 6:
                            tempShort[2] = b;
                            break;
                        case 5:
                            tempShort[3] = b;
                            dataLength = convToInt(tempShort);
                            break;
                        default:
                            break;
                    }
                                    
                    filterOut--;
                    continue;
                }
                if (b == -1 || b == 10)
                {
                    // \n or end of stream will end the key.
                    break;
                }
                if (b == 1)
                {
                    // Happens from zabbix sender: the ZBX header is sent by the getter. In that case, ignore length (8 next bytes).
                    writer = new StringWriter();
                    filterOut = 8;
                    indx=-8;
                    continue;
                }
                
                if (writer!=null) writer.write(b); 
             
            }
            
            socket.setSoTimeout(10000);
             
            if (writer!=null) key=writer.toString();
            //log.debug("key.l " + key.length() + " dataLength " + dataLength);
            //log.debug("key " + key);
           
            String res = "";
           
            if (key.length()!=dataLength){
                log.error("key.length()!=dataLength");
            }  else {
                res = getResponseForKey(key);
            }          
        

            // ////////////////////////////////////////////
            // Send data back to Zabbix
            // ////////////////////////////////////////////

            log.debug("response: " + res);
            
            if (res.length()==0){
                res = "ZBX_NOTSUPPORTED\0res.length()==0";
            } else if (res.equals("undefined")) {
                //res = "";
                res = "ZBX_NOTSUPPORTED\0undefined";
            }
            
           writeZBXMessage(socket.getOutputStream(), res.getBytes(Charset.forName("ISO-8859-1")));
           
   
            
        }   catch(SocketTimeoutException ste) {
            log.error(client + ": Timeout Detected.");
        }
        catch (Exception e) {
            log.error(e);
        }
        finally  {
        
            try { if(br != null) { br.close(); } } catch(Exception e) { }
            try { socket.close(); } catch(Exception e) { 
                log.error(e);
            }
          
            
        }
    }

  

       
    private String getResponseForKey(String key){
          
            String res="";

            // Key analysis
            log.debug("Requested key: " + key);
            String key_root = key.split("\\[")[0];
            
            log.trace("key_root: " + key_root);
            
            try {
                
                if ("zbx.maintenance".equals(key_root)) {
                 
                  if (key.split("\\[").length>=2){
                      String[] key_args = key.split("\\[")[1].replace("]", "").split(",");
                      //log.debug("key_root: " + key_root);
                       
                      switch (key_args[0]) {

                            case "tasks_count":
                                res= Integer.toString(hmap_cmd.size());                                    
                                break;

                            case "tasks_time_max":
                                long dif=System.currentTimeMillis();
                                for (Map.Entry<UUID, Long> entry : hmap_time.entrySet()) {
                                    if (entry.getValue()< dif) dif=entry.getValue();                
                                }
                                dif = (System.currentTimeMillis()-dif)/1000;
                                res = Long.toString(dif);
                                break;
                                
                            case "uptime":
                                long difup=System.currentTimeMillis()-startTime;
                                int idifup=(int)difup/1000;
                                res = Integer.toString(idifup);
                                break;
                                
                            case "ping":
                                if (sendPingToSocket().startsWith("PONG")){
                                    res="1";
                                } else res="0";

                                break;
                                            
                            case "version":
                                res = ""+Version.getAgentVersion();
                                break;

                            default:
                                res = "";
                            } 
                   }
                }  
        } catch (Exception e){
              log.error("EX121 " + e);      
        }
            
        return res;    
    }
        
            // Pass an array of four shorts which convert from LSB first 
     private int convToInt(short[] sb){
       int answer = sb[0]  & 0xFF;
       answer += (sb[1] & 0x00FF) << 8 ;
       answer += (sb[2] & 0x0000FF) << 16  ;
       answer += (sb[3]& 0x000000FF)<< 24  ;
       return answer;        
     }
     
public String sendPingToSocket() {
        String line="";
        
        try (Socket socket = new Socket("localhost", webport)) {
 
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            writer.println("ping");
 
            InputStream input = socket.getInputStream();
 
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            line = reader.readLine();
             
            /*while ((line = reader.readLine()) != null) {
                log.debug(line);
                //System.out.println(line);
            }*/
            
            writer.close();
            reader.close();
            output.close();
 
        } catch (UnknownHostException ex) {
 
            log.error("Server not found: " + ex.getMessage());
 
        } catch (IOException ex) {
 
            log.error("I/O error: " + ex.getMessage());
        }
        
        return line;
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
                log.error("EX255 " + e);
                 
                ret =false;
            }
            return ret;
       }
            
}
