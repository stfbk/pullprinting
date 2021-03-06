var preq = require('request-promise'),
    url = require('url'),
    shortid = require('shortid'),
    _ = require('lodash');
    var btoa = require('btoa');

var _printerIDSpooler=process.env._printerIDSpooler;

var GCPClient = module.exports = function (opts, tokenExpires) {
    this.options = _.defaults(opts, {
        oauthVersion: 'v3'
    });

    if (!this.options.clientId) {
        throw new Error('Missing required parameter: { clientId: \'...\' }');
    }
    if (!this.options.clientSecret) {
        throw new Error('Missing required parameter: { clientSecret: \'...\' }');
    }
    if (!this.options.accessToken) {
        throw new Error('Missing required parameter: { accessToken: \'...\' }');
    }
    /*if (!this.options.refreshToken) {
        throw new Error('Missing required parameter: { refreshToken: \'...\' }');
    }*/
};

var failRetry = function (func) {
    return function () {
        var self = this, args = Array.prototype.slice.call(arguments);
        return func.apply(self, args)
            .then(function (result) {
                self.isRetry = false;
                return result;
            })
            .catch(function (err) {
                if (!self.isRetry && err.statusCode == 403) {
                    return self._refreshToken()
                        .then(function () {
                            self.isRetry = true;
                            return func.apply(self, args);
                        })
                }
                throw err;
            });
    }
};

GCPClient.prototype._refreshToken = function () {
    var self = this;
    return preq({
        method: 'POST',
        uri: 'https://www.googleapis.com/oauth2/' + this.options.oauthVersion + '/token',
        form: {
            'client_id': this.options.clientId,
            'client_secret': this.options.clientSecret,
            'refresh_token': this.options.refreshToken,
            'grant_type': 'refresh_token'
        },
        json: true
    })
        .then(function (result) {
            self.tokenExpires = (Date.now() / 1000 | 0) + result['expires_in'];
            return self.options.accessToken = result['access_token'];
        });
};

GCPClient.prototype.getPrinters =failRetry(function (cb) {
	return preq({
        method: 'POST',
        uri: 'https://www.google.com/cloudprint/search',
        headers: {
            'X-CloudPrint-Proxy': 'node-gcp',
            'Authorization': 'OAuth ' + this.options.accessToken
        },
        json: true
    })
        .then(function (result) {
            var printers = [];
            _.forEach(result.printers, function (printer) {
                if(printer.id != _printerIDSpooler){
                printers.push({
                    id: printer.id,
                    name: printer.displayName,
                    description: printer.description,
                    type: printer.type,
                    status: printer.connectionStatus
                });
            }
        });
            return printers;
        })
        .nodeify(cb);
});

GCPClient.prototype.getPrinter = failRetry(function (id, cb) {
    return preq({
        method: 'POST',
        uri: 'https://www.google.com/cloudprint/printer',
        form: {
            'printerid': id
        },
        headers: {
            'X-CloudPrint-Proxy': 'node-gcp',
            'Authorization': 'OAuth ' + this.options.accessToken
        },
        json: true
    })
        .then(function (result) {
            return result.printers[0];
        })
        .nodeify(cb);
});

GCPClient.prototype.getQueuedJobs = failRetry(function (id, cb) {
    return preq({
        method: 'POST',
        uri: 'https://www.google.com/cloudprint/jobs',
        form: {
            'printerid': id,
            'status': 'IN_PROGRESS'
        },
        headers: {
            'X-CloudPrint-Proxy': 'node-gcp',
            'Authorization': 'OAuth ' + this.options.accessToken
        },
        json: true
    })
        .then(function (result) {
            return result;
        })
        .nodeify(cb);
});

GCPClient.prototype.deleteJob = failRetry(function (jobid, cb) {
    return preq({
        method: 'POST',
        uri: 'https://www.google.com/cloudprint/deletejob',
        form: {
            'jobid': jobid,
        },
        headers: {
            'X-CloudPrint-Proxy': 'node-gcp',
            'Authorization': 'OAuth ' + this.options.accessToken
        },
        json: true
    })
        .then(function (result) {
            return result;
        })
        .nodeify(cb);
});

GCPClient.prototype.print = failRetry(function (printerId, content, contentType, title, ticket, settings, cb) {
    if (_.isPlainObject(printerId)) {
        settings = printerId;
        printerId = null;
        if (_.isFunction(content)) {
            cb = content;
            content = null;
        }
    } else if (_.isFunction(title)) {
        cb = title;
        title = null;
    } else if (_.isFunction(settings)) {
        cb = settings;
        settings = null;
    }
	else if(_.isFunction(ticket)){
		cb = ticket;
        	ticket = null;
	}
    settings = settings || {};
	//console.log("ticket_info: ",ticket);
    if (settings.printerId) {
        settings.printerid = settings.printerId;
        delete settings.printerid;
    }

    // convert to base64
    let buffer64 = new Buffer(content).toString("base64");
    content = buffer64;

    settings.title = settings.title || title || 'UNTITLED JOB ' + shortid.generate();
    settings.contentType = settings.contentType || contentType;
    settings.printerid = settings.printerid || printerId;
    settings.content = settings.content || content;
    settings.contentTransferEncoding = 'base64';

    settings.ticket = settings.ticket || ticket;


    //console.log("settings", settings);

    return preq({
        method: 'POST',
        uri: 'https://www.google.com/cloudprint/submit',
        form: settings,
        headers: {
            'X-CloudPrint-Proxy': 'node-gcp',
            'Authorization': 'OAuth ' + this.options.accessToken
        },
        json: true
    });
});

GCPClient.prototype.download = failRetry(function (jobid) {
    return preq({
        method: 'POST',
        uri: 'https://www.google.com/cloudprint/download',
        form: {
            'id': jobid,
        },
        encoding: "binary",
        headers: {
            'accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8',
            'accept-encoding': 'gzip, deflate, br',
            'accept-language': 'it-IT,it;q=0.9,en-US;q=0.8,en;q=0.7',
            'content-type': 'application/x-www-form-urlencoded',
            //'origin': 'https://www.google.com',
            'X-CloudPrint-Proxy': 'node-gcp',
            'Authorization': 'OAuth ' + this.options.accessToken,
        },
        json: true
    });
});


GCPClient.prototype.getMail = failRetry(function (cb) {

    return preq({
        method: 'GET',
        uri: 'https://openidconnect.googleapis.com/v1/userinfo',
        headers: {
        'cache-control': 'no-cache' ,
        'Authorization': 'OAuth ' + this.options.accessToken
    }
        })
    .then(function (result) {
        var mail_domain="";
        var obj = JSON.parse(result);
        //check the value of obj!
        //console.log(obj);
        try{
            var mail = (obj.email);
            var mail_domain = mail.split("@")[1];
        }
        catch(error) {
            console.log("something wrong - GOOGLE");
            console.error(error);

            mail_domain = "";
        }
        return(mail_domain);
    })
    .nodeify(cb);
});


GCPClient.prototype.getMail_Enterprise = failRetry(function (cb) {

    return preq({
        method: 'GET',
        uri: 'https://openidconnect.googleapis.com/v1/userinfo',
        headers: {
        'cache-control': 'no-cache' ,
        'Authorization': 'OAuth ' + this.options.accessToken
    }
        })
    .then(function (result) {
        var mail_domain="";
        var obj = JSON.parse(result);
        //console.log(obj);

        try{
            var mail = (obj.email);
        }
        catch(error) {
            console.log("something wrong - Enterprise");
            console.error(error);
            
            var mail = "";
        }
        return(mail);
    })
    .nodeify(cb);
});

GCPClient.prototype.getMail_SC = failRetry(function (accessToken,sc_token,cb) {

    //console.log("sc-token", sc_token);
    //console.log("google-access-token", accessToken);

    return preq({
        method: 'POST',
        uri: 'https://am-test.smartcommunitylab.it/aac//token_introspection', //userinfo
        form: {
            'token': sc_token,
        },
        headers: {
        'cache-control': 'no-cache' ,
        'Authorization': "Basic " + btoa("e9610874-1548-4311-a663-472ba9c1ce33" + ":" + "b906d4df-01bc-4212-adf2-e3cdac9be731")
    }
        })
    .then(function (result) {
        //console.log(result);
        var obj = JSON.parse(result);
        var username = (obj.username);
        return(username);
    })
    .nodeify(cb);
});


  GCPClient.prototype.jobLookup = failRetry(function (jobid) {
        return preq({
            method: 'POST',
            uri: 'https://www.google.com/cloudprint/job',
            form: {
                'jobid': jobid
            },
            headers: {
                'X-CloudPrint-Proxy': 'node-gcp',
                'Authorization': 'OAuth ' + this.options.accessToken
            },
            json: true
        })
    });

 GCPClient.prototype.jobTicket = failRetry(function (jobid) {
        return preq({
            method: 'POST',
            uri: 'https://www.google.com/cloudprint/ticket',
            form: {
                'jobid': jobid,
		'use_cjt':'true'
            },
            headers: {
                'X-CloudPrint-Proxy': 'node-gcp',
                'Authorization': 'OAuth ' + this.options.accessToken
            },
            json: true
        })
    });
