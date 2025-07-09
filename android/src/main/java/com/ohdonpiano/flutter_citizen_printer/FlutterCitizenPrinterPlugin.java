package com.ohdonpiano.flutter_citizen_printer;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

import android.content.Context;
import android.hardware.usb.UsbDevice;

import com.citizen.sdk.labelprint.LabelPrinter;
import com.citizen.sdk.labelprint.LabelConst;
import com.citizen.sdk.labelprint.LabelDesign;
import com.citizen.sdk.labelprint.CitizenPrinterInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class FlutterCitizenPrinterPlugin implements FlutterPlugin, MethodChannel.MethodCallHandler {
    private MethodChannel channel;
    private Context context;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPlugin.FlutterPluginBinding binding) {
        channel = new MethodChannel(binding.getBinaryMessenger(), "flutter_citizen_printer");
        context = binding.getApplicationContext();
        channel.setMethodCallHandler(this);
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
            else result.error("PRINT_ERROR", CitizenPrinterErrorCodes.errorMessage(res), null);
        } else if (call.method.equals("printImageUSB")) {
            byte[] bytes = call.argument("imageBytes");
            int res = printImageUSB(bytes);
            if (res == LabelConst.CLS_SUCCESS) result.success(null);
            else result.error("PRINT_ERROR", CitizenPrinterErrorCodes.errorMessage(res), null);
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
                result.error("DETECT_ERROR", CitizenPrinterErrorCodes.errorMessage(err[0]), null);
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
                result.error("DETECT_ERROR", CitizenPrinterErrorCodes.errorMessage(err[0]), null);
            }
        } else if (call.method.equals("getUsbStatus")) {
            LabelPrinter printer = new LabelPrinter();
            printer.setContext(context);
            int r = printer.connect(LabelConst.CLS_PORT_USB, (UsbDevice) null);
            if (r != LabelConst.CLS_SUCCESS) {
                result.error("CONNECT_ERROR", "Code: " + r, null);
                return;
            }
            int res = printer.printerCheck();
            printer.disconnect();
            result.success(res);
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
        try {
            java.io.File file = new java.io.File(context.getCacheDir(), "print.png");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
            fos.write(imageBytes);
            fos.close();
            design.drawBitmap(file.getAbsolutePath(), LabelConst.CLS_RT_NORMAL, width, height, 0, 0, LabelConst.CLS_PRT_RES_300, LabelConst.CLS_UNIT_MILLI);
            r = printer.print(design, 1);
        } catch (Exception e) {
            return -1;
        } finally {
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
        try {
            java.io.File file = new java.io.File(context.getCacheDir(), "print.png");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
            fos.write(imageBytes);
            fos.close();
            design.drawBitmap(file.getAbsolutePath(), LabelConst.CLS_RT_NORMAL, 0, 0, 0, 0, LabelConst.CLS_PRT_RES_300, LabelConst.CLS_UNIT_MILLI);
            r = printer.print(design, 1);
        } catch (Exception e) {
            return -1;
        } finally {
            printer.disconnect();
        }
        return r;
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPlugin.FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }
}

