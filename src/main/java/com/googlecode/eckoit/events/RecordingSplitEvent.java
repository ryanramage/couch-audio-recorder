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
public class RecordingSplitEvent {
    private File finishedFile;
    private boolean isFinal = false;
    private String recordingID;

    public RecordingSplitEvent(File finishedFile, String recordingID) {
        this.recordingID = recordingID;
        this.finishedFile = finishedFile;
    }

    public File getFinishedFile() {
        return finishedFile;
    }

    /**
     * @param finishedFile the finishedFile to set
     */
    public void setFinishedFile(File finishedFile) {
        this.finishedFile = finishedFile;
    }

    /**
     * @return the isFinal
     */
    public boolean isIsFinal() {
        return isFinal;
    }

    /**
     * @param isFinal the isFinal to set
     */
    public void setIsFinal(boolean isFinal) {
        this.isFinal = isFinal;
    }

    /**
     * @return the recordingID
     */
    public String getRecordingID() {
        return recordingID;
    }

    /**
     * @param recordingID the recordingID to set
     */
    public void setRecordingID(String recordingID) {
        this.recordingID = recordingID;
    }

}
