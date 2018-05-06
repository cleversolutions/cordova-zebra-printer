var exec = require('cordova/exec');

exports.echo = function (arg0, success, error) {
    exec(success, error, 'ZebraPrinter', 'echo', [arg0]);
};

exports.discover = function (success, error) {
    exec(success, error, 'ZebraPrinter', 'discover', []);
};

exports.connect = function (address, success, error) {
    exec(success, error, 'ZebraPrinter', 'connect', [address]);
};

exports.disconnect = function (success, error) {
    exec(success, error, 'ZebraPrinter', 'disconnect', []);
};

exports.isConnected = function (success, error) {
    exec(success, error, 'ZebraPrinter', 'isConnected', []);
};

exports.print = function (cpcl, success, error) {
    exec(success, error, 'ZebraPrinter', 'print', [cpcl]);
};