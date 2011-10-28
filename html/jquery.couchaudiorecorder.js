/**
 * Created by .
 * User: ryan
 * Date: 11-07-16
 * Time: 9:30 AM
 * To change this template use File | Settings | File Templates.
 */
(function( $ ){
  $.fn.couchaudiorecorder = function(options) {

        var RecordingState = {
            UNKNOWN : "UNKNOWN",
            RECORDING_COMPLETE : "RECORDING_COMPLETE",
            POST_PROCESSING_STARTED : "POST_PROCESSING_STARTED",
            STOP_COMPLETE : "STOP_COMPLETE",
            STOP_ASKED : "STOP_ASKED",
            START_COMPLETE : "START_COMPLETE",
            START_ASKED : "START_ASKED",
            RECORDER_ASKED : "RECORDER_ASKED",
            RECORDER_AVAILABLE : "RECORDER_AVAILABLE"
        }


        function recordingStatus(doc) {
            var recordingState = doc.recordingState;
            if (recordingState != null) {
                if (recordingState.recordingComplete != null) {
                    return RecordingState.RECORDING_COMPLETE;
                }
                if (recordingState.postProcessingStarted != null) {
                    return RecordingState.POST_PROCESSING_STARTED;
                }
                if (recordingState.stopComplete != null) {
                    return RecordingState.STOP_COMPLETE;
                }
                if (recordingState.stopAsked != null) {
                    return RecordingState.STOP_ASKED;
                }
                if (recordingState.startComplete != null) {
                    return RecordingState.START_COMPLETE;
                }
                if (recordingState.startAsked != null) {
                    return RecordingState.START_ASKED;
                }
                if (recordingState.recorderAsked != null) {
                    return RecordingState.RECORDER_ASKED;
                }
                if (recordingState.recorderAvailable != null) {
                    return RecordingState.RECORDER_AVAILABLE;
                }
            }
            return RecordingState.UNKNOWN;
        }

        function init(doc, settings) {
            // subscribe!
            var $changes = settings.db.changes(null, { filter : "couchaudiorecorder/recordings", id : doc._id});
            $changes.onChange = function (data) {
                console.log(data);
            }


            var status = recordingStatus(doc);
            console.log(status);
        }


        if (!options.db) throw "Please provide a db";
        var settings = {
            launchRecorderUrl : "recorder.jnlp",
            recordingDocumentID : null
        }
        if ( options ) {
            $.extend( settings, options );
        }
        if (!settings.recordingDocumentID) {
            settings.recordingDocumentID = "recording-" + new Date().getTime()
        }

        settings.db.openDoc(settings.recordingDocumentID, {
            success : function(doc) {
                console.log(doc);
            },
            error : function() {
                // create the doc
                var doc = {
                    _id : settings.recordingDocumentID,
                    recordingState : {}
                }
                settings.db.saveDoc(doc, {
                    success : function(){
                        init(doc, settings);
                    }
                })

            }

        })

  };
})( jQuery );