package com.tct.transfer.util;

import android.os.Environment;

public class DefaultValue {

    //public final static String TAG = "ContentTransfer";
    public final static int QR_CODE_LEN = 400;

    public final static int REQUEST_CODE_QUERY_FILE = 0;
    public final static int REQUEST_CODE_SCAN = 1;
    public final static int REQUEST_CODE_PERMISSION = 2;

    public final static int MESSAGE_WIFI_STATUS_CHANGED = 1;
    public final static int MESSAGE_WIFI_DISCOVER = 2;
    public final static int MESSAGE_REGISTER = 3;
    public final static int MESSAGE_TRANSFER_START = 4;
    public final static int MESSAGE_TRANSFER_STATUS = 5;
    public final static int MESSAGE_SET_CUSTOM_DEVICE = 6;
    public final static int MESSAGE_LOG = 100;

    public final static int PORT_HEART_BEAT = 52870;
    public final static int PORT_TRANSFER = 52872;
    public final static int SOCKET_CONNECT_TIMEOUT = 5000;

    public static String recvPath() {
        return Environment.getExternalStorageDirectory() + "/" + "TctRecvPicture";
    }

    public final static int TRANSFER_START = 0;
    public final static int TRANSFER_END = 1;
    public final static int TRANSFER_ERROR = 100;
}
