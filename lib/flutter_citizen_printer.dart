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

  /// Detects Citizen printers on the network.
  /// Returns a list of [CitizenPrinterDetails] containing the IP address and device name of each detected printer.
  /// If no printers are found, it returns an empty list.
  /// The [timeout] parameter specifies the maximum time to wait for the detection to complete.
  /// The default timeout is 3 seconds.
  ///
  static Future<List<CitizenPrinterDetails>> detectPrinters(
      {int timeout = 3}) async {
    final List<Object?> ips = await _channel.invokeMethod(
      'detectPrinters',
      {'timeout': timeout},
    );
    return ips.map((e) => CitizenPrinterDetails.fromMap(e as Map)).toList();
  }

  /// Searches for Citizen label printers on the network.
  /// Returns a list of IP addresses of the found printers.
  /// If no printers are found, it returns an empty list.
  /// The [timeout] parameter specifies the maximum time to wait for the search to complete.
  /// The default timeout is 3 seconds.
  ///
  static Future<List<String>> searchLabelPrinter({int timeout = 3}) async {
    final List<dynamic> ips = await _channel.invokeMethod(
      'searchLabelPrinter',
      {'timeout': timeout},
    );
    return ips.cast<String>();
  }

  /// Gets the USB status of the connected Citizen printer.
  /// Returns a list of [CitizenPrinterUsbStatus] representing the current status.
  /// If the printer is not connected or if there is an error, it returns an empty list.
  /// The status codes are based on the Citizen printer documentation
  ///
  static Future<List<CitizenPrinterUsbStatus>> getUsbStatus() async {
    final int rawStatus = await _channel.invokeMethod('getUsbStatus');
    return CitizenPrinterUsbStatus.fromCode(rawStatus);
  }

  /// Prints an image to a network printer identified by [ipAddress].
  /// [imageBytes] is the image data to be printed.
  /// [width] and [height] are the dimensions of the image in pixels.
  /// This method requires the printer to be connected to the network and reachable via the provided IP address.
  /// Throws an exception if the printer is not found or if there is an error during printing.
  ///
  static Future<void> printImageWiFi(
      String ip, Uint8List imageBytes, int width, int height) async {
    await _channel.invokeMethod('printImageWiFi', {
      'ip': ip,
      'imageBytes': imageBytes,
      'width': width,
      'height': height,
    });
  }

  /// Prints an image to a USB printer. To be used with only one USB printer connected to the device.
  /// This method will automatically detect the connected USB printer and print the image, avoiding the need to specify a device ID.
  /// [imageBytes] is the image data to be printed.
  /// This method requires the USB printer to be connected and recognized by the system.
  /// Throws an exception if the printer is not found or if there is an error during printing.
  ///
  static Future<void> printImageUSB(Uint8List imageBytes) async {
    await _channel.invokeMethod('printImageUSB', {'imageBytes': imageBytes});
  }

  /// Search for USB printers connected to the device.
  /// /// If [includeSerialNumbers] is true, the serial number will be included in the
  /// returned [UsbPrinterInfo] objects.
  /// Note that if includeSerialNumbers is true, the method may take longer to execute
  /// as it retrieves serial numbers for each USB printer, and a specific permission
  /// may be required to access the serial number for each USB device.
  /// If [includeSerialNumbers] is false, the serial number will be an empty string
  /// in the returned [UsbPrinterInfo] objects.
  /// Returns a list of [UsbPrinterInfo] objects representing the found USB printers.
  ///
  static Future<List<UsbPrinterInfo>> searchUsbPrinters(
      {bool includeSerialNumbers = false}) async {
    final List<dynamic> devices =
        await _channel.invokeMethod('searchUsbPrinters', {
      'includeSerialNumbers': includeSerialNumbers,
    });
    return devices
        .map((e) => UsbPrinterInfo.fromMap(Map<String, dynamic>.from(e as Map)))
        .toList();
  }

  /// Prints an image to a specific USB printer identified by [deviceId].
  /// [deviceId] is the unique identifier of the USB printer.
  /// [imageBytes] is the image data to be printed.
  /// This method requires the USB printer to be connected and recognized by the system.
  /// Throws an exception if the printer is not found or if there is an error during printing.
  ///
  static Future<void> printImageUsbSpecific(
      String deviceId, Uint8List imageBytes) async {
    await _channel.invokeMethod('printImageUsbSpecific', {
      'deviceId': deviceId,
      'imageBytes': imageBytes,
    });
  }
}
