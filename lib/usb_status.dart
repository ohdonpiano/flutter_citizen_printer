enum CitizenPrinterUsbStatus {
  // Status codes based on Citizen printer documentation
  success(0, 'Success'),
  printerNotReady(1, 'Printer not ready'),
  paperEmpty(2, 'Paper empty'),
  ribbonEmpty(4, 'Ribbon empty'),
  coverOpen(8, 'Cover open'),
  printerOffline(16, 'Printer offline'),
  cutterError(32, 'Cutter error'),
  printerError(64, 'Printer error'),
  headUp(128, 'Head up'),
  pauseStatus(256, 'Pause status'),
  labelTaken(512, 'Label taken'),
  unknown(-1, 'Unknown status');

  const CitizenPrinterUsbStatus(this.code, this.message);

  final int code;
  final String message;

  static List<CitizenPrinterUsbStatus> fromCode(int statusCode) {
    List<CitizenPrinterUsbStatus> statuses = [];

    if (statusCode == 0) {
      return [CitizenPrinterUsbStatus.success];
    }

    // Check each bit flag
    for (CitizenPrinterUsbStatus status in CitizenPrinterUsbStatus.values) {
      if (status.code > 0 && (statusCode & status.code) != 0) {
        statuses.add(status);
      }
    }

    if (statuses.isEmpty) {
      statuses.add(CitizenPrinterUsbStatus.unknown);
    }

    return statuses;
  }
}
