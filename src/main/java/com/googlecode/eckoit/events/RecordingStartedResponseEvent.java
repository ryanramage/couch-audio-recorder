/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.googlecode.eckoit.events;

/**
 *
 * @author ryan
 */
public class RecordingStartedResponseEvent {

    private String recordingID;

    public RecordingStartedResponseEvent(String recordingID) {
        this.recordingID = recordingID;
    }

    /**
     * @return the recordingID
     */
    public String getRecordingID() {
        return recordingID;
    }

}
