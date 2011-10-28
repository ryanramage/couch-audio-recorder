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
public class ConversionFinishedEvent {
    private File finishedFile;
    private File availableToStream;
    private int streamDuration;
    private String segmentCount;


    public ConversionFinishedEvent(File finishedFile) {
        this.finishedFile = finishedFile;
    }

    public File getFinishedFile() {
        return finishedFile;
    }

    /**
     * @return the availableToStream
     */
    public File getAvailableToStream() {
        return availableToStream;
    }

    /**
     * @param availableToStream the availableToStream to set
     */
    public void setAvailableToStream(File availableToStream) {
        this.availableToStream = availableToStream;
    }

    /**
     * @return the streamDuration
     */
    public int getStreamDuration() {
        return streamDuration;
    }

    /**
     * @param streamDuration the streamDuration to set
     */
    public void setStreamDuration(int streamDuration) {
        this.streamDuration = streamDuration;
    }

    /**
     * @return the segmentCount
     */
    public String getSegmentCount() {
        return segmentCount;
    }

    /**
     * @param segmentCount the segmentCount to set
     */
    public void setSegmentCount(String segmentCount) {
        this.segmentCount = segmentCount;
    }


}
