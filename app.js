
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
      {from:"/audio/:id/stream.m3u8", to:'_list/streamAudio/byStream.m3u8', query : {key: ":id"}}
    , {from:"/audio/:id/:doc/:attachment", to:'../../:doc/:attachment'}
    , {from:"/audio/:doc/:attachment", to:'../../:doc/:attachment'}
    ];


  ddoc.views.recordings = {
      map : function(doc) {
          var prefix = "com.eckoit.recording:";
          if (doc._id.slice(0, prefix.length) == prefix) {
              emit(doc._id, null);
          }
      }
  }


  ddoc.views['byStream.m3u8'] = {
      map: function(doc) {
         var prefix = "com.eckoit.recording:";
          if (doc._id.slice(0, prefix.length) == prefix) {
              var temp = {};
              for (i in doc.recordingState) {
                  temp[i] = doc.recordingState[i];
              }
              temp._rev =  doc._rev;
              emit(doc._id, temp);
          }
          if (doc.type && doc.type == "com.eckoit.recordingSegment" && doc.recording && doc._attachments) {
              function endsWith(str, suffix) {
                  return (str[str.length - 1] == suffix);
              }


              var attachment;
              for (attachment in doc._attachments) {
                  if (endsWith(attachment, 'ts')) {
                      break;
                  }
              }
              var attachmentInfo = doc._attachments[attachment];
              var data = {_id: doc._id, attachmentName : attachment, attachmentInfo: attachmentInfo, _rev: doc._rev};
              if (doc.startTime) {
                  data.startTime = doc.startTime;
              }
              emit(doc.recording, data);
          }
      }
  }

  ddoc.lists.streamAudio = function(head, req) {

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

  ddoc.filters.recordings = function(doc, req) {
      var prefix = "com.eckoit.recording:";
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




 ddoc.shows['recorder.jnlp'] = function(doc, req) {

        // make our arguments
        var args  = {};
        args.url = 'http://' + req.headers.Host;
        args.db = req.userCtx.db;
        args.recording = req.query.recording;
        args.user = req.userCtx.name;
        if (args.user == null) {
            args.user = req.query.user;
        }
        var args_str = JSON.stringify(args);


	var ddoc2 = req.path[2];
	var codebase = 'http://' + req.headers.Host + '/' + req.path[0] + '/_design/'+ddoc2 +'/';
	var defaults = { codebase : codebase, href : '_show/recorder.jnlp?recording=' + req.query.recording + '&user=' + args.user};
	var result = '<?xml version=\"1.0\" encoding=\"utf-8\"?>';
	result += '<jnlp spec=\"1.5+\" codebase=\"'+codebase+'\" href=\"'+defaults.href+'\">';
	result += '<information><title>Couch Audio Recorder</title><vendor>Eckoit Inc</vendor><homepage>http://eckoit.com</homepage><description kind=\"one-line\">A audio recorder for your couch.</description>';


        splash = '../../skin/noun_project_171_1.png';
        icon = '../../skin/record_icon.png'
	
	result += '<icon kind=\"splash\" href=\"'+ splash +'\"/>';
	result += '<icon href=\"' + icon + '\"/>';
	result += ' <offline-allowed/> ';
	result += ' <shortcut online=\"false\">';
	result += '   <desktop/>';
	result += '   <menu submenu=\"Couch Audio Recorder"/>';
	result += ' </shortcut>';
	result += '</information>';
	result += '  <security><all-permissions/></security>';
	result += '  <resources> <j2se version=\"1.6+\" initial-heap-size=\"32m\" max-heap-size=\"128m\" /> ';
	String.prototype.endsWith = function(suffix) { return this.indexOf(suffix, this.length - suffix.length) !== -1; };
	for (var a in this._attachments) { if (a.endsWith('.jar')) { var main = ''; if (a == 'couch-audio-recorder-1.0-SNAPSHOT.jar') main = 'main=\"true\"'; result += ' <jar href=\"'+a+'\" '+main+'/> '; } }
	result += '</resources>';
	result += '  <application-desc main-class=\"com.googlecode.eckoit.audio.ui.SimpleTrayRecorder\">';


        result += '  <argument>'+ args_str + '</argument>';

 

	result += ' </application-desc>';
	result += '</jnlp>';
	return { 'headers' : {'Content-Type' : 'application/x-java-jnlp-file'}, 'body' :  result }
 }





 ddoc.validate_doc_update = function(newDoc, oldDoc, userCtx, secObj) {
      // we only care about recordings
      var prefix = "com.eckoit.recording:";
      if (newDoc._id.slice(0, prefix.length) == prefix) {
        var docUser = null;
        if (oldDoc && oldDoc.userCtx) {
            docUser = oldDoc.userCtx.name;
        }
        if (newDoc._deleted && docUser) {
            if (!userCtx.name) throw ({forbidden : "You must be logged in to delete this"});
            if (userCtx.name != docUser) throw ({forbidden : "Only the recording creator can delete"});
        }
      }
 }



  couchapp.loadAttachments(ddoc, path.join(__dirname, 'html'));