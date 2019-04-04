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
    private Connection printerConnection;
    private com.zebra.sdk.printer.ZebraPrinter printer;
    private String macAddress;
    static final String lock = "ZebraPluginLock";

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        Log.v("EMO", "Execute on ZebraPrinter Plugin called");
        if (action.equals("discover")) {
            this.discover(args, callbackContext);
            return true;
        } else if (action.equals("connect")) {
            this.connect(args, callbackContext);
            return true;
        } else if (action.equals("print")) {
            this.print(args, callbackContext);
            return true;
        } else if (action.equals("isConnected")) {
            this.isConnected(args, callbackContext);
            return true;
        } else if (action.equals("disconnect")) {
            this.disconnect(args, callbackContext);
            return true;
        } else if (action.equals("printerStatus")) {
            this.printerStatus(args, callbackContext);
            return true;
        }
        return false;
    }

    private void printerStatus(JSONArray args, final CallbackContext callbackContext) {
        final ZebraPrinter instance = this;

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                JSONObject status = instance.GetPrinterStatus();
                if (status != null) {
                    callbackContext.success(status);
                } else {
                    callbackContext.error("Failed to get status.");
                }
            }
        });
    }

    private void discover(JSONArray args, final CallbackContext callbackContext) {
        final ZebraPrinter instance = this;
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                JSONArray printers = instance.NonZebraDiscovery();
                if (printers != null) {
                    callbackContext.success(printers);
                } else {
                    callbackContext.error("Discovery Failed");
                }
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
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                printer = instance.connect(address);
                if (printer != null) {
                    callbackContext.success();
                } else {
                    callbackContext.error("Connect Failed");
                }
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
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                if (instance.printCPCL(cpcl)) {
                    callbackContext.success();
                } else {
                    callbackContext.error("Print Failed. Printer Likely Disconnected.");
                }
            }
        });
    }

    private void isConnected(JSONArray args, final CallbackContext callbackContext) {
        final ZebraPrinter instance = this;
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                boolean result = instance.isConnected();
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, result));
                callbackContext.success();
            }
        });
    }

    private void disconnect(JSONArray args, final CallbackContext callbackContext) {
        final ZebraPrinter instance = this;
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                instance.disconnect();
                callbackContext.success();
            }
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

    private com.zebra.sdk.printer.ZebraPrinter connect(String macAddress) {
        if (isConnected())
            disconnect();
        printerConnection = null;
        this.macAddress = macAddress;
        printerConnection = new BluetoothConnection(macAddress);
        synchronized (ZebraPrinter.lock) {
            try {
                printerConnection.open();
            }

            catch (ConnectionException e) {
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
        }else{

        }

        return errorStatus;
    }

    private JSONArray NonZebraDiscovery() {
        JSONArray printers = new JSONArray();

        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            Set<BluetoothDevice> devices = adapter.getBondedDevices();

            for (Iterator<BluetoothDevice> it = devices.iterator(); it.hasNext();) {
                BluetoothDevice device = it.next();
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
