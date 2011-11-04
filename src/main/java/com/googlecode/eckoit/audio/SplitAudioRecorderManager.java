/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.googlecode.eckoit.audio;

import com.googlecode.eckoit.events.ConversionFinishedEvent;
import com.googlecode.eckoit.events.PostProcessingStartedEvent;
import com.googlecode.eckoit.events.RecordingCompleteEvent;
import com.googlecode.eckoit.events.RecordingSplitEvent;
import com.googlecode.eckoit.events.RecordingStartClickedEvent;
import com.googlecode.eckoit.events.RecordingStartedResponseEvent;
import com.googlecode.eckoit.events.RecordingStopClickedEvent;
import com.googlecode.eckoit.events.RecordingStoppedResponseEvent;
import com.googlecode.eckoit.events.StreamReadyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.LineUnavailableException;
import org.bushe.swing.event.EventBus;
import org.bushe.swing.event.EventSubscriber;

/**
 *
 * @author ryan
 */
public class SplitAudioRecorderManager  {



    SplitAudioRecorder recorder =  SplitAudioRecorder.getSingletonObject();
    ContinousAudioConvereter cac;

    private SplitAudioRecorderConfiguration configuration;


    File rootDir;
    File wavDir;
    File intermediateDir;
    File finalDir;
    private String mixer = "default";
    private float gain = 1;
    


    String currentRecordingID;

    RecordingSplitEvent lastFile;



    public SplitAudioRecorderManager(String ffmpeg, File rootDir,  SplitAudioRecorderConfiguration config) {
        this.rootDir = rootDir;
        this.wavDir = mkDirIfNotExist(new File(rootDir, "wav"));
        this.intermediateDir = mkDirIfNotExist(new File(rootDir, "intermediate"));
        this.finalDir = mkDirIfNotExist(new File(rootDir, "final"));
        this.configuration = config;

        recorder.setConfig(config);
        recorder.setRoot(wavDir);

        cac = new ContinousAudioConvereter(ffmpeg, intermediateDir, config);
        cac.start();



        /**
         * INTERNAL RECORDING EVENTS --------------------------------------------------------------------
         */
        EventBus.subscribeStrongly(RecordingSplitEvent.class, new EventSubscriber<RecordingSplitEvent>() {

            @Override
            public void onEvent(RecordingSplitEvent t) {
                File wav = t.getFinishedFile();
                if (t.isIsFinal()) {
                    lastFile = t;
                    EventBus.publish(new PostProcessingStartedEvent());
                }
            }
        });
        
        EventBus.subscribeStrongly(ConversionFinishedEvent.class, new EventSubscriber<ConversionFinishedEvent>() {
            @Override
            public void onEvent(ConversionFinishedEvent t) {
                if (t instanceof StreamReadyEvent) return; // ignore these. 

                File wav = t.getFinishedFile();
                if (lastFile != null && lastFile.getFinishedFile().equals(wav)) {
                    try {
                        File[] results = mergeFiles(lastFile.getRecordingID());
                        EventBus.publish(new RecordingCompleteEvent(results));
                    } catch (FileNotFoundException ex) {
                        Logger.getLogger(SplitAudioRecorderManager.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        Logger.getLogger(SplitAudioRecorderManager.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
        
        /**
         *
         * External Recording Events =-----------------------------------------------
         */

        EventBus.subscribeStrongly(RecordingStartClickedEvent.class, new EventSubscriber<RecordingStartClickedEvent>() {
            @Override
            public void onEvent(RecordingStartClickedEvent t) {
                currentRecordingID = t.getRecordingID();
                try {
                    if (t.getConfig() != null) {
                        configuration = t.getConfig();
                        cac.setConfig(configuration);
                        recorder.setConfig(configuration);
                    }
                    recorder.startRecording(currentRecordingID, mixer, gain);
                    EventBus.publish(new RecordingStartedResponseEvent(currentRecordingID));
                } catch (LineUnavailableException ex) {
                    Logger.getLogger(SplitAudioRecorderManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });

        EventBus.subscribeStrongly(RecordingStopClickedEvent.class, new EventSubscriber<RecordingStopClickedEvent>() {
            @Override
            public void onEvent(RecordingStopClickedEvent t) {
                recorder.stopRecording();
                EventBus.publish(new RecordingStoppedResponseEvent());
            }
        });


    }


    private File mkDirIfNotExist(File dir) {
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }



    protected void complete(File[] results) {
        
    }



    protected File[] mergeFiles(String recordingId) throws FileNotFoundException, IOException {
        RecordingFinishedHelper helper = new RecordingFinishedHelper(intermediateDir, finalDir, cac.getFfmpegcmd());
        return helper.recordingFinished(recordingId);
    }

    /**
     * @return the mixer
     */
    public String getMixer() {
        return mixer;
    }

    /**
     * @param mixer the mixer to set
     */
    public void setMixer(String mixer) {
        this.mixer = mixer;
    }

    /**
     * @return the gain
     */
    public float getGain() {
        return gain;
    }

    /**
     * @param gain the gain to set
     */
    public void setGain(float gain) {
        this.gain = gain;
    }

    /**
     * @param config the config to set
     */
    public void setConfig(SplitAudioRecorderConfiguration config) {
        this.configuration = config;
    }


}
