import 'dart:async';
import 'package:flutter/services.dart';

class FlutterCitizenPrinter {
  static const MethodChannel _channel = MethodChannel('flutter_citizen_printer');

  static Future<List<String>> detectPrinters({int timeout = 3}) async {
    final List<dynamic> ips = await _channel.invokeMethod(
      'detectPrinters',
      {'timeout': timeout},
    );
    return ips.cast<String>();
  }

  static Future<void> printImageWiFi(String ip, Uint8List imageBytes) async {
    await _channel.invokeMethod('printImageWiFi', {'ip': ip, 'imageBytes': imageBytes});
  }

  static Future<void> printImageUSB(Uint8List imageBytes) async {
    await _channel.invokeMethod('printImageUSB', {'imageBytes': imageBytes});
  }
}