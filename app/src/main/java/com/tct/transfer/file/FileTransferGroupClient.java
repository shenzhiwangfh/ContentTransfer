package com.tct.transfer.file;

import android.content.Context;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.tct.transfer.DefaultValue;
import com.tct.transfer.log.LogUtils;
import com.tct.transfer.log.Messenger;
import com.tct.transfer.wifi.WifiP2pDeviceInfo;

public class FileTransferGroupClient extends Thread {

    private final static String TAG = "FileTransferGroupClient";

    private Context context;
    //private String ip;
    private int port;
    private WifiP2pDeviceInfo myDevice;
    private FileBean bean;

    public FileTransferGroupClient(Context context, /*String ip,*/ int port, WifiP2pDeviceInfo myDevice, FileBean bean) {
        this.context = context;
        //this.ip = ip;
        this.port = port;
        this.myDevice = myDevice;
        this.bean = bean;
    }

    public interface TransferStatue {
        void sendStatue(int status, String file);
    }

    private TransferStatue mTransferStatue;

    public void setTransferStatue(TransferStatue transferStatue) {
        mTransferStatue = transferStatue;
    }

    @Override
    public void run() {
        Socket socket = new Socket();
        try {
            LogUtils.e(TAG, "FileTransferGroupClient,begin,(ip=" + myDevice.getPeerIp() + ",port=" + port + ")");
            socket.bind(null);
            socket.connect((new InetSocketAddress(myDevice.getPeerIp(), port)), /*DefaultValue.SOCKET_CONNECT_TIMEOUT*/60000);

            if (mTransferStatue != null)
                mTransferStatue.sendStatue(DefaultValue.TRANSFER_START, bean.name);

            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            if(myDevice.isServer()) {
                Messenger.sendMessage("client,server,send");
                FileTransfer.sendFile(in, out, bean);
            } else {
                Messenger.sendMessage("client,client,recv");
                FileTransfer.recvFile(in, out, bean);
            }

            Messenger.sendMessage("FileTransferGroupClient,end transfer");
            LogUtils.e(TAG, "FileTransferGroupClient,end transfer");
            if (mTransferStatue != null)
                mTransferStatue.sendStatue(DefaultValue.TRANSFER_END, bean.path);

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
