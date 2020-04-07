const express = require('express')
const https = require("https"),
  fs = require("fs");

const options = {
  key: fs.readFileSync("/home/pullprinting/FBK Print API Nodejs NOReSFRESH/certificates/pullprinting_fbk_eu_key.pem"),
  cert: fs.readFileSync("/home/pullprinting/FBK Print API Nodejs NOReSFRESH/certificates/pullprinting_fbk_eu.pem")
};

const app = express();

app.use((req, res) => {
  res.writeHead(200);
  res.end("hello world\n");
});

app.listen(8080);

https.createServer(options, app).listen(8080);

