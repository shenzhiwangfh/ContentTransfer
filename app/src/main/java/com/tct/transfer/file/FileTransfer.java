package com.tct.transfer.file;

import android.net.Uri;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import com.tct.transfer.DefaultValue;
import com.tct.transfer.util.Utils;

public class FileTransfer {

    public static void recvFile(InputStream in, OutputStream out, FileBean bean) {

        try {
            int bodyLen = 0;

            //InputStream in = socket.getInputStream();
            byte len[] = new byte[4];
            in.read(len, 0, 4);
            bodyLen = Utils.byte2int(len, 0);
            byte beanBytes[] = new byte[bodyLen];
            in.read(beanBytes, 0, bodyLen);
            bean = (FileBean) Utils.byteArrayToObject(beanBytes);

            Log.e(DefaultValue.TAG, "client=" + bean.toString());

            //OutputStream out = socket.getOutputStream();
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

            //InputStream inputstream = socket.getInputStream();
            FileUtil.copyFile(in, new FileOutputStream(file));
        } catch (IOException e) {

        }
    }

    public static void sendFile(InputStream in, OutputStream out, FileBean bean) {

        try {
        int bodyLen = 0;

        //OutputStream out = client.getOutputStream();
        byte beanBytes[] = Utils.objectToByteArray(bean);
        bodyLen = beanBytes.length;
        byte outBytes[] = Utils.byteMergerAll(Utils.int2byte(bodyLen), beanBytes);
        out.write(outBytes);
        Log.e(DefaultValue.TAG, "server," + bean.toString());

        //status:tct ok/error
        //InputStream in = client.getInputStream();
        byte len[] = new byte[4];
        in.read(len, 0, 4);
        bodyLen = Utils.byte2int(len, 0);
        byte status[] = new byte[bodyLen];
        in.read(status, 0, bodyLen);
        boolean ready = isReady(Utils.byte2string(status));

        Log.e(DefaultValue.TAG, "server,ready=" + ready);
        Log.e(DefaultValue.TAG, "server,path uri=" + Uri.parse(bean.path).toString());

        if (ready) {
            InputStream is = new FileInputStream(new File(bean.path));
            FileUtil.copyFile(is, out);
        }
        } catch (IOException e) {

        }
    }

    private static boolean isReady(String status) {
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
