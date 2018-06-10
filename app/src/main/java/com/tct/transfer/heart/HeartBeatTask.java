package com.tct.transfer.heart;


import com.tct.transfer.util.DefaultValue;
import com.tct.transfer.log.LogUtils;
import com.tct.transfer.util.Utils;
import com.tct.transfer.wifi.WifiP2pDeviceInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class HeartBeatTask extends Thread {

    private final static String TAG = "HeartBeatTask";

    private int port;
    private WifiP2pDeviceInfo myDevice;
    //private WifiP2pDeviceInfo customDevice;
    //private int mIndex = 0;

    //private FileBean bean;

    public interface OnSetCustomDevice {
        void onSet(WifiP2pDeviceInfo device);
        //void onResult(boolean result);
    }

    private OnSetCustomDevice mOnSetCustomDevice;

    public void setOnCustomDevice(OnSetCustomDevice onSetCustomDevice) {
        this.mOnSetCustomDevice = onSetCustomDevice;
    }

    public HeartBeatTask(int port, WifiP2pDeviceInfo device/*, FileBean bean*/) {
        this.port = port;
        this.myDevice = device;
        //this.bean = bean;
    }

    @Override
    public void run() {
        if (myDevice.isOwner()) {
            try {
                LogUtils.e(TAG, "group owner,start,port=" + port);
                ServerSocket serverSocket = new ServerSocket(port);
                Socket client = serverSocket.accept();
                //ip = client.getInetAddress().getHostAddress();

                if (client.isConnected()) {
                    //out
                    OutputStream out = client.getOutputStream();
                    writeMyDevice(out, myDevice);

                    //in
                    InputStream in = client.getInputStream();
                    readCustomDevice(in);

                    //transfer file
                    /*
                    if (myDevice.isServer()) {
                        FileTransfer.sendFile(in, out, bean);
                    } else {
                        FileTransfer.recvFile(in, out, bean);
                    }
                    */

                    //out.flush();
                    in.close();
                    out.close();
                }

                LogUtils.e(TAG, "group owner,end");
                client.close();
                serverSocket.close();
            } catch (IOException e) {
                LogUtils.e(TAG, "group owner,e=" + e.toString());
            }
        } else {
            Socket socket = new Socket();
            try {
                String peerIP = myDevice.getPeerIp();
                LogUtils.e(TAG, "group client,start(ip=" + peerIP + ",port=" + port + ")");
                socket.bind(null);
                socket.connect((new InetSocketAddress(peerIP, port)), DefaultValue.SOCKET_CONNECT_TIMEOUT);

                if (socket.isConnected()) {
                    //out
                    OutputStream out = socket.getOutputStream();
                    writeMyDevice(out, myDevice);

                    //in
                    InputStream in = socket.getInputStream();
                    readCustomDevice(in);

                    //transfer file
                    /*
                    if (myDevice.isServer()) {
                        FileTransfer.sendFile(in, out, bean);
                    } else {
                        FileTransfer.recvFile(in, out, bean);
                    }
                    */

                    //out.flush();
                    in.close();
                    out.close();
                }

                LogUtils.e(TAG, "group client,end");
            } catch (IOException e) {
                LogUtils.e(TAG, "group client,e=" + e.toString());
            } finally {
                if (socket.isConnected()) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // Give up
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void writeMyDevice(OutputStream out, WifiP2pDeviceInfo myDevice) {
        try {
            byte myDeviceBytes[] = Utils.objectToByteArray(myDevice);
            int totalLen = myDeviceBytes.length;
            byte outBytes[] = Utils.byteMergerAll(Utils.int2byte(totalLen), myDeviceBytes);
            out.write(outBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readCustomDevice(InputStream in) {
        try {
            byte len[] = new byte[4];
            in.read(len, 0, 4);
            int totalLen = Utils.byte2int(len, 0);
            byte customDeviceBytes[] = new byte[totalLen];
            in.read(customDeviceBytes, 0, totalLen);
            WifiP2pDeviceInfo customDevice = (WifiP2pDeviceInfo) Utils.byteArrayToObject(customDeviceBytes);
            //customDevice.setIp(ip);

            if (mOnSetCustomDevice != null) {
                mOnSetCustomDevice.onSet(customDevice);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
