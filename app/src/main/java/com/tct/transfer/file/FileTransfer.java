package com.tct.transfer.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.tct.transfer.util.DefaultValue;
import com.tct.transfer.log.LogUtils;
import com.tct.transfer.log.Messenger;
import com.tct.transfer.util.FileSizeUtil;
import com.tct.transfer.util.FileUtil;
import com.tct.transfer.util.Utils;

public class FileTransfer {
    
    private final static String TAG = "FileTransfer";

    public static void recvFile(InputStream in, OutputStream out, FileBean bean, TransferStatus listener) {

        try {
            bean.action = 1; //download
            bean.status = 0; //start
            //bean.time = System.currentTimeMillis();
            if(listener != null) listener.sendStatus(bean);

            bean.status = 1; //ing

            //recv bean info
            byte len[] = new byte[4];
            in.read(len, 0, 4);
            int bodyLen = Utils.byte2int(len, 0);
            byte beanBytes[] = new byte[bodyLen];
            in.read(beanBytes, 0, bodyLen);
            bean = (FileBean) Utils.byteArrayToObject(beanBytes);
            bean.action = 1;

            bean.name = bean.path.substring(bean.path.lastIndexOf("/") + 1);
            bean.path = DefaultValue.recvPath() + "/" + bean.name;
            LogUtils.e(TAG, "client,bean=" + bean.toString());

            //send ready ok
            writeStatus(out, DefaultValue.NETWORK_STATUS_OK);
            LogUtils.e(TAG, "client,ready ok");

            //recv file start
            File file = new File(bean.path);
            File dirs = new File(DefaultValue.recvPath());

            if (!dirs.exists())
                dirs.mkdirs();
            file.createNewFile();

            OutputStream os = new FileOutputStream(file);
            copyFile(in, os, bean, listener);
            //os.close();

            //judge md5
            String md5 = FileUtil.getFileMD5(file);
            if(md5 != null && md5.equals(bean.md5)) {
                bean.result = 0; //succeed
            } else {
                bean.result = 1; //failed
            }

            //send result
            String result = (bean.result == 0) ? DefaultValue.NETWORK_STATUS_OK : DefaultValue.NETWORK_STATUS_ERROR;
            writeStatus(out, result);
            //Messenger.sendMessage(result);

            bean.status = 2; //end
            if(listener != null) listener.sendStatus(bean);
        } catch (IOException e) {
            bean.result = 1;
        }
    }

    public static void sendFile(InputStream in, OutputStream out, FileBean bean, TransferStatus listener) {

        try {
            //send bean info
            File sendFile = new File(bean.path);
            bean.name = sendFile.getName();
            bean.md5 = FileUtil.getFileMD5(sendFile);
            bean.size = (long) FileSizeUtil.getFileOrFilesSize(bean.path, FileSizeUtil.SIZETYPE_B);
            bean.action = 0; //upload
            bean.status = 0; //start
            //bean.time = System.currentTimeMillis();
            if(listener != null) listener.sendStatus(bean);

            bean.status = 1;

            byte beanBytes[] = Utils.objectToByteArray(bean);
            int bodyLen = beanBytes.length;
            byte outBytes[] = Utils.byteMergerAll(Utils.int2byte(bodyLen), beanBytes);
            out.write(outBytes);
            LogUtils.e(TAG, "server,bean=" + bean.toString());

            //recv is ready
            boolean ready = isOK(readStatus(in));
            LogUtils.e(TAG, "server,ready=" + ready);

            //send file start
            if (ready) {
                InputStream is = new FileInputStream(new File(bean.path));
                copyFile(is, out, bean, listener);
                //is.close();
            }

            String result = readStatus(in);
            bean.result = isOK(result) ? 0 : 1;
            //Messenger.sendMessage(result);

            bean.status = 2; //end
            if(listener != null) listener.sendStatus(bean);
        } catch (IOException e) {
            bean.result = 1;
        }
    }

    private static boolean copyFile(InputStream in, OutputStream out, FileBean bean, TransferStatus listener) {
        byte buf[] = new byte[1024];
        int len;

        bean.transferSize = 0;
        bean.time = System.currentTimeMillis();

        try {
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
                bean.transferSize += len;
                bean.elapsed = System.currentTimeMillis() - bean.time;
                if (listener != null) listener.sendStatus(bean);

                if(bean.transferSize == bean.size) {
                    break;
                }
            }
            //out.close();
            //in.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private static String readStatus(InputStream in) {
        try {
            byte len[] = new byte[4];
            in.read(len, 0, 4);
            byte statusBytes[] = new byte[Utils.byte2int(len, 0)];
            in.read(statusBytes, 0, Utils.byte2int(len, 0));
            return Utils.byte2string(statusBytes);
        } catch (IOException e) {
            return null;
        }
    }

    private static void writeStatus(OutputStream out, String status) {
        try {
            byte statusBytes[] = Utils.string2byte(status);
            byte outBytes[] = Utils.byteMergerAll(Utils.int2byte(statusBytes.length), statusBytes);
            out.write(outBytes);
        } catch (IOException e) {

        }
    }

    private static boolean isOK(String status) {
        if (status == null || status.isEmpty())
            return false;

        if (status.equals(DefaultValue.NETWORK_STATUS_OK)) {
            return true;
        } else if (status.equals(DefaultValue.NETWORK_STATUS_ERROR)) {
            return false;
        } else {
            return false;
        }
    }
}
