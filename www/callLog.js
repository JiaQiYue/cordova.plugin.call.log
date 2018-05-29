var exec = require('cordova/exec');

var callLog = module.exports;

callLog.getCallLog = function (phoneNumber, success, error) {

        cordova.exec(success,error,'callLog','getCallLog',[phoneNumber]);

}