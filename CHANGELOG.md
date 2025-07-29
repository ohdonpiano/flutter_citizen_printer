## 0.0.4

*   **Correzione di bug**: Risolto problema di cast nel metodo `searchUsbPrinters()` che causava l'errore `type '_Map<Object?, Object?>' is not a subtype of type 'Map<String, dynamic>' in type cast`

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