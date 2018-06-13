module.exports = {
    connectReader: function (readerAddress) {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'connectReader', [readerAddress]));
    },
    disconnectReader: function () {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'disconnectReader', []));
     },
    authenticate: function () {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'authenticate', []));
    },
    listenForAdpuResponse: function (resolve, reject) {
        return cordova.exec(resolve, reject, 'Acs', 'listenForAdpuResponse', []);
    },
    listenForCardStatusAvailable: function (resolve, reject) {
        return cordova.exec(resolve, reject, 'Acs', 'listenForCardStatusAvailable', [])
    },
        listenForConnectionState: function () {
         return cordova.exec(resolve, reject, 'Acs', 'listenForConnectionState', [])
        },
    startPolling: function () {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'startPolling', []));
    },
    stopPolling: function () {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'stopPolling', []));
    },
    startScan: function (resolve, reject) {
        return cordova.exec(resolve, reject, 'Acs', 'startScan', []);
    },
    stopScan: function () {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'stopScan', []));
    },
    getCardStatus: function () {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'getCardStatus', []));
    },
    requestCardId: function () {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'requestCardId', []));
    },
    powerOnCard:  function (){
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'powerOnCard', []));
    },
    powerOffCard:  function (){
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'powerOffCard', []));
    },
}