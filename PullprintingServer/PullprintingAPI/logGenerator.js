

var __dirname = "/home/pullprinting/log/";
const path = require('path');
var fs = require('fs');


module.exports = {
 salvaLog: function(user,doc,n_pag,stampante,cie){
var d = new Date();  

 if (!fs.existsSync(__dirname)){
    fs.mkdirSync(__dirname);
}

fs.appendFile(__dirname + d.getFullYear()+"-"+(d.getMonth()+1)+"-"+d.getDate()+ '.txt', 
  "user:"+user+" doc:" + doc + " n_pag:" + n_pag + " stampante:" + stampante+" " +" CIE:"+cie  + " "+ d.getHours()+":"+d.getMinutes()+":"+d.getSeconds()+":"+d.getMilliseconds()+"\n", 
 function (err) {
    if (err) throw err;
    console.log('Log salvato!');
  });
  }
};

