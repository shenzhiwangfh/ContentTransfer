package com.tct.transfer.heart;

import android.content.ContentResolver;
import android.util.Log;

import com.tct.transfer.DefaultValue;
import com.tct.transfer.TransferActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class HeartBeatTask extends Thread {

    private boolean server;
    private String ip;
    private int port;

    private int mIndex = 0;

    public interface OnClientIp {
        void onSet(String ip);
    }

    private OnClientIp mOnClientIp;

    public void setOnClientIp(OnClientIp onClientIp) {
        this.mOnClientIp = onClientIp;
    }

    public HeartBeatTask(boolean server, String ip, int port) {
        this.ip = ip;
        this.port = port;
        this.server = server;
    }

    @Override
    public void run() {

        Log.e(DefaultValue.TAG, "HeartBeatTask,run");

        if (server) {
            try {
                ServerSocket serverSocket = new ServerSocket(port);
                Socket client = serverSocket.accept();

                Log.e(DefaultValue.TAG, "HeartBeatTask,run,accept");

                String clientIp = client.getInetAddress().toString();
                //if (clientIp.startsWith("/"))
                //    clientIp.substring(1);
                if(mOnClientIp != null)
                    mOnClientIp.onSet(clientIp);

                Log.e(DefaultValue.TAG, "HeartBeatTask,run,clientIp=" + clientIp);

                InputStream in = client.getInputStream();
                while (client.isConnected()) {
                    readStream(in, mIndex);
                    mIndex++;

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    Log.e(DefaultValue.TAG, "server,mindex=" + mIndex);
                }

                serverSocket.close();
            } catch (IOException e) {
                Log.e(DefaultValue.TAG, e.toString());
            }
        } else {

            /*
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            */

            Socket socket = new Socket();
            try {
                Log.e(DefaultValue.TAG, "client111");

                socket.bind(null);
                socket.connect((new InetSocketAddress(ip, port)), DefaultValue.SOCKET_CONNECT_TIMEOUT);

                Log.e(DefaultValue.TAG, "client,connect=" + socket.isConnected());

                OutputStream out = socket.getOutputStream();
                while (socket.isConnected()) {
                    out.write(int2ByteArray(mIndex));
                    out.flush();
                    mIndex++;

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    Log.e(DefaultValue.TAG, "client,mindex=" + mIndex);
                }
            } catch (IOException e) {
                Log.e(DefaultValue.TAG, "client111,e=" + e);
            } finally {

                Log.e(DefaultValue.TAG, "client111,finally");

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

    public boolean readStream(InputStream inputStream, int index) {
        byte buf[] = new byte[4];
        try {
            inputStream.read(buf);
            int indexNow = byteArray2Int(buf);
            inputStream.close();
            return ((indexNow - index) == 1);
        } catch (IOException e) {
            return false;
        }
    }

    public static int byteArray2Int(byte[] b) {
        if(b.length != 4)
            return 0;
        return b[3] & 0xFF | (b[2] & 0xFF) << 8 | (b[1] & 0xFF) << 16 | (b[0] & 0xFF) << 24;
    }

    public static byte[] int2ByteArray(int a) {
        return new byte[]{
                (byte) ((a >> 24) & 0xFF),
                (byte) ((a >> 16) & 0xFF),
                (byte) ((a >> 8) & 0xFF),
                (byte) (a & 0xFF)
        };
    }
}
