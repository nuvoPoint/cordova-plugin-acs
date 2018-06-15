module.exports = {
    connectReader: function (readerAddress) {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'connectReader', [readerAddress]));
    },
    disconnectReader: function () {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'disconnectReader', []));
    },
    enableNotifications: function () {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'enableNotifications', []));
    },
    authenticate: function () {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'authenticate', []));
    },
    listenForAdpuResponse: function (resolve, reject) {
        return cordova.exec(resolve, reject, 'Acs', 'listenForAdpuResponse', []);
    },
    listenForEscapeResponse: function (resolve, reject) {
        return cordova.exec(resolve, reject, 'Acs', 'listenForEscapeResponse', []);
    },
    listenForCardStatus: function (resolve, reject) {
        return cordova.exec(resolve, reject, 'Acs', 'listenForCardStatus', [])
    },
    listenForConnectionState: function (resolve, reject) {
        return cordova.exec(resolve, reject, 'Acs', 'listenForConnectionState', [])
    },
    startScan: function (resolve, reject) {
        return cordova.exec(resolve, reject, 'Acs', 'startScan', []);
    },
    stopScan: function () {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'stopScan', []));
    },
    transmitAdpuCommand: function () {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'transmitAdpuCommand', []));
    },
    transmitEscapeCommand: function () {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'transmitEscapeCommand', []));
    },
}