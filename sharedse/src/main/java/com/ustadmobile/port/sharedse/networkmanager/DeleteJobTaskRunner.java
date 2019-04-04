package com.ustadmobile.port.sharedse.networkmanager;

import java.util.HashMap;
import java.util.Hashtable;

/**
 * Abstract class which used to implement platform specific job delete task
 *
 * @author kileha3
 */
public abstract class DeleteJobTaskRunner implements Runnable {

    protected HashMap<String , String>  args;

    protected Object context;

    /**
     * Constructor for testing purpose
     */
    public DeleteJobTaskRunner(){}

    /**
     * Constructor used when creating new instance of a task runner
     * @param context Platform application context
     * @param args arguments to be passed.
     */
    public DeleteJobTaskRunner(Object context, HashMap<String , String> args){
        this.args = args;
        this.context = context;
    }
}
