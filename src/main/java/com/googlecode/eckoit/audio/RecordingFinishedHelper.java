/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.googlecode.eckoit.audio;


import com.googlecode.eckoit.util.Slugger;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author ryan
 */
public class RecordingFinishedHelper {

    File recordingInProgressDir;
    File recordingCompleteDir;
    String ffmpegComand;
    


    public RecordingFinishedHelper(File recordingInProgressDir, File recordingCompleteDir, String ffmpeg) {
        this.recordingInProgressDir = recordingInProgressDir;
        this.recordingCompleteDir = recordingCompleteDir;
        this.ffmpegComand = ffmpeg;

    }

    public File[] recordingFinished(String recordingId) throws FileNotFoundException, IOException {

        String safeId = Slugger.generateSlug(recordingId);
        File parent = new File(recordingInProgressDir, safeId);



        File[] mp3s = findFiles(parent, ".mp3");
        List<File> sortedMp3s = sortFilesNumerically(mp3s);
        File finalMp3_bad = new File(recordingInProgressDir, safeId + ".mp3.tmp");
        mergeFiles(finalMp3_bad, sortedMp3s);

        File finalMp3_good = new File(recordingCompleteDir, safeId + ".mp3");
        try {
            polishMp3(finalMp3_bad, finalMp3_good);
        } catch (Exception ex) {
            Logger.getLogger(RecordingFinishedHelper.class.getName()).log(Level.SEVERE, null, ex);
            finalMp3_good = finalMp3_bad; // bad is the new good, for now
        }


        File[] oggs = findFiles(parent, ".ogg");
        List<File> sortedOggs = sortFilesNumerically(oggs);
        File finalOgg = new File(recordingCompleteDir, safeId + ".ogg");
        mergeFiles(finalOgg, sortedOggs);

        return new File[] {finalMp3_good, finalOgg};
        
    }



    public List<File> findScreenShots(String recordingId) {
        String safeId = Slugger.generateSlug(recordingId);
        File parent = new File(recordingInProgressDir, safeId);
        File dir = new File(parent, safeId);
        File[] screenShots = findFiles(dir, ".png");
        return (List<File>) Arrays.asList(screenShots);
    }

    protected List<File> sortFilesNumerically(File[] files) {
        List<File> sorted = Arrays.asList(files);
        Comparator<File> nameSort = new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                long o1_name = getFilenameAsInt(o1);
                long o2_name = getFilenameAsInt(o2);
                return (int)(o1_name - o2_name);
            }
        };
        Collections.sort(sorted, nameSort);
        return sorted;
    }

    protected long getFilenameAsInt(File file) {
        String filename = file.getName();
        return Long.parseLong(filename.substring(0, filename.lastIndexOf(".")));
    }


    protected File[] findFiles(File dir, final String suffix) {
        return dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (name.endsWith(suffix)) return true;
                return false;
            }
        });
    }

    public void mergeFiles(File mergedFile, List<File> audios) throws FileNotFoundException, IOException {

        FileOutputStream fos = new FileOutputStream(mergedFile);
        // we assume they are ordered
        for (int i=0; i < audios.size(); i++) {
            File audio = audios.get(i);
            FileInputStream in = new FileInputStream(audio);
            IOUtils.copy(in, fos);
            in.close();
        }
        fos.close();


        // fix the file


    }

    private void polishMp3(File finalMp3_bad, File finalMp3_good) throws InterruptedException, IOException {
        FFMpegConverter converter = new FFMpegConverter(ffmpegComand, FFMpegConverter.ENCODER_MP3);
        converter.fixMP3(finalMp3_bad, finalMp3_good);
    }


}
