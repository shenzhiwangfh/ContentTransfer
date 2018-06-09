package com.tct.transfer.file;

import android.content.Context;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import com.tct.transfer.DefaultValue;
import com.tct.transfer.log.LogUtils;
import com.tct.transfer.log.Messenger;
import com.tct.transfer.wifi.WifiP2pDeviceInfo;

public class FileTransferGroupOwner extends Thread {

    private final static String TAG = "FileTransferGroupOwner";

    private Context context;
    private int port;
    private WifiP2pDeviceInfo myDevice;
    private FileBean bean;

    public FileTransferGroupOwner(Context context, int port, WifiP2pDeviceInfo myDevice, FileBean bean) {
        this.context = context;
        this.port = port;
        this.myDevice = myDevice;
        this.bean = bean;
    }

    public interface TransferStatue {
        void sendStatue(int status);
    }

    private TransferStatue mTransferStatue;

    public void setTransferStatue(TransferStatue transferStatue) {
        mTransferStatue = transferStatue;
    }

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            Socket client = serverSocket.accept();

            if (mTransferStatue != null)
                mTransferStatue.sendStatue(DefaultValue.TRANSFER_START);

            LogUtils.e(TAG, "FileTransferGroupOwner,begin,port=" + port);
            //bean.md5 = FileUtil.getFileMD5(new File(bean.path));

            InputStream in = client.getInputStream();
            OutputStream out = client.getOutputStream();
            if(myDevice.isServer()) {
                Messenger.sendMessage("owner,server,send");
                FileTransfer.sendFile(in, out, bean);
            } else {
                Messenger.sendMessage("owner,client,recv");
                FileTransfer.recvFile(in, out, bean);
            }
            
            Messenger.sendMessage("FileTransferGroupOwner, end transfer");
            LogUtils.e(TAG, "FileTransferGroupOwner,end transfer");
            if (mTransferStatue != null)
                mTransferStatue.sendStatue(DefaultValue.TRANSFER_END);

            out.close();
            client.close();
            serverSocket.close();
        } catch (IOException e) {
            LogUtils.e(TAG, "FileTransferGroupOwner,transfer error=" + e);
            if (mTransferStatue != null)
                mTransferStatue.sendStatue(DefaultValue.TRANSFER_ERROR);
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
