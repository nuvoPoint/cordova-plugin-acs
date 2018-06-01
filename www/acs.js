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

ACS.prototype.scanForDevices = function (success, failure) {
  cordova.exec(success, failure, "Acs", "scanForDevices", []);
};

if (typeof module != 'undefined' && module.exports) {
    module.exports = ACS;
}