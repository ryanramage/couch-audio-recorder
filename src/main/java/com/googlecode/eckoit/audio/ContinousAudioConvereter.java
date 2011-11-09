/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.googlecode.eckoit.audio;


import com.github.couchapptakeout.events.ExitApplicationMessage;
import com.googlecode.eckoit.events.ConversionFinishedEvent;


import com.googlecode.eckoit.events.RecordingSplitEvent;
import com.googlecode.eckoit.events.StreamReadyEvent;
import com.googlecode.eckoit.util.Slugger;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bushe.swing.event.EventBus;
import org.bushe.swing.event.EventSubscriber;

/**
 * Scans a source directory for any new files, converts them and copies them into the
 * destination directory
 * @author ryan
 */
public class ContinousAudioConvereter extends Thread implements  EventSubscriber<RecordingSplitEvent>  {





    private File destDir;
    private boolean running = true;
    private LinkedBlockingQueue<RecordingSplitEvent> completedRecordings;

    private String ffmpegcmd;
    private SplitAudioRecorderConfiguration config;


    public ContinousAudioConvereter(String ffmpegcmd, File destDir, SplitAudioRecorderConfiguration config) {

        this.destDir = destDir;
        this.ffmpegcmd = ffmpegcmd;
        this.config = config;
        this.completedRecordings = new LinkedBlockingQueue<RecordingSplitEvent>();
        
        EventBus.subscribeStrongly(RecordingSplitEvent.class, this);
        EventBus.subscribeStrongly(ExitApplicationMessage.class, new EventSubscriber<ExitApplicationMessage>() {
            @Override
            public void onEvent(ExitApplicationMessage t) {
                running = false;
                completedRecordings.add(new RecordingSplitEvent(null, null, 0));
            }
        });

        
    }

    @Override
    public void run() {

        while (running) {
            try {
                RecordingSplitEvent recordingFinished = completedRecordings.take();
                File wav = recordingFinished.getFinishedFile();
                if (wav != null) {
                    if (tooFresh(wav)) {
                        completedRecordings.add(recordingFinished);
                    } else {
                        Logger.getLogger(ContinousAudioConvereter.class.getName()).log(Level.INFO, "converting wav: " + wav.getAbsolutePath());
                        doConversion(recordingFinished.getRecordingID(), wav, recordingFinished.getStartTime());

                    }
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(ContinousAudioConvereter.class.getName()).log(Level.SEVERE, null, ex);
            }
            sleep();
        }
        System.out.println("Continous Audio Converter Shutdown");
        
        
    }

    protected void doConversion(String recordingID, File wav, long sectionStartTime) {
        File mp3 = null;
        File ogg = null;
        try {
            mp3 = convertToMP3(recordingID, wav);
            if (config.isStream()) {
                File ts = convertToTs(recordingID, mp3);

                StreamReadyEvent sre = new StreamReadyEvent(mp3, "video/MP2T", sectionStartTime);
                sre.setAvailableToStream(ts);
                sre.setStreamDuration((int) (SplitAudioRecorder.getSplitTime() / 1000));
                sre.setSegmentCount(getSegmentCount(wav));
                EventBus.publish(sre);
            }
            ogg = convertToOGG(recordingID, wav);
        } catch (Exception ex) {
            Logger.getLogger(ContinousAudioConvereter.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (ogg != null && ogg.exists() && mp3 != null && mp3.exists()) {

            ConversionFinishedEvent finished = new ConversionFinishedEvent(wav);            
            
            
            EventBus.publish(finished);
        }
    }

    /**
     * If the file is too fresh, we dont want to begin conversion
     * @param wav
     * @return
     */
    private boolean tooFresh(File wav) {
        long now = System.currentTimeMillis();
        long timestamp = wav.lastModified();
        if ((now - timestamp) < 2000) return true;
        else return false;
    }


    private synchronized File convertToMP3(String recordingID, File wav) throws InterruptedException, IOException {
        File mp3 = getFileForDocument(recordingID, wav, ".mp3");
        if (mp3.exists()) return mp3;


        long bitrate = config.getMp3Bitrate();
        long frequency = config.getMp3Frequency();

        File mp3Temp = getFileForDocument(recordingID, wav, ".mp3.tmp");
        FFMpegConverter converter = new FFMpegConverter(getFfmpegcmd(), FFMpegConverter.ENCODER_MP3);
        //System.out.println("Converting to mp3");
        converter.convert(wav, bitrate, frequency, mp3Temp, true);
        //System.out.println("Renaming");
        mp3Temp.renameTo(mp3);
        return mp3;
    }
    private synchronized File convertToOGG(String recordingID, File wav) throws InterruptedException, IOException {
        File ogg = getFileForDocument(recordingID, wav, ".ogg");
        if (ogg.exists()) return ogg;


        long bitrate = config.getOggBitrate();
        long frequency = config.getOggFrequency();
        
        File oggTemp = getFileForDocument(recordingID, wav, "tmp.ogg");
        FFMpegConverter converter = new FFMpegConverter(getFfmpegcmd(), FFMpegConverter.ENCODER_VORBIS);

        converter.convert(wav, bitrate, frequency, oggTemp, true);
        oggTemp.renameTo(ogg);
        return ogg;
    }
    private File convertToTs(String recordingID, File mp3) throws InterruptedException, IOException {
        FFMpegConverter converter = new FFMpegConverter(getFfmpegcmd(), FFMpegConverter.ENCODER_MP3);
        File ts = getFileForDocument(recordingID, mp3, ".ts");
        converter.makeTS(mp3, ts);
        return ts;
    }
    private File getFileForDocument(String recordingID, File wav, String suffix) {
        // just do sibblings
        String count = getSegmentCount(wav);


        String safeId = Slugger.generateSlug(recordingID);

        File parent = new File(destDir, safeId);
        parent.mkdirs();

        
        
        return new File(parent, count + suffix);
    }

    private String getSegmentCount(File wav) {
        return wav.getName().substring(0, wav.getName().lastIndexOf('.'));
    }



    private void sleep() {
        try {
            sleep(2000);
        } catch (InterruptedException ex) {
            Logger.getLogger(ContinousAudioConvereter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void onEvent(RecordingSplitEvent recordingSplit) {
        completedRecordings.add(recordingSplit);
    }

    /**
     * @param config the config to set
     */
    public void setConfig(SplitAudioRecorderConfiguration config) {
        this.config = config;
    }

    /**
     * @return the ffmpegcmd
     */
    public String getFfmpegcmd() {
        return ffmpegcmd;
    }



}
