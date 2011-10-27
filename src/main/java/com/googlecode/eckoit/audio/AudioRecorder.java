/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.googlecode.eckoit.audio;

import java.io.File;
import javax.sound.sampled.LineUnavailableException;

/**
 *
 * @author ryan
 */
public interface AudioRecorder {

    boolean isRecording();

    void startRecording(String recordingID, String mixer, float gain) throws LineUnavailableException;

    /**
     * Stops the recording.
     *
     * @return the length the recording in seconds.
     */
    long stopRecording();

}
