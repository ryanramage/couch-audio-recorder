/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
(function( $ ) {


    var refreshStreamInterval;
    var loadNextMediaTimer;
    var playerReady = false;
    var $player;
    var playlistAndRecordDoc;
    var nextIndex = 0;
    var $status;

    // The public methods
    var methods = {
        init : function(options) {
            return this.each(function() {

                // sort out the options
                if (!options || !options.db) {
                    $.error( 'please provide a db in the options' );
                }
                if (!options || !options.stream) {
                    $.error( 'please provide a stream in the options' );
                }
                var settings = {
                    // not sure what I need yet
                    swfPath: "/couchaudiorecorder/js/jPlayer"
                }

                $.extend( settings, options );

                // get the element
                var $this = $(this);
                $this.addClass('couchaudiostreamer');
                data = $this.data('couchaudiostreamer', {
                    element : $this,
                    settings: settings
                });



                function updateStream() {
                    getPlaylistAndRecordDoc(settings.db, settings.stream, function(plrd) {
                        playlistAndRecordDoc = plrd;
                        var status = $.couchaudiorecorder.recordingStatus(playlistAndRecordDoc.recordDoc);
                        if (status === "recordingComplete" || status === "postProcessingStarted" || status === "stopComplete" ) {
                            clearInterval(refreshStreamInterval);
                        }
                    });
                }
                updateStream();
                refreshStreamInterval = setInterval(updateStream, 10000);

                $player = $('<div class="player"></div>');
                $this.append($player);
                $player.jPlayer({
                    swfPath: settings.swPath,
                    ready : function() {
                        playerReady = true;
                        loadNextMedia($player);
                    }
                }).bind($.jPlayer.event.ended, function(event) {
                    loadNextMedia($player);
                });


                $status = $('<div class="status"></div>');
                $this.append($status);


            });
        }
    }


    function loadNextMedia(player) {
        if (playlistAndRecordDoc && playlistAndRecordDoc.recordDoc) {
            var status = $.couchaudiorecorder.recordingStatus(playlistAndRecordDoc.recordDoc);
            if (playlistAndRecordDoc.playlist && playlistAndRecordDoc.playlist.length > 0) {
                if (nextIndex < playlistAndRecordDoc.playlist.length ) {
                    var doc = playlistAndRecordDoc.playlist[nextIndex++];
                    playDoc(player, doc);
                    return;
                } else {
                    // we have come to the end. 2 cases, stream is over, or end of buffer
                    if (status === "recordingComplete") {
                        uiFinished();
                        return;
                    
                    } 
                }
            }

        }

        loadNextMediaTimer = setTimeout(loadNextMedia, 100, player); // waiting!
        uiWaiting();
    }

    function uiWaiting() {
        $status.text("Buffering....");
    }


    function uiPlaying(doc) {
        var status = $.couchaudiorecorder.recordingStatus(playlistAndRecordDoc.recordDoc);
        if (status === "recordingComplete") {
            $status.text("Playing (event is complete)");
        } else {
            var ourTime = new Date().getTime();
            var docStartTime = new Date(doc.startTime);
            var difSec = (ourTime - docStartTime) / 1000;

            $status.text("Live. (well " + difSec + " seconds behind)");
        }

    }

    function uiFinished() {
        $status.text("Stream finished");
    }


    function playDoc(player, doc) {
        var attachment = findMp3AttachmentName(doc);
        var url = 'audio/' + doc._id + '/' + attachment;
        player.jPlayer("setMedia", {
            mp3: url
        }).jPlayer("play");;
        uiPlaying(doc);
    }

    function findMp3AttachmentName(doc) {
      var attachment;
      for (attachment in doc._attachments) {
          if (attachment.match(/mp3$/)) {
              return attachment;
          }
      }
      return null;
    }
    
    function endsWith(str, suffix) {
       return (str[str.length - 1] == suffix);
    }



    function getPlaylistAndRecordDoc(db, docId, callback) {
        db.view('couchaudiorecorder/byStream.m3u8', {
                key : docId,
                include_docs : true,
                success : function(results) {
                    var playlistAndRecordDoc = {
                        playlist : []
                    };
                    $.each(results.rows, function(i, row) {
                          var prefix = "com.eckoit.recording:";
                          if (row.doc._id.slice(0, prefix.length) == prefix) {
                             playlistAndRecordDoc.recordDoc = row.doc;
                          }
                          if (row.doc.type && row.doc.type == "com.eckoit.recordingSegment" && row.doc.recording && row.doc._attachments) {
                              playlistAndRecordDoc.playlist.push(row.doc);

                          }
                    });
                    callback(playlistAndRecordDoc);
                }
        });
    }



    // bind to the jQuery object, dispatch names to methods
    $.fn.couchaudiostreamer = function(method) {
        if (methods[method]) {
            return methods[method].apply(this, Array.prototype.slice.call(arguments, 1));
        } else if (typeof method === 'object' || !method) {
            return methods.init.apply(this, arguments);
        } else {
            $.error('Method ' + method + ' does not exist on jQuery.couchaudiostreamer');
        }
    }

}) ( jQuery )

