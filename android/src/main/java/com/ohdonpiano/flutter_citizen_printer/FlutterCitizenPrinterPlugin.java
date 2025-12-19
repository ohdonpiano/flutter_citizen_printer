package com.ohdonpiano.flutter_citizen_printer;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import com.citizen.sdk.labelprint.LabelPrinter;
import com.citizen.sdk.labelprint.LabelConst;
import com.citizen.sdk.labelprint.LabelDesign;
import com.citizen.sdk.labelprint.CitizenPrinterInfo;

import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// Class to represent USB printer information
class UsbPrinterInfo {
    public String deviceId;
    public String deviceName;
    public String manufacturerName;
    public String productName;
    public int vendorId;
    public int productId;
    public String serialNumber;
    public UsbDevice usbDevice;

    public UsbPrinterInfo(UsbDevice device) {
        this.usbDevice = device;
        this.deviceId = String.valueOf(device.getDeviceId());
        this.deviceName = device.getDeviceName();
        this.manufacturerName = device.getManufacturerName() != null ? device.getManufacturerName() : "";
        this.productName = device.getProductName() != null ? device.getProductName() : "";
        this.vendorId = device.getVendorId();
        this.productId = device.getProductId();
        this.serialNumber = ""; // Will be populated later with permission
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("deviceId", deviceId);
        map.put("deviceName", deviceName);
        map.put("manufacturerName", manufacturerName);
        map.put("productName", productName);
        map.put("vendorId", vendorId);
        map.put("productId", productId);
        map.put("serialNumber", serialNumber);
        return map;
    }

    public String toString() {
        return "UsbPrinterInfo{" +
                "deviceId='" + deviceId + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", manufacturerName='" + manufacturerName + '\'' +
                ", productName='" + productName + '\'' +
                ", vendorId=" + vendorId +
                ", productId=" + productId +
                ", serialNumber='" + serialNumber + '\'' +
                '}';
    }
}

public class FlutterCitizenPrinterPlugin implements FlutterPlugin, MethodChannel.MethodCallHandler {
    private static final String ACTION_USB_PERMISSION = "com.citizen.USB_PERMISSION";
    private static final String ACTION_USB_SERIAL_PERMISSION = "com.citizen.USB_SERIAL_PERMISSION";
    private static final long PERMISSION_TIMEOUT = 10000; // Aumentato a 10 secondi
    private static final long GLOBAL_SEARCH_TIMEOUT = 60000; // 60 secondi per l'intera ricerca
    private MethodChannel channel;
    private Context context;
    private UsbManager usbManager;
    private Map<String, UsbDevice> usbDeviceMap = new HashMap<>();
    private BroadcastReceiver usbReceiver;
    private Map<String, PendingPrintRequest> pendingPrintRequests = new HashMap<>();

    // Variables for asynchronous USB search management with serial number
    private MethodChannel.Result pendingSearchResult;
    private ArrayList<UsbPrinterInfo> currentSearchPrinters;
    private ArrayList<UsbDevice> citizenDevicesQueue;
    private int currentDeviceIndex;
    private boolean includeSerialNumbers;
    private android.os.Handler searchTimeoutHandler;
    private Runnable searchTimeoutRunnable;
    private Runnable globalTimeoutRunnable;
    private long searchStartTime;
    private boolean isSearchingSerialNumbers = false; // Flag per indicare se stiamo cercando numeri seriali

    // Class to manage pending print requests
    private static class PendingPrintRequest {
        public UsbDevice device;
        public byte[] imageBytes;
        public MethodChannel.Result result;

        public PendingPrintRequest(UsbDevice device, byte[] imageBytes, MethodChannel.Result result) {
            this.device = device;
            this.imageBytes = imageBytes;
            this.result = result;
        }
    }

    @Override
    public void onAttachedToEngine(@NonNull FlutterPlugin.FlutterPluginBinding binding) {
        channel = new MethodChannel(binding.getBinaryMessenger(), "flutter_citizen_printer");
        context = binding.getApplicationContext();
        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        channel.setMethodCallHandler(this);

        // Register the BroadcastReceiver for USB permissions
        usbReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (ACTION_USB_PERMISSION.equals(action)) {
                    synchronized (this) {
                        UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                        if (device != null) {
                            String deviceKey = String.valueOf(device.getDeviceId());
                            PendingPrintRequest pendingRequest = pendingPrintRequests.remove(deviceKey);

                            if (pendingRequest != null) {
                                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                                    // Permission granted
                                    int res = executePrintWithDevice(pendingRequest.device, pendingRequest.imageBytes);
                                    if (res == LabelConst.CLS_SUCCESS) {
                                        pendingRequest.result.success(null);
                                    } else {
                                        pendingRequest.result.error("PRINT_ERROR", String.valueOf(res), null);
                                    }
                                } else {
                                    // Permission denied
                                    pendingRequest.result.error("PERMISSION_DENIED", "USB permission denied", null);
                                }
                            }
                        }
                    }
                } else if (ACTION_USB_SERIAL_PERMISSION.equals(action)) {
                    synchronized (this) {
                        UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                        if (device != null && isSearchingSerialNumbers) {
                            // Cancel any existing timeout for this device
                            if (searchTimeoutHandler != null && searchTimeoutRunnable != null) {
                                searchTimeoutHandler.removeCallbacks(searchTimeoutRunnable);
                            }

                            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                                // Permission granted, read the serial number
                                System.out.println("Permission GRANTED for device: " + device.getDeviceId());
                                processSerialNumberForDevice(device);
                            } else {
                                // Permission denied, continue with next device
                                System.out.println("Permission DENIED for device: " + device.getDeviceId());
                                processNextCitizenDevice();
                            }
                        }
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(ACTION_USB_SERIAL_PERMISSION);
        // For Android 13+ we need to specify the RECEIVER_NOT_EXPORTED flag
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            context.registerReceiver(usbReceiver, filter);
        }
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        if (call.method.equals("printImageWiFi")) {
            String ip = call.argument("ip");
            byte[] bytes = call.argument("imageBytes");
            int width = call.argument("width");
            int height = call.argument("height");
            int res = printImageWiFi(ip, bytes, width, height);
            if (res == LabelConst.CLS_SUCCESS) result.success(null);
            else result.error("PRINT_ERROR", String.valueOf(res), null);
        } else if (call.method.equals("printImageUSB")) {
            byte[] bytes = call.argument("imageBytes");
            int res = printImageUSB(bytes);
            if (res == LabelConst.CLS_SUCCESS) result.success(null);
            else result.error("PRINT_ERROR", String.valueOf(res), null);
        } else if (call.method.equals("printImageUsbSpecific")) {
            String deviceId = call.argument("deviceId");
            byte[] bytes = call.argument("imageBytes");
            printImageUsbSpecificWithPermission(deviceId, bytes, result);
        } else if (call.method.equals("detectPrinters")) {
            int timeout = call.argument("timeout");
            int[] err = new int[1];
            LabelPrinter printer = new LabelPrinter();
            printer.setContext(context);
            CitizenPrinterInfo[] list = printer.searchCitizenPrinter(
                    LabelConst.CLS_PORT_WiFi,
                    timeout,
                    err
            );
            if (err[0] == LabelConst.CLS_SUCCESS) {
                ArrayList<Map<String, String>> ips = new ArrayList<>();
                for (CitizenPrinterInfo info : list) {
                    HashMap<String, String> data = new HashMap<>();
                    data.put("ipAddress", info.ipAddress);
                    data.put("deviceName", info.deviceName);
                    ips.add(data);
                }
                result.success(ips);
            } else {
                result.error("DETECT_ERROR", String.valueOf(err[0]), null);
            }
        } else if (call.method.equals("searchLabelPrinter")) {
            int timeout = call.argument("timeout");
            int[] err = new int[1];
            LabelPrinter printer = new LabelPrinter();
            printer.setContext(context);
            String[] list = printer.searchLabelPrinter(
                    LabelConst.CLS_PORT_WiFi,
                    timeout,
                    err
            );
            if (err[0] == LabelConst.CLS_SUCCESS) {
                result.success(list);
            } else {
                result.error("DETECT_ERROR", String.valueOf(err[0]), null);
            }
        } else if (call.method.equals("getUsbStatus")) {
            LabelPrinter printer = new LabelPrinter();
            printer.setContext(context);
            int res = printer.connect(LabelConst.CLS_PORT_USB, (UsbDevice) null);
            if (res != LabelConst.CLS_SUCCESS) {
                result.error("CONNECT_ERROR", String.valueOf(res), null);
                return;
            }
            int checkRes = printer.printerCheck();
            printer.disconnect();
            result.success(checkRes);
        } else if (call.method.equals("searchUsbPrinters")) {
            try {
                Boolean includeSerial = call.argument("includeSerialNumbers");
                searchUsbPrintersWithOptionalSerialNumbers(result, includeSerial != null ? includeSerial : false);
            } catch (Exception e) {
                // Clean up in case of error
                cleanupSearchState();
                result.error("USB_SEARCH_ERROR", e.getMessage(), null);
            }
        } else if (call.method.equals("cancelUsbSearch")) {
            // New method to force cancel any pending search
            cancelPendingSearch(result);
        } else {
            result.notImplemented();
        }
    }

    private int printImageWiFi(String ip, byte[] imageBytes, int width, int height) {
        LabelPrinter printer = new LabelPrinter();
        printer.setContext(context);
        int r = printer.connect(LabelConst.CLS_PORT_WiFi, ip);
        if (r != LabelConst.CLS_SUCCESS) return r;
        LabelDesign design = new LabelDesign();
        java.io.File file = null;
        java.io.FileOutputStream fos = null;
        try {
            file = new java.io.File(context.getCacheDir(), "print.png");
            fos = new java.io.FileOutputStream(file);
            fos.write(imageBytes);
            fos.close();
            fos = null;
            design.drawBitmap(file.getAbsolutePath(), LabelConst.CLS_RT_NORMAL, width, height, 0, 0, LabelConst.CLS_PRT_RES_300, LabelConst.CLS_UNIT_MILLI);
            r = printer.print(design, 1);
        } catch (Exception e) {
            r = -1;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception ignored) {
                }
            }
            if (file != null && file.exists()) {
                file.delete();
            }
            printer.disconnect();
        }
        return r;
    }

    private int printImageUSB(byte[] imageBytes) {
        LabelPrinter printer = new LabelPrinter();
        printer.setContext(context);
        int r = printer.connect(LabelConst.CLS_PORT_USB, (UsbDevice) null);
        if (r != LabelConst.CLS_SUCCESS) return r;
        LabelDesign design = new LabelDesign();
        java.io.File file = null;
        java.io.FileOutputStream fos = null;
        try {
            file = new java.io.File(context.getCacheDir(), "print.png");
            fos = new java.io.FileOutputStream(file);
            fos.write(imageBytes);
            fos.close();
            fos = null;
            design.drawBitmap(file.getAbsolutePath(), LabelConst.CLS_RT_NORMAL, 0, 0, 0, 0, LabelConst.CLS_PRT_RES_300, LabelConst.CLS_UNIT_MILLI);
            r = printer.print(design, 1);
        } catch (Exception e) {
            r = -1;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception ignored) {
                }
            }
            if (file != null && file.exists()) {
                file.delete();
            }
            printer.disconnect();
        }
        return r;
    }

    private ArrayList<Map<String, Object>> searchUsbPrinters() {
        ArrayList<Map<String, Object>> printerList = new ArrayList<>();
        usbDeviceMap.clear();

        for (UsbDevice device : usbManager.getDeviceList().values()) {
            // Use getDeviceId() as key instead of getDeviceName()
            String deviceKey = String.valueOf(device.getDeviceId());
            usbDeviceMap.put(deviceKey, device);
            UsbPrinterInfo printerInfo = new UsbPrinterInfo(device);
            printerList.add(printerInfo.toMap());
        }
        return printerList;
    }

    private void printImageUsbSpecificWithPermission(String deviceId, byte[] imageBytes, MethodChannel.Result result) {
        searchUsbPrinters();
        UsbDevice device = usbDeviceMap.get(deviceId);
        if (device == null) {
            result.error("DEVICE_NOT_FOUND", "Device not found: " + deviceId, null);
            return;
        }

        // Check if we already have permission
        if (usbManager.hasPermission(device)) {
            // We already have permission, proceed directly with printing
            int res = executePrintWithDevice(device, imageBytes);
            if (res == LabelConst.CLS_SUCCESS) {
                result.success(null);
            } else {
                result.error("PRINT_ERROR", String.valueOf(res), null);
            }
            return;
        }

        // Request permission for the USB device
        int flags = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
                ? PendingIntent.FLAG_IMMUTABLE
                : 0;
        PendingIntent permissionIntent = PendingIntent.getBroadcast(
                context, 0, new Intent(ACTION_USB_PERMISSION), flags
        );

        // Save the pending print request using deviceId as key
        pendingPrintRequests.put(deviceId, new PendingPrintRequest(device, imageBytes, result));

        usbManager.requestPermission(device, permissionIntent);
    }

    private int executePrintWithDevice(UsbDevice device, byte[] imageBytes) {
        LabelPrinter printer = new LabelPrinter();
        printer.setContext(context);
        int r = printer.connect(LabelConst.CLS_PORT_USB, device);
        if (r != LabelConst.CLS_SUCCESS) {
            System.err.println("Error connecting to USB printer: " + r);
            return r;
        }
        LabelDesign design = new LabelDesign();
        java.io.File file = null;
        java.io.FileOutputStream fos = null;
        try {
            file = new java.io.File(context.getCacheDir(), "print.png");
            fos = new java.io.FileOutputStream(file);
            fos.write(imageBytes);
            fos.close();
            fos = null;
            design.drawBitmap(file.getAbsolutePath(), LabelConst.CLS_RT_NORMAL, 0, 0, 0, 0, LabelConst.CLS_PRT_RES_300, LabelConst.CLS_UNIT_MILLI);
            r = printer.print(design, 1);
        } catch (Exception e) {
            System.err.println("Error during print operation: " + e.getMessage());
            r = -1;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception ignored) {
                }
            }
            if (file != null && file.exists()) {
                file.delete();
            }
            printer.disconnect();
        }
        return r;
    }

    private void searchUsbPrintersWithOptionalSerialNumbers(MethodChannel.Result result, boolean includeSerialNumbers) {
        // If there's already a search in progress, return error
        if (pendingSearchResult != null || isSearchingSerialNumbers) {
            result.error("SEARCH_IN_PROGRESS", "Another USB search is already in progress", null);
            return;
        }

        this.includeSerialNumbers = includeSerialNumbers;

        // If serial numbers are not requested, use simple synchronous search
        if (!includeSerialNumbers) {
            ArrayList<Map<String, Object>> printerList = searchUsbPrinters();
            result.success(printerList);
            return;
        }

        // Set search flag to prevent concurrent searches
        isSearchingSerialNumbers = true;

        // Initialize data structures for asynchronous search with serial numbers
        pendingSearchResult = result;
        currentSearchPrinters = new ArrayList<>();
        citizenDevicesQueue = new ArrayList<>();
        currentDeviceIndex = 0;
        usbDeviceMap.clear();

        // Start global timeout
        startGlobalSearchTimeout();

        // Collect all USB devices
        Collection<UsbDevice> usbDeviceList = usbManager.getDeviceList().values();
        System.out.println("Total USB devices found: " + usbDeviceList.size());

        for (UsbDevice device : usbDeviceList) {
            String deviceKey = String.valueOf(device.getDeviceId());
            usbDeviceMap.put(deviceKey, device);
            UsbPrinterInfo printerInfo = new UsbPrinterInfo(device);

            // Filter only CITIZEN devices for serial number reading optimization
            String manufacturerName = device.getManufacturerName();
            if (manufacturerName != null && manufacturerName.toUpperCase().contains("CITIZEN")) {
                citizenDevicesQueue.add(device);
                currentSearchPrinters.add(printerInfo);
                System.out.println("Found CITIZEN device: " + printerInfo.toString());
            } else {
                // Add non-CITIZEN devices to results without serial number
                currentSearchPrinters.add(printerInfo);
                System.out.println("Found non-CITIZEN device: " + printerInfo.toString());
            }
        }

        System.out.println("CITIZEN devices to process for serial numbers: " + citizenDevicesQueue.size());

        // If there are no CITIZEN devices, return results immediately
        if (citizenDevicesQueue.isEmpty()) {
            finishSearchAndReturnResults();
        } else {
            // Start asynchronous process for CITIZEN devices only
            processNextCitizenDevice();
        }
    }

    private void processNextCitizenDevice() {
        if (currentDeviceIndex >= citizenDevicesQueue.size()) {
            // All CITIZEN devices have been processed
            finishSearchAndReturnResults();
            return;
        }

        UsbDevice device = citizenDevicesQueue.get(currentDeviceIndex);

        // Only process if we need serial numbers
        if (!includeSerialNumbers) {
            currentDeviceIndex++;
            processNextCitizenDevice();
            return;
        }

        // Check if we already have permission for this device
        if (usbManager.hasPermission(device)) {
            // We already have permission, read serial number directly
            processSerialNumberForDevice(device);
        } else {
            // Request permission to read serial number
            requestSerialNumberPermission(device);
        }
    }

    private void requestSerialNumberPermission(UsbDevice device) {
        if (!includeSerialNumbers) {
            // Skip permission request if serial numbers are not needed
            currentDeviceIndex++;
            processNextCitizenDevice();
            return;
        }

        int flags = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
                ? PendingIntent.FLAG_IMMUTABLE
                : 0;

        PendingIntent permissionIntent = PendingIntent.getBroadcast(
                context, 0, new Intent(ACTION_USB_SERIAL_PERMISSION), flags
        );

        System.out.println("Requesting serial number permission for device: " + device.getDeviceId());
        usbManager.requestPermission(device, permissionIntent);

        // Start timeout handler
        startPermissionTimeoutHandler(device);
    }

    private void startPermissionTimeoutHandler(UsbDevice device) {
        // Cancel any existing timeout handler for the previous device
        if (searchTimeoutHandler != null && searchTimeoutRunnable != null) {
            searchTimeoutHandler.removeCallbacks(searchTimeoutRunnable);
        }

        // Start a new timeout handler
        searchStartTime = System.currentTimeMillis();
        searchTimeoutRunnable = new Runnable() {
            @Override
            public void run() {
                // Only proceed with timeout if we're still searching and this is still the current device
                if (!isSearchingSerialNumbers || currentDeviceIndex >= citizenDevicesQueue.size()) {
                    return; // Search was completed or cancelled
                }

                UsbDevice currentDevice = citizenDevicesQueue.get(currentDeviceIndex);
                if (currentDevice.getDeviceId() != device.getDeviceId()) {
                    return; // We've moved to a different device
                }

                // Check if the permission was granted within the timeout period
                if (System.currentTimeMillis() - searchStartTime >= PERMISSION_TIMEOUT) {
                    // Timeout reached, proceed to next device
                    System.out.println("Permission request timed out for device: " + device.getDeviceId());
                    currentDeviceIndex++;
                    processNextCitizenDevice();
                } else {
                    // Reschedule the timeout check
                    if (isSearchingSerialNumbers) {
                        searchTimeoutHandler.postDelayed(this, 1000);
                    }
                }
            }
        };

        if (searchTimeoutHandler == null) {
            searchTimeoutHandler = new android.os.Handler();
        }
        searchTimeoutHandler.postDelayed(searchTimeoutRunnable, 1000);
    }

    private void processSerialNumberForDevice(UsbDevice device) {
        if (includeSerialNumbers) {
            try {
                // Find the corresponding UsbPrinterInfo object and update the serial number
                String deviceId = String.valueOf(device.getDeviceId());
                for (UsbPrinterInfo printerInfo : currentSearchPrinters) {
                    if (printerInfo.deviceId.equals(deviceId)) {
                        // Read the serial number with granted permission
                        String serialNumber = device.getSerialNumber();
                        printerInfo.serialNumber = serialNumber != null ? serialNumber : "";
                        System.out.println("Serial number for device " + deviceId + ": " + printerInfo.serialNumber);
                        break;
                    }
                }
            } catch (SecurityException e) {
                System.err.println("Security exception reading serial number for device " + device.getDeviceId() + ": " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Error reading serial number for device " + device.getDeviceId() + ": " + e.getMessage());
            }
        }

        // Move to next device
        currentDeviceIndex++;
        processNextCitizenDevice();
    }

    private void finishSearchAndReturnResults() {
        if (pendingSearchResult == null) {
            return; // No search in progress
        }

        try {
            // Clean up timeout handlers first
            cleanupTimeoutHandler();

            // Convert results to Map for response
            ArrayList<Map<String, Object>> resultList = new ArrayList<>();
            for (UsbPrinterInfo printerInfo : currentSearchPrinters) {
                resultList.add(printerInfo.toMap());
            }

            System.out.println("USB search completed. Found " + resultList.size() + " devices:");
            for (Map<String, Object> printer : resultList) {
                System.out.println("- " + printer);
            }

            // Send results
            pendingSearchResult.success(resultList);
        } catch (Exception e) {
            pendingSearchResult.error("SEARCH_COMPLETION_ERROR", e.getMessage(), null);
        } finally {
            // Clean up state variables
            pendingSearchResult = null;
            currentSearchPrinters = null;
            citizenDevicesQueue = null;
            currentDeviceIndex = 0;
            isSearchingSerialNumbers = false; // Reset search flag
        }
    }

    // New method to cancel any pending USB search
    private void cancelPendingSearch(MethodChannel.Result result) {
        if (pendingSearchResult == null) {
            result.success("No search in progress");
            return;
        }

        // Clean up state variables
        cleanupSearchState();

        result.success("Pending USB search canceled");
    }

    // Helper method to clean up search state variables
    private void cleanupSearchState() {
        // Clean up timeout handlers first
        cleanupTimeoutHandler();

        pendingSearchResult = null;
        currentSearchPrinters = null;
        citizenDevicesQueue = null;
        currentDeviceIndex = 0;
        isSearchingSerialNumbers = false; // Reset search flag
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPlugin.FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);

        // Unregister receiver only if it was registered
        if (usbReceiver != null) {
            try {
                context.unregisterReceiver(usbReceiver);
            } catch (IllegalArgumentException e) {
                // Receiver might already be unregistered
            }
            usbReceiver = null;
        }

        // Clean up pending requests and notify errors
        for (PendingPrintRequest request : pendingPrintRequests.values()) {
            request.result.error("PLUGIN_DETACHED", "Plugin was detached before print could complete", null);
        }
        pendingPrintRequests.clear();
        searchTimeoutRunnable = null;
    }

    // New method to start global timeout for entire search process
    private void startGlobalSearchTimeout() {
        cleanupTimeoutHandler(); // Clean up any existing timeout

        searchTimeoutHandler = new android.os.Handler();
        globalTimeoutRunnable = new Runnable() {
            @Override
            public void run() {
                System.out.println("Global search timeout reached. Completing search with current results.");
                finishSearchAndReturnResults();
            }
        };

        // Set global timeout to 60 seconds (longer than individual permission timeout)
        searchTimeoutHandler.postDelayed(globalTimeoutRunnable, GLOBAL_SEARCH_TIMEOUT);

        // Clean up maps
        usbDeviceMap.clear();
    }

    // Helper method to clean up timeout handlers
    private void cleanupTimeoutHandler() {
        if (searchTimeoutHandler != null && searchTimeoutRunnable != null) {
            searchTimeoutHandler.removeCallbacks(searchTimeoutRunnable);
            searchTimeoutHandler = null;
            searchTimeoutRunnable = null;
        }
        if (searchTimeoutHandler != null && globalTimeoutRunnable != null) {
            searchTimeoutHandler.removeCallbacks(globalTimeoutRunnable);
            searchTimeoutHandler = null;
            globalTimeoutRunnable = null;
        }
    }
}

