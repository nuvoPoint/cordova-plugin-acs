// Empty constructor
function Acs() { }

// The function that passes work along to native shells
// Message is a string, duration may be 'long' or 'short'

Acs.connectReader = function (readerAddress, success, failure) {
  cordova.exec(success, failure, "CardReader", "connectReader", [readerAddress]);
};


// Installation constructor that binds Acs to window
Acs.install = function () {
  if (!window.plugins) {
    window.plugins = {};
  }
  window.plugins.acs = new Acs();
  return window.plugins.acs;
};
cordova.addConstructor(Acs.install);