import Foundation
import ExternalAccessory

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitor.ionicframework.com/docs/plugins/ios
 */
@objc(ZebraPlugin)
public class ZebraPlugin: CDVPlugin {
    var printerConnection: ZebraPrinterConnection?
    
    @objc func echo(command: CDVInvokedUrlCommand) {
        var pluginResult = CDVPluginResult(
          status: CDVCommandStatus_ERROR
        )
        let value = command.arguments[0] as? String ?? ""
        pluginResult = CDVPluginResult(
            status: CDVCommandStatus_OK,
            value: value
        )

        self.commandDelegate!.send(
          pluginResult,
          callbackId: command.callbackId
        )
    }

    @objc func discover(command: CDVInvokedUrlCommand){
        
        let manager = EAAccessoryManager.shared()
        let accessories = manager.connectedAccessories
        
        var devices = [[String:AnyObject]]
        accessories.forEach { (accessory) in
            let name = accessory.name
            var device = [String: AnyObject] 
            device["name"] = name
            device["address"] = accessory.serialNumber
            device["manufacturer"] = accessory.manufacturer
            device["modelNumber"] = accessory.modelNumber
            device["connected"] = accessory.isConnected
            devices.append(device)
        }
        pluginResult = CDVPluginResult(
            status: CDVCommandStatus_OK,
            printers: devices
        )
        self.commandDelegate!.send(
          pluginResult,
          callbackId: command.callbackId
        )
    }
    
    // @objc func print(_ call: CAPPluginCall) {
    //     //TODO
    //     let cpcl = call.getString("cpcl") ?? ""
    //     if( isConnected()){
    //         let data = cpcl.data(using: .utf8)
    //         var error: NSError?
    //         printerConnection!.write(data, error:&error)
    //         if let actualError = error{
    //             call.error("An Error Occurred: \(actualError)")
    //         }
    //     }else{
    //         call.error("no printer connected")
    //     }
    //     call.success()
    // }
    
    // @objc func isConnected(_ call: CAPPluginCall){
    //     call.success([
    //         "connected": isConnected()
    //         ])
    // }
    
    // private func isConnected() -> Bool{
    //     return (printerConnection != nil && printerConnection!.isConnected())
    // }
    
    // @objc func printerStatus(_ call:CAPPluginCall){
    //     //TODO
    //     //let address = call.getString("MACAddress") ?? ""
    //     //Return status
    //     call.success([
    //         "connected": true,
    //         "isReadyToPrint": false,
    //         "isPaused": false,
    //         "isReceiveBufferFull": false,
    //         "isRibbonOut": false,
    //         "isPaperOut": false,
    //         "isHeadTooHot": false,
    //         "isHeadOpen": false,
    //         "isHeadCold": false,
    //         "isPartialFormatInProgress": false,
    //         ])
    // }
    
    // @objc func connect(_  call:CAPPluginCall){
    //     //TODO
    //     let address = call.getString("MACAddress") ?? ""
        
    //     printerConnection = MfiBtPrinterConnection(serialNumber: address)
    //     printerConnection?.open()
    //     if( isConnected()){
    //         let printer = try? ZebraPrinterFactory.getInstance(printerConnection as! NSObjectProtocol & ZebraPrinterConnection)
            
    //         if(printer == nil)
    //         {
    //             call.error("Error connecting to printer")
    //         }
    //     }else{
    //         call.error("Could not connect to printer")
    //     }
        
    //     call.success([
    //         "success": true
    //         ])
        
    // }
    
    // @objc func disconnect(_ call:CAPPluginCall){
    //     //TODO
    //     call.success()
    // }
    

    
}
