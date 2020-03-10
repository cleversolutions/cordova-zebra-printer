var __extends = (this && this.__extends) || (function () {
    var extendStatics = Object.setPrototypeOf ||
        ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
        function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
/**
 * This is a template for new plugin wrappers
 *
 * TODO:
 * - Add/Change information below
 * - Document usage (importing, executing main functionality)
 * - Remove any imports that you are not using
 * - Remove all the comments included in this template, EXCEPT the @Plugin wrapper docs and any other docs you added
 * - Remove this note
 *
 */
import { Injectable } from '@angular/core';
import { Plugin, Cordova, IonicNativePlugin } from '@ionic-native/core';
/**
 * @name Zebra Printer
 * @description
 * This plugin does something
 *
 * @usage
 * ```typescript
 * import { ZebraPrinter } from '@ionic-native/zebra-printer';
 *
 *
 * constructor(private zebraPrinter: ZebraPrinter) { }
 *
 * ...
 *
 *
 * this.zebraPrinter.functionName('Hello', 123)
 *   .then((res: any) => console.log(res))
 *   .catch((error: any) => console.error(error));
 *
 * ```
 */
var ZebraPrinter = (function (_super) {
    __extends(ZebraPrinter, _super);
    function ZebraPrinter() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    ZebraPrinter.prototype.print = function (cpcl) { return; };
    ZebraPrinter.prototype.isConnected = function () { return; };
    ZebraPrinter.prototype.printerStatus = function (adderss) { return; };
    ZebraPrinter.prototype.connect = function (adress) { return; };
    ZebraPrinter.prototype.disconnect = function () { return; };
    ZebraPrinter.prototype.discover = function () { return; };
    ZebraPrinter.decorators = [
        { type: Injectable },
    ];
    /** @nocollapse */
    ZebraPrinter.ctorParameters = function () { return []; };
    __decorate([
        Cordova(),
        __metadata("design:type", Function),
        __metadata("design:paramtypes", [String]),
        __metadata("design:returntype", Promise)
    ], ZebraPrinter.prototype, "print", null);
    __decorate([
        Cordova(),
        __metadata("design:type", Function),
        __metadata("design:paramtypes", []),
        __metadata("design:returntype", Promise)
    ], ZebraPrinter.prototype, "isConnected", null);
    __decorate([
        Cordova(),
        __metadata("design:type", Function),
        __metadata("design:paramtypes", [String]),
        __metadata("design:returntype", Promise)
    ], ZebraPrinter.prototype, "printerStatus", null);
    __decorate([
        Cordova(),
        __metadata("design:type", Function),
        __metadata("design:paramtypes", [String]),
        __metadata("design:returntype", Promise)
    ], ZebraPrinter.prototype, "connect", null);
    __decorate([
        Cordova(),
        __metadata("design:type", Function),
        __metadata("design:paramtypes", []),
        __metadata("design:returntype", Promise)
    ], ZebraPrinter.prototype, "disconnect", null);
    __decorate([
        Cordova(),
        __metadata("design:type", Function),
        __metadata("design:paramtypes", []),
        __metadata("design:returntype", Promise)
    ], ZebraPrinter.prototype, "discover", null);
    __decorate([
        Cordova(),
        __metadata("design:type", Function),
        __metadata("design:paramtypes", []),
        __metadata("design:returntype", Promise)
    ], ZebraPrinter.prototype, "requestUsbPermission", null);
    __decorate([
        Cordova(),
        __metadata("design:type", Function),
        __metadata("design:paramtypes", []),
        __metadata("design:returntype", Promise)
    ], ZebraPrinter.prototype, "connectUSB", null);
    ZebraPrinter = __decorate([
        Plugin({
            pluginName: 'ZebraPrinter',
            plugin: 'ca-cleversolutions-zebraprinter',
            pluginRef: 'cordova.plugins.zebraPrinter',
            repo: 'git@github.com:cleversolutions/cordova-zebra-printer.git',
            install: '',
            installVariables: [],
            platforms: ['Android', 'iOS'] // Array of platforms supported, example: ['Android', 'iOS']
        })
    ], ZebraPrinter);
    return ZebraPrinter;
}(IonicNativePlugin));
export { ZebraPrinter };
//# sourceMappingURL=index.js.map