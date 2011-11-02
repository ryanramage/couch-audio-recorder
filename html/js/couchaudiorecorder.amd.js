define(['jquery'], function ($) {

    defaults = {
        launchRecorderUrl : "recorder.jnlp",
        recordingDocumentID : null
    };


    var CouchAudioRecorder = function(element, options) {
        this.options = $.extend( {}, defaults, options) ;
        this._defaults = defaults;
        this._name = pluginName;
        this._elem = element;
        this.init();

        
    }


    CouchAudioRecorder.prototype.init = function() {
        
    }


    CouchAudioRecorder.prototype.newRecording = function() {
        
    }


    CouchAudioRecorder.prototype.loadRecording = function(recordingID) {

    }



    //Define the module value by returning a value.
    return CouchAudioRecorder;
});