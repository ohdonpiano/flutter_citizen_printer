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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// Classe per rappresentare informazioni su stampanti USB
class UsbPrinterInfo {
    public String deviceId;
    public String deviceName;
    public String manufacturerName;
    public String productName;
    public int vendorId;
    public int productId;
    public UsbDevice usbDevice;

    public UsbPrinterInfo(UsbDevice device) {
        this.usbDevice = device;
        this.deviceId = String.valueOf(device.getDeviceId());
        this.deviceName = device.getDeviceName();
        this.manufacturerName = device.getManufacturerName() != null ? device.getManufacturerName() : "";
        this.productName = device.getProductName() != null ? device.getProductName() : "";
        this.vendorId = device.getVendorId();
        this.productId = device.getProductId();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("deviceId", deviceId);
        map.put("deviceName", deviceName);
        map.put("manufacturerName", manufacturerName);
        map.put("productName", productName);
        map.put("vendorId", vendorId);
        map.put("productId", productId);
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
                '}';
    }
}

public class FlutterCitizenPrinterPlugin implements FlutterPlugin, MethodChannel.MethodCallHandler {
    private static final String ACTION_USB_PERMISSION = "com.citizen.USB_PERMISSION";
    private MethodChannel channel;
    private Context context;
    private UsbManager usbManager;
    private Map<String, UsbDevice> usbDeviceMap = new HashMap<>();
    private BroadcastReceiver usbReceiver;
    private Map<String, PendingPrintRequest> pendingPrintRequests = new HashMap<>();

    // Classe per gestire le richieste di stampa in attesa
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

        // Registro il BroadcastReceiver per le autorizzazioni USB
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
                                    // L'autorizzazione è stata concessa
                                    int res = executePrintWithDevice(pendingRequest.device, pendingRequest.imageBytes);
                                    if (res == LabelConst.CLS_SUCCESS) {
                                        pendingRequest.result.success(null);
                                    } else {
                                        pendingRequest.result.error("PRINT_ERROR", String.valueOf(res), null);
                                    }
                                } else {
                                    // L'autorizzazione è stata negata
                                    pendingRequest.result.error("PERMISSION_DENIED", "USB permission denied", null);
                                }
                            }
                        }
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        // Per Android 13+ dobbiamo specificare il flag RECEIVER_NOT_EXPORTED
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
                ArrayList<Map<String, Object>> printers = searchUsbPrinters();
                System.out.println("Found USB Printers: ");
                for (Map<String, Object> printer : printers) {
                    System.out.println("- " + printer);
                }
                result.success(printers);
            } catch (Exception e) {
                result.error("USB_SEARCH_ERROR", e.getMessage(), null);
            }
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
            // Usa getDeviceId() come chiave invece di getDeviceName()
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

        // Verifica se abbiamo già il permesso
        if (usbManager.hasPermission(device)) {
            // Abbiamo già il permesso, procedi direttamente con la stampa
            int res = executePrintWithDevice(device, imageBytes);
            if (res == LabelConst.CLS_SUCCESS) {
                result.success(null);
            } else {
                result.error("PRINT_ERROR", String.valueOf(res), null);
            }
            return;
        }

        // Richiedi il permesso per il dispositivo USB
        int flags = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
                ? PendingIntent.FLAG_IMMUTABLE
                : 0;
        PendingIntent permissionIntent = PendingIntent.getBroadcast(
                context, 0, new Intent(ACTION_USB_PERMISSION), flags
        );

        // Salva la richiesta di stampa in attesa usando deviceId come chiave
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

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPlugin.FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);

        // Deregistra il receiver solo se è stato registrato
        if (usbReceiver != null) {
            try {
                context.unregisterReceiver(usbReceiver);
            } catch (IllegalArgumentException e) {
                // Il receiver potrebbe essere già stato deregistrato
            }
            usbReceiver = null;
        }

        // Pulisci le richieste pendenti e notifica gli errori
        for (PendingPrintRequest request : pendingPrintRequests.values()) {
            request.result.error("PLUGIN_DETACHED", "Plugin was detached before print could complete", null);
        }
        pendingPrintRequests.clear();

        // Pulisci le mappe
        usbDeviceMap.clear();
    }
}
