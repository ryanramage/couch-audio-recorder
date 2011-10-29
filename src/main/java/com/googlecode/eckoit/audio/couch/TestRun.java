/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.googlecode.eckoit.audio.couch;

import com.googlecode.eckoit.audio.SplitAudioRecorderConfiguration;
import com.googlecode.eckoit.audio.SplitAudioRecorderManager;
import java.io.File;
import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbConnector;
import org.ektorp.impl.StdCouchDbInstance;

/**
 *
 * @author ryan
 */
public class TestRun {


     public static void main(String args[]) {
        try {
            // my machine
            String ffmpeg = "/Applications/eckoit/lib/ffmpeg";
            File file = new File("target");
            SplitAudioRecorderConfiguration config = new SplitAudioRecorderConfiguration();
            config.setStream(true);
            SplitAudioRecorderManager recorder = new SplitAudioRecorderManager(ffmpeg, file, config);
            
            
            HttpClient client = new StdHttpClient.Builder().url("http://localhost:5983").build();
            CouchDbInstance db = new StdCouchDbInstance(client);
            CouchDbConnector connector = new StdCouchDbConnector("dbg", db);

            CouchDBRecording dbRecorder = new CouchDBRecording(connector);
            
            dbRecorder.watch();




        } catch (MalformedURLException ex) {
            Logger.getLogger(TestRun.class.getName()).log(Level.SEVERE, null, ex);
        }


    }

}
