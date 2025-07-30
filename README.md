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
  flutter_citizen_printer: ^0.0.5    
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

### Multiple USB Printers Support (Enhanced in v0.0.5)

#### Search for USB printers with serial numbers
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
  print('Serial Number: ${printer.serialNumber}'); // NEW in v0.0.5 - populated for CITIZEN devices
}
```

**Note on Serial Numbers**: Serial numbers are automatically retrieved for CITIZEN devices only. The plugin intelligently requests USB permissions only for CITIZEN branded devices to optimize the user experience and avoid unnecessary permission dialogs for non-CITIZEN USB devices.

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
- `serialNumber`: Device serial number (populated for CITIZEN devices) **NEW in v0.0.5**
- `displayName`: User-friendly display name

### Methods

#### searchUsbPrinters()
Returns a list of all connected USB printers as `List<UsbPrinterInfo>`. 

**Enhanced in v0.0.5**: Now includes serial number reading for CITIZEN devices with optimized permission handling. The method:
- Scans all USB devices
- Identifies CITIZEN devices by manufacturer name
- Asynchronously requests permissions only for CITIZEN devices
- Populates serial numbers for devices where permission is granted
- Returns complete device information including serial numbers

#### printImageUsbSpecific(String deviceId, Uint8List imageBytes)
Prints an image to a specific USB printer identified by its device ID.

#### printImageUSB(Uint8List imageBytes)
Prints an image to the first available USB printer (legacy method).

#### printImageWiFi(String ip, Uint8List imageBytes, int width, int height)
Prints an image to a WiFi printer at the specified IP address.

#### detectPrinters({int timeout = 3})
Discovers Citizen printers on the local network via WiFi.

#### getUsbStatus()
Gets the status of the USB printer connection.

## Example

See the complete example in the `/example` folder which demonstrates:
- Searching for multiple USB printers with serial number support
- Selecting a specific printer from a list
- Printing to the selected printer
- WiFi printer discovery and printing

## Performance & Optimization

**Version 0.0.5** introduces several performance optimizations:

- **Smart Permission Requests**: Only requests USB permissions for CITIZEN branded devices, reducing unnecessary user prompts
- **Asynchronous Processing**: Serial number reading is handled asynchronously to prevent UI blocking
- **Sequential Permission Handling**: Processes one device at a time to avoid permission conflicts
- **Concurrent Search Prevention**: Prevents multiple simultaneous USB searches that could cause conflicts

## Changelog

### Version 0.0.5
- **Enhanced USB Serial Number Support**: Added asynchronous serial number reading for CITIZEN USB printers
- **Performance optimizations**: Smart filtering and sequential permission handling
- **Technical improvements**: All code comments translated to English for consistency

### Version 0.0.4
- **Bug fix**: Fixed cast issue in `searchUsbPrinters()` method

### Version 0.0.3
- **Multiple USB printers support**: Added ability to manage and print to multiple USB printers simultaneously
- New `searchUsbPrinters()` method to find all connected USB printers
- New `printImageUsbSpecific()` method to print to a specific printer
- Added `UsbPrinterInfo` class for printer information
- Enhanced example app with printer selection UI

### Version 0.0.2
- Bug fixes and improvements

### Version 0.0.1
- Initial release with basic USB and WiFi printing support
