package com.tct.transfer.heart;

import android.content.ContentResolver;
import android.util.Log;

import com.tct.transfer.DefaultValue;
import com.tct.transfer.TransferActivity;
import com.tct.transfer.file.FileBean;
import com.tct.transfer.file.FileTransfer;
import com.tct.transfer.file.FileUtil;
import com.tct.transfer.util.Utils;
import com.tct.transfer.wifi.WifiP2pDeviceInfo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class HeartBeatTask extends Thread {

    private boolean owner;
    private boolean server;
    private String ip;
    private int port;
    private WifiP2pDeviceInfo myDevice;
    //private WifiP2pDeviceInfo customDevice;
    //private int mIndex = 0;

    private FileBean bean;

    public interface OnSetCustomDevice {
        void onSet(WifiP2pDeviceInfo device);
        //void onResult(boolean result);
    }

    private OnSetCustomDevice mOnSetCustomDevice;

    public void setOnCustomDevice(OnSetCustomDevice onSetCustomDevice) {
        this.mOnSetCustomDevice = onSetCustomDevice;
    }

    public HeartBeatTask(boolean owner, boolean server, String ip, int port, WifiP2pDeviceInfo device, FileBean bean) {
        this.owner = owner;
        this.server = server;
        this.ip = ip;
        this.port = port;
        this.myDevice = device;
        this.bean = bean;
    }

    @Override
    public void run() {
        if (owner) {
            try {
                Log.e(DefaultValue.TAG, "HeartBeatTask,owner,start,port=" + port);
                ServerSocket serverSocket = new ServerSocket(port);
                Socket client = serverSocket.accept();
                ip = client.getInetAddress().getHostAddress();

                if (client.isConnected()) {
                    //out
                    OutputStream out = client.getOutputStream();
                    writeMyDevice(out, myDevice);

                    //in
                    InputStream in = client.getInputStream();
                    readCustomDevice(in);

                    if(server) {
                        FileTransfer.sendFile(in, out, bean);
                    } else {
                        FileTransfer.recvFile(in, out, bean);
                    }



                    //out.flush();
                    in.close();
                    out.close();
                }

                Log.e(DefaultValue.TAG, "HeartBeatTask,owner,end," + server);
                client.close();
                serverSocket.close();
            } catch (IOException e) {
                Log.e(DefaultValue.TAG, "owner,e=" + e.toString());
            }
        } else {
            Socket socket = new Socket();
            try {
                Log.e(DefaultValue.TAG, "HeartBeatTask,client,start(ip=" + ip + ",port=" + port + ")");
                socket.bind(null);
                socket.connect((new InetSocketAddress(ip, port)), DefaultValue.SOCKET_CONNECT_TIMEOUT);

                if (socket.isConnected()) {
                    //out
                    OutputStream out = socket.getOutputStream();
                    writeMyDevice(out, myDevice);

                    //in
                    InputStream in = socket.getInputStream();
                    readCustomDevice(in);

                    if(server) {
                        FileTransfer.sendFile(in, out, bean);
                    } else {
                        FileTransfer.recvFile(in, out, bean);
                    }

                    //out.flush();
                    in.close();
                    out.close();
                }

                Log.e(DefaultValue.TAG, "HeartBeatTask,client,end," + server);
            } catch (IOException e) {
                Log.e(DefaultValue.TAG, "client,e=" + e.toString());
            } finally {
                if (socket != null) {
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
            customDevice.setIp(ip);

            if(mOnSetCustomDevice != null) {
                mOnSetCustomDevice.onSet(customDevice);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
