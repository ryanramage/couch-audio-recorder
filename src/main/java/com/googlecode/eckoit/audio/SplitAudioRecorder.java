/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.googlecode.eckoit.audio;

import com.googlecode.eckoit.events.RecordingSplitEvent;
import com.googlecode.eckoit.util.Slugger;
import java.io.IOException;
import java.io.File;

import java.util.Timer;
import java.util.TimerTask;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.Mixer;
import org.bushe.swing.event.EventBus;

/**
 *
 * @author ryan
 */
public class SplitAudioRecorder implements AudioRecorder {
    private static long splitTime = 10000L; // 10 seconds
    private static SplitAudioRecorder singletonObject;

    private boolean isRecording = false;
    private long recordingStart;
    private TargetDataLine m_line;
    private AudioFileFormat.Type m_targetType;
    private SplitableAudioInputStream m_audioInputStream;
    private File m_outputFile;
    private Timer timer;
    private int sectionCount;
    private static File root;
    private File recordingFolder;
    private File section;

    private static SplitAudioRecorderConfiguration config = new SplitAudioRecorderConfiguration();


    private String recordingID;


    public static void setSplitTime(long splitTime) {
        SplitAudioRecorder.splitTime = splitTime;
    }

    public static long getSplitTime() {
        return splitTime;
    }

    public static void setConfig(SplitAudioRecorderConfiguration configSetup) {
        config = configSetup;
    }


    public static void setRoot(File dir) {
        root = dir;
        if (!root.isDirectory()) {
            throw new RuntimeException("A directory is expected");
        }
    }


    @Override
    public synchronized void startRecording(String recordingID, final String mixer, float gain) throws LineUnavailableException {


        if (isRecording || m_line != null || (m_line != null && m_line.isOpen())) {
            throw new LineUnavailableException();
        }


        String safeFileName = Slugger.generateSlug(recordingID);

        recordingFolder = new File(root, safeFileName);
        recordingFolder.mkdirs();

        sectionCount = 0;
        isRecording = true;
        recordingStart = System.currentTimeMillis();
        this.recordingID = recordingID;

        section = nextSectionFile();
        

        AudioFormat audioFormat = new AudioFormat(config.getWavSampleRate(), config.getWavSampleSize(), 1, true, true);
        audioFormat = SimpleAudioRecorder.getBestAudioFormat(audioFormat, mixer);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
        Mixer selectedMixer = SimpleAudioRecorder.getSelectedMixer(mixer);
        if (selectedMixer == null) {

            m_line = (TargetDataLine) AudioSystem.getLine(info);
            selectedMixer = AudioSystem.getMixer(null);

        } else {
            m_line = (TargetDataLine) selectedMixer.getLine(info);
        }
        m_line.open(audioFormat);

        //FloatControl fc = (FloatControl) selectedMixer.getControl(FloatControl.Type.MASTER_GAIN);
        //System.out.println("Master Gain min: " + fc.getMinimum());
        //System.out.println("Master Gain min: " + fc.getMaximum());
        //ystem.out.println("Master Gain cur: " + fc.getValue());
        //fc.setValue(MIN_PRIORITY);
        AudioFileFormat.Type targetType = AudioFileFormat.Type.WAVE;
        m_audioInputStream = new SplitableAudioInputStream(new AudioInputStream(m_line));
        m_targetType = targetType;
         m_outputFile = section;
        new Recorder().start();

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                sectionCount++;
                File oldsection = section;
                section = nextSectionFile(); 
                split(oldsection, section);
 
            }
        }, splitTime, splitTime);
    }

    private File nextSectionFile() {
        return new File(recordingFolder,  sectionCount + ".wav");
    }


    private void split(File oldsection, File newsectiom) {
        m_outputFile = newsectiom;
        m_audioInputStream = m_audioInputStream.clone();
        new Recorder().start();

        long splitStartTime = System.currentTimeMillis() - splitTime;

        EventBus.publish(new RecordingSplitEvent(oldsection, recordingID, splitStartTime));
    }

    /** Stops the recording.
     */
    @Override
    public synchronized long stopRecording() {
        if (isRecording) {
            m_line.stop();
            m_line.close();
            m_line = null;
            timer.cancel();
            isRecording = false;
            long now = System.currentTimeMillis();
            // final section
            long splitStartTime = now - splitTime;
            RecordingSplitEvent rse = new RecordingSplitEvent(section, recordingID, splitStartTime);
            rse.setIsFinal(true);
            EventBus.publish(rse);
            return (now - recordingStart)/1000;
        }
        return -1;
    }

    @Override
    public boolean isRecording() {
        return isRecording;
    }







    private SplitAudioRecorder() {

    }
    public static synchronized SplitAudioRecorder getSingletonObject() {
            if (singletonObject == null) {
                    singletonObject = new SplitAudioRecorder();
            }
            return singletonObject;
    }


    private class Recorder extends Thread {

        @Override
        public void run() {
            try {
                AudioSystem.write(m_audioInputStream, m_targetType, m_outputFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /** Starts the recording.
        To accomplish this, (i) the line is started and (ii) the
        thread is started.
         */
        @Override
        public void start() {
            m_line.start();
            super.start();
        }
    }
    
    @Override
    public Object clone() throws CloneNotSupportedException {
            throw new CloneNotSupportedException();
    }
}
