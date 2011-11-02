/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
(function( $ ) {



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


    // The public methods
    var methods = {
        init : function(options) {
            return this.each(function() {

                // sort out the options
                if (!options || !options.db) {
                    $.error( 'please provide a db in the options' );
                }
                var settings = {
                    launchRecorderUrl : "recorder.jnlp"
                }

                $.extend( settings, options );
                
                // get the element
                var $this = $(this);
                $this.addClass('couchaudiorecorder');
                

                data = $this.data('couchaudiorecorder', {
                    element : $this,
                    settings: settings
                });                
            });
        },
        newRecording : function(recordingOptions) {
            var recordingSettings = {
                _id : "com.eckoit.recording:" + new Date().getTime()
            }
            if ( recordingOptions ) {
                $.extend( recordingSettings, recordingOptions );
            }

            var $this = $(this),
                data  = $this.data('couchaudiorecorder');


            // create the doc
            var doc = {
                _id : recordingSettings._id,
                recordingState : {}
            }

            if (recordingSettings.settings) {
                doc.settings = recordingSettings.settings;
            }

            data.settings.db.saveDoc(doc, {
                success : function(){
                    initRecording(doc, data);
                }
            })
        },
        loadRecording : function(recordingId) {

            var $this = $(this),
                data  = $this.data('couchaudiorecorder');


            data.settings.db.openDoc(recordingId, {
                success : function(doc) {
                    console.log(data.settings);
                    initRecording(doc, data);
                },
                error : function() {
                    $.error( 'Unable to load recording: ' + recordingId );
                }
            })
        }
        

    }



    function recorderNotFound(doc, data) {
        console.log("none found");
        data.element.append('<p>Please start your <a href="'+data.settings.launchRecorderUrl+'">Audio Recorder Plugin</a> </p>');
    }


    function recorderFound(doc, data) {
        clearTimeout(data.findRecorderTimeoutID);
        console.log("recorder found");
        createControlPanel(doc, data);
    }


    function createControlPanel(doc, data) {

        var mic = $('<div class="mic-icon" ></div>');

        var start =  $('<button class="start" >Start</button>');
        var stop  =  $('<button class="stop" disabled="disabled">Stop</button>');
        var timer =  $('<div   class="timer">00:00:00</div>');
        var status = $('<div   class="status"></div>');

        var mainDiv = $('<div></div>');
        mainDiv.append(mic);
        mainDiv.append(timer)

        var bottomDiv = $('<div></div>');
        bottomDiv.append(start);
        bottomDiv.append(stop);

        mainDiv.append(bottomDiv);
        mainDiv.append(status);
        data.element.html(mainDiv);

        start.click( function() {
            startRecording(doc, data);
        });

        stop.click(function() {
            stopRecording(doc, data);
        })

    }

    function startRecording(doc, data) {
        doc.recordingState.startAsked = new Date().getTime();
        data.settings.db.saveDoc(doc, {
            success : function(){
                // prob should start a timer here...just in case we
                // have lost the recorder
            }
        })
    }

    function startRecordingConfirmed(doc, data) {
        console.log('Start confirmed!');
        // ok, start the timer
        data.updateTimerDisplayID = setInterval(updateTimerDisplay, 1000, doc)
        // change the buttons
        $('.couchaudiorecorder button.start').attr('disabled', 'disabled');
        $('.couchaudiorecorder button.stop').removeAttr("disabled");
    }

    var timeFormat = {
            showHour: true,
            showMin: true,
            showSec: true,
            padHour: true,
            padMin: true,
            padSec: true,
            sepHour: ":",
            sepMin: ":",
            sepSec: ""
    };

    var convertTime = function(s) {
            var myTime = new Date(s * 1000);
            var hour = myTime.getUTCHours();
            var min = myTime.getUTCMinutes();
            var sec = myTime.getUTCSeconds();
            var strHour = (timeFormat.padHour && hour < 10) ? "0" + hour : hour;
            var strMin = (timeFormat.padMin && min < 10) ? "0" + min : min;
            var strSec = (timeFormat.padSec && sec < 10) ? "0" + sec : sec;
            return ( strHour + timeFormat.sepHour ) + ((timeFormat.showMin) ? strMin + timeFormat.sepMin : "") + ((timeFormat.showSec) ? strSec + timeFormat.sepSec : "");
    };

    function updateTimerDisplay(doc) {
        var startTime = doc.recordingState.startComplete;
        var now = new Date().getTime();
        var elapsed = convertTime((now - startTime) / 1000);
        $('.couchaudiorecorder div.timer').text(elapsed); // format me please
    }


    function stopRecording(doc, data) {
        $('.couchaudiorecorder button.stop').attr('disabled', 'disabled');
        $('.couchaudiorecorder div.status').text('Stopping...');
        // note this doc, is probably stale.
        doc = data.settings.db.openDoc(doc._id, {
            success: function(doc) {
                doc.recordingState.stopAsked = new Date().getTime();
                data.settings.db.saveDoc(doc, {
                    success : function() {
                        // again, should have a timer to confirm...
                    }
                })


            }
        });
        clearInterval(data.updateTimerDisplayID);
    }

    function stopRecordingConfirmed(doc, data) {

        $('.couchaudiorecorder div.status').text('Finishing...');
    }


    function recordingCompleteConfirmed(doc, data) {
        $('.couchaudiorecorder div.status').text('Recording Complete!');
    }


    function findRecorder(doc, data) {
        console.log("Find recorder");
        if (!doc.recordingState) {
            doc.recordingState = {}
        }
        doc.recordingState.recorderAsked = new Date().getTime();
        data.settings.db.saveDoc(doc, {
            success: function() {
                // start a timer.
                data.findRecorderTimeoutID = setTimeout(recorderNotFound, 2000, doc, data);
            }
        });
    }


    function stateEvent(doc, state, data) {
        console.log(state);
        if (state == RecordingState.UNKNOWN) {
            // lets ask for a recorder
            findRecorder(doc, data);
        }
        if (state == RecordingState.RECORDER_AVAILABLE) {
            // show the start button
            recorderFound(doc, data);
        }
        if (state == RecordingState.START_COMPLETE) {
            startRecordingConfirmed(doc, data);
        }
        if (state == RecordingState.STOP_COMPLETE) {
            stopRecordingConfirmed(doc, data);
        }
        if (state == RecordingState.RECORDING_COMPLETE) {
            recordingCompleteConfirmed(doc, data);
        }
    }



    function initRecording(doc, data) {
        // subscribe!
        //var $changes = settings.db.changes(null, {filter : "couchaudiorecorder/recordings", id : doc._id});
        data.settings.db.changes(null, {filter : "couchaudiorecorder/recordings", id : doc._id}).onChange(function (change) {
            console.log('Got change: ');
            console.log(change)
            // get the doc
            data.settings.db.openDoc(doc._id, {
                success : function(doc) {
                    console.log("got doc");
                    var status = $.couchaudiorecorder.recordingStatus(doc);
                    stateEvent(doc, status, data);
                }
            })
        });
        var state = $.couchaudiorecorder.recordingStatus(doc);
        stateEvent(doc, state, data);
    }


    $.couchaudiorecorder = {
        recordingStatus : function(doc) {
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
    };



    // bind to the jQuery object, dispatch names to methods
    $.fn.couchaudiorecorder = function(method) {
        console.log("couchaudiorecorder");
        console.log(method);
        if (methods[method]) {
            return methods[method].apply(this, Array.prototype.slice.call(arguments, 1));
        } else if (typeof method === 'object' || !method) {
            return methods.init.apply(this, arguments);
        } else {
            $.error('Method ' + method + ' does not exist on jQuery.couchaudiorecorder');
        }
    }

}) ( jQuery )

