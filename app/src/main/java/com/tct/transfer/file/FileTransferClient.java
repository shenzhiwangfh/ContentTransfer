package com.tct.transfer.file;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

import com.tct.transfer.DefaultValue;
import com.tct.transfer.util.Utils;

public class FileTransferClient extends Thread {

    private Context context;
    private String ip;
    private int port;
    private FileBean bean;

    public FileTransferClient(Context context, String ip, int port) {
        this.context = context;
        this.ip = ip;
        this.port = port;
        //this.bean = bean;
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
            Log.e(DefaultValue.TAG, "FileTransferClient,begin,(ip=" + ip + ",port=" + port + ")");
            Log.e(DefaultValue.TAG, "FileTransferClient,start transfer11");

            socket.bind(null);

            Log.e(DefaultValue.TAG, "FileTransferClient,start transfer22,timeout=" );
            socket.connect((new InetSocketAddress(ip, port)), /*DefaultValue.SOCKET_CONNECT_TIMEOUT*/60000);

            Log.e(DefaultValue.TAG, "FileTransferClient,start transfer33");

            if (mTransferStatue != null)
                mTransferStatue.sendStatue(DefaultValue.TRANSFER_START, bean.name);

            int bodyLen = 0;

            InputStream in = socket.getInputStream();
            byte len[] = new byte[4];
            in.read(len, 0, 4);
            bodyLen = Utils.byte2int(len, 0);
            byte beanBytes[] = new byte[bodyLen];
            in.read(beanBytes, 0, bodyLen);
            bean = (FileBean) Utils.byteArrayToObject(beanBytes);

            Log.e(DefaultValue.TAG, "client=" + bean.toString());

            OutputStream out = socket.getOutputStream();
            byte status[] = Utils.string2byte(FileUtil.STATUS_OK);
            bodyLen = status.length;
            byte outBytes[] = Utils.byteMergerAll(Utils.int2byte(bodyLen), status);
            out.write(outBytes);

            Log.e(DefaultValue.TAG, "client,ready ok");

            bean.name = bean.path.substring(bean.path.lastIndexOf("/") + 1);
            bean.path = DefaultValue.recvPath() + "/" + bean.name;

            File file = new File(bean.path);
            File dirs = new File(DefaultValue.recvPath());

            if (!dirs.exists())
                dirs.mkdirs();
            file.createNewFile();

            InputStream inputstream = socket.getInputStream();
            FileUtil.copyFile(inputstream, new FileOutputStream(file));

            Log.e(DefaultValue.TAG, "FileTransferClient,end transfer");
            if (mTransferStatue != null)
                mTransferStatue.sendStatue(DefaultValue.TRANSFER_END, bean.path);

            socket.close();
        } catch (IOException e) {
            Log.e(DefaultValue.TAG, "FileTransferClient,e=" + e.toString());
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
