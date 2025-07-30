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

class UsbPrinterInfo {
  final String deviceId;
  final String deviceName;
  final String manufacturerName;
  final String productName;
  final int vendorId;
  final int productId;
  final String serialNumber;

  UsbPrinterInfo({
    required this.deviceId,
    required this.deviceName,
    required this.manufacturerName,
    required this.productName,
    required this.vendorId,
    required this.productId,
    required this.serialNumber,
  });

  factory UsbPrinterInfo.fromMap(Map<String, dynamic> map) {
    return UsbPrinterInfo(
      deviceId: map['deviceId'] as String,
      deviceName: map['deviceName'] as String,
      manufacturerName: map['manufacturerName'] as String,
      productName: map['productName'] as String,
      vendorId: map['vendorId'] as int,
      productId: map['productId'] as int,
      serialNumber: map['serialNumber'] as String? ?? '',
    );
  }

  String get displayName {
    if (manufacturerName.isNotEmpty && productName.isNotEmpty) {
      return '$deviceId ($manufacturerName $productName)';
    } else if (productName.isNotEmpty) {
      return '$deviceId $productName)';
    } else if (manufacturerName.isNotEmpty) {
      return '$deviceId ($manufacturerName)';
    }
    return deviceName;
  }

  @override
  String toString() {
    return 'UsbPrinterInfo(deviceId: $deviceId, deviceName: $deviceName, manufacturerName: $manufacturerName, productName: $productName, vendorId: $vendorId, productId: $productId, serialNumber: $serialNumber)';
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

  static Future<List<UsbPrinterInfo>> searchUsbPrinters() async {
    final List<dynamic> devices =
        await _channel.invokeMethod('searchUsbPrinters');
    return devices
        .map((e) => UsbPrinterInfo.fromMap(Map<String, dynamic>.from(e as Map)))
        .toList();
  }

  static Future<void> printImageUsbSpecific(
      String deviceId, Uint8List imageBytes) async {
    await _channel.invokeMethod('printImageUsbSpecific', {
      'deviceId': deviceId,
      'imageBytes': imageBytes,
    });
  }
}
