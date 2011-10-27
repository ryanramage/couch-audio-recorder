/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.googlecode.eckoit.audio;


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
import org.apache.commons.io.IOUtils;

/**
 *
 * @author ryan
 */
public class RecordingFinishedHelper {

    File recordingInProgressDir;
    File recordingCompleteDir;


    public RecordingFinishedHelper(File recordingInProgressDir, File recordingCompleteDir) {
        this.recordingInProgressDir = recordingInProgressDir;
        this.recordingCompleteDir = recordingCompleteDir;
    }

    public File[] recordingFinished(String recordingId) throws FileNotFoundException, IOException {

        File parent = new File(recordingInProgressDir, recordingId);



        File[] mp3s = findFiles(parent, ".mp3");
        List<File> sortedMp3s = sortFilesNumerically(mp3s);
        File finalMp3 = new File(recordingCompleteDir, recordingId + ".mp3");
        mergeFiles(finalMp3, sortedMp3s);

        File[] oggs = findFiles(parent, ".ogg");
        List<File> sortedOggs = sortFilesNumerically(oggs);
        File finalOgg = new File(recordingCompleteDir, recordingId + ".ogg");
        mergeFiles(finalOgg, sortedOggs);

        return new File[] {finalMp3, finalOgg};
        
    }



    public List<File> findScreenShots(String recordingId) {
        File parent = new File(recordingInProgressDir, recordingId);
        File dir = new File(parent, recordingId);
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
    }


}
