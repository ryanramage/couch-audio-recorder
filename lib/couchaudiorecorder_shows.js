/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

var base64 = require('base64');



exports.recorder_jnlp = function(doc, req) {

        var dd = req.path[2];

        // make our arguments
        var args  = {};
        args.url = 'http://' + req.headers.Host;
        args.db = req.userCtx.db;
        args.recording = req.query.recording;
        args.user = req.userCtx.name;
        if (args.user == null) {
            args.user = req.query.user;
        }
        args.dd = dd;
        var args_str = JSON.stringify(args);

        var href = '_show/recorder.jnlp?recording=' + req.query.recording;
        if (args.user) {
            href += '&user=' + args.user;
        }

	
	var codebase = 'http://' + req.headers.Host + '/' + req.path[0] + '/_design/'+dd +'/';


	var defaults = { codebase : codebase, href : href};
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
	//String.prototype.endsWith = function(suffix) { return this.indexOf(suffix, this.length - suffix.length) !== -1; };
	//for (var a in this._attachments) { if (a.endsWith('.jar')) { var main = ''; if (a == 'couch-audio-recorder-1.0-SNAPSHOT.jar') main = 'main=\"true\"'; result += ' <jar href=\"'+a+'\" '+main+'/> '; } }

        // to be fixed
        result += ' <jar href=\"couchaudiorecorder/webstart/couch-audio-recorder-1.0-SNAPSHOT.jar\" main=\"true\" /> ';
        result += ' <jar href=\"couchaudiorecorder/webstart/commons-codec-1.4.jar\"  /> ';
        result += ' <jar href=\"couchaudiorecorder/webstart/commons-io-2.0.1.jar\"  /> ';
        result += ' <jar href=\"couchaudiorecorder/webstart/commons-lang-2.6.jar\"  /> ';
        result += ' <jar href=\"couchaudiorecorder/webstart/commons-logging-1.1.1.jar\"  /> ';
        result += ' <jar href=\"couchaudiorecorder/webstart/eventbus-1.4.jar\"  /> ';
        result += ' <jar href=\"couchaudiorecorder/webstart/httpclient-4.1.1.jar\"  /> ';
        result += ' <jar href=\"couchaudiorecorder/webstart/httpclient-cache-4.1.1.jar\"  /> ';
        result += ' <jar href=\"couchaudiorecorder/webstart/httpcore-4.1.jar\"  /> ';
        result += ' <jar href=\"couchaudiorecorder/webstart/jackson-core-asl-1.8.4.jar\"  /> ';
        result += ' <jar href=\"couchaudiorecorder/webstart/jackson-mapper-asl-1.8.4.jar\"  /> ';

        result += ' <jar href=\"couchaudiorecorder/webstart/org.ektorp-1.2.1.jar\"  /> ';
        result += ' <jar href=\"couchaudiorecorder/webstart/plugins-1.0-SNAPSHOT.jar\"  /> ';
        result += ' <jar href=\"couchaudiorecorder/webstart/slf4j-api-1.6.1.jar\"  /> ';
        result += ' <jar href=\"couchaudiorecorder/webstart/swing-layout-1.0.3.jar\"  /> ';




	result += '</resources>';
	result += '  <application-desc main-class=\"com.googlecode.eckoit.audio.ui.SimpleTrayRecorder\">';


        result += '  <argument>'+ args_str + '</argument>';



	result += ' </application-desc>';
	result += '</jnlp>';



	return { 'headers' : {'Content-Type' : 'application/x-java-jnlp-file'}, 'body' :  result }
 }