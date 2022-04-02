/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.nucleusweb.server;

/**
 *
 * @author Dominik
 */
public class CommandApi {

  
    private String alias;
    private String group;
    private String command; 
    private String host; 
    private String description;
    private String module;
    private boolean lock;
    
        /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the alias
     */
    public String getAlias() {
        return alias;
    }

    /**
     * @param alias the alias to set
     */
    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * @return the group
     */
    public String getGroup() {
        return group;
    }

    /**
     * @param group the group to set
     */
    public void setGroup(String group) {
        this.group = group;
    }

    /**
     * @return the command
     */
    public String getCommand() {
        return command;
    }

    /**
     * @param command the command to set
     */
    public void setCommand(String command) {
        this.command = command;
    }

    /**
     * @return the lock
     */
    public boolean isLock() {
        return lock;
    }

    /**
     * @param lock the lock to set
     */
    public void setLock(boolean lock) {
        this.lock = lock;
    }

    /**
     * @return the module
     */
    public String getModule() {
        return module;
    }

    /**
     * @param module the module to set
     */
    public void setModule(String module) {
        this.module = module;
    }

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host the host to set
     */
    public void setHost(String host) {
        this.host = host;
    }

 
   
}
