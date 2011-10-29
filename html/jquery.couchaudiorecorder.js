/**
 * Created by .
 * User: ryan
 * Date: 11-07-16
 * Time: 9:30 AM
 * To change this template use File | Settings | File Templates.
 */
(function( $ ){
  $.fn.couchaudiorecorder = function(options) {

        var element = this;
        element.addClass('couchaudiorecorder');

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

        var RecordingState = {
            UNKNOWN : "UNKNOWN",
            RECORDING_COMPLETE : "recordingComplete",
            POST_PROCESSING_STARTED : "postProcessingStarted",
            STOP_COMPLETE : "stopComplete",
            STOP_ASKED : "stopAsked",
            START_COMPLETE : "startComplete",
            START_ASKED : "startAsked",
            RECORDER_ASKED : "recorderAsked",
            RECORDER_AVAILABLE : "recorderAvailable"
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
                if (recordingState.recorderAvailable != null) {
                    return RecordingState.RECORDER_AVAILABLE;
                }
                if (recordingState.recorderAsked != null) {
                    return RecordingState.RECORDER_ASKED;
                }

            }
            return RecordingState.UNKNOWN;
        }

        var findRecorderTimeoutID = null;

        function recorderNotFound(doc, settings) {
            console.log("none found");
            element.append('<p>Please start your <a href="'+settings.launchRecorderUrl+'">Audio Recorder Plugin</a> </p>');
        }


        function recorderFound(doc, settings) {
            clearTimeout(findRecorderTimeoutID);
            console.log("recorder found");
            createControlPanel(doc, settings);
        }


        function createControlPanel(doc, settings) {
            var start =  $('<button class="start" >Start</button>');
            var stop  =  $('<button class="stop" disabled="disabled">Stop</button>');
            var timer =  $('<span   class="timer">00:00:00</span>');
            var status = $('<span   class="status"></span');

            var topDiv = $('<div></div>');
            topDiv.append(timer).append(status);

            var bottomDiv = $('<div></div>');
            bottomDiv.append(start);
            bottomDiv.append(stop);

            element.append(topDiv);
            element.append(bottomDiv);

            start.click( function() {
                startRecording(doc, settings);
            });

            stop.click(function() {
                stopRecording(doc, settings);
            })

        }

        function startRecording(doc, settings) {
            doc.recordingState.startAsked = new Date().getTime();
            settings.db.saveDoc(doc, {
                success : function(){
                    // prob should start a timer here...just in case we
                    // have lost the recorder
                }
            })
        }

        var updateTimerDisplayID;

        function startRecordingConfirmed(doc, settings) {
            console.log('Start confirmed!');
            // ok, start the timer
            updateTimerDisplayID = setInterval(updateTimerDisplay, 1000, doc)
            // change the buttons
            $('.couchaudiorecorder button.start').attr('disabled', 'disabled');
            $('.couchaudiorecorder button.stop').removeAttr("disabled");
        }

        function updateTimerDisplay(doc) {
            var startTime = doc.recordingState.startComplete;
            var now = new Date().getTime();
            var elapsed = now - startTime;
            $('.couchaudiorecorder span.timer').text(elapsed); // format me please
        }


        function stopRecording(doc, settings) {
            $('.couchaudiorecorder button.stop').attr('disabled', 'disabled');
            $('.couchaudiorecorder span.status').text('Stopping...');
            // note this doc, is probably stale.
            doc = settings.db.openDoc(doc._id, {
                success: function(doc) {
                    doc.recordingState.stopAsked = new Date().getTime();
                    settings.db.saveDoc(doc, {
                        success : function() {
                            // again, should have a timer to confirm...
                        }
                    })


                }
            });
            clearInterval(updateTimerDisplayID);
        }

        function stopRecordingConfirmed(doc, settings) {
            
            $('.couchaudiorecorder span.status').text('Finishing...');
        }


        function recordingCompleteConfirmed(doc, settings) {
            $('.couchaudiorecorder span.status').text('Recording Complete!');
        }


        function findRecorder(doc, settings) {
            console.log("Find recorder");
            if (!doc.recordingState) {
                doc.recordingState = {}
            }
            doc.recordingState.recorderAsked = new Date().getTime();
            settings.db.saveDoc(doc, {
                success: function() {
                    // start a timer.
                    findRecorderTimeoutID = setTimeout(recorderNotFound, 2000, doc, settings);
                }
            });
        }


        function stateEvent(doc, state, settings) {
            console.log(state);
            if (state == RecordingState.UNKNOWN) {
                // lets ask for a recorder
                findRecorder(doc, settings);
            }
            if (state == RecordingState.RECORDER_AVAILABLE) {
                // show the start button
                recorderFound(doc, settings);
            }
            if (state == RecordingState.START_COMPLETE) {
                startRecordingConfirmed(doc, settings);
            }
            if (state == RecordingState.STOP_COMPLETE) {
                stopRecordingConfirmed(doc, settings);
            }
            if (state == RecordingState.RECORDING_COMPLETE) {
                recordingCompleteConfirmed(doc, settings);
            }
        }



        function init(doc, settings) {
            // subscribe!
            //var $changes = settings.db.changes(null, {filter : "couchaudiorecorder/recordings", id : doc._id});
            settings.db.changes(null, {filter : "couchaudiorecorder/recordings", id : doc._id}).onChange(function (data) {
                console.log('Got change: ');
                console.log(data)
                // get the doc
                settings.db.openDoc(doc._id, {
                    success : function(doc) {
                        console.log("got doc");
                        var status = recordingStatus(doc);
                        stateEvent(doc, status, settings);
                    }
                })
            });
            var state = recordingStatus(doc);
            stateEvent(doc, state, settings);
        }




        settings.db.openDoc(settings.recordingDocumentID, {
            success : function(doc) {
                init(doc, settings);
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