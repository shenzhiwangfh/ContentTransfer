package com.tct.transfer.log;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

public class Messenger {
    private static Context mContext;
    private static Handler mHandler;
    private static int MESSAGE_CODE;

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

    public static void clearMessage() {
        if (!init) return;

        sb.delete(0, sb.length());
        log();
    }

    public static void sendMessage(int resId, Object... args) {
        if (!init) return;

        clearMessage();
        sb.append(String.format(mContext.getResources().getString(resId), args));//.append("\n");
        log();
    }

    public static void sendMessage(String msg, Object... args) {
        if (!init) return;

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
