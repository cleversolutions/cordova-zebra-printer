package com.apache.cordova.plugins.zebra;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;

import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.printer.PrinterStatus;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;

public class ZebraPrinter extends CordovaPlugin {
    private Connection printerConnection;
    private com.zebra.sdk.printer.ZebraPrinter printer;
    private static final String lock = "ZebraPluginLock";

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        Log.v("EMO", "Execute on ZebraPrinter Plugin called");
        switch (action) {
            case "discover":
                this.discover(callbackContext);
                return true;
            case "connect":
                this.connect(args, callbackContext);
                return true;
            case "print":
                this.print(args, callbackContext);
                return true;
            case "isConnected":
                this.isConnected(callbackContext);
                return true;
            case "disconnect":
                this.disconnect(callbackContext);
                return true;
            case "printerStatus":
                this.printerStatus(callbackContext);
                return true;
        }
        return false;
    }

    private void printerStatus(final CallbackContext callbackContext) {
        final ZebraPrinter instance = this;

        cordova.getThreadPool().execute(() -> {
            JSONObject status = instance.GetPrinterStatus();
            if (status != null) {
                callbackContext.success(status);
            } else {
                callbackContext.error("Failed to get status.");
            }
        });
    }

    private void discover(final CallbackContext callbackContext) {
        final ZebraPrinter instance = this;
        cordova.getThreadPool().execute(() -> {
            JSONArray printers = instance.NonZebraDiscovery();
            if (printers != null) {
                callbackContext.success(printers);
            } else {
                callbackContext.error("Discovery Failed");
            }
        });
    }

    private void connect(JSONArray args, final CallbackContext callbackContext) {
        final ZebraPrinter instance = this;
        final String address;
        try {
            address = args.getString(0);
        } catch (JSONException e) {
            e.printStackTrace();
            callbackContext.error("Connect Failed: " + e.getMessage());
            return;
        }
        cordova.getThreadPool().execute(() -> {
            if (instance.connect(address)) {
                callbackContext.success();
            } else {
                callbackContext.error("Connect Failed");
            }
        });
    }

    private void print(JSONArray args, final CallbackContext callbackContext) {
        final ZebraPrinter instance = this;
        final String cpcl;
        try {
            cpcl = args.getString(0);
        } catch (JSONException e) {
            e.printStackTrace();
            callbackContext.error("Print Failed: " + e.getMessage());
            return;
        }
        cordova.getThreadPool().execute(() -> {
            if (instance.printCPCL(cpcl)) {
                callbackContext.success();
            } else {
                callbackContext.error("Print Failed. Printer Likely Disconnected.");
            }
        });
    }

    private void isConnected(final CallbackContext callbackContext) {
        final ZebraPrinter instance = this;
        cordova.getThreadPool().execute(() -> {
            boolean result = instance.isConnected();
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, result));
            callbackContext.success();
        });
    }

    private void disconnect(final CallbackContext callbackContext) {
        final ZebraPrinter instance = this;
        cordova.getThreadPool().execute(() -> {
            instance.disconnect();
            callbackContext.success();
        });
    }

    private boolean printCPCL(String cpcl) {
        try {
            if (!isConnected()) {
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
            return false;
        }
        return true;
    }

    private boolean isConnected() {
        return printerConnection != null && printerConnection.isConnected();
    }

    private boolean connect(String macAddress) {
        synchronized (ZebraPrinter.lock) {
            Log.v("EMO", "Printer - Connecting...");
            //disconnect if we are already connected
            try {
                if (printerConnection != null && printerConnection.isConnected()) {
                    printerConnection.close();
                    printerConnection = null;
                    printer = null;
                }
            }catch (Exception ex){
                Log.v("EMO", "Printer - Failed to close connection before connecting", ex);
            }

            //create a new BT connection
            printerConnection = new BluetoothConnection(macAddress);

            //check that it isn't null
            if(printerConnection == null){
                return false;
            }

            //open that connection
            try {
                printerConnection.open();
            } catch (Exception e) {
                Log.v("EMO", "Printer - Failed to open connection", e);
                printerConnection = null;
                printer = null;
                return false;
            }

            //check if it opened
            if (printerConnection != null && printerConnection.isConnected()) {
                //try to get a printer
                try {
                    printer = ZebraPrinterFactory.getInstance(printerConnection);
                } catch (Exception e) {
                    Log.v("EMO", "Printer - Error...", e);
                    closePrinter();
                    return false;
                }
                return true;
            }else {
                //printer was null or not connected
                return false;
            }
        }
    }

    private void disconnect() {
        synchronized (ZebraPrinter.lock) {
            closePrinter();
        }
    }

    private void closePrinter(){
        try {
            if (printerConnection != null) {
                printerConnection.close();
                printerConnection = null;
            }
            printer = null;
        } catch (ConnectionException e) {
            e.printStackTrace();
        }
    }

    private JSONObject GetPrinterStatus() {
        JSONObject errorStatus = new JSONObject();
        try{
            errorStatus.put("connected", false);
            errorStatus.put("isReadyToPrint", false);
            errorStatus.put("isPaused", false);
            errorStatus.put("isReceiveBufferFull", false);
            errorStatus.put("isRibbonOut", false);
            errorStatus.put("isPaperOut", false);
            errorStatus.put("isHeadTooHot", false);
            errorStatus.put("isHeadOpen", false);
            errorStatus.put("isHeadCold", false);
            errorStatus.put("isPartialFormatInProgress", false);        
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        if (isConnected() && printer != null) {
            try{
                JSONObject status = new JSONObject();
                PrinterStatus zebraStatus = printer.getCurrentStatus();
                status.put("connected", true);
                status.put("isReadyToPrint", zebraStatus.isReadyToPrint);
                status.put("isPaused", zebraStatus.isPaused);
                status.put("isReceiveBufferFull", zebraStatus.isReceiveBufferFull);
                status.put("isRibbonOut", zebraStatus.isRibbonOut);
                status.put("isPaperOut", zebraStatus.isPaperOut);
                status.put("isHeadTooHot", zebraStatus.isHeadTooHot);
                status.put("isHeadOpen", zebraStatus.isHeadOpen);
                status.put("isHeadCold", zebraStatus.isHeadCold);
                status.put("isPartialFormatInProgress", false);
                return status;
            } catch (Exception e) {
                System.err.println(e.getMessage());
                return errorStatus;
            }
        }

        return errorStatus;
    }

    private JSONArray NonZebraDiscovery() {
        JSONArray printers = new JSONArray();

        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            Set<BluetoothDevice> devices = adapter.getBondedDevices();

            for (BluetoothDevice device : devices) {
                String name = device.getName();
                String mac = device.getAddress();

                JSONObject p = new JSONObject();
                p.put("name", name);
                p.put("address", mac);
                printers.put(p);

            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return printers;
    }

}
