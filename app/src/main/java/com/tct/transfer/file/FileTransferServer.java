package com.tct.transfer.file;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import com.tct.transfer.DefaultValue;
import com.tct.transfer.util.Utils;

public class FileTransferServer extends Thread {

    private Context context;
    private int port;
    private FileBean bean;

    public FileTransferServer(Context context, int port, FileBean bean) {
        this.context = context;
        this.port = port;
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
            Log.e(DefaultValue.TAG, "FileTransferServer,begin,port=" + port);
            bean.md5 = FileUtil.getFileMD5(new File(bean.path));

            ServerSocket serverSocket = new ServerSocket(port);
            Socket client = serverSocket.accept();

            if (mTransferStatue != null)
                mTransferStatue.sendStatue(DefaultValue.TRANSFER_START);

            int bodyLen = 0;

            OutputStream out = client.getOutputStream();
            byte beanBytes[] = Utils.objectToByteArray(bean);
            bodyLen = beanBytes.length;
            byte outBytes[] = Utils.byteMergerAll(Utils.int2byte(bodyLen), beanBytes);
            out.write(outBytes);
            Log.e(DefaultValue.TAG, "server," + bean.toString());

            //status:tct ok/error
            InputStream in = client.getInputStream();
            byte len[] = new byte[4];
            in.read(len, 0, 4);
            bodyLen = Utils.byte2int(len, 0);
            byte status[] = new byte[bodyLen];
            in.read(status, 0, bodyLen);
            boolean ready = isReady(Utils.byte2string(status));

            Log.e(DefaultValue.TAG, "server,ready=" + ready);
            Log.e(DefaultValue.TAG, "server,path uri=" + Uri.parse(bean.path).toString());

            if (ready) {
                OutputStream stream = client.getOutputStream();
                //ContentResolver cr = context.getContentResolver();
                InputStream is = new FileInputStream(new File(bean.path));
                //try {
                //    is = cr.openInputStream(Uri.parse(bean.path));
                //} catch (FileNotFoundException e) {
                //}

                FileUtil.copyFile(is, stream);
            }

            Log.e(DefaultValue.TAG, "FileTransferServer,end transfer");
            if (mTransferStatue != null)
                mTransferStatue.sendStatue(DefaultValue.TRANSFER_END);

            out.close();
            client.close();
            serverSocket.close();
        } catch (IOException e) {
            Log.e(DefaultValue.TAG, "FileTransferServer,transfer error=" + e);
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
