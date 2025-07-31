## 0.0.6

*   **BREAKING ENHANCEMENT**: Made serial number reading optional in `searchUsbPrinters()` method
*   **New features**:
    *   Added optional `includeSerialNumbers` parameter to `searchUsbPrinters()` (defaults to `false`)
    *   Enhanced performance by avoiding permission requests when serial numbers are not needed
    *   Maintained backward compatibility while providing opt-in serial number functionality
*   **Performance improvements**:
    *   Fast default behavior: USB search without serial numbers requires no permissions
    *   Smart permission optimization: only requests USB permissions for CITIZEN devices when serial numbers are explicitly requested
    *   Reduced user interruption by eliminating unnecessary permission dialogs
*   **Technical enhancements**:
    *   Improved asynchronous handling to support both fast and detailed USB searches
    *   Enhanced code documentation and comments in English for international development
    *   Better separation of concerns between basic device enumeration and detailed device information

## 0.0.5

*   **Enhanced USB Serial Number Support**: Added asynchronous serial number reading for CITIZEN USB printers with optimized permission handling
*   **New features**:
    *   Asynchronous serial number retrieval for CITIZEN devices only (optimized to avoid unnecessary permission requests)
    *   Sequential permission handling to prevent conflicts between multiple USB device access requests
    *   Enhanced `UsbPrinterInfo` class with `serialNumber` field populated for CITIZEN devices
    *   Improved error handling and logging for USB device management
*   **Performance optimizations**:
    *   Smart filtering: only requests permissions for devices with "CITIZEN" manufacturer name
    *   Non-blocking USB search process that handles permission dialogs gracefully
    *   Prevention of concurrent search operations to avoid conflicts
*   **Technical improvements**:
    *   All code comments translated to English for consistency
    *   Enhanced BroadcastReceiver for handling both print and serial number permissions
    *   Better state management for asynchronous operations

## 0.0.4

*   **Bug fix**: Fixed cast issue in `searchUsbPrinters()` method that caused `type '_Map<Object?, Object?>' is not a subtype of type 'Map<String, dynamic>' in type cast` error

## 0.0.3

*   **Multiple USB printers support**: Added ability to manage and print to multiple USB printers simultaneously.
*   **New features**:
    *   `searchUsbPrinters()` - Search for all connected USB printers
    *   `printImageUsbSpecific(deviceId, imageBytes)` - Print to a specific USB printer by device ID
    *   `UsbPrinterInfo` class to represent USB printer information (device ID, name, manufacturer, vendor/product IDs)
*   **Enhanced printer identification**: Each USB printer is now uniquely identified by device ID
*   **Improved example app**: Added UI to select and print to specific USB printers

## 0.0.2

*   Bug fixes and improvements.

## 0.0.1

*   Initial release.
*   Features:
    *   Printer discovery via WiFi.
    *   Printing of bitmap images via WiFi.
    *   Printing of bitmap images via USB.