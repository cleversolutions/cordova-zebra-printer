package com.apache.cordova.plugins.zebra;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import java.util.Map;
import java.util.Set;

import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.printer.PrinterLanguage;
import com.zebra.sdk.printer.PrinterStatus;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;
import com.zebra.sdk.printer.discovery.BluetoothDiscoverer;
import com.zebra.sdk.printer.discovery.DiscoveredPrinter;
import com.zebra.sdk.printer.discovery.DiscoveredPrinterBluetooth;
import com.zebra.sdk.printer.discovery.DiscoveryHandler;


public class ZebraPrinter extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("echo")) {
            String message = args.getString(0);
            this.echo(message, callbackContext);
            return true;
        }else if (action.equals("discover")){
            this.discover(callbackContext);
            return true;
        }
        return false;
    }

    private void discover(CallbackContext callbackContext) {
        JSONArray printers = this.NonZebraDiscovery();
        if (printers != null) {
            callbackContext.success(printers);
        } else {
            callbackContext.error("Discovery Failed");
        }
    }

    private void echo(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            callbackContext.success(message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }

    private boolean printCPCL(String cpcl)
    {
        try {
            if(!isConnected()) {
                Log.v("EMO", "Printer Not Connected");
                return false;
            }

            byte[] configLabel = cpcl.getBytes();
            printerConnection.write(configLabel);

            if (printerConnection instanceof BluetoothConnection) {
                String friendlyName = ((BluetoothConnection) printerConnection).getFriendlyName();
                System.out.println(friendlyName);
            }
        } catch (ConnectionException e) {
            Log.v("EMO", "Error Printing", e);
            return  false;
        }
        return true;
    }

    private boolean isConnected(){
        return printerConnection != null && printerConnection.isConnected();
    }

    private com.zebra.sdk.printer.ZebraPrinter connect(String macAddress) {
        if( isConnected()) disconnect();
        printerConnection = null;
        this.macAddress = macAddress;
        printerConnection = new BluetoothConnection(macAddress);
        synchronized(ZebraPrinter.lock) {
            try {
                printerConnection.open();
            }

            catch (ConnectionException e)
            {
                Log.v("EMO", "Printer - Failed to open connection", e);
                disconnect();
            }
            printer = null;
            if (printerConnection.isConnected()) {
                try {
                    printer = ZebraPrinterFactory.getInstance(printerConnection);
                    PrinterLanguage pl = printer.getPrinterControlLanguage();
                } catch (ConnectionException e) {
                    Log.v("EMO", "Printer - Error...", e);
                    printer = null;
                    disconnect();
                } catch (ZebraPrinterLanguageUnknownException e) {
                    Log.v("EMO", "Printer - Unknown Printer Language", e);
                    printer = null;
                    disconnect();
                }
            }
        }
        return printer;
    }

    private void disconnect() {
        synchronized (ZebraPrinter.lock) {
            try {
                if (printerConnection != null) {
                    printerConnection.close();
                }
            } catch (ConnectionException e) {
                e.printStackTrace();
            }
        }
    }

    //This doesn't seem to return any printers
    private void discoverWithZebraSDK(final PluginCall  call){
        class BTDiscoveryHandler implements DiscoveryHandler {
            List<JSObject> printers = new LinkedList<JSObject>();
            PluginCall call;

            public BTDiscoveryHandler(PluginCall call) { this.call = call; }

            public void discoveryError(String message)
            {
                call.error(message);
            }

            public void discoveryFinished()
            {
                JSObject ret = new JSObject();
                ret.put("printers", printers);
                call.success(ret);
            }

            @Override
            public void foundPrinter(DiscoveredPrinter printer){
                DiscoveredPrinterBluetooth pr = (DiscoveredPrinterBluetooth) printer;
                try
                {
                    Map<String,String> map = pr.getDiscoveryDataMap();

                    for (String settingsKey : map.keySet()) {
                        System.out.println("Key: " + settingsKey + " Value: " + printer.getDiscoveryDataMap().get(settingsKey));
                    }

                    String name = pr.friendlyName;
                    String mac = pr.address;
                    JSObject p = new JSObject();
                    p.put("name",name);
                    p.put("address", mac);
                    for (String settingsKey : map.keySet()) {
                        System.out.println("Key: " + settingsKey + " Value: " + map.get(settingsKey));
                        p.put(settingsKey,map.get(settingsKey));
                    }
                    printers.add(p);
                } catch (Exception e) {
                    Log.v("EMO", "Discovery Error - Error...", e);
                }
            }
        }

        final Context context = this.getContext();
        new Thread(new Runnable() {

            public void run() {
                try {
                    BluetoothDiscoverer.findPrinters(context, new BTDiscoveryHandler(call));
                } catch (Exception e) {
                    call.error(e.getMessage());
                }
            }
        }).start();
    }

    private JSONArray NonZebraDiscovery(){

        if (message != null && message.length() > 0) {
            callbackContext.success(message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }

        JSONArray printers = new JSONArray();

        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            Set<BluetoothDevice> devices = adapter.getBondedDevices();

            for (Iterator<BluetoothDevice> it = devices.iterator(); it.hasNext(); ) {
                BluetoothDevice device = it.next();
                String name = device.getName();
                String mac = device.getAddress();

                JSONObject p = new JSObject();
                p.put("name",name);
                p.put("address", mac);
                printers.put(p);

            }
        }catch (Exception e){
            System.err.println(e.getMessage());
        }
        return  printers;
    }
}
