package com.tct.transfer.util;

import android.net.Uri;
import android.os.Environment;

import com.tct.transfer.database.FileBeanHelper;
import com.tct.transfer.database.FileBeanProvider;

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

    public final static Uri uri = Uri.parse("content://" + FileBeanProvider.AUTHORITY + "/" + FileBeanHelper.TABLE_NAME);

    //0:image, 1:video, 2:text, 3:audio, 4:other
    public final static int TYPE_IMAGE = 0;
    public final static int TYPE_VIDEO = 1;
    public final static int TYPE_TEXT = 2;
    public final static int TYPE_AUDIO = 3;
    public final static int TYPE_OTHER = 4;

    public final static String NETWORK_STATUS_OK = "TCT_OK";
    public final static String NETWORK_STATUS_ERROR = "TCT_ERROR";

    public final static int STATUS_INIT = 0;
    public final static int STATUS_PEER = 1;
    public final static int STATUS_CONNECTED = 2;
    public final static int STATUS_COMPLETE = 3;
}
