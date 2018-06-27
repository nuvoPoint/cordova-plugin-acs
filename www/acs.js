const acs = {
    connectGatt: function (macAddress) {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'connectGatt', [macAddress]));
    },
    disconnectGatt: function () {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'disconnectGatt', []));
    },
        detectReader: function () {
            return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'detectReader', []));
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
        listenForNfcConnectionState: function (resolve, reject) {
            return cordova.exec(resolve, reject, 'Acs', 'listenForNfcConnectionState', [])
        },
    startScan: function (resolve, reject) {
        return cordova.exec(resolve, reject, 'Acs', 'startScan', []);
    },
    stopScan: function (resolve, reject) {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'stopScan', []));
    },
    transmitAdpuCommand: function (command) {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'transmitAdpuCommand', [command]));
    },
    transmitEscapeCommand: function (command) {
        return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'transmitEscapeCommand', [command]));
    },
        requestBt: function (resolve, reject) {
            return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'requestBt', []));
        },
                requestBtPermissions: function (resolve, reject) {
                    return new Promise((resolve, reject) => cordova.exec(resolve, reject, 'Acs', 'requestBtPermissions', []));
                },
    AcsErrorCodes: {},
    AcsCardStatus: {},
    AcsConnectionState: {},
};

(function (AcsErrorCodes) {
    AcsErrorCodes[AcsErrorCodes["ERR_UNKNOWN"] = 0] = "ERR_UNKNOWN";
    AcsErrorCodes[AcsErrorCodes["ERR_READER_NOT_INITIALIZED"] = 1] = "ERR_READER_NOT_INITIALIZED";
    AcsErrorCodes[AcsErrorCodes["ERR_OPERATION_FAILED"] = 2] = "ERR_OPERATION_FAILED";
    AcsErrorCodes[AcsErrorCodes["ERR_OPERATION_TIMED_OUT"] = 3] = "ERR_OPERATION_TIMED_OUT";
    AcsErrorCodes[AcsErrorCodes["ERR_BT_IS_OFF"] = 4] = "ERR_BT_IS_OFF";
    AcsErrorCodes[AcsErrorCodes["ERR_BT_ERROR"] = 5] = "ERR_BT_ERROR";
    AcsErrorCodes[AcsErrorCodes["ERR_SCAN_IN_PROGRESS"] = 6] = "ERR_SCAN_IN_PROGRESS";
    AcsErrorCodes[AcsErrorCodes["ERR_SCAN_FAILED"] = 7] = "ERR_SCAN_FAILED";
    AcsErrorCodes[AcsErrorCodes["ERR_GATT_ALREADY_CONNECTED"] = 8] = "ERR_GATT_ALREADY_CONNECTED";
    AcsErrorCodes[AcsErrorCodes["ERR_GATT_CONNECTION_IN_PROGRESS"] = 9] = "ERR_GATT_CONNECTION_IN_PROGRESS";
    AcsErrorCodes[AcsErrorCodes["ERR_GATT_CONNECTION_CANCELLED"] = 10] = "ERR_GATT_CONNECTION_CANCELLED";
    AcsErrorCodes[AcsErrorCodes["ERR_READER_TYPE_NOT_SUPPORTED"] = 11] = "ERR_READER_TYPE_NOT_SUPPORTED";
})(acs.AcsErrorCodes);

(function (AcsCardStatus) {
    AcsCardStatus[AcsCardStatus["CARD_ABSENT"] = 1] = "CARD_ABSENT";
    AcsCardStatus[AcsCardStatus["CARD_PRESENT"] = 2] = "CARD_PRESENT";
    AcsCardStatus[AcsCardStatus["CARD_POWER_SAVING_MODE"] = 3] = "CARD_POWER_SAVING_MODE";
    AcsCardStatus[AcsCardStatus["CARD_POWERED"] = 4] = "CARD_POWERED";
})(acs.AcsCardStatus);

(function (AcsConnectionState) {
    AcsConnectionState[AcsConnectionState["CON_UNKNOWN"] = 0] = "CON_UNKNOWN";
    AcsConnectionState[AcsConnectionState["CON_DISCONNECTED"] = 1] = "CON_DISCONNECTED";
    AcsConnectionState[AcsConnectionState["CON_CONNECTED"] = 2] = "CON_CONNECTED";
    AcsConnectionState[AcsConnectionState["CON_CONNECTING"] = 3] = "CON_CONNECTING";
    AcsConnectionState[AcsConnectionState["CON_DISCONNECTING"] = 4] = "CON_DISCONNECTING";
})(acs.AcsConnectionState);

module.exports = acs;