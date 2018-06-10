package com.tct.transfer.file;

import android.content.Context;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import com.tct.transfer.log.LogUtils;
import com.tct.transfer.log.Messenger;
import com.tct.transfer.wifi.WifiP2pDeviceInfo;

public class FileTransferGroupOwner extends Thread {

    private final static String TAG = "FileTransferGroupOwner";

    private Context context;
    private int port;
    private WifiP2pDeviceInfo myDevice;
    private FileBean bean;
    private TransferStatus listener;

    public FileTransferGroupOwner(Context context, int port, WifiP2pDeviceInfo myDevice, FileBean bean, TransferStatus listener) {
        this.context = context;
        this.port = port;
        this.myDevice = myDevice;
        this.bean = bean;
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            Socket client = serverSocket.accept();

            //if (listener != null)
            //    listener.sendStatus(DefaultValue.TRANSFER_START, bean);

            LogUtils.e(TAG, "FileTransferGroupOwner,begin,port=" + port);
            //bean.md5 = FileUtil.getFileMD5(new File(bean.path));

            InputStream in = client.getInputStream();
            OutputStream out = client.getOutputStream();
            if(myDevice.isServer()) {
                Messenger.sendMessage("owner,server,send");
                FileTransfer.sendFile(in, out, bean, listener);
            } else {
                Messenger.sendMessage("owner,client,recv");
                FileTransfer.recvFile(in, out, bean, listener);
            }
            
            Messenger.sendMessage("FileTransferGroupOwner, end transfer");
            LogUtils.e(TAG, "FileTransferGroupOwner,end transfer");
            //if (listener != null)
            //    listener.sendStatus(DefaultValue.TRANSFER_END, bean);

            in.close();
            out.close();
            client.close();
            serverSocket.close();
        } catch (IOException e) {
            LogUtils.e(TAG, "FileTransferGroupOwner,transfer error=" + e);
            //if (listener != null)
            //    listener.sendStatus(DefaultValue.TRANSFER_ERROR, bean);
        }
    }

    private boolean isReady(String status) {
        if (status == null || status.isEmpty())
            return false;

        if (status.equals(FileUtil.STATUS_OK)) {
            return true;
        } else if (status.equals(FileUtil.STATUS_ERROR)) {
            return false;
        } else {
            return false;
        }
    }

}
