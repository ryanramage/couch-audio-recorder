/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


exports.recordings = function(doc, req) {
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
