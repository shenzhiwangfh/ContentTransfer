package com.tct.transfer.heartbeat;

import com.tct.transfer.util.Utils;
import com.tct.transfer.wifi.WifiP2pDeviceInfo;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class ReadThread extends Thread {

    private Socket socket;
    private HeartBeatTask.OnSetCustomDevice listener;

    private OnFinishListener mOnFinishListener;

    public interface OnFinishListener {
        void finish();
    }

    public ReadThread(Socket socket, HeartBeatTask.OnSetCustomDevice listener, OnFinishListener onFinishListener) {
        this.socket = socket;
        this.listener = listener;
        this.mOnFinishListener = onFinishListener;
    }

    @Override
    public void run() {
        try {
            InputStream in = socket.getInputStream();
            byte buf[] = new byte[4];
            in.read(buf);
            int totalLen = Utils.byte2int(buf, 0);

            int readLen = 0;
            int len;
            byte customDeviceBytes[] = new byte[totalLen];
            while ((len = in.read(customDeviceBytes)) != -1) {
                readLen += len;
            }

            WifiP2pDeviceInfo customDevice = (WifiP2pDeviceInfo) Utils.byteArrayToObject(customDeviceBytes);
            if (listener != null) {
                //listener.onResult(readLen == totalLen);
                String customIp = socket.getInetAddress().toString();
                //customDevice.setIp(customIp);
                listener.onSet(customDevice);
            }

            in.close();
            //LogUtils.e(TAG, "ReadThread,in,customDevice=" + customDevice);
        } catch (IOException e) {
            //LogUtils.e(TAG, "ReadThread,e=" + e.toString());
        }

        mOnFinishListener.finish();
    }
}
