/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.nucleusweb.server;

import static biz.szydlowski.nucleusweb.server.NucleusServer.allowedConn;
import static biz.szydlowski.nucleusweb.server.NucleusServer.allowedExc;
import static biz.szydlowski.nucleusweb.server.NucleusServer.allowedWebhook;
import biz.szydlowski.utils.MonitorThread;
import biz.szydlowski.utils.RejectedExecutionHandlerImpl;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.net.ServerSocketFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Dominik
 */
public class NucleusRemoteControlServer  extends Thread {
   
    protected int serverPort   = 8080;
    protected ServerSocket serverSocket = null;
    protected boolean isStopped    = false;
    protected Thread runningThread = null;
    private ThreadPoolExecutor executorPool = null; 
    //private  Socket clientSocket = null;
    
    static final Logger logger = LogManager.getLogger(NucleusRemoteControlServer.class);
    
    public  NucleusRemoteControlServer (int port){
        this.serverPort = port;
    }

    @Override
    public void run(){
        synchronized(this){
            this.runningThread = Thread.currentThread();
            this.runningThread.setName("JobberRemoteControlServer");
        }
        try {
            openServerSocket();           
        } catch (Exception e){
            isStopped = true;
            logger.error(e);
            
        } 
        
        RejectedExecutionHandlerImpl rejectionHandler = new RejectedExecutionHandlerImpl();
        
        
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        //creating the ThreadPoolExecutor
        executorPool = new ThreadPoolExecutor(2, 10, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10), threadFactory, rejectionHandler);
        executorPool.allowCoreThreadTimeOut(true);
                     
        
         //start the monitoring thread
        MonitorThread monitor = new MonitorThread(executorPool, 60);
        Thread monitorThread = new Thread(monitor);
        monitorThread.setName("monitorThread-RC");
        monitorThread.start();
        
        while(!isStopped()){ 
            boolean allowedconn=false;
            
            Socket clientSocket = null;
                 
            try {
               clientSocket = this.serverSocket.accept();
              // clientSocket.setKeepAlive(true);
               
               
                String address = clientSocket.getInetAddress().getHostAddress();
                 
                Iterator<String> iteratorweb = allowedConn.iterator();
              
                while(iteratorweb.hasNext()){
                    String obj = iteratorweb.next();
                    if (obj.equals("all")){
                         allowedconn=true;
                         break;
                    } else {
                        if (obj.startsWith("*") || obj.endsWith("*")){
                            //gwiazdka
                            Pattern r = Pattern.compile(obj);
                            if ( r.matcher(address).find()){
                              allowedconn=true;
                              break;
                            } 
                            
                        } else {
                             if (obj.equalsIgnoreCase(address)){
                                 allowedconn=true;
                                 break;
                             }
                        }
                       
                    }
                }  
   
              //A client has connected to this server. Send welcome message
               
            } catch (IOException e) {
                if(isStopped()) {
                    logger.info("Server Stopped.") ;
                    return;
                }
                throw new RuntimeException("Error accepting client connection", e);
            }   
        
             executorPool.execute(new Thread(new RemoteControlRunnable(clientSocket, allowedconn)));
      
         
        }
        logger.info("Server Stopped.") ;
    }


    private synchronized boolean isStopped() {
        return this.isStopped;
    }
   
    public void stopSever() {
        this.isStopped = true;
        try {
            logger.info("Currently active threads: " + Thread.activeCount());
            if (serverSocket!=null) serverSocket.close();
            interruptAll();
            this.serverSocket.close();
        } catch (IOException e) {
            logger.error("Error closing server", e);
        }
    }

    
   private void interruptAll(){ 
      executorPool.shutdownNow();
   }

    private void openServerSocket() {
        try {
             serverSocket = ServerSocketFactory.getDefault().createServerSocket(serverPort,50);
      
            //this.serverSocket = new ServerSocket(this.serverPort);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open port " + this.serverPort, e);
        }
    }

}