var exec = require('cordova/exec');

module.exports = {
    connectReader: function (readerAddress) {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'connectReader', [readerAddress]));
    },
    authenticate: function () {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'authenticate', []));
    },
    SetAdpuResponseCallback: function () {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'setAdpuResponseCallback', []));
    },
    SetCardAvailableCallback: function () {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'setCardAvailableCallback', []));
    },
    startPolling: function () {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'startPolling', []));
    },
    stopPolling: function () {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'stopPolling', []));
    },
    startScan: function () {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'startScan', []));
    },
    stopScan: function() {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'stopScan', []));
    },
    getConnectionState: function (){
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'getConnectionState', []));
    },
    getCardStatus: function (){
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'getCardStatus', []));
    },
    requestCardId: function (){
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'requestCardId', []));
    },
}