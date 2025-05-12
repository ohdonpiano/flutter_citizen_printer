package com.ohdonpiano.flutter_citizen_printer;

public class CitizenPrinterErrorCodes {
    /// The operation has been done successfully.
    public static final int CLS_SUCCESS = 0;

    /// The printer is already connected.
    public static final int CLS_E_CONNECTED = 1001;

    /// The printer is not connected.
    public static final int CLS_E_DISCONNECT = 1002;

    /// Failed to connect to the printer.
    public static final int CLS_E_NOTCONNECT = 1003;

    /// Failed to confirm the model name after connecting to the printer.
    public static final int CLS_E_CONNECT_NOTFOUND = 1004;

    /// No context is specified.
    public static final int CLS_E_NOCONTEXT = 1006;

    /// The Bluetooth device setting is invalid.
    public static final int CLS_E_BT_DISABLE = 1007;

    /// The Bluetooth device is not found.
    public static final int CLS_E_BT_NODEVICE = 1008;

    /// The operation is not supported by the printer or an invalid parameter was used.
    public static final int CLS_E_ILLEGAL = 1101;

    /// The printer is offline.
    public static final int CLS_E_OFFLINE = 1102;

    /// The file name does not exist.
    public static final int CLS_E_NOEXIST = 1103;

    /// The service could not perform the requested procedure.
    public static final int CLS_E_FAILURE = 1104;

    /// The service has been timed out waiting for a response from the printer.
    public static final int CLS_E_TIMEOUT = 1105;

    /// A printer could not be found for the printer search.
    public static final int CLS_E_NO_LIST = 1106;

    /// The file format is not supported.
    public static final int CLS_EPTR_BADFORMAT = 1203;

    public static String errorMessage(int errorCode) {
        switch (errorCode) {
            case CLS_SUCCESS:
                return "Success";
            case CLS_E_CONNECTED:
                return "Printer is already connected";
            case CLS_E_DISCONNECT:
                return "Printer is not connected";
            case CLS_E_NOTCONNECT:
                return "Failed to connect to the printer";
            case CLS_E_CONNECT_NOTFOUND:
                return "Failed to confirm the model name after connecting to the printer";
            case CLS_E_NOCONTEXT:
                return "No context is specified";
            case CLS_E_BT_DISABLE:
                return "Bluetooth device setting is invalid";
            case CLS_E_BT_NODEVICE:
                return "Bluetooth device is not found";
            case CLS_E_ILLEGAL:
                return "Operation is not supported by the printer or an invalid parameter was used";
            case CLS_E_OFFLINE:
                return "Printer is offline";
            case CLS_E_NOEXIST:
                return "File name does not exist";
            case CLS_E_FAILURE:
                return "Service could not perform the requested procedure";
            case CLS_E_TIMEOUT:
                return "Service has been timed out waiting for a response from the printer";
            case CLS_E_NO_LIST:
                return "Printer could not be found for the printer search";
            case CLS_EPTR_BADFORMAT:
                return "File format is not supported";
            default:
                return "Unknown error code: " + errorCode;
        }
    }
}
