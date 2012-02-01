/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


exports.recordings = {
      map : function(doc) {
          var prefix = "com.eckoit.recording:";
          if (doc._id.slice(0, prefix.length) == prefix) {
              emit(doc._id, null);
          }
      }
}


exports.byStream_m3u8 = {
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

