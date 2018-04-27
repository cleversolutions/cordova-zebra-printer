var exec = require('cordova/exec');

exports.echo = function (arg0, success, error) {
    exec(success, error, 'ZebraPrinter', 'echo', [arg0]);
};

exports.discover = function (success, error) {
    exec(success, error, 'ZebraPrinter', 'discover');
};