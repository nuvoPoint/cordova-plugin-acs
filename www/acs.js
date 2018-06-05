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

ACS.prototype.authenticate = function (success, failure) {
  cordova.exec(success, failure, "Acs", "authenticate", []);
};

ACS.prototype.SetADPUResponseCallback = function (success, failure) {
  cordova.exec(success, failure, "Acs", "SetADPUResponseCallback", []);
};

ACS.prototype.SetCardAvailableCallback = function (success, failure) {
  cordova.exec(success, failure, "Acs", "SetCardAvailableCallback", []);
};


ACS.prototype.startPolling = function (success, failure) {
  cordova.exec(success, failure, "Acs", "startPolling", []);
};

ACS.prototype.stopPolling = function (success, failure) {
  cordova.exec(success, failure, "Acs", "stopPolling", []);
};


ACS.prototype.startScan = function (success, failure) {
  cordova.exec(success, failure, "Acs", "startScan", []);
};

ACS.prototype.stopScan = function (success, failure) {
  cordova.exec(success, failure, "Acs", "stopScan", []);
};

ACS.prototype.getConnectionState = function (success, failure) {
  cordova.exec(success, failure, "Acs", "getConnectionState", []);
};

ACS.prototype.getCardStatus = function (success, failure) {
  cordova.exec(success, failure, "Acs", "getCardStatus", []);
};

ACS.prototype.requestCardId = function (success, failure) {
  cordova.exec(success, failure, "Acs", "requestCardId", []);
};


if (typeof module != 'undefined' && module.exports) {
    module.exports = ACS;
}