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
public class RecordingCompleteEvent {

    private File[] recordings;


    public RecordingCompleteEvent(File[] recordings) {
        this.recordings = recordings;
    }

    /**
     * @return the recordings
     */
    public File[] getRecordings() {
        return recordings;
    }



}
