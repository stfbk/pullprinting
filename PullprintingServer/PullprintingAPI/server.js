const express = require('express')
var CloudPrint = require('./myGCP');
var promiseRequest = require('request-promise');
var multer = require('multer')
var upload = multer({ dest: 'uploads/' })
var download = multer({ dest: 'download/' })
var fs = require('fs');
var request = require('request');
var logGenerator = require('/home/pullprinting/FBK Print API Nodejs NOReSFRESH/logGenerator.js');
const utf8 = require('utf8');
const https = require("https"),
    fs1 = require("fs");
const options = {
    key: fs1.readFileSync("/home/pullprinting/FBK Print API Nodejs NOReSFRESH/certificates/pullprinting_fbk_eu_key.pem"),
    cert: fs1.readFileSync("/home/pullprinting/FBK Print API Nodejs NOReSFRESH/certificates/pullprinting_fbk_eu.pem")
};
const app = express();
const port = 8080;
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
//app.listen(port, () => console.log("Listening on port: ", port));
var _printerMoveToExample = "ebe62373-81ac-84db-be72-7806388dc21c" 
var adminAccessToken = "ya29.GlsBB7Kb4gwMNPow_EtVexpSRMaviWu1hwDy34uQ-pn34DRt9BQN1S-FPxt5-KeNznZ2NaDHNc3BcWNpdk5Vli8RjFlqkMEfAlpcYn9l3NeZPM1fEglFI1RLo00y";
var adminRefreshToken = "1/Ev7Jv5w3xOPdy67n7NIwMvVo7im-kx2CuBnj1P0Q2JnyT1kF4tquS4-itHrQWaeF";

app.get('/', function (req, res) {
    res.send('Stay away');
})

app.get('/list_printer', (req, res) => {
    try {
        let accessToken = req.query.access_token;
        accessToken = utf8.encode(accessToken);
        if (!isValidToken(accessToken)) {
            console.log("invalid accessToken");
            res.sendStatus(403);
        }
        else {
            var printClientUser = new CloudPrint({
                clientId: "306547292588-9nsde993lvbicbov5tt6olpeu5hbm5jv.apps.googleusercontent.com",
                clientSecret: "ODzxsWa02b3pMeDAq5LZvAg_",
                accessToken: accessToken
            });
            var printClientAdmin = new CloudPrint({
                clientId: "306547292588-9nsde993lvbicbov5tt6olpeu5hbm5jv.apps.googleusercontent.com",
                clientSecret: "ODzxsWa02b3pMeDAq5LZvAg_",
                accessToken: adminAccessToken,
                refreshToken: adminRefreshToken
            });
            printClientUser.getMail()
                .then(function (domain) {
                    //res.send(domain);
                    console.log(domain);
                    if (!isValidDomain(domain)) {
                        res.status(401);
                        res.send('User not Authorized, use an FBK account');
                    } else {
                        console.log("La MAIL è FBK.EU");
                        console.log("-------LIST PRINTER---------");
                        printClientAdmin.getPrinters()
                            .then(function (printers) {
                                res.send(printers);
                                console.log(printers);
                            }).catch(e => {
                                res.sendStatus(403);
                            });
                    }
                })
                .catch(e => {
                    res.sendStatus(403);
                });
        }
    }
    catch (e) {
        res.sendStatus(500);
    }
});

app.get('/get_mail', (req, res) => {
    let accessToken = req.query.access_token;
    console.log(accessToken);
    var printClient = new CloudPrint({
        clientId: "306547292588-9nsde993lvbicbov5tt6olpeu5hbm5jv.apps.googleusercontent.com",
        clientSecret: "ODzxsWa02b3pMeDAq5LZvAg_",
        accessToken: accessToken
    });
    console.log("-------check MAIL form user---------");
    printClient.getMail()
        .then(function (domain) {
            res.send(domain);
            console.log(domain);
        });
});

app.get('/get_mail_SC', (req, res) => {
    let accessToken = req.query.access_token;
    let sc_token = req.query.token;
    console.log("accessToken:" + accessToken);
    console.log("sc_token:" + sc_token)
    var printClient = new CloudPrint({
        clientId: "306547292588-9nsde993lvbicbov5tt6olpeu5hbm5jv.apps.googleusercontent.com",
        clientSecret: "ODzxsWa02b3pMeDAq5LZvAg_",
        accessToken: accessToken
    });
    console.log("-------check MAIL form user SMART COMMUNITY---------");
    printClient.getMail_SC(accessToken, sc_token)
        .then(function (domain) {
            res.send(domain);
            console.log(domain);
        });
});

app.post('/print_token_SC', download.single('file'), (req, res) => {
    let accessToken = req.body.access_token; 
    let JobID = req.body.JobID;
    let printer_id_to_move = req.body.printerDestinationId;
    let sc_token = req.body.token;
    if (!isValidToken(accessToken)) {
        console.log("invalid accessToken");
        res.sendStatus(400);
    } else if (!isValidPrinter(printer_id_to_move)) {
        console.log("invalid printer");
        res.sendStatus(400);
    } else if (!isValidTokenSC(sc_token)) {
        console.log("invalid CIE token");
        res.sendStatus(400);
    }
    else if (!isValidJobID(JobID)) {
        console.log("invalid jobID");
        res.sendStatus(400);
    }
    else {
        var printClientUser = new CloudPrint({
            clientId: "306547292588-9nsde993lvbicbov5tt6olpeu5hbm5jv.apps.googleusercontent.com",
            clientSecret: "ODzxsWa02b3pMeDAq5LZvAg_",
            accessToken: accessToken,
            refreshToken: ""
        });
        var printClientAdmin = new CloudPrint({
            clientId: "306547292588-9nsde993lvbicbov5tt6olpeu5hbm5jv.apps.googleusercontent.com",
            clientSecret: "ODzxsWa02b3pMeDAq5LZvAg_",
            accessToken: adminAccessToken,
            refreshToken: adminRefreshToken 
        });
        var printClientSC = new CloudPrint({
            clientId: "306547292588-9nsde993lvbicbov5tt6olpeu5hbm5jv.apps.googleusercontent.com",
            clientSecret: "ODzxsWa02b3pMeDAq5LZvAg_",
            accessToken: accessToken
        });
        var mail_SC;
        var mail_FBK;
        console.log("getMail_SC");
        printClientSC.getMail_SC(accessToken, sc_token)
            .then(function (user) {
                mail_SC = user;
                console.log(user);
                console.log("getMail_FBK");
                printClientSC.getMail_FBK()
                    .then(function (user) {
                        mail_FBK = user;
                        console.log(user);
                        console.log("getMail");
                        printClientUser.getMail()
                            .then(function (domain) {
                                console.log(domain);
                                console.log("FBK:" + mail_FBK + " ,SC:" + mail_SC);
                                if (!isValidDomain(domain) || mail_FBK != mail_SC) {
                                    res.status(401);
                                    res.send('User not Authorized, use an FBK account or CIE is not the correct one');
                                    console.log("User not Authorized, use an FBK account or CIE is not the correct one");
                                }
                                else {
                                    console.log("download job?");
                                    let p = printClientUser.download(JobID);
                                    p.then(data => {
                                        console.log("copiojob");
                                        let path = "./download/" + JobID + ".pdf";
                                        fs.writeFile(path, data, 'binary', (err) => {
                                            if (err) {
                                                console.log(err);
                                            }
                                            else {
                                                console.log("The file was saved!");
                                                console.log(path);
                                            }
                                            console.log("--------------------------------------------------");
                                            fs.readFile("./download/" + JobID + ".pdf", (err, content) => {
                                                let p2 = printClientAdmin.print(printer_id_to_move, content, "application/pdf", "docMossoCorrettamente");//qui admin
                                                p2.then(data => {
                                                    console.log("data", data);
                                                    console.log("data", data.success);
                                                    if (data.success == false) {
                                                        res.status(401);
                                                        res.send(data.message);
                                                        console.log("datamessage:" + data.message);
                                                    }
                                                    else {
                                                        console.log("Print job nella stampante " + printer_id_to_move);
                                                        console.log("--------------------------------------------------");
                                                        let p4 = printClientUser.jobLookup(JobID);
                                                        p4.then(data => {
                                                            ownerid = data.job.ownerId;
                                                            title = data.job.title;
                                                            numberOfPages = data.job.numberOfPages;
                                                            let p3 = printClientUser.deleteJob(JobID);
                                                            p3.then(data => {
                                                                console.log("job eliminato " + JobID);
                                                                fs.unlink("./download/" + JobID + ".pdf", function (err) {
                                                                    if (err) throw err;
                                                                    console.log('File deleted!');
                                                                });
                                                                logGenerator.salvaLog(ownerid, title, numberOfPages, printer_id_to_move, "SI");
                                                                res.send({ 'STATUS': 'SUBMITTED' });
                                                            }).catch(e => {
                                                                res.sendStatus(500);
                                                            });
                                                        }).catch(e => {
                                                            res.sendStatus(500);
                                                        });
                                                    }
                                                }).catch(e => {
                                                    res.sendStatus(500);
                                                });
                                            });
                                        });
                                    }).catch(e => {
                                        res.sendStatus(500);
                                    });
                                }
                            }).catch(e => {
                                res.sendStatus(403);
                            });
                    }).catch(e => {
                        res.sendStatus(403);
                    });
            }).catch(e => {
                res.sendStatus(403);
            });
    }
});

app.post('/print_token_easy', download.single('file'), (req, res) => {
    let accessToken = req.body.access_token;
    let JobID = req.body.JobID;
    let printer_id_to_move = req.body.printerDestinationId;
    console.log(printer_id_to_move);
    console.log(accessToken);
    console.log(JobID);
    console.log(adminRefreshToken);
    if (!isValidToken(accessToken)) {
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
    else {
        var printClientUser = new CloudPrint({
            clientId: "306547292588-9nsde993lvbicbov5tt6olpeu5hbm5jv.apps.googleusercontent.com",
            clientSecret: "ODzxsWa02b3pMeDAq5LZvAg_",
            accessToken: accessToken,
            refreshToken: "" 
        });
        var printClientAdmin = new CloudPrint({
            clientId: "306547292588-9nsde993lvbicbov5tt6olpeu5hbm5jv.apps.googleusercontent.com",
            clientSecret: "ODzxsWa02b3pMeDAq5LZvAg_",
            accessToken: adminAccessToken,
            refreshToken: adminRefreshToken 
        });
        var mail_FBK;
        console.log("getMail_FBK");
        printClientUser.getMail_FBK()
            .then(function (user) {
                mail_FBK = user;
                console.log(user);

                console.log("getMail");
                printClientUser.getMail()
                    .then(function (domain) {
                        console.log(domain);
                        console.log("FBK:" + mail_FBK);
                        console.log(isValidDomain(domain));
                        if (!isValidDomain(domain)) {
                            res.status(401);
                            res.send('User not Authorized, use an FBK account');
                            console.log("User not Authorized, use an FBK account");
                        }
                        else {
                            let p = printClientUser.download(JobID);
                            p.then(data => {
                                console.log("copiojob");
                                let path = "./download/" + JobID + ".pdf";
                                fs.writeFile(path, data, 'binary', (err) => {
                                    if (err) {
                                        console.log(err);
                                    }
                                    else {
                                        console.log("The file was saved!");
                                        console.log(path);
                                    }
                                    console.log("--------------------------------------------------");
                                    fs.readFile("./download/" + JobID + ".pdf", (err, content) => {
                                        let p2 = printClientAdmin.print(printer_id_to_move, content, "application/pdf", "docMossoCorrettamente");//qui admin
                                        p2.then(data => {
                                            console.log(data);
                                            console.log("data", data.success);
                                            if (data.success == false) {
                                                res.status(401);
                                                res.send(data.message);
                                                console.log("datamessage:" + data.message);
                                            }
                                            else {
                                                console.log("Print job nella stampante " + printer_id_to_move);
                                                console.log("--------------------------------------------------");
                                                let p4 = printClientUser.jobLookup(JobID);
                                                p4.then(data => {
                                                    ownerid = data.job.ownerId;
                                                    title = data.job.title;
                                                    numberOfPages = data.job.numberOfPages;
                                                    let p3 = printClientUser.deleteJob(JobID);
                                                    p3.then(data => {
                                                        console.log("job eliminato " + JobID);
                                                        fs.unlink("./download/" + JobID + ".pdf", function (err) {
                                                            if (err) throw err;
                                                            console.log('File deleted!');
                                                        });
                                                        logGenerator.salvaLog(ownerid, title, numberOfPages, printer_id_to_move, "NO");
                                                        res.send({ 'STATUS': 'SUBMITTED' });
                                                    }).catch(e => {
                                                        res.sendStatus(500);
                                                    });
                                                }).catch(e => {
                                                    res.sendStatus(500);
                                                });
                                            }
                                        }).catch(e => {
                                            res.sendStatus(500);
                                        });
                                    });
                                });
                            }).catch(e => {
                                res.sendStatus(500);
                            });
                        }
                    }).catch(e => {
                        res.sendStatus(403);
                    });
            }).catch(e => {
                res.sendStatus(403);
            });
    }
});

app.post('/deletejob', (req, res) => {
    let accessToken = req.query.access_token;
    let JobID = req.query.jobid;
    var printClient = new CloudPrint({
        clientId: "306547292588-9nsde993lvbicbov5tt6olpeu5hbm5jv.apps.googleusercontent.com",
        clientSecret: "ODzxsWa02b3pMeDAq5LZvAg_",
        accessToken: accessToken
    });
    console.log("-------check valid_user---------");
    printClient.deleteJob(JobID)
        .then(function (status) {
            res.send(status);
            console.log(status);
        });
});

/*app.post('/newJobs', upload.single('file'), (req, res) => {
    for (i = 0; i < req.body.nFile; i++) {
        let accessToken = req.body.access_token;
        let filePath = req.file.path;
        console.log("filePath: " + filePath);
        let contentType = req.body.contentType;
        let title = req.body.title;
        var printClient = new CloudPrint({
            clientId: "306547292588-9nsde993lvbicbov5tt6olpeu5hbm5jv.apps.googleusercontent.com",
            clientSecret: "ODzxsWa02b3pMeDAq5LZvAg_",
            accessToken: accessToken,
        });
        fs.readFile(filePath, (err, content) => {
            let p = printClient.print("a8d6c750-f342-f616-5a67-9188eacfdee2", content, contentType, title);
            p.then(data => {
                console.log(data);
                var jobID = data.job.id;
                res.send({ 'STATUS': 'SUBMITTED', 'ID': jobID });
            });
        });
    }
});*/

function isValidDomain(domain) {
    var __dirname = "/home/pullprinting/"
    var fs = require('fs'),
        contents = fs.readFileSync(__dirname + 'domain.txt', 'utf8');
    data = contents.split("\n");
    valid_domain = data.includes(domain);
    console.log("controllo dominio..." + valid_domain);
    return valid_domain;
}

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
    if (accessToken === undefined || accessToken === null || accessToken === "" || accessToken.substring(0, 5) !== "ya29." || accessToken.length !== 135 || isAlphanumeric(accessToken) !== true) { return false; }
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