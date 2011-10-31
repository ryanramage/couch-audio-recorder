/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.googlecode.eckoit.audio.couch;

import com.github.couchapptakeout.events.ExitApplicationMessage;
import com.googlecode.eckoit.audio.SplitAudioRecorder;
import com.googlecode.eckoit.audio.SplitAudioRecorderConfiguration;
import com.googlecode.eckoit.audio.SplitAudioRecorderManager;
import com.googlecode.eckoit.events.ConversionFinishedEvent;
import com.googlecode.eckoit.events.PostProcessingStartedEvent;
import com.googlecode.eckoit.events.RecordingCompleteEvent;
import com.googlecode.eckoit.events.RecordingSplitEvent;
import com.googlecode.eckoit.events.RecordingStartClickedEvent;
import com.googlecode.eckoit.events.RecordingStartedResponseEvent;
import com.googlecode.eckoit.events.RecordingStopClickedEvent;
import com.googlecode.eckoit.events.RecordingStoppedResponseEvent;
import com.googlecode.eckoit.events.StreamReadyEvent;
import com.googlecode.eckoit.events.UploadingFinishedEvent;
import com.googlecode.eckoit.events.UploadingStartedEvent;
import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bushe.swing.event.EventBus;
import org.bushe.swing.event.EventSubscriber;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.ektorp.AttachmentInputStream;
import org.ektorp.CouchDbConnector;
import org.ektorp.changes.ChangesCommand;
import org.ektorp.changes.ChangesFeed;
import org.ektorp.changes.DocumentChange;

/**
 *
 * @author ryan
 */
public class CouchDBRecording {

    private CouchDbConnector connector;
    private ChangesFeed feed;
    private String recordingDocIdPrefex = "recording-";
    private String recorderUUID;

    private ObjectNode currentRecordingDoc;
    private boolean running = true;
    private boolean streamAudio = true;

    public CouchDBRecording(final CouchDbConnector connector) {
        this.connector = connector;
        this.recorderUUID = UUID.randomUUID().toString();

        EventBus.subscribeStrongly(ExitApplicationMessage.class, new EventSubscriber<ExitApplicationMessage>() {
            @Override
            public void onEvent(ExitApplicationMessage t) {
                running = false;
                feed.cancel();
            }
        });
        EventBus.subscribeStrongly(RecordingStartedResponseEvent.class, new EventSubscriber<RecordingStartedResponseEvent>() {
            @Override
            public void onEvent(RecordingStartedResponseEvent t) {

                // get the current doc
                currentRecordingDoc = connector.get(ObjectNode.class, t.getRecordingID());


                ObjectNode recordingState = getRecordingState(currentRecordingDoc);
                recordingState.put("startComplete", new Date().getTime());
                connector.update(currentRecordingDoc);
            }
        });

        EventBus.subscribeStrongly(RecordingStoppedResponseEvent.class, new EventSubscriber<RecordingStoppedResponseEvent>() {
            @Override
            public void onEvent(RecordingStoppedResponseEvent t) {
                ObjectNode recordingState = getRecordingState(currentRecordingDoc);
                recordingState.put("stopComplete", new Date().getTime());
                connector.update(currentRecordingDoc);
            }
        });

        EventBus.subscribeStrongly(PostProcessingStartedEvent.class, new EventSubscriber<PostProcessingStartedEvent>() {
            @Override
            public void onEvent(PostProcessingStartedEvent t) {
                ObjectNode recordingState = getRecordingState(currentRecordingDoc);
                recordingState.put("postProcessingStarted", new Date().getTime());
                connector.update(currentRecordingDoc);
            }
        });
        EventBus.subscribeStrongly(RecordingCompleteEvent.class, new EventSubscriber<RecordingCompleteEvent>() {
            @Override
            public void onEvent(RecordingCompleteEvent t) {
                ObjectNode recordingState = getRecordingState(currentRecordingDoc);
                recordingState.put("recordingComplete", new Date().getTime());
                connector.update(currentRecordingDoc);

                EventBus.publish(new UploadingStartedEvent());
                String id = currentRecordingDoc.get("_id").getTextValue();
                String revision = currentRecordingDoc.get("_rev").getTextValue();
                try {
                    // mp3
                    FileInputStream fis = new FileInputStream(t.getRecordings()[0]);
                    AttachmentInputStream ais = new AttachmentInputStream("complete.mp3",fis, "audio/mp3");
                    revision = connector.createAttachment(id, revision, ais);
                    ais.close();
                    fis.close();
                } catch(Exception e) {

                }
                try {
                    if (t.getRecordings().length == 2){
                        // ogg
                        FileInputStream fis = new FileInputStream(t.getRecordings()[1]);
                        AttachmentInputStream ais = new AttachmentInputStream("complete.ogg",fis, "audio/ogg");
                        revision = connector.createAttachment(id, revision, ais);
                        ais.close();
                        fis.close();
                    }
                } catch (Exception e) {

                }
                EventBus.publish(new UploadingFinishedEvent());
                currentRecordingDoc = null;
            }
        });

        EventBus.subscribeStrongly(StreamReadyEvent.class, new EventSubscriber<StreamReadyEvent>() {
            @Override
            public void onEvent(StreamReadyEvent t) {
                System.out.println("Streaming");
                if (streamAudio) {
                    try {


                        System.out.println("out");
                        // create a doc
                        ObjectMapper map = new ObjectMapper();
                        ObjectNode node = map.createObjectNode();
                        node.put("type", "recording-segment");
                        node.put("recording", currentRecordingDoc.get("_id").getTextValue());
                        node.put("startTime", t.getStartTime());
                        connector.create(node);

                        // attach
                        FileInputStream fis = new FileInputStream(t.getAvailableToStream());
                        String name = "fileSequence" + t.getSegmentCount() + ".ts";
                        AttachmentInputStream ais = new AttachmentInputStream(name,fis, t.getContentType());

                        String id = node.get("_id").getTextValue();
                        String rev = node.get("_rev").getTextValue();
                        rev =  connector.createAttachment(id, rev, ais);
                        ais.close();
                    fis.close();
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        });


    }



    protected ObjectNode getRecordingState(ObjectNode recordingDoc) {
        return (ObjectNode)recordingDoc.get("recordingState");
    }
    
    public void watch() {
        ChangesCommand cmd = new ChangesCommand.Builder().filter("couchaudiorecorder/recordings").build();
        System.out.println("CouchDB recorder starting");
        while (running) {
            try {
                feed = connector.changesFeed(cmd);

                while (feed.isAlive() && running) {
                    DocumentChange change = feed.next();
                    System.out.println("Got a change!");
                    String docId = change.getId();
                    System.out.println(docId);
                    ObjectNode doc = loadRecordingDoc(docId);
                    if (doc != null) {
                        System.out.println("has doc");
                        processRecordingDoc(doc);
                    }
                }
            } catch (Exception e) {
                try {
                    // this catches any interrupted exceptions
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(CouchDBRecording.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        System.out.println("CouchDB recorder finished");

    }

    public void stop() {
        System.out.println("Its over for you");
        running = false;
        feed.cancel();
    }


    protected void processRecordingDoc(ObjectNode doc) {
        final RecordingState state = detectState(doc);
        System.out.println("state: " + state.name());
        if (state == RecordingState.RECORDER_ASKED) {
            System.out.println("Recorder asked");
            if (currentRecordingDoc == null) {                
                 System.out.println("we are programmed to receive");
                // we are free...lets volenteer
                ObjectNode recordingState = getRecordingState(doc);
                recordingState.put("recorderAvailable", recorderUUID);
                connector.update(doc);
            }
            return;
        }
        if (state == RecordingState.RECORDER_AVAILABLE || state == RecordingState.UNKNOWN) {
            // skipp
            System.out.println("Skip");
            if (currentRecordingDoc == null) {
                System.out.println("Recording doc empty");
            } else {
                System.out.println("we are committed");
            }
        }
        else if(isOurRecorder(doc)) {

            if (state == RecordingState.RECORDING_COMPLETE) {
                currentRecordingDoc = null; // make us available
                return;
            }
            currentRecordingDoc = doc; // keeps the rev up
            if (state == RecordingState.START_ASKED) {
                EventBus.publish(new RecordingStartClickedEvent(doc.get("_id").getTextValue()));
            }
            if (state == RecordingState.STOP_ASKED) {
                EventBus.publish(new RecordingStopClickedEvent());
            }
        }

    }

    protected boolean isOurRecorder(ObjectNode doc) {
        ObjectNode recordingState = getRecordingState(doc);
        if (recordingState != null) {
            JsonNode node = recordingState.get("recorderAvailable");
            if (node != null) {
                String uuid = node.getTextValue();
                if (recorderUUID.equals(uuid)) {
                    return true;
                }
            }
        }
        return false;
    }



    protected ObjectNode loadRecordingDoc(String docID) {
        if (docID.startsWith(recordingDocIdPrefex)) {
            ObjectNode doc = connector.get(ObjectNode.class, docID);
            System.out.println(doc);
            return doc;
        }
        return null;
    }


    protected RecordingState detectState(ObjectNode doc) {
        JsonNode recordingState = doc.get("recordingState");
        if (recordingState != null) {                  
            if (recordingState.get("recordingComplete") != null) {
                return RecordingState.RECORDING_COMPLETE;
            }
            if (recordingState.get("postProcessingStarted") != null) {
                return RecordingState.POST_PROCESSING_STARTED;
            }
            if (recordingState.get("stopComplete") != null) {
                return RecordingState.STOP_COMPLETE;
            }
            if (recordingState.get("stopAsked") != null) {
                return RecordingState.STOP_ASKED;
            }
            if (recordingState.get("startComplete") != null) {
                return RecordingState.START_COMPLETE;
            }
            if (recordingState.get("startAsked") != null) {
                return RecordingState.START_ASKED;
            }
            if (recordingState.get("recorderAvailable") != null) {
                return RecordingState.RECORDER_AVAILABLE;
            }
            if (recordingState.get("recorderAsked") != null) {
                return RecordingState.RECORDER_ASKED;
            }

        }
        return RecordingState.UNKNOWN;
    }


    /**
     * @return the recordingDocIdPrefex
     */
    public String getRecordingDocIdPrefex() {
        return recordingDocIdPrefex;
    }

    /**
     * @param recordingDocIdPrefex the recordingDocIdPrefex to set
     */
    public void setRecordingDocIdPrefex(String recordingDocIdPrefex) {
        this.recordingDocIdPrefex = recordingDocIdPrefex;
    }


    public enum RecordingState {
        UNKNOWN, 
        RECORDER_ASKED, RECORDER_AVAILABLE,
        START_ASKED, START_COMPLETE,
        STOP_ASKED, STOP_COMPLETE,
        POST_PROCESSING_STARTED, RECORDING_COMPLETE
    }







}
