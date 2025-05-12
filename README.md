# flutter_citizen_printer

Flutter plugin for Citizen label printers via USB and WiFi.

## Requirements

- Flutter 3.x
- Android SDK 19+
- Proprietary library `CSJLabelLib_Android.jar` (provided separately)

## Installation

Add to your `pubspec.yaml`:
```yaml
dependencies:
  flutter_citizen_printer: ^0.0.1    
```

### Android Native Library

1. Download and accept the `CSJLabelLib_Android.jar` along with its EULA (see `EULA_Citizen_Systems_Japan.txt`).
2. Copy `CSJLabelLib_Android.jar` into the `android/libs/` folder of your Flutter project.

## Usage

Import the plugin:
```dart
import 'package:flutter_citizen_printer/flutter_citizen_printer.dart';
```

### Print via USB
```dart
final bytes = (await rootBundle.load('assets/sample.bmp')).buffer.asUint8List();
await FlutterCitizenPrinter.printImageUSB(bytes);
```

### Print via WiFi
```dart
final bytes = (await rootBundle.load('assets/sample.bmp')).buffer.asUint8List();
await FlutterCitizenPrinter.printImageWiFi(
  '192.168.0.100', // printer IP
  bytes,
);
```

### Discover printers on LAN
```dart
final ips = await FlutterCitizenPrinter.detectPrinters(3); // 3 seconds timeout
print('Detected printers: $ips');
```

## EULA

The native library `CSJLabelLib_Android.jar` is subject to the EULA from Citizen Systems Japan (see `EULA_Citizen_Systems_Japan.txt`). You must accept the terms before using it.

## Example

See the `example/` folder for a complete app demonstrating printing via USB, WiFi, and printer discovery.