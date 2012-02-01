/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


exports.streamAudio = function(head, req) {

        var prefix = "com.eckoit.recording:";

        start({ "headers" : {"Content-type" : "application/vnd.apple.mpegurl"}});
        var first = true;


        while (row = getRow()) {
            if (first) {  // bug see http://stackoverflow.com/questions/7595662/couchdb-list-api-cant-seem-to-return-plain-text
                send("#EXTM3U\n");
                send("#EXT-X-TARGETDURATION:10\n");
                send('#EXT-X-PLAYLIST-TYPE:EVENT\n');
                if (row.value.startTime) {
                    var dt = new Date(row.value.startTime).toISOString() ;
                    send('##EXT-X-PROGRAM-DATE-TIME:' + dt + '\n');
                }
                send('#EXT-X-MEDIA-SEQUENCE:0\n');
                first = false;
            }
            if (row.id.slice(0, prefix.length) == prefix) {
                // this is the last one. This is the main recording.
                if (row.value.stopComplete) {
                    // end the stream
                    send('#EXT-X-ENDLIST\n');
                }
            } else {
                send('#EXTINF:10,\n'); // this means 10 seconds. We really should be getting this passed to us.
                var url_prefix = 'http://' + req.headers.Host + '/' + req.userCtx.db + '/_design/couchaudiorecorder/_rewrite/audio/'
                if (req.query.url) {
                    url_prefix = req.query.url;
                }
                send(url_prefix + row.id + '/' + row.value.attachmentName + '\n');
            }
        }

  }