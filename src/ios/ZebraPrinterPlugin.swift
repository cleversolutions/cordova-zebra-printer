import Foundation
import ExternalAccessory

@objc(ZebraPrinterPlugin)
class ZebraPrinterPlugin: CDVPlugin {
    var printerConnection: ZebraPrinterConnection?
    var printer: ZebraPrinter?
    
    /**
     * Discover connectable zebra printers
     *
     */
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
    
    /**
     * Get the status of the printer we are currently connected to
     *
     */
    @objc func printerStatus(_ command: CDVInvokedUrlCommand){
        DispatchQueue.global(qos: .background).async {
            var status = [String: Any]()
            status["connected"] = false
            status["isReadyToPrint"] = false
            status["isPaused"] = false
            status["isReceiveBufferFull"] = false
            status["isRibbonOut"] = false
            status["isPaperOut"] = false
            status["isHeadTooHot"] = false
            status["isHeadOpen"] = false
            status["isHeadCold"] = false
            status["isPartialFormatInProgress"] = false
            
            if(self.printerConnection != nil && self.printerConnection!.isConnected() && self.printer != nil){
                let zebraPrinterStatus = try? self.printer?.getCurrentStatus()
                if(zebraPrinterStatus != nil){
                    NSLog("Got Printer Status")
                    if(zebraPrinterStatus!.isReadyToPrint) { NSLog("Read To Print"); }
                    else {
                        let message = PrinterStatusMessages.init(printerStatus: zebraPrinterStatus)
                        if(message != nil)
                        {
                            NSLog("Printer Not Ready. " + (message!.getStatusMessage() as! [String]).joined(separator: ", "))
                        }else{
                            NSLog("Printer Not Ready.")
                        }
                    }
                    
                    status["connected"] = true
                    status["isReadyToPrint"] = zebraPrinterStatus?.isReadyToPrint
                    status["isPaused"] = zebraPrinterStatus?.isPaused
                    status["isReceiveBufferFull"] = zebraPrinterStatus?.isReceiveBufferFull
                    status["isRibbonOut"] = zebraPrinterStatus?.isRibbonOut
                    status["isPaperOut"] = zebraPrinterStatus?.isPaperOut
                    status["isHeadTooHot"] = zebraPrinterStatus?.isHeadTooHot
                    status["isHeadOpen"] = zebraPrinterStatus?.isHeadOpen
                    status["isHeadCold"] = zebraPrinterStatus?.isHeadCold
                    status["isPartialFormatInProgress"] = zebraPrinterStatus?.isPartialFormatInProgress
                    
                    NSLog("ZebraPrinter:: returning status")
                    let pluginResult = CDVPluginResult(
                        status: CDVCommandStatus_OK,
                        messageAs: status
                    )
                    self.commandDelegate!.send(
                        pluginResult,
                        callbackId: command.callbackId
                    )
                    return
                }else{
                    // printer has no status... this happens when the printer turns off, but the driver still thinks it is connected
                    NSLog("ZebraPrinter:: Got a printer but no status. Sadness.")
                    let pluginResult = CDVPluginResult(
                        status: CDVCommandStatus_ERROR,
                        messageAs: "Printer Has No Status"
                    )
                    self.commandDelegate!.send(
                        pluginResult,
                        callbackId: command.callbackId
                    )
                    return
                }
            }else{
                NSLog("ZebraPrinter:: status of disconnected printer")
                // if the printer isn't connected return success with disconnect status
                let pluginResult = CDVPluginResult(
                    status: CDVCommandStatus_OK,
                    messageAs: status
                )
                self.commandDelegate!.send(
                    pluginResult,
                    callbackId: command.callbackId
                )
                return
            }
        }
    }
    
    /**
     * Print the cpcl
     *
     */
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
                    NSLog("ZebraPrinter:: error printing -> " + (error?.localizedDescription ?? "Unknonwn Error"))
                    let pluginResult = CDVPluginResult(
                        status: CDVCommandStatus_ERROR
                    )
                    self.commandDelegate!.send(
                        pluginResult,
                        callbackId: command.callbackId
                    )
                }else{
                    NSLog("ZebraPrinter:: print completed")
                    let pluginResult = CDVPluginResult(
                        status: CDVCommandStatus_OK
                    )
                    self.commandDelegate!.send(
                        pluginResult,
                        callbackId: command.callbackId
                    )
                }
            }else{
                NSLog("ZebraPrinter:: not connected")
                let pluginResult = CDVPluginResult(
                    status: CDVCommandStatus_ERROR,
                    messageAs: "Printer Not Connected"
                )
                self.commandDelegate!.send(
                    pluginResult,
                    callbackId: command.callbackId
                )
            }
        }
    }
    
    /**
     * Check if we are connectd to the printer or not
     *
     */
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

    /**
     * Check if we are connectd to the printer or not
     *
     */
    private func isConnected() -> Bool{
        //printerConnection!.isConnected lies, it says it's open when it isn't
        return self.printerConnection != nil && (self.printerConnection?.isConnected() ?? false)
    }

    /**
     * Connect to a printer by serialNumber
     *
     */
    @objc func connect(_  command: CDVInvokedUrlCommand){
        DispatchQueue.global(qos: .background).async {
            let address = command.arguments[0] as? String ?? ""
            if(address == ""){
                NSLog("ZebraPrinter:: empty printer address")
                let pluginResult = CDVPluginResult(
                    status: CDVCommandStatus_ERROR,
                    messageAs: "Invalid Address"
                )
                self.commandDelegate!.send(
                    pluginResult,
                    callbackId: command.callbackId
                )
                return
            }
            
            NSLog("ZebraPrinter:: connecting to " + address)
            
            //try to close an existing connection
            if(self.printerConnection != nil){
                self.printerConnection?.close()
            }
            
            //clear out our existing printer & connection
            self.printerConnection = nil;
            self.printer = nil;
            
            //create and open a new connection
            self.printerConnection = MfiBtPrinterConnection(serialNumber: address)
            NSLog("ZebraPrinter:: got connection. opening...")
            self.printerConnection?.open()
            NSLog("ZebraPrinter:: opened connection")
            
            if( self.isConnected()){
                NSLog("ZebraPrinter:: getting printer")
                self.printer = try? ZebraPrinterFactory.getInstance(self.printerConnection as? NSObjectProtocol & ZebraPrinterConnection)
                NSLog("ZebraPrinter:: got printer")
                
                if(self.printer == nil)
                {
                    NSLog("ZebraPrinter:: nil printer")
                    let pluginResult = CDVPluginResult(
                        status: CDVCommandStatus_ERROR,
                        messageAs: "Printer Null"
                    )
                    self.commandDelegate!.send(
                        pluginResult,
                        callbackId: command.callbackId
                    )
                }else{
                    NSLog("ZebraPrinter:: connected")
                    let pluginResult = CDVPluginResult(
                        status: CDVCommandStatus_OK,
                        messageAs: "Printer Connected"
                    )
                    self.commandDelegate!.send(
                        pluginResult,
                        callbackId: command.callbackId
                    )
                }
            }else{
                NSLog("ZebraPrinter:: not connected")
                let pluginResult = CDVPluginResult(
                    status: CDVCommandStatus_ERROR,
                    messageAs: "Printer Not Connected"
                )
                self.commandDelegate!.send(
                    pluginResult,
                    callbackId: command.callbackId
                )
            }
        }
    }

    /**
     * Disconnect fromt the currently connected printer
     *
     */
    @objc func disconnect(_ command: CDVInvokedUrlCommand){
        //close the connection and set it to nil
        if(self.printerConnection != nil){
            self.printerConnection?.close()
            self.printerConnection = nil
            self.printer = nil
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
