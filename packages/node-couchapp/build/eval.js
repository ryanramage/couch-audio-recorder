// this is because some require paths changed in the npm-compatible version
// of kanso, we want this package to support older installs too
var tryRequire = function (a, b) {
    try {
        return require(a);
    }
    catch (e) {
        // throw if this one fails
        return require(b);
    }
};

var packages = require('kanso/packages'),
    couchapp = require('couchapp'),
    watch = require('watch'),
    mimetypes = tryRequire('mime', 'node-mime/mime'),
    path = require('path'),
    utils = require('kanso/utils'),
    async = require('async'),
    fs = require('fs');


module.exports = function (root, path, settings, doc, callback) {
    if (settings.app) {
        try {
            var require_path = utils.abspath(settings.app, path);

            var mod = require(require_path);
            var fake_url = 'http://localhost:5983/db';
            var app = couchapp.createApp(mod, fake_url, function (app) {
                app.prepare();
                addAttachments(app, function (err, app) {
                    if (err) {
                        return callback(err);
                    }
                    doc = packages.merge(doc, app.doc);

                    if (doc.__attachments) {
                        delete doc.__attachments;
                    }

                    callback(null, doc);
                });
            });
        }
        catch (e) {
            return callback(e);
        }
    }
    else {
        callback(null, doc);
    }
};


function addAttachments(app, callback) {
    var pending = 0;


    // we're not using this
    delete app.doc.attachments_md5;

    if (!app.doc.__attachments || !app.doc.__attachments.length) {
        delete app.doc.__attachments;
        return callback(null, app);
    }

    // adapted from node.couchapp.js/main.js
    app.doc.__attachments.forEach(function (att) {
        watch.walk(att.root, {ignoreDotFiles:true}, function (err, files) {

            //console.log(files);
            if (err) {
                return callback(err);
            }
            var keys = Object.keys(files);
            if (!keys.length) {
                return callback(null, app);
            }
            async.forEach(keys, function (f, cb) {

                fs.stat(f, function(err, stats) {
                    if (stats.isDirectory()) return cb();
                    fs.readFile(f, function (err, data) {
                        if (err) {
                            return cb(err);
                        }
                        f = f.replace(att.root, att.prefix || '');
                        if (f[0] === '/') {
                            f = f.slice(1);
                        }
                        var d = data.toString('base64');
                        var mime = mimetypes.lookup(path.extname(f).slice(1));
                        app.doc._attachments[f] = {data: d, content_type: mime};
                        cb();
                    })
                });
            },
            function (err) {
                console.log(err);
                callback(err, app);
            });
        })
    })
};
