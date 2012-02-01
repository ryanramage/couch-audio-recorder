/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


exports.validate_doc_update = function(newDoc, oldDoc, userCtx, secObj) {
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