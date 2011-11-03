/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.googlecode.eckoit.events;

/**
 *
 * Dont you just love long, clear names?
 *
 * @author ryan
 */
public class RecorderJustStartedWithARecordingDocRequested {

    private String recordingDocId;



    public RecorderJustStartedWithARecordingDocRequested(String recordingDocId) {
        this.recordingDocId = recordingDocId;
    }

    /**
     * @return the recordingDocId
     */
    public String getRecordingDocId() {
        return recordingDocId;
    }



}
