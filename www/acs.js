var exec = cordova.require('cordova/exec');

// Empty constructor
var ACS = function () {
    console.log('ACS instanced');
};

ACS.prototype.init = function (msg, onSuccess, onError) {
    var errorCallback = function (obj) {
        onError(obj);
    };

    var successCallback = function (obj) {
        onSuccess(obj);
    };

    exec(successCallback, errorCallback, 'Acs', 'init', [msg]);
};

// The function that passes work along to native shells
// Message is a string, duration may be 'long' or 'short'

ACS.prototype.connectReader = function (readerAddress, success, failure) {
  cordova.exec(success, failure, "Acs", "connectReader", [readerAddress]);
};

ACS.prototype.getConnectionStatus = function (success, failure) {
  cordova.exec(success, failure, "Acs", "getConnectionStatus", []);
};

ACS.prototype.getCardStatus = function (success, failure) {
  cordova.exec(success, failure, "Acs", "getCardStatus", []);
};

ACS.prototype.listenForADPU = function (success, failure) {
  cordova.exec(success, failure, "Acs", "listenForADPU", []);
};

ACS.prototype.stopListeningForADPU = function (success, failure) {
  cordova.exec(success, failure, "Acs", "stopListeningForADPU", []);
};

ACS.prototype.startScan = function (success, failure) {
  cordova.exec(success, failure, "Acs", "startScan", []);
};

ACS.prototype.stopScan = function (success, failure) {
  cordova.exec(success, failure, "Acs", "stopScan", []);
};

if (typeof module != 'undefined' && module.exports) {
    module.exports = ACS;
}