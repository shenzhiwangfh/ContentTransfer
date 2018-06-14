package com.tct.transfer.file;

import android.content.Context;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.tct.transfer.log.LogUtils;
import com.tct.transfer.log.Messenger;
import com.tct.transfer.util.DefaultValue;
import com.tct.transfer.wifi.WifiP2pDeviceInfo;

public class FileTransferGroupClient extends Thread {

    private final static String TAG = "FileTransferGroupClient";

    private Context context;
    private int port;
    private WifiP2pDeviceInfo myDevice;
    private FileBean bean;
    private TransferStatus listener;

    public FileTransferGroupClient(Context context, int port, WifiP2pDeviceInfo myDevice, String path, TransferStatus listener) {
        this.context = context;
        this.port = port;
        this.myDevice = myDevice;
        this.bean = new FileBean();
        this.bean.path = path;
        this.listener = listener;
    }

    @Override
    public void run() {
        Socket socket = new Socket();
        try {
            //Messenger.sendMessage("FileTransferGroupClient,begin,(ip=" + myDevice.getPeerIp() + ",port=" + port + ")");
            //if (listener != null)
            //    listener.sendStatus(DefaultValue.TRANSFER_START, bean);

            socket.bind(null);
            socket.connect((new InetSocketAddress(myDevice.getPeerIp(), port)), DefaultValue.SOCKET_CONNECT_TIMEOUT);

            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            if(myDevice.isServer()) {
                //Messenger.sendMessage("client,server,send");
                FileTransfer.sendFile(in, out, bean, listener);
            } else {
                //Messenger.sendMessage("client,client,recv");
                FileTransfer.recvFile(in, out, bean, listener);
            }

            //Messenger.sendMessage("FileTransferGroupClient,end transfer");
            //if (listener != null)
            //    listener.sendStatus(DefaultValue.TRANSFER_END, bean);

            byte end[] = new byte[1];
            in.read(end, 0, 1);

            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            LogUtils.e(TAG, "FileTransferGroupClient,e=" + e.toString());
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
