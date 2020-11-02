const express = require('express')
var CloudPrint = require('./myGCP');
var promiseRequest = require('request-promise');
var multer = require('multer')
var download = multer({ dest: 'download/' })
//var fs = require('fs');
var fs = require('memfs');
var request = require('request');
var logGenerator = require('./logGenerator.js');
const utf8 = require('utf8');

const https = require("https"),
    fs1 = require("fs");

const options = {
    key: fs1.readFileSync("./certificates/pullprinting_key.pem"),
    cert: fs1.readFileSync("./certificates/pullprinting.pem")
};

const app = express();
const port = 8080;

const rateLimit = require("express-rate-limit");
const limiter = rateLimit({
  windowMs: 1 * 60 * 1000, // 1 minute
  max: 50 // limit each IP to 50 requests per windowMs
});
app.use(limiter); //Limit all requests

var helmet = require('helmet');
app.use(helmet());
app.disable('x-powered-by');
app.disable('X-DNS-Prefetch-Control');
app.disable('X-Frame-Options');
app.disable('Strict-Transport-Security');
app.disable('X-Download-Options');
app.disable('X-Content-Type-Options');
app.disable('X-XSS-Protection');
app.disable('Content-Type');
app.disable('Content-Length');
app.disable('ETag');
app.disable('Date');

app.listen(8080);
https.createServer(options, app).listen(8000);

var _printerIDSpooler = process.env._printerIDSpooler;
var _printerMoveToExample = process.env._printerMoveToExample;
var adminAccessToken = process.env.adminAccessToken;
var adminRefreshToken = process.env.adminRefreshToken;

var _clientId = process.env.clientId
var _clientSecret = process.env.clientSecret

app.get('/', function (req, res) {
    res.send('Stay away') // Express will catch this on its own.
})

app.get('/printer_list_request', (req, res) => {
    try {
            var printClientAdmin = new CloudPrint({
                clientId: _clientId,
                clientSecret: _clientSecret,
                accessToken: adminAccessToken,
                refreshToken: adminRefreshToken
            });
                printClientAdmin.getPrinters()
                    .then(function (printers) {
                            res.send(printers);
                            }).catch(e => {
                                res.sendStatus(403);
                            });              
    }
    catch (e) {
		console.log(e);
        res.sendStatus(500);
    }
});



function getFilesizeInBytes(filename) {
    const stats = fs.statSync(filename)
    const fileSizeInBytes = stats.size
    return fileSizeInBytes
}

app.post('/print_secure_request', download.single('file'), (req, res) => {

    let accessToken = req.body.access_token;
    let JobID = req.body.JobID;
    let printer_id_to_move = req.body.printerDestinationId;
    let sc_token = req.body.token;


    if (!isValidToken(accessToken)) {
        console.log("invalid accessToken");
        res.sendStatus(400);
    } 
	if (!isValidPrinter(printer_id_to_move)) {
        console.log("invalid printer");
        res.sendStatus(400);
    }
	if (!isValidTokenSC(sc_token)) {
        console.log("invalid CIE token");
        res.sendStatus(400);
    }
	if (!isValidJobID(JobID)) {
        console.log("invalid jobID");
        res.sendStatus(400);
    }
   
	var printClientUser = new CloudPrint({
		clientId: _clientId,
		clientSecret: _clientSecret,
		accessToken: accessToken,
		refreshToken: ""
	});

	var printClientAdmin = new CloudPrint({
		clientId: _clientId,
		clientSecret: _clientSecret,
		accessToken: adminAccessToken,
		refreshToken: adminRefreshToken
	});

	var printClientSC = new CloudPrint({
		clientId: _clientId,
		clientSecret: _clientSecret,
		accessToken: accessToken
	});

	var mail_SC;
	var mail_Enterprise;
	
	printClientSC.getMail_SC(accessToken, sc_token, _clientId, _clientSecret)
		.then(function (user) {
			mail_SC = user;
			printClientSC.getMail_Enterprise()
				.then(function (user) {
					mail_Enterprise = user;
					printClientUser.getMail()
						.then(function (domain) {
							if (!isValidDomain(domain) || mail_Enterprise != mail_SC) {
								res.status(401);
								res.send('User not Authorized, use an Enterprise account or CIE is not the correct one');
							}
							else {
								let p0 = printClientUser.jobTicket(JobID);
								p0.then(data => {
									
									let token_propriety= JSON.stringify(data);
									
									let prefix = "/" //"./download/"
									let p = printClientUser.download(JobID);
									
									p.then(data => {
										let path = prefix + JobID + ".pdf";
										fs.writeFile(path, data, 'binary', (err) => {
											if (err) {
												console.log(err);
											}
											//stampa su stampante corretta
											fs.readFile(prefix + JobID + ".pdf", (err, content) => {
												let p2 = printClientAdmin.print(printer_id_to_move, content, "application/pdf", "docMossoCorrettamente", token_propriety);
												p2.then(data => {
													if (data.success == false) {
														res.status(401);
														res.send(data.message);
													}
													else {
														let p4 = printClientUser.jobLookup(JobID);
														p4.then(data => {
															
															ownerid = data.job.ownerId;
															title = data.job.title;
															numberOfPages = data.job.numberOfPages;
															
															let p3 = printClientUser.deleteJob(JobID);
															p3.then(data => {
																fs.unlink(prefix + JobID + ".pdf", function (err) {
																	if (err) throw err;
																});
																logGenerator.salvaLog(ownerid, title, numberOfPages, printer_id_to_move, color, "SI");
																res.send({ 'STATUS': 'SUBMITTED' });
															}).catch(e => {
																console.log(e);
																res.sendStatus(500);
															});
														}).catch(e => {
															console.log(e);
															res.sendStatus(500);
														});
													}
												}).catch(e => {
													console.log(e);
													res.sendStatus(500);
												});
											});
										});
									}).catch(e => {
										console.log(e);
										res.sendStatus(500);
									});
								}
								).catch(e => {
									console.log(e);
									res.sendStatus(403);
								});
							}
						}).catch(e => {
							console.log(e);
							res.sendStatus(403);
						});
				}).catch(e => {
					console.log(e);
					res.sendStatus(403);
				});
		}).catch(e => {
			console.log(e);
			res.sendStatus(403);
		});
});

app.post('/print_request', download.single('file'), (req, res) => {

    let accessToken = req.body.access_token;
    let JobID = req.body.JobID;
    let printer_id_to_move = req.body.printerDestinationId;

    if(!isValidToken(accessToken)){
		console.log("invalid accessToken");
		res.sendStatus(400);
	}
	
    if (!isValidPrinter(printer_id_to_move)) {
        console.log("invalid printer");
        res.sendStatus(400);
    }
	
    if (!isValidJobID(JobID)) {
        console.log("invalid job");
        res.sendStatus(400);
    }

	var printClientUser = new CloudPrint({
		clientId: _clientId,
		clientSecret: _clientSecret,
		accessToken: accessToken,
		refreshToken: "" 
	});

	var printClientAdmin = new CloudPrint({
		clientId: _clientId,
		clientSecret: _clientSecret,
		accessToken: adminAccessToken,
		refreshToken: adminRefreshToken
	});

	var mail_Enterprise;
	printClientUser.getMail_Enterprise()
		.then(function (user) {
			mail_Enterprise = user;
			printClientUser.getMail()
				.then(function (domain) {
					if (!isValidDomain(domain)) {
						res.status(401);
						res.send('User not Authorized, use an Enterprise account');
					}
					else {
						let p0 = printClientUser.jobTicket(JobID);
						p0.then(data => {

							let token_propriety= JSON.stringify(data);

							let prefix = "/" //"./download/"
							
							let p = printClientUser.download(JobID);
							p.then(data => {
								let path = prefix + JobID + ".pdf";
								fs.writeFile(path, data, 'binary', (err) => {
									if (err) {
										console.log(err);
									}
									
									fs.readFile(prefix + JobID + ".pdf", (err, content) => {
										let p2 = printClientAdmin.print(printer_id_to_move, content, "application/pdf", "docMossoCorrettamente",token_propriety);//qui admin
										p2.then(data => {
											if (data.success == false) {
												res.status(401);
												res.send(data.message);
											}
											else {
												let p4 = printClientUser.jobLookup(JobID);
												p4.then(data => {
													ownerid = data.job.ownerId;
													title = data.job.title;
													numberOfPages = data.job.numberOfPages;
													
													let p3 = printClientUser.deleteJob(JobID);
													p3.then(data => {
														fs.unlink(prefix + JobID + ".pdf", function (err) {
															if (err) throw err;
														});
														logGenerator.salvaLog(ownerid, title, numberOfPages, printer_id_to_move, color, "NO");
														res.send({ 'STATUS': 'SUBMITTED' });
													}).catch(e => {
														console.log(e);
														res.sendStatus(500);
													});
												}).catch(e => {
													console.log(e);
													res.sendStatus(500);
												});
											}
										}).catch(e => {
											console.log(e);
											res.sendStatus(500);
										});
									});
								});
							});
						}).catch(e => {
							console.log(e);
							res.sendStatus(500);
						});
					}
				}).catch(e => {
					console.log(e);
					res.sendStatus(403);
					
				});
		}).catch(e => {
			console.log(e);
			res.sendStatus(403);
		});
});





app.post('/deletejob', (req, res) => {
    let accessToken = req.query.access_token;
    let JobID = req.query.jobid;

    var printClient = new CloudPrint({
        clientId: _clientId,
        clientSecret: _clientSecret,
        accessToken: accessToken
    });
    printClient.deleteJob(JobID)
        .then(function (status) {
            res.send(status);
        });

});


function isValidDomain(domain) {
    var fs = require('fs'),
        contents = fs.readFileSync('./domain.txt', 'utf8');
    data = contents.split("\n");
    valid_domain = data.includes(domain);
    return valid_domain;
}


app.get('/printer_model', (req, res) => {
	//console.log("/printer_model");
    try {
        let accessToken = req.query.access_token;
	let printer_id=req.query.printer_id;
	//console.log(printer_id);
        //console.log(accessToken);

        /*if (!isValidToken(accessToken)) {
            console.log("invalid accessToken");
            res.sendStatus(403);
        }
        else*/ {
            var printClientUser = new CloudPrint({
                clientId: _clientId,
                clientSecret: _clientSecret,
                accessToken: accessToken
                //refreshToken: refreshToken
            });

            var printClientAdmin = new CloudPrint({
                clientId: _clientId,
                clientSecret: _clientSecret,
                accessToken: adminAccessToken,
                refreshToken: adminRefreshToken
            });
            //console.log("-------check valid_user---------");
            //if printClientUser is right user admin token for showing printer


            printClientUser.getMail()
                .then(function (domain) {
                    //res.send(domain);
                    //console.log(domain);
                    if (!isValidDomain(domain)) {
                        res.status(401);
                        res.send('User not Authorized, use an Enterprise account');
                    } else {
                        //console.log("La MAIL è Enterprise");
                        printClientAdmin.getPrinter(printer_id)
                            .then(
				function (models) {
                                res.send(models.name);
                                //console.log(models.name);
                            }).catch(e => {
								console.log(e);
                                res.sendStatus(403);
                            });
                    }
                })
                .catch(e => {
                    console.log(e);
                    res.sendStatus(403);
                });
        }
    }
    catch (e) {
        console.log(e);
        res.sendStatus(500);
    }
});





function extractTokenColorInfo() {

}


//leave last one
app.get('/*', function (req, res) {
    res.sendStatus(404);
});

app.post('/*', function (req, res) {
    res.sendStatus(501);
});

app.put('/*', function (req, res) {
    res.sendStatus(405);
});



function isValidToken(accessToken) {
if (accessToken === undefined || accessToken === null || accessToken === "" || accessToken.substring(0, 5) !== "ya29." || isAlphanumeric(accessToken) !== true) 
	{ return false; }
    else { return true; }
}

function isValidPrinter(printer) {
    if (printer === undefined || printer === null || printer === "" || printer.length !== 36 || isAlphanumeric(printer) !== true) { return false; }
    else { return true; }
}

function isValidTokenSC(sc_token) {
    if (sc_token === undefined || sc_token === null || sc_token === "" || sc_token.length !== 36 || isAlphanumeric(sc_token) !== true) { return false; }
    else { return true; }
}

function isValidJobID(jobID) {
    if (jobID === undefined || jobID === null || jobID === "" || jobID.length !== 36 || isAlphanumeric(jobID) !== true) { return false; }
    else { return true; }
}


function isAlphanumeric(str) {
    regexp = /^[a-z0-9_\-\.]+$/i;

    if (regexp.test(str)) {
        return true;
    }
    else {
        return false;
    }
}
