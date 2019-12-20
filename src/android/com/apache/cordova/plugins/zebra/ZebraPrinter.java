package com.apache.cordova.plugins.zebra;

import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.comm.UsbConnection;
import com.zebra.sdk.printer.PrinterStatus;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.discovery.DiscoveredPrinter;
import com.zebra.sdk.printer.discovery.DiscoveredPrinterUsb;
import com.zebra.sdk.printer.discovery.DiscoveryHandler;
import com.zebra.sdk.printer.discovery.UsbDiscoverer;

public class ZebraPrinter extends CordovaPlugin {
    private Connection printerConnection;
    private com.zebra.sdk.printer.ZebraPrinter printer;
    private UsbPermissionResolver usbPermissionResolver;
    private static final String lock = "ZebraPluginLock";

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        usbPermissionResolver = new UsbPermissionResolver(cordova);
    }

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
            case "requestUsbPermission":
                usbPermissionResolver.resolvePermission(callbackContext);
                return true;
            case "connectUSB":
                this.connectUSB(callbackContext);
                return true;

        }
        return false;
    }

    /***
     * Get the printer status. Cordova boilerplate.
     * @param callbackContext
     */
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

    /***
     * Discover Zebra bluetooth devices. Cordova boilerplate
     * @param callbackContext
     */
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

    private void connectUSB(final CallbackContext callbackContext) {
        final ZebraPrinter instance = this;
        if (!usbPermissionResolver.hasPermissionToCommunicate) {
            callbackContext.error("NO_PERMISSION");
        } else {
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    synchronized (ZebraPrinter.lock) {
                        Log.v("EMO", "Printer - Connecting to USB");
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

                        if (usbPermissionResolver.discoveredPrinterUsb == null) {
                            callbackContext.error("NO_DEVICE");
                            return;
                        }

                        UsbDevice PrinterUsbDevice = usbPermissionResolver.discoveredPrinterUsb.device;

                        // create new USB connection
                        printerConnection = new UsbConnection(usbPermissionResolver.mUsbManager, PrinterUsbDevice);

                        //check that it isn't null
                        if (printerConnection == null) {
                            callbackContext.error("USB_CONNECTION_FAILED");
                            return;
                        }

                        //open that connection
                        try {
                            printerConnection.open();
                        } catch (Exception e) {
                            Log.v("EMO", "Printer - Failed to open connection", e);
                            printerConnection = null;
                            printer = null;
                            return;
                        }

                        //check if it opened
                        if (printerConnection != null && printerConnection.isConnected()) {
                            //try to get a printer
                            try {
                                printer = ZebraPrinterFactory.getInstance(printerConnection);
                                callbackContext.success();
                            } catch (Exception e) {
                                Log.v("EMO", "Printer - Error...", e);
                                closePrinter();
                                callbackContext.error("PRINTER_ERROR: " + e.getMessage());
                                return;
                            }
                            return;
                        }else {
                            //printer was null or not connected
                            callbackContext.error("PRINTER_NOT_CONNECTED");
                            return;
                        }
                    }
                }
            });
        }
    }

    /***
     * Connect to a printer identified by it's macAddress. Cordova boilerplate.
     * @param args
     * @param callbackContext
     */
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

    /***
     * Print the cpcl to the currently connected zebra printer. Cordova boilerplate
     * @param args
     * @param callbackContext
     */
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
            if (instance.printRawData(cpcl)) {
                callbackContext.success();
            } else {
                callbackContext.error("Print Failed. Printer Likely Disconnected.");
            }
        });
    }

    /***
     * Determine if the printer is currently connected. Cordova boilerplate.
     * @param callbackContext
     */
    private void isConnected(final CallbackContext callbackContext) {
        final ZebraPrinter instance = this;
        cordova.getThreadPool().execute(() -> {
            boolean result = instance.isConnected();
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, result));
            callbackContext.success();
        });
    }

    /***
     * Disconnect from the currently connected printer. Cordova boilerplate.
     * @param callbackContext
     */
    private void disconnect(final CallbackContext callbackContext) {
        final ZebraPrinter instance = this;
        cordova.getThreadPool().execute(() -> {
            instance.disconnect();
            callbackContext.success();
        });
    }

    /***
     * Prints the CPCL or ZPL formatted message to the currently connected printer.
     * @param rawDataString
     * @return
     */
    private boolean printRawData(String rawDataString) {
        try {
            if (!isConnected()) {
                Log.v("EMO", "Printer Not Connected");
                return false;
            }

            byte[] configLabel = rawDataString.getBytes();
            printerConnection.write(configLabel);

            if (printerConnection instanceof BluetoothConnection) {
                String friendlyName = ((BluetoothConnection) printerConnection).getFriendlyName();
                System.out.println(friendlyName);
            } else if (printerConnection instanceof UsbConnection) {
                // I dunno, just copy paste
                String friendlyName = ((UsbConnection) printerConnection).getDeviceName();
                System.out.println(friendlyName);
            }
        } catch (ConnectionException e) {
            Log.v("EMO", "Error Printing", e);
            return false;
        }
        return true;
    }

    /***
     * Returns boolean indicating if there is a printer currently connected
     * @return
     */
    private boolean isConnected() {
        return printerConnection != null && printerConnection.isConnected();
    }

    /***
     * Connects to a printer identified by the macAddress
     * @param macAddress
     * @return
     */
    private boolean connect(String macAddress) {
        synchronized (ZebraPrinter.lock) {
            Log.v("EMO", "Printer - Connecting to " + macAddress);
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

    /***
     * Disconnects from the currently connected printer
     */
    private void disconnect() {
        synchronized (ZebraPrinter.lock) {
            closePrinter();
        }
    }

    /***
     * Essentially does a disconnect but outside of the lock. Only use this inside of a lock.
     */
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

    /***
     * Get the status of the currently connected printer
     * @return
     */
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

    /***
     * Find Zebra printers we can connect to
     * @return
     */
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

    class UsbPermissionResolver {
        private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
        private CallbackContext usbPermissionCallback = null;
        PendingIntent mPermissionIntent;
        boolean hasPermissionToCommunicate = false;
        UsbManager mUsbManager;
        DiscoveredPrinterUsb discoveredPrinterUsb;

        // Catches intent indicating if the user grants permission to use the USB device
        private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (ACTION_USB_PERMISSION.equals(action)) {
                    synchronized (this) {
                        UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        PluginResult result = null;
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            if (device != null) {
                                hasPermissionToCommunicate = true;
                                result = new PluginResult(PluginResult.Status.OK,"PERMISSION_GRANTED");
                            } else {
                                result = new PluginResult(PluginResult.Status.ERROR,"NO_DEVICE");
                            }
                        } else {
                            result = new PluginResult(PluginResult.Status.ERROR,"NO_PERMISSION");
                        }
                        result.setKeepCallback(true);
                        usbPermissionCallback.sendPluginResult(result);
                        usbPermissionCallback = null;
                    }
                }
            }
        };

        UsbPermissionResolver(CordovaInterface cordova) {
            Context ctx = cordova.getContext();
            mUsbManager = (UsbManager) ctx.getSystemService(Context.USB_SERVICE);
            mPermissionIntent = PendingIntent.getBroadcast(ctx, 0, new Intent(ACTION_USB_PERMISSION), 0);
            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
            ctx.registerReceiver(mUsbReceiver, filter);
        }

        public void resolvePermission(CallbackContext cb) {
            usbPermissionCallback = cb;
            new Thread(new Runnable() {
                public void run() {
                    // Find connected printers
                    UsbDiscoveryHandler handler = new UsbDiscoveryHandler();
                    UsbDiscoverer.findPrinters(cordova.getContext(), handler);
                    try {
                        if (handler.printers != null && handler.printers.size() > 0)
                        {
                            discoveredPrinterUsb = handler.printers.get(0);
                            mUsbManager.requestPermission(discoveredPrinterUsb.device, mPermissionIntent);
                        } else {
                            cb.success("NO_DEVICE");
                        }
                    } catch (Exception e) {
                        cb.error(e.getMessage() + e.getLocalizedMessage());
                    }
                }
            }).start();
        }

        // Handles USB device discovery
        class UsbDiscoveryHandler implements DiscoveryHandler {
            public List<DiscoveredPrinterUsb> printers;

            public UsbDiscoveryHandler() {
                printers = new LinkedList<DiscoveredPrinterUsb>();
            }

            public void foundPrinter(final DiscoveredPrinter printer) {
                printers.add((DiscoveredPrinterUsb) printer);
            }

            public void discoveryFinished() {
            }

            public void discoveryError(String message) {
            }
        }
    }

}