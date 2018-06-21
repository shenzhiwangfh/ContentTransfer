package com.tct.transfer.log;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.tct.transfer.util.DefaultValue;

public class Messenger {
    private static Context mContext;
    private static Handler mHandler;
    private static int MESSAGE_CODE;
    private static int mStatus = DefaultValue.STATUS_INIT;

    public final static int LEVEL0 = 0;
    public final static int LEVEL1 = 1;
    public final static int LEVEL2 = 2;
    public final static int LEVEL3 = 3;

    private static StringBuilder sb = new StringBuilder();
    private static boolean init = false;

    public Messenger() {

    }

    public static void init(Context context, Handler handler, int code) {
        mContext = context;
        mHandler = handler;
        MESSAGE_CODE = code;
        init = true;
    }

    public static void setStatus(int status) {
        mStatus = status;
    }

    public static void clearMessage() {
        if (!init) return;

        sb.delete(0, sb.length());
        log();
    }

    public static void reset() {
        mStatus = DefaultValue.STATUS_INIT;
    }

    public static void sendMessage(int level, int resId, Object... args) {
        if (!init) return;
        if (level < mStatus) return;

        clearMessage();
        sb.append(String.format(mContext.getResources().getString(resId), args));//.append("\n");
        log();
    }

    public static void sendMessage(int level, String msg, Object... args) {
        if (!init) return;
        if (level < mStatus) return;

        clearMessage();
        sb.append(String.format(msg, args));//.append("\n");
        log();
    }

    private static void log() {
        Message message = Message.obtain();
        message.obj = sb.toString();
        message.what = MESSAGE_CODE;
        mHandler.sendMessage(message);
    }
}
