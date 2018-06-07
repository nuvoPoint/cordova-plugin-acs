const bindNodeCallback = require('rxjs').bindNodeCallback;
const defer = require('rxjs').defer;

module.exports = {
    connectReader: function (readerAddress) {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'connectReader', [readerAddress]));
    },
    authenticate: function () {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'authenticate', []));
    },
    listenForAdpuResponse: function () {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'listenForAdpuResponse', []));
    },
    listenForCardStatusAvailable: function () {
        const ass = (failure, success) => {
            cordova.exec(success, failure, 'Acs', 'listenForCardStatusAvailable', []);
        };

        const setCard = bindNodeCallback(ass);

        return defer(() => setCard());
    },
    startPolling: function () {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'startPolling', []));
    },
    stopPolling: function () {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'stopPolling', []));
    },
    startScan: function () {
        const ass = (failure, success) => {
            cordova.exec(success, failure, 'Acs', 'startScan', []);
        };

        const setCard = bindNodeCallback(ass);

        return defer(() => setCard());
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