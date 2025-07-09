import 'dart:async';
import 'package:flutter/services.dart';
import 'package:flutter_citizen_printer/usb_status.dart';

class CitizenPrinterDetails {
  final String ipAddress;
  final String deviceName;

  CitizenPrinterDetails({required this.ipAddress, required this.deviceName});

  factory CitizenPrinterDetails.fromMap(Map map) {
    return CitizenPrinterDetails(
      ipAddress: map['ipAddress'] as String,
      deviceName: map['deviceName'] as String,
    );
  }
}

class FlutterCitizenPrinter {
  static const MethodChannel _channel =
      MethodChannel('flutter_citizen_printer');

  static Future<List<CitizenPrinterDetails>> detectPrinters(
      {int timeout = 3}) async {
    final List<Object?> ips = await _channel.invokeMethod(
      'detectPrinters',
      {'timeout': timeout},
    );
    return ips.map((e) => CitizenPrinterDetails.fromMap(e as Map)).toList();
  }

  static Future<List<String>> searchLabelPrinter({int timeout = 3}) async {
    final List<dynamic> ips = await _channel.invokeMethod(
      'searchLabelPrinter',
      {'timeout': timeout},
    );
    return ips.cast<String>();
  }

  static Future<List<CitizenPrinterUsbStatus>> getUsbStatus() async {
    final int rawStatus = await _channel.invokeMethod('getUsbStatus');
    return CitizenPrinterUsbStatus.fromCode(rawStatus);
  }

  static Future<void> printImageWiFi(
      String ip, Uint8List imageBytes, int width, int height) async {
    await _channel.invokeMethod('printImageWiFi', {
      'ip': ip,
      'imageBytes': imageBytes,
      'width': width,
      'height': height,
    });
  }

  static Future<void> printImageUSB(Uint8List imageBytes) async {
    await _channel.invokeMethod('printImageUSB', {'imageBytes': imageBytes});
  }
}
