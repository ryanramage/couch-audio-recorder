/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.googlecode.eckoit.audio.ui;

import com.github.couchapptakeout.events.ExitApplicationMessage;
import com.googlecode.eckoit.audio.SplitAudioRecorderConfiguration;
import com.googlecode.eckoit.audio.SplitAudioRecorderManager;
import com.googlecode.eckoit.audio.couch.CouchDBRecording;
import com.googlecode.eckoit.events.RecorderJustStartedWithARecordingDocRequested;
import com.googlecode.eckoit.events.RecordingStartedResponseEvent;
import com.googlecode.eckoit.events.RecordingStoppedResponseEvent;
import com.googlecode.eckoit.util.FFMpegSetterUpper;
import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import org.apache.commons.lang.StringUtils;
import org.bushe.swing.event.EventBus;
import org.bushe.swing.event.EventSubscriber;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
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
public class SimpleTrayRecorder {


    private TrayIcon trayIcon;
    private CouchDbConnector connector;
    private File workingDir;
    SplitAudioRecorderManager recorder;
    CouchDBRecording dbRecorder;
    Image normal;
    Image on;
    JFrame controlPanel;

    MenuItem showPanel;


    public SimpleTrayRecorder(CouchDbConnector conn, File working, String designDoc, String userName) {
        this.connector = conn;
        this.workingDir = working;

        normal = createImage("/record_icon_small.png");
        on = createImage("/record_icon_on_small.png");

        SystemTray tray = SystemTray.getSystemTray();
        if (!SystemTray.isSupported()) {
            throw new RuntimeException("Tray Not Supported");
        }
        trayIcon = new TrayIcon(normal, "Couch Audio Recorder");
        final PopupMenu popup = createMenu();

        trayIcon.setPopupMenu(popup);
        try {
            tray.add(trayIcon);
            //registerEvents();
        } catch (AWTException e) {

        }

        FFMpegSetterUpper fu = new FFMpegSetterUpper();
        String ffmpeg = fu.ffmpegLocation(workingDir, connector, "_design/" +  designDoc);


        SplitAudioRecorderConfiguration config = new SplitAudioRecorderConfiguration();
        config.setStream(true);
        recorder = new SplitAudioRecorderManager(ffmpeg, workingDir, config);
        dbRecorder = new CouchDBRecording(connector, designDoc);
        dbRecorder.setUserName(userName);

        // added to ensure no kids left behind
        Runtime.getRuntime().addShutdownHook(new Thread() {
           @Override
           public void run() {
             EventBus.publish(new ExitApplicationMessage());
           }
          });        


        new Thread(new Runnable() {
            @Override
            public void run() {
                dbRecorder.watch();
            }
        }).start();
        watchForRecordingChanges();


        controlPanel = new JFrame();
        SimpleRecordPanel panel = new SimpleRecordPanel();
        controlPanel.getContentPane().setLayout(new BorderLayout());
        controlPanel.getContentPane().add(panel, BorderLayout.CENTER);

        controlPanel.setSize(250, 150);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        controlPanel.setLocation( (screen.width/2) - (250/2) , (screen.height/2) - (150/2) );
        addShowPanelMenuItem();
    }


    protected void watchForRecordingChanges() {
        EventBus.subscribeStrongly(RecordingStartedResponseEvent.class, new EventSubscriber<RecordingStartedResponseEvent>() {
            @Override
            public void onEvent(RecordingStartedResponseEvent t) {
                // change to on icon
                trayIcon.setImage(on);
                // add menu item to get control panel
                
                

            }
        });
        EventBus.subscribeStrongly(RecordingStoppedResponseEvent.class, new EventSubscriber<RecordingStoppedResponseEvent>() {
            @Override
            public void onEvent(RecordingStoppedResponseEvent t) {
                //trayIcon.getPopupMenu().remove(showPanel);
                trayIcon.setImage(normal);
            }
        });

        

    }



    private PopupMenu createMenu() {
        final PopupMenu popup = new PopupMenu("Couch Audio Recorder");
        {
            MenuItem item = new MenuItem("Exit");
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    EventBus.publish(new ExitApplicationMessage());
                    try {
                        Thread.sleep(1000);
                        System.exit(0);
                    } catch (Exception ex) {
                    }
                    
                }
            });
            popup.add(item);

        }
        return popup;
    }


    private void addShowPanelMenuItem() {
        if(showPanel == null) {
            showPanel = new MenuItem("Recording Panel");
            showPanel.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    controlPanel.setVisible(true);
                }
            });
        }
        trayIcon.getPopupMenu().add(showPanel);
    }








    //Obtain the image URL
    protected static Image createImage(String path) {
        URL imageURL = SimpleTrayRecorder.class.getResource(path);

        if (imageURL == null) {
            System.err.println("Resource not found: " + path);
            return null;
        } else {
            return (new ImageIcon(imageURL)).getImage();
        }

    }


     public static void main(String args[]) {
        try {

            System.out.println("There are " + args.length + " args");
            for (String arg : args) {
                System.out.println("Arg: " + arg);
            }

            // the defaults:
            String url = "http://localhost:5983";
            String dbName  = "dbg";            
            String recordingDocId = null;
            String user = null;
            String ddoc = null;

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, false);
            try {
                System.out.println("Setting with json");
                JsonNode node = mapper.readTree(args[0]);
                url = getUrl(node.get("url").getTextValue());
                dbName = node.get("db").getTextValue();
                recordingDocId = node.get("recording").getTextValue();


                try {
                    user = node.get("user").getTextValue();
                } catch(Exception ex) {
                    // no user
                }


                ddoc = node.get("dd").getTextValue();
                System.out.println("Json reading complete");

            } catch(Exception ex) {
                if (args.length == 2) {
                    // clean and easy
                    try {
                        url = getUrl(args[0]);
                        dbName  = getDb(args[0]);
                    } catch (Exception e) {

                    }

                    if (args.length > 1 && StringUtils.isNotEmpty(args[1])) {
                        recordingDocId = args[1];
                    }
                } else if (args.length == 1) {
                    args = args[0].split(" ");
                    try {
                        url = getUrl(args[0]);
                        dbName  = getDb(args[0]);
                    } catch (Exception e) {

                    }

                    if (args.length > 1 && StringUtils.isNotEmpty(args[1])) {
                        recordingDocId = args[1];
                    }
                }
            }





            System.out.println("Url: " + url);
            System.out.println("db: " + dbName);
            System.out.println("doc: " + recordingDocId);
            System.out.println("user: " + user);
            System.out.println("ddoc_name: " + ddoc);



            // my machine
            HttpClient client = new StdHttpClient.Builder().url(url).build();
            CouchDbInstance db = new StdCouchDbInstance(client);
            CouchDbConnector connector = new StdCouchDbConnector(dbName, db);

            File dir = getWorkingDir();
            SimpleTrayRecorder str = new SimpleTrayRecorder(connector, dir, ddoc, user);

            if (recordingDocId != null) {
                System.out.println("There is a recording doc: " + recordingDocId);
                EventBus.publish(new RecorderJustStartedWithARecordingDocRequested(recordingDocId));
            }


        } catch (Exception ex) {
            Logger.getLogger(SimpleTrayRecorder.class.getName()).log(Level.SEVERE, null, ex);
        }


    }



    protected static String getUrl(String str) throws MalformedURLException {
        URL url = new URL(str);
        StringBuilder builder = new StringBuilder();
        builder.append(url.getProtocol())
               .append("://")
               .append(url.getHost());
        if (url.getPort() > 0) {
            builder.append(":").append(url.getPort());
        } else {
            builder.append(":").append(80);
        }


        return builder.toString();

    }


    protected static String getDb(String str) throws MalformedURLException {
        URL url = new URL(str);
        return url.getPath();
    }


    public static File getWorkingDir() {
       String userHome = System.getProperty("user.home");
       File homeDir = new File(userHome, ".couchaudiorecorder");
       if (!homeDir.exists()) {
           homeDir.mkdirs();
       }
       return homeDir;
    }




}
