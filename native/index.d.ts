import { IonicNativePlugin } from '@ionic-native/core';
export interface Printer {
    name: string;
    address: string;
}
export interface DiscoveryResult {
    printers: Array<Printer>;
}
export interface PrinterStatus {
    connected: boolean;
    isReadyToPrint?: boolean;
    isPaused?: boolean;
    isReceiveBufferFull?: boolean;
    isRibbonOut?: boolean;
    isPaperOut?: boolean;
    isHeadTooHot?: boolean;
    isHeadOpen?: boolean;
    isHeadCold?: boolean;
    isPartialFormatInProgress?: boolean;
}
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
export declare class ZebraPrinter extends IonicNativePlugin {
    echo(options: {
        value: string;
    }): Promise<any>;
    print(options: {
        cpcl: string;
    }): Promise<any>;
    isConnected(): Promise<boolean>;
    printerStatus(options: {
        MACAddress: string;
    }): Promise<PrinterStatus>;
    connect(options: {
        MACAddress: string;
    }): Promise<boolean>;
    disconnect(): Promise<boolean>;
    discover(): Promise<DiscoveryResult>;
}
