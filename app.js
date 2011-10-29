
var couchapp = require('couchapp')
    , path = require('path');

  ddoc = {
      _id: '_design/couchaudiorecorder'
    , views: {}
    , lists: {}
    , shows: {}
    , filters: {}
  }

  module.exports = ddoc;

  ddoc.language = "javascript";
  ddoc.rewrites = [
      {from:"/", to:'index.html'}
    , {from:"/audio/:id/stream.m3u8", to:'_list/streamAudio/byStream.m3u8', query : {key: ":id"}}
    , {from:"/audio/:id/:doc/:attachment", to:'../../:doc/:attachment'}
    , {from:"/api", to:'../../'}
    , {from:"/api/*", to:'../../*'}

    , {from:"/*", to:'*'}
    ];


  ddoc.views.recordings = {
      map : function(doc) {
          var prefix = "recording-";
          if (doc._id.slice(0, prefix.length) == prefix) {
              emit(doc._id, null);
          }
      }
  }


  ddoc.views['byStream.m3u8'] = {
      map: function(doc) {
         var prefix = "recording-";
          if (doc._id.slice(0, prefix.length) == prefix) {
              emit(doc._id, doc.recordingState);
          }
          if (doc.type && doc.type == "recording-segment" && doc.recording && doc._attachments) {
              for (first in doc._attachments) break;
              var attachmentInfo = doc._attachments[first];
              var data = {_id: doc._id, attachmentName : first, attachmentInfo: attachmentInfo};
              if (doc.startTime) {
                  data.startTime = doc.startTime;
              }
              emit(doc.recording, data);
          }
      }
  }

  ddoc.lists.streamAudio = function(head, req) {

        var prefix = "recording-";

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
                send(row.id + '/' + row.value.attachmentName + '\n');
            }
        }

  }

  ddoc.filters.recordings = function(doc, req) {
      var prefix = "recording-";
      if (doc._id.slice(0, prefix.length) == prefix) {
          if (req.query.id) {
             if (req.query.id == doc._id) {
                 return true;
             } else return false;
          }
          return true;

      }
      return false;
  }








/** add views/shows/lists below **/



  couchapp.loadAttachments(ddoc, path.join(__dirname, 'html'));