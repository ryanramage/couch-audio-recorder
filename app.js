
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
    , {from:"/api", to:'../../'}
    , {from:"/api/*", to:'../../*'}
    , {from:"/audio/:id/stream.m3u8", to:'_show/streamAudio/:id'}
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

  ddoc.shows.streamAudio = function(doc, req) {

    var body =  "#EXTM3U\n";
        body += "#EXT-X-TARGETDURATION:10\n";
        body += "#EXT-X-MEDIA-SEQUENCE:0\n";


    return {
        "headers" : { "Content-Type" : "application/x-mpegURL" },
        "body" : body
    }
  }

  couchapp.loadAttachments(ddoc, path.join(__dirname, 'html'));