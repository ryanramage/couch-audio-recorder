/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.googlecode.eckoit.events;

/**
 *
 * @author ryan
 */
public class RecordingStartClickedEvent {

    private String recordingID;
    
    public RecordingStartClickedEvent(String recordingID) {
        this.recordingID = recordingID;
    }

    /**
     * @return the recordingID
     */
    public String getRecordingID() {
        return recordingID;
    }

}
