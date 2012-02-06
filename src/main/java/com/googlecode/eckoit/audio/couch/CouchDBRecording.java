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
import com.googlecode.eckoit.events.RecorderJustStartedWithARecordingDocRequested;
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
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.bushe.swing.event.EventBus;
import org.bushe.swing.event.EventSubscriber;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
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
    private String recordingDocIdPrefex = "com.eckoit.recording:";
    private String recorderUUID;
    private String userName;

    private ObjectNode currentRecordingDoc;
    private boolean running = true;
    private boolean streamAudio = true;
    private String ddoc;

    public CouchDBRecording(final CouchDbConnector connector, String ddoc) {
        this.connector = connector;
        this.recorderUUID = UUID.randomUUID().toString();
        this.ddoc = ddoc;

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
                try {
                    currentRecordingDoc = connector.get(ObjectNode.class, t.getRecordingID());
                } catch(Exception e) {
                    // if we are here, then we have come from an internal request, we need to create the doc
                    ObjectMapper mapper = new ObjectMapper();
                    currentRecordingDoc = mapper.createObjectNode();
                    currentRecordingDoc.put("_id", t.getRecordingID());
                    ObjectNode recordingState = currentRecordingDoc.putObject("recordingState");
                    long dt = System.currentTimeMillis();
                    recordingState.put("recorderAsked", dt);
                    recordingState.put("recorderAvailable", getRecorderUUID());
                    recordingState.put("startAsked", dt);

                    if (StringUtils.isNotEmpty(userName)) {
                        ObjectNode userCtx = currentRecordingDoc.putObject("userCtx");
                        userCtx.put("name", userName);
                        userCtx.putArray("roles");
                    }



                }

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
                //ObjectNode recordingState = getRecordingState(currentRecordingDoc);
                //recordingState.put("postProcessingStarted", new Date().getTime());
                //connector.update(currentRecordingDoc);
            }
        });
        EventBus.subscribeStrongly(RecordingCompleteEvent.class, new EventSubscriber<RecordingCompleteEvent>() {
            @Override
            public void onEvent(RecordingCompleteEvent t) {

                String id = currentRecordingDoc.get("_id").getTextValue();
                currentRecordingDoc = connector.get(ObjectNode.class, id);
                {
                    ObjectNode recordingState = getRecordingState(currentRecordingDoc);
                    recordingState.put("postProcessingStarted", new Date().getTime());
                    connector.update(currentRecordingDoc);
                }

                EventBus.publish(new UploadingStartedEvent());
                
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


                // lame extra get
                currentRecordingDoc = loadRecordingDoc(id);
                ObjectNode recordingState = getRecordingState(currentRecordingDoc);
                recordingState.put("recordingComplete", new Date().getTime());
                connector.update(currentRecordingDoc);
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
                        node.put("type", "com.eckoit.recordingSegment");
                        node.put("recording", currentRecordingDoc.get("_id").getTextValue());
                        node.put("startTime", t.getStartTime());
                        connector.create(node);

                        String id = node.get("_id").getTextValue();
                        String rev = node.get("_rev").getTextValue();
                        {
                            // attach stream
                            FileInputStream fis = new FileInputStream(t.getAvailableToStream());
                            String name = "fileSequence" + t.getSegmentCount() + ".ts";
                            AttachmentInputStream ais = new AttachmentInputStream(name,fis, t.getContentType());

                            
                            rev =  connector.createAttachment(id, rev, ais);
                            ais.close();
                            fis.close();
                        }
                        {
                            // attach mp3
                            FileInputStream fis = new FileInputStream(t.getFinishedFile());
                            String name = "fileSequence" + t.getSegmentCount() + ".mp3";
                            AttachmentInputStream ais = new AttachmentInputStream(name,fis, "audio/mp3");

                            rev =  connector.createAttachment(id, rev, ais);
                            ais.close();
                            fis.close();
                        }
                        
                    
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        });


        EventBus.subscribeStrongly(RecorderJustStartedWithARecordingDocRequested.class, new EventSubscriber<RecorderJustStartedWithARecordingDocRequested>() {
            @Override
            public void onEvent(RecorderJustStartedWithARecordingDocRequested t) {
                ObjectNode testDoc = connector.get(ObjectNode.class, t.getRecordingDocId());
                RecordingState state = detectState(testDoc);
                System.out.println("The state of the requested doc: " + state.name());
                if (state == RecordingState.START_ASKED) {
                    processRecordingDoc(testDoc);
                } else if (state == RecordingState.RECORDER_AVAILABLE || state == RecordingState.RECORDER_ASKED) {
                    // ok we will update with our id,
                    ObjectNode recordingState = getRecordingState(testDoc);
                    recordingState.put("recorderAvailable", getRecorderUUID());
                    connector.update(testDoc);
                }
                // no idea how to handle the other states right now.
            }
        });


    }



    protected ObjectNode getRecordingState(ObjectNode recordingDoc) {
        return (ObjectNode)recordingDoc.get("recordingState");
    }
    
    public void watch() {
        ChangesCommand cmd = new ChangesCommand.Builder().filter(ddoc + "/recordings").build();
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



    protected boolean areWeTheRecorder(ObjectNode doc, String whoAmI){
        System.out.println("who ami '" + whoAmI + "'");
        if (whoAmI == null) return true; // I guess we handle it all
        JsonNode userCtx = doc.get("userCtx");
        if (userCtx != null) {
            String docUserName = userCtx.get("name").getTextValue();
            System.out.println("doc user: '" + docUserName + "'");
            if (StringUtils.isEmpty(docUserName)) return true; // no one is specified. 

            return (whoAmI.equals(docUserName));
        } else {
            System.out.println("The usetCtx is null");
            return true; // no one is specified. 
        }

    }




    protected void processRecordingDoc(ObjectNode doc) {
        final RecordingState state = detectState(doc);
        System.out.println("state: " + state.name());
        if (state == RecordingState.RECORDER_ASKED) {
            System.out.println("Recorder asked");
            if (currentRecordingDoc == null) {
                System.out.println("current recording doc is null");
                if (areWeTheRecorder(doc, userName)) {
                    System.out.println("we are programmed to receive");
                    // we are free...lets volenteer
                    ObjectNode recordingState = getRecordingState(doc);
                    recordingState.put("recorderAvailable", getRecorderUUID());
                    connector.update(doc);
                }
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
                RecordingStartClickedEvent rsce = new RecordingStartClickedEvent(doc.get("_id").getTextValue());
                SplitAudioRecorderConfiguration settings = loadConfig(doc);
                if (settings != null) {
                    rsce.setConfig(settings);
                }
                EventBus.publish(rsce);
            }
            if (state == RecordingState.STOP_ASKED) {
                EventBus.publish(new RecordingStopClickedEvent());
            }
        }

    }


    protected SplitAudioRecorderConfiguration loadConfig(ObjectNode doc) {
        JsonNode settings = doc.get("settings");
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(settings, SplitAudioRecorderConfiguration.class);
        } catch (Exception ex) {
            return null;
        } 
    }





    protected boolean isOurRecorder(ObjectNode doc) {
        ObjectNode recordingState = getRecordingState(doc);
        if (recordingState != null) {
            JsonNode node = recordingState.get("recorderAvailable");
            if (node != null) {
                String uuid = node.getTextValue();
                if (getRecorderUUID().equals(uuid)) {
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

    /**
     * @return the recorderUUID
     */
    public String getRecorderUUID() {
        return recorderUUID;
    }

    /**
     * @param recorderUUID the recorderUUID to set
     */
    public void setRecorderUUID(String recorderUUID) {
        this.recorderUUID = recorderUUID;
    }

    /**
     * @return the userName
     */
    public String getUserName() {
        return userName;
    }

    /**
     * @param userName the userName to set
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }


    public enum RecordingState {
        UNKNOWN, 
        RECORDER_ASKED, RECORDER_AVAILABLE,
        START_ASKED, START_COMPLETE,
        STOP_ASKED, STOP_COMPLETE,
        POST_PROCESSING_STARTED, RECORDING_COMPLETE
    }







}
