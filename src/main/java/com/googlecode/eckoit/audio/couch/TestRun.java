/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.googlecode.eckoit.audio.couch;

import com.github.couchapptakeout.events.ExitApplicationMessage;
import com.googlecode.eckoit.audio.SplitAudioRecorderConfiguration;
import com.googlecode.eckoit.audio.SplitAudioRecorderManager;
import com.googlecode.eckoit.util.FFMpegSetterUpper;
import java.io.File;
import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bushe.swing.event.EventBus;
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
            HttpClient client = new StdHttpClient.Builder().url("http://localhost:5983").build();
            CouchDbInstance db = new StdCouchDbInstance(client);
            CouchDbConnector connector = new StdCouchDbConnector("dbg", db);
           
            File file = new File("target");

            FFMpegSetterUpper fu = new FFMpegSetterUpper();
            String ffmpeg = fu.ffmpegLocation(file, connector, "_design/couchaudiorecorder");
            

            SplitAudioRecorderConfiguration config = new SplitAudioRecorderConfiguration();
            config.setStream(true);
            SplitAudioRecorderManager recorder = new SplitAudioRecorderManager(ffmpeg, file, config);
                       
            final CouchDBRecording dbRecorder = new CouchDBRecording(connector);


            new Thread(new Runnable() {
                @Override
                public void run() {
                    dbRecorder.watch();
                }
            }).start();


            
// added to ensure no kids left behind
        Runtime.getRuntime().addShutdownHook(new Thread() {
           @Override
           public void run() {
             EventBus.publish(new ExitApplicationMessage());
           }
          });



        } catch (Exception ex) {
            Logger.getLogger(TestRun.class.getName()).log(Level.SEVERE, null, ex);
        }


    }

}
