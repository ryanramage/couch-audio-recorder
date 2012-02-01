/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.googlecode.eckoit.util;

import com.github.couchapptakeout.events.utils.DefaultUnzipper;
import com.github.couchapptakeout.events.utils.Unzipper;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.SystemUtils;
import org.ektorp.AttachmentInputStream;
import org.ektorp.CouchDbConnector;

/**
 *
 * @author ryan
 */
public class FFMpegSetterUpper {



    /**
     * This will do everything in its power to get a ffmpeg name back.
     *
     * @param workingDir The working dir that will hold a cached copy of ffmpeg
     * @param connector A couchdb that holds binary copies of ffmpeg
     * @param designDocName the design doc that holds the binary copies
     * @return
     * @throws NotImplementedException ffmpeg cant be found anywhere!
     */
    public String ffmpegLocation(File workingDir, CouchDbConnector connector, String designDocName) throws NotImplementedException {
        String localFile = findLocalFFMpeg(workingDir);
        if (localFile != null) return localFile;

        // test system wide
        String system = findFFmepg();
        if (system != null) return system;

        return installFFMpegAndReturnLocation(workingDir, connector, designDocName);

    }


    protected String installFFMpegAndReturnLocation(File workingDir, CouchDbConnector connector, String designDocName) throws NotImplementedException {
        try {
            installLocalffmpg(workingDir, connector, designDocName);
            String localFile = findLocalFFMpeg(workingDir);
            if (localFile != null) return localFile;

            // if we are here, we cant find ffmpeg
            throw new NotImplementedException("FFMpeg can't be found.");

        } catch (IOException ex) {
            Logger.getLogger(FFMpegSetterUpper.class.getName()).log(Level.SEVERE, null, ex);
            throw new NotImplementedException("FFMpeg can't be found.");
        }
    }



    String windowsAttachmentName = "couchaudiorecorder/binaries/ffmpeg-win32.zip";
    String macAttachmentName = "couchaudiorecorder/binaries/ffmpeg-mac64.zip";


    List<String> windowsLocations = Arrays.asList(
        "C:\\Program Files\\Participatory Culture Foundation\\Miro Video Converter\\ffmpeg-bin\\ffmpeg.exe",
        "D:\\Program Files\\Participatory Culture Foundation\\Miro Video Converter\\ffmpeg-bin\\ffmpeg.exe",
        "C:\\Program Files\\FFmpeg for Audacity\\ffmpeg.exe",
        "D:\\Program Files\\FFmpeg for Audacity\\ffmpeg.exe"
    );
    List<String> macLocations = Arrays.asList(
        "/Applications/eckoit/lib/ffmpeg",
        "/Applications/Miro Video Converter.app/Contents/Resources/ffmpeg"
    );
    List<String> linuxLocations = Arrays.asList(
        "/usr/bin/ffmpeg"
    );



    protected String findLocalFFMpeg(File workingDir) {
        System.out.println(workingDir.getAbsolutePath());
        File testFile = null;
         if (SystemUtils.IS_OS_WINDOWS) {
            testFile = new File(workingDir, "ffmpeg.exe");
        }
        else if (SystemUtils.IS_OS_MAC_OSX) {
            testFile = new File(workingDir, "ffmpeg");

        }
        if (testFile == null) return null;
        if (testFile.exists() && testFile.isFile()) {
            return testFile.getAbsolutePath();
        }
        return null;
    }



    protected String findFFmepg() {
        String location = null;
        if (SystemUtils.IS_OS_WINDOWS) {
            location = checkLocations(windowsLocations);
        }
        else if (SystemUtils.IS_OS_MAC_OSX) {
            location = checkLocations(macLocations);

        } else if (SystemUtils.IS_OS_LINUX) {
            location = checkLocations(linuxLocations);
        }
        return location;        
    }


    protected boolean ffmpegCheck(String location) {
        File f = new File(location);
        if (f.exists() && f.isFile()) return true;
        else return false;
    }

    private String checkLocations(List<String> windowsLocations) {
        for(String location : windowsLocations) {
            if (ffmpegCheck(location)) return location;
        }
        return null;
    }




    protected void installLocalffmpg(File storDir, CouchDbConnector connector, String designDoc) throws IOException {
        String attachmentName = null;
        if (SystemUtils.IS_OS_WINDOWS) {
            attachmentName = windowsAttachmentName;
        }
        else if (SystemUtils.IS_OS_MAC_OSX) {
            attachmentName = macAttachmentName;

        } else {
            return;
        }
        File zip = downloadLocalZip(storDir, connector, designDoc, attachmentName);
        unzip(zip, storDir);
        zip.delete();
    }




    protected File downloadLocalZip(File storeDir, CouchDbConnector connector, String designDoc, String attachmentName) throws IOException {
        AttachmentInputStream is = connector.getAttachment(designDoc, attachmentName);
        File zipFile = new File(storeDir,"ffmpeg.zip");
        FileUtils.copyInputStreamToFile(is, zipFile);
        return zipFile;
    }


    protected void unzip(File zipFile, File dir) throws IOException {
        Unzipper uz = new DefaultUnzipper();
        uz.doUnzip(zipFile, dir);
    }



}
