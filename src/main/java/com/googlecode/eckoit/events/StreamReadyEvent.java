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
public class StreamReadyEvent extends ConversionFinishedEvent {


    private String contentType;
    private long startTime;


    public StreamReadyEvent(File finishedFile, String contentType, long startTime) {
        super(finishedFile);
        this.contentType = contentType;
        this.startTime = startTime;
    }

    /**
     * @return the contentType
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * @return the startTime
     */
    public long getStartTime() {
        return startTime;
    }

}
