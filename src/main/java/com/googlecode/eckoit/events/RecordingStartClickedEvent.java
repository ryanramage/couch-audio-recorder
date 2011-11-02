/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.googlecode.eckoit.events;

import com.googlecode.eckoit.audio.SplitAudioRecorderConfiguration;

/**
 *
 * @author ryan
 */
public class RecordingStartClickedEvent {

    private String recordingID;
    private SplitAudioRecorderConfiguration config;
    
    public RecordingStartClickedEvent(String recordingID) {
        this.recordingID = recordingID;
    }

    /**
     * @return the recordingID
     */
    public String getRecordingID() {
        return recordingID;
    }

    /**
     * @return the config
     */
    public SplitAudioRecorderConfiguration getConfig() {
        return config;
    }

    /**
     * @param config the config to set
     */
    public void setConfig(SplitAudioRecorderConfiguration config) {
        this.config = config;
    }

}
