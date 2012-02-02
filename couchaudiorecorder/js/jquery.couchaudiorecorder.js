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
                    launchRecorderUrl : "../_show/recorder.jnlp",
                    designDoc : 'couchaudiorecorder'
                }

                $.extend( settings, options );
                
                // get the element
                var $this = $(this);
                $this.addClass('couchaudiorecorder');
                

                data = $this.data('couchaudiorecorder', {
                    element : $this,
                    settings: settings
                });
                uiLoading($this);
            });
        },
        newRecording : function(recordingOptions, userCtx) {
            var recordingSettings = {
                _id : "com.eckoit.recording:" + new Date().getTime()
            }

            recordingSettings.settings = $.couchaudiorecorder.quality.high;


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

            if (userCtx) {
                doc.userCtx = userCtx;
            }

            if (recordingSettings.settings) {
                doc.settings = recordingSettings.settings;
            }

            // put any additional properties direct on the doc
            if (recordingSettings.additionalProperties) {
                $.extend( doc, recordingSettings.additionalProperties );
            }


            data.settings.db.saveDoc(doc, {
                success : function(){
                    initRecording(doc, data);
                    var state = $.couchaudiorecorder.recordingStatus(doc);
                    stateEvent(doc, state, data);

                    

                }
            })
        },
        loadRecording : function(recordingId) {

            var $this = $(this),
                data  = $this.data('couchaudiorecorder');


            data.settings.db.openDoc(recordingId, {
                success : function(doc) {
                    initRecording(doc, data);
                    var state = $.couchaudiorecorder.recordingStatus(doc);
                    if (state == 'recorderAvailable' || state == 'startAsked') {
                        doc.recordingState = {};
                        // ask for the recorder again.
                        findRecorder(doc, data);


                    } else {
                        
                        // get the ui in the right state
                        makeUIRightForPreviousRecording(doc, data);
                        data.element.trigger(state, doc);
                    }
                },
                error : function() {
                    $.error( 'Unable to load recording: ' + recordingId );
                }
            })
        }
        

    }


    function makeUIRightForPreviousRecording(doc, data) {
        var state = $.couchaudiorecorder.recordingStatus(doc);
        if (state == RecordingState.RECORDER_AVAILABLE || state == RecordingState.UNKNOWN) {
            stateEvent(doc, state, data);
        }
        else if (state == RecordingState.RECORDER_ASKED) {
            recorderNotFound(doc, data);
        }
        else {
            createControlPanel(doc, data);
            if (state == RecordingState.START_COMPLETE) {
                startRecordingConfirmed(doc, data);
            }
            if (state == RecordingState.STOP_COMPLETE) {
                uiStartRecordingConfirmed();
                uiStopRecording() ;
            }
            if (state == RecordingState.RECORDING_COMPLETE) {
                uiStartRecordingConfirmed();
                uiStopRecording() ;
                uiRecordingFinished();
                
            }
        }
        
    }


    function startedPluginPoll(doc, data) {
        console.log('polling doc');
        data.settings.db.openDoc(doc._id, {
            success : function(doc) {
                var state = $.couchaudiorecorder.recordingStatus(doc);
                if (state == 'recorderAvailable') {
                    // cancel and resubscribe
                    if (data.promise) {
                        try {
                            data.promise.stop();
                        } catch (e) {}
                    }
                    initRecording(doc, data);

                    // get the ui in the right state
                    makeUIRightForPreviousRecording(doc, data);
                    data.element.trigger(state, doc);
                    clearTimeout(data.startPluginTimeoutID);




                }
            }
        });
        data.startPluginTimeoutID = setTimeout(startedPluginPoll, 1000, doc, data);
    }


    function recorderNotFound(doc, data) {
        var url = data.settings.launchRecorderUrl;
        if (doc._id) {
            url += "?recording=" + doc._id;
        }
        var link = $('<a href="'+url+'">Audio Recorder Plugin</a>');
        link.click(function() {
            startedPluginPoll(doc, data);
            link.hide();
            var waiting = link.parent().append('<div></div>');
            uiLoading(waiting, 'Waiting For Plugin...');
        })
        data.element.html('<p>Please start your </p>');
        data.element.append(link);
    }


    function recorderFound(doc, data) {
        clearTimeout(data.findRecorderTimeoutID);
        createControlPanel(doc, data);
    }


    function uiLoading(div, message) {
        if (!message) message = "Loading...";
        var icon  =  $('<div   class="icon"  ></div>')
        var status = $('<div   class="status">'+ message +'</div>');
        var mainDiv = $('<div></div>');
        mainDiv.append(icon);
        mainDiv.append(status);
        div.html(mainDiv);
        icon.show();
    }



    function createControlPanel(doc, data) {

        var mic = $('<div class="mic-icon" ></div>');

        var start =  $('<button class="btn start" >Start</button>');
        var stop  =  $('<button class="btn stop" disabled="disabled">Stop</button>');
        var timer =  $('<div   class="timer">00:00:00</div>');
        var icon  =  $('<div   class="icon"  ></div>')
        var status = $('<div   class="status"></div>');

        var statusDiv = $('<div></div>');


        var mainDiv = $('<div></div>');
        mainDiv.append(mic);
        mainDiv.append(timer)

        var bottomDiv = $('<div></div>');
        bottomDiv.append(start);
        bottomDiv.append(stop);

        mainDiv.append(bottomDiv);

        statusDiv.append(icon);
        statusDiv.append(status);


        mainDiv.append(statusDiv);
        data.element.html(mainDiv);

        start.click( function() {
            startRecording(doc, data);
        });

        stop.click(function() {
            stopRecording(doc, data);
        })

    }

    function startRecording(doc, data) {
        uiStartRecording();
        doc.recordingState.startAsked = new Date().getTime();
        data.settings.db.saveDoc(doc, {
            success : function(){
                // prob should start a timer here...just in case we
                // have lost the recorder
                //
                // update the view
                data.settings.db.view(data.settings.designDoc + '/recordings', {
                    key : doc._id,
                    success : function(ignore) {

                    }
                })
            }
        })
    }


    function uiStartRecording() {
        $('.couchaudiorecorder button.start').attr('disabled', 'disabled');
        $('.couchaudiorecorder button.stop' ).attr('disabled', 'disabled');
    }





    function startRecordingConfirmed(doc, data) {
        // ok, start the timer
        data.updateTimerDisplayID = setInterval(updateTimerDisplay, 1000, doc)
        uiStartRecordingConfirmed();
    }

    function uiStartRecordingConfirmed() {
        // change the buttons
        $('.couchaudiorecorder button.start').attr('disabled', 'disabled');
        $('.couchaudiorecorder button.stop').removeAttr("disabled");
        $('.couchaudiorecorder .mic-icon').addClass('mic-recording');
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


    function uiStopRecording() {
        $('.couchaudiorecorder button.stop').attr('disabled', 'disabled');
        $('.couchaudiorecorder div.status').text('Processing...');
        $('.couchaudiorecorder .mic-icon').removeClass('mic-recording');
        $('.couchaudiorecorder div.icon').show();
    }


    function stopRecording(doc, data) {
        uiStopRecording();
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
        // these are just in case we did not init the stop event
        uiStopRecording();
        $('.couchaudiorecorder div.status').text('Processing...');
        $('.couchaudiorecorder div.icon').show();
        clearInterval(data.updateTimerDisplayID);
    }






    function uiUploadingConfirmed(doc, data) {
        $('.couchaudiorecorder div.status').text('Uploading...');
    }

    function uploadingConfirmed(doc, data) {
        // not much to this
        uiUploadingConfirmed(doc, data);
    }

    function uiRecordingFinished() {
        $('.couchaudiorecorder div.status').text('Recording Complete!');
        $('.couchaudiorecorder div.icon').hide();
    }


    function recordingCompleteConfirmed(doc, data) {
        uiStopRecording();
        uiRecordingFinished(); // seems lame, but to be complete.
        // these are just in case we did not init the stop event
        
        clearInterval(data.updateTimerDisplayID);

        // update the view
        data.settings.db.view(data.settings.designDoc + '/recordings', {
            key : doc._id,
            success : function(ignore) {

            }
        })

    }


    function findRecorder(doc, data) {
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
        data.element.trigger(state, doc);

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
        if (state == RecordingState.POST_PROCESSING_STARTED) {
            uploadingConfirmed(doc, data);
        }
        if (state == RecordingState.RECORDING_COMPLETE) {
            recordingCompleteConfirmed(doc, data);
        }
    }



    function initRecording(doc, data) {
        // subscribe!
        //var $changes = settings.db.changes(null, {filter : "couchaudiorecorder/recordings", id : doc._id});
        data.promise = data.settings.db.changes(null, {filter : data.settings.designDoc + "/recordings", id : doc._id});
        data.promise.onChange(function (change) {
            // get the doc
            data.settings.db.openDoc(doc._id, {
                success : function(doc) {
                    var status = $.couchaudiorecorder.recordingStatus(doc);
                    stateEvent(doc, status, data);


                }
            })
        });

    }


    $.couchaudiorecorder = {
        deleteRecording : function(docId, db, callback) {
            db.view( data.settings.designDoc + '/byStream.m3u8', {
                key : docId,
                success : function(results) {

                   var count = results.rows.length;

                   $.each(results.rows, function(i, row) {
                       var doc = {_id : row.id};
                       $.extend(doc, row.value);
                       db.removeDoc(doc, {
                           success : function() {
                               count-=1;
                               if (count == 0) {
                                   callback();
                               }
                           }
                       });
                   });


                    //var ids = $.map(results.rows, function(val, i) {
                    //    console.log(val);
                    //    return val.id;
                    //});



                    // not working
                    //db.bulkRemove({docs:ids}, {
                    //   success : function() {
                    //       callback();
                    //   }
                    //});
                    
                }
            });
        },
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
        },
        quality : {
            low : {
                stream : true,

                wavSampleRate : 16000.0,
                wavSampleSize : 16,

                mp3Bitrate : 24000,
                mp3Frequency : 16000,

                oggBitrate : 24000,
                oggFrequency : 22050
            },
            med : {
                stream : true,

                wavSampleRate : 44100.0,
                wavSampleSize : 16,

                mp3Bitrate : 48000,
                mp3Frequency : 22050,

                oggBitrate : 24000,
                oggFrequency : 22050
            },
            high : {
                stream : true,

                wavSampleRate : 44100.0,
                wavSampleSize : 16,

                mp3Bitrate : 128000,
                mp3Frequency : 44100,

                oggBitrate : 24000,
                oggFrequency : 22050
            }
        }
    };



    // bind to the jQuery object, dispatch names to methods
    $.fn.couchaudiorecorder = function(method) {
        if (methods[method]) {
            return methods[method].apply(this, Array.prototype.slice.call(arguments, 1));
        } else if (typeof method === 'object' || !method) {
            return methods.init.apply(this, arguments);
        } else {
            $.error('Method ' + method + ' does not exist on jQuery.couchaudiorecorder');
        }
    }

}) ( jQuery )

