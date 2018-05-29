// Empty constructor
var ACS = {}

// The function that passes work along to native shells
// Message is a string, duration may be 'long' or 'short'

ACS.connectReader = function (readerAddress, success, failure) {
  cordova.exec(success, failure, "CardReader", "connectReader", [readerAddress]);
};

window.ACS = ACS;