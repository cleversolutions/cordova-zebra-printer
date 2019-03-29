import Foundation
import ExternalAccessory

@objc(ZebraPrinterPlugin)
class ZebraPrinterPlugin: CDVPlugin {
    var printerConnection: ZebraPrinterConnection?
    
    @objc func echo(_ command: CDVInvokedUrlCommand) {
        var pluginResult = CDVPluginResult(
            status: CDVCommandStatus_ERROR
        )
        let value = command.arguments[0] as? String ?? ""
        pluginResult = CDVPluginResult(
            status: CDVCommandStatus_OK,
            messageAs: value
        )
        
        self.commandDelegate!.send(
            pluginResult,
            callbackId: command.callbackId
        )
    }
    
    @objc func discover(_ command: CDVInvokedUrlCommand){
        DispatchQueue.global(qos: .background).async {
            let manager = EAAccessoryManager.shared()
            let accessories = manager.connectedAccessories
            
            var devices = [Any]()
            accessories.forEach { (accessory) in
                let name = accessory.name
                var device = [String: Any]()
                device["name"] = name
                device["address"] = accessory.serialNumber
                device["manufacturer"] = accessory.manufacturer
                device["modelNumber"] = accessory.modelNumber
                device["connected"] = accessory.isConnected
                devices.append(device)
            }
            let pluginResult = CDVPluginResult(
                status: CDVCommandStatus_OK,
                messageAs: devices
            )
            self.commandDelegate!.send(
                pluginResult,
                callbackId: command.callbackId
            )
        }
    }
    
    @objc func print(_ command: CDVInvokedUrlCommand) {
        DispatchQueue.global(qos: .background).async {
            let cpcl = command.arguments[0] as? String ?? ""
            if( self.isConnected()){
                let data = cpcl.data(using: .utf8)
                var error: NSError?
                // it seems self.isConnected() can lie if the printer has power cycled
                // a workaround is to close and reopen the connection
                self.printerConnection!.close()
                self.printerConnection!.open()
                self.printerConnection!.write(data, error:&error)
                if error != nil{
                    let pluginResult = CDVPluginResult(
                        status: CDVCommandStatus_ERROR
                    )
                    self.commandDelegate!.send(
                        pluginResult,
                        callbackId: command.callbackId
                    )
                    return
                }
            }else{
                let pluginResult = CDVPluginResult(
                    status: CDVCommandStatus_ERROR
                )
                self.commandDelegate!.send(
                    pluginResult,
                    callbackId: command.callbackId
                )
                return
            }
            let pluginResult = CDVPluginResult(
                status: CDVCommandStatus_OK
            )
            self.commandDelegate!.send(
                pluginResult,
                callbackId: command.callbackId
            )
        }
    }
    
    @objc func isConnected(_ command: CDVInvokedUrlCommand){
        let pluginResult = CDVPluginResult(
            status: CDVCommandStatus_OK,
            messageAs: isConnected()
        )
        self.commandDelegate!.send(
            pluginResult,
            callbackId: command.callbackId
        )
    }
    
    private func isConnected() -> Bool{
        return (printerConnection != nil && printerConnection!.isConnected())
    }
    
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
    
    @objc func connect(_  command: CDVInvokedUrlCommand){
        DispatchQueue.global(qos: .background).async {
            var pluginResult = CDVPluginResult(
                status: CDVCommandStatus_ERROR
            )
            let address = command.arguments[0] as? String ?? ""
            if(address == ""){
                self.commandDelegate!.send(
                    pluginResult,
                    callbackId: command.callbackId
                )
                return
            }
            self.printerConnection = MfiBtPrinterConnection(serialNumber: address)
            self.printerConnection?.open()
            if( self.isConnected()){
                let printer = try? ZebraPrinterFactory.getInstance(self.printerConnection as! NSObjectProtocol & ZebraPrinterConnection)
                
                if(printer == nil)
                {
                    self.commandDelegate!.send(
                        pluginResult,
                        callbackId: command.callbackId
                    )
                    return
                }
            }else{
                self.commandDelegate!.send(
                    pluginResult,
                    callbackId: command.callbackId
                )
                return
                
            }
            
            pluginResult = CDVPluginResult(
                status: CDVCommandStatus_OK
            )
            self.commandDelegate!.send(
                pluginResult,
                callbackId: command.callbackId
            )
        }
    }
    
    @objc func disconnect(_ command: CDVInvokedUrlCommand){
        //TODO
        if(isConnected()){
            printerConnection?.close()
        }
        
        let pluginResult = CDVPluginResult(
            status: CDVCommandStatus_OK
        )
        self.commandDelegate!.send(
            pluginResult,
            callbackId: command.callbackId
        )
    }
    
    
    
}
