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
  flutter_citizen_printer: ^0.0.6    
```

### Android Native Library

1. Download and accept the `CSJLabelLib_Android.jar` along with its EULA (see `EULA_Citizen_Systems_Japan.txt`).
2. Copy `CSJLabelLib_Android.jar` into the `android/libs/` folder of your Flutter project.

## Usage

Import the plugin:
```dart
import 'package:flutter_citizen_printer/flutter_citizen_printer.dart';
```

### Print via USB (single printer)
```dart
final bytes = (await rootBundle.load('assets/sample.bmp')).buffer.asUint8List();
await FlutterCitizenPrinter.printImageUSB(bytes);
```

### Multiple USB Printers Support

#### Search for USB printers (Fast - Default Behavior)
```dart
List<UsbPrinterInfo> printers = await FlutterCitizenPrinter.searchUsbPrinters();
print('Found ${printers.length} USB printers');

for (UsbPrinterInfo printer in printers) {
  print('Printer: ${printer.displayName}');
  print('Device ID: ${printer.deviceId}');
  print('Manufacturer: ${printer.manufacturerName}');
  print('Product: ${printer.productName}');
  print('Vendor ID: ${printer.vendorId}');
  print('Product ID: ${printer.productId}');
  print('Serial Number: ${printer.serialNumber}'); // Empty by default
}
```

#### Search for USB printers with serial numbers (Requires User Permission)
```dart
// NEW in v0.0.6: Optional serial number reading
List<UsbPrinterInfo> printers = await FlutterCitizenPrinter.searchUsbPrinters(
  includeSerialNumbers: true, // This will request permissions
);

for (UsbPrinterInfo printer in printers) {
  print('Printer: ${printer.displayName}');
  print('Serial Number: ${printer.serialNumber}'); // Populated for CITIZEN devices with permission
}
```

**Important Notes on Serial Numbers:**
- **Default behavior**: Serial numbers are NOT retrieved to avoid permission requests and maintain fast search performance
- **Optimized for CITIZEN devices**: When `includeSerialNumbers: true`, the plugin only requests USB permissions for devices with "CITIZEN" in the manufacturer name
- **User permission required**: Reading serial numbers requires explicit USB permission from the user for each CITIZEN device
- **May not be available**: Some printers may not have serial numbers set by the manufacturer

#### Print to specific USB printer
```dart
// Select a specific printer from the list
UsbPrinterInfo selectedPrinter = printers[0]; // or from user selection

// Print to the selected printer
final bytes = (await rootBundle.load('assets/sample.bmp')).buffer.asUint8List();
await FlutterCitizenPrinter.printImageUsbSpecific(
  selectedPrinter.deviceId,
  bytes,
);
```

### Print via WiFi
```dart
final bytes = (await rootBundle.load('assets/sample.bmp')).buffer.asUint8List();
await FlutterCitizenPrinter.printImageWiFi(
  '192.168.0.100', // printer IP
  bytes,
  width, // image width
  height, // image height
);
```

### Discover printers on LAN
```dart
final ips = await FlutterCitizenPrinter.detectPrinters(timeout: 3); // 3 seconds timeout
print('Detected printers: $ips');
```

### Check USB printer status
```dart
final status = await FlutterCitizenPrinter.getUsbStatus();
print('USB Status: ${status.map((e) => e.name).join(", ")}');
```

## API Reference

### Classes

#### UsbPrinterInfo
Represents information about a USB printer:
- `deviceId`: Unique identifier for the printer
- `deviceName`: System device name
- `manufacturerName`: Manufacturer name
- `productName`: Product name
- `vendorId`: USB vendor ID
- `productId`: USB product ID
- `serialNumber`: Device serial number (requires permission when `includeSerialNumbers: true`)

#### CitizenPrinterDetails
Represents a WiFi printer discovered on the network:
- `ipAddress`: IP address of the printer
- `deviceName`: Name of the printer device

### Methods

#### `searchUsbPrinters({bool includeSerialNumbers = false})`
**NEW in v0.0.6**: Enhanced with optional serial number reading
- `includeSerialNumbers`: If `true`, requests USB permissions to read serial numbers from CITIZEN devices
- Returns: `Future<List<UsbPrinterInfo>>`
- **Performance**: Fast when `includeSerialNumbers` is `false` (default), slower when `true` due to permission requests

#### `printImageUsbSpecific(String deviceId, Uint8List imageBytes)`
Print to a specific USB printer using its device ID
- `deviceId`: Device ID from `UsbPrinterInfo.deviceId`
- `imageBytes`: Image data to print

#### `printImageUSB(Uint8List imageBytes)`
Print to the first available USB printer

#### `printImageWiFi(String ip, Uint8List imageBytes, int width, int height)`
Print via WiFi
- `ip`: Printer IP address
- `imageBytes`: Image data to print
- `width`: Image width in pixels
- `height`: Image height in pixels

#### `detectPrinters({int timeout = 3})`
Discover CITIZEN printers on the local network
- `timeout`: Search timeout in seconds
- Returns: `Future<List<CitizenPrinterDetails>>`

#### `getUsbStatus()`
Check the status of the first USB printer
- Returns: `Future<List<CitizenPrinterUsbStatus>>`

## Changelog

### v0.0.6
- **BREAKING ENHANCEMENT**: Made serial number reading optional in `searchUsbPrinters()`
- Added `includeSerialNumbers` parameter (defaults to `false` for better performance)
- Optimized USB permission requests to only target CITIZEN devices
- Improved search performance by avoiding unnecessary permission dialogs
- Updated all comments to English for international compatibility

### v0.0.5
- Added support for multiple USB printers with automatic serial number detection
- Enhanced USB printer search with detailed device information
- Added asynchronous permission handling for USB devices

## License

This project is licensed under the BSD 3-Clause License - see the LICENSE file for details.

**Note**: The Citizen SDK library (`CSJLabelLib_Android.jar`) is proprietary and subject to Citizen Systems' EULA.
