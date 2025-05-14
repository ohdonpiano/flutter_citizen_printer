enum CitizenPrinterUsbStatus {
  ready,
  coverOpen,
  paperEnd,
  error,
  pause,
  bufferFull,
  cutterError,
  ribbonEnd,
  unknown;

  static List<CitizenPrinterUsbStatus> fromCode(int code) {
    final statuses = <CitizenPrinterUsbStatus>[];
    if (code == 0x00) {
      statuses.add(ready);
    } else {
      if (code & 0x01 != 0) statuses.add(coverOpen);
      if (code & 0x02 != 0) statuses.add(paperEnd);
      if (code & 0x04 != 0) statuses.add(error);
      if (code & 0x08 != 0) statuses.add(pause);
      if (code & 0x10 != 0) statuses.add(bufferFull);
      if (code & 0x20 != 0) statuses.add(cutterError);
      if (code & 0x40 != 0) statuses.add(ribbonEnd);
      if (statuses.isEmpty) statuses.add(unknown);
    }
    return statuses;
  }
}