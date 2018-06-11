module.exports = {
    connectReader: function (readerAddress) {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'connectReader', [readerAddress]));
    },
    authenticate: function () {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'authenticate', []));
    },
    listenForAdpuResponse: function (resolve, reject) {
        cordova.exec(resolve, reject, 'Acs', 'listenForAdpuResponse', []);
    },
    listenForCardStatusAvailable: function (resolve, reject) {
        cordova.exec(resolve, reject, 'Acs', 'listenForCardStatusAvailable', [])
    },
    startPolling: function () {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'startPolling', []));
    },
    stopPolling: function () {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'stopPolling', []));
    },
    startScan: function () {
        cordova.exec(success, failure, 'Acs', 'startScan', []);
    },
    stopScan: function () {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'stopScan', []));
    },
    getConnectionState: function () {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'getConnectionState', []));
    },
    getCardStatus: function () {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'getCardStatus', []));
    },
    requestCardId: function () {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'requestCardId', []));
    },
}