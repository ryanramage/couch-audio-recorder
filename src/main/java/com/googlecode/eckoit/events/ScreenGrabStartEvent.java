/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.googlecode.eckoit.events;

import java.io.File;

/**
 *
 * @author ryan
 */
public class ScreenGrabStartEvent {
    private File storageDir;
    private long intervalInMilli;
    private boolean presentationMode = false;
    
    public ScreenGrabStartEvent(File storageDir) {
        this.storageDir = storageDir;
        intervalInMilli = 5000; // default to 5 seconds
    }
    public ScreenGrabStartEvent(File storageDir, long intervalInMilli) {
        this.storageDir = storageDir;
        this.intervalInMilli = intervalInMilli; 
    }
    public ScreenGrabStartEvent(File storageDir, boolean presentationMode) {
        this.storageDir = storageDir;
        this.presentationMode = presentationMode;
    }
    public long getIntervalInMilli(){
        return this.intervalInMilli;
    }
    
    public File getStorageDir() {
        return storageDir;
    }

    public boolean getPresentationMode() {
        return presentationMode;
    }

}
