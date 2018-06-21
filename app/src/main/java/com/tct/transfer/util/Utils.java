package com.tct.transfer.util;

import android.content.Context;

import com.tct.transfer.R;
import com.tct.transfer.log.LogUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Utils {

    public static byte[] string2byte(String str) {
        if (str == null) {
            return null;
        }
        byte[] byteArray = str.getBytes();
        return byteArray;
    }

    public static String byte2string(byte[] byteArray) {
        if (byteArray == null) {
            return null;
        }
        String str = new String(byteArray);
        return str;
    }

    public static int byte2int(byte[] byteArray, int offset) {
        if (byteArray.length != 4)
            return 0;
        return byteArray[offset + 3] & 0xFF
                | (byteArray[offset + 2] & 0xFF) << 8
                | (byteArray[offset + 1] & 0xFF) << 16
                | (byteArray[offset] & 0xFF) << 24;
    }

    public static byte[] int2byte(int a) {
        return new byte[]{
                (byte) ((a >> 24) & 0xFF),
                (byte) ((a >> 16) & 0xFF),
                (byte) ((a >> 8) & 0xFF),
                (byte) (a & 0xFF)
        };
    }

    /**
     * 对象转Byte数组
     *
     * @param obj
     * @return
     */
    public static byte[] objectToByteArray(Object obj) {
        byte[] bytes = null;
        ByteArrayOutputStream byteArrayOutputStream = null;
        ObjectOutputStream objectOutputStream = null;
        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(obj);
            objectOutputStream.flush();
            bytes = byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
        } finally {
            if (objectOutputStream != null) {
                try {
                    objectOutputStream.close();
                } catch (IOException e) {
                }
            }
            if (byteArrayOutputStream != null) {
                try {
                    byteArrayOutputStream.close();
                } catch (IOException e) {
                }
            }

        }
        return bytes;
    }

    /**
     * Byte数组转对象
     *
     * @param bytes
     * @return
     */
    public static Object byteArrayToObject(byte[] bytes) {
        Object obj = null;
        ByteArrayInputStream byteArrayInputStream = null;
        ObjectInputStream objectInputStream = null;
        try {
            byteArrayInputStream = new ByteArrayInputStream(bytes);
            objectInputStream = new ObjectInputStream(byteArrayInputStream);
            obj = objectInputStream.readObject();
        } catch (Exception e) {
        } finally {
            if (byteArrayInputStream != null) {
                try {
                    byteArrayInputStream.close();
                } catch (IOException e) {
                }
            }
            if (objectInputStream != null) {
                try {
                    objectInputStream.close();
                } catch (IOException e) {
                }
            }
        }
        return obj;
    }

    public static byte[] byteMergerAll(byte[]... values) {
        int length = 0;
        for (int i = 0; i < values.length; i++) {
            length += values[i].length;
        }
        byte[] allByte = new byte[length];
        int countLength = 0;
        for (int i = 0; i < values.length; i++) {
            byte[] b = values[i];
            System.arraycopy(b, 0, allByte, countLength, b.length);
            countLength += b.length;
        }
        return allByte;
    }

    public static String long2time(Context context, long elapsed) {
        if(elapsed < 1000) elapsed = 1000;

        int hour = (int) (elapsed / 1000 / 60 / 60);
        int minute = (int)((elapsed / 1000 / 60) % 60);
        int second = (int)((elapsed / 1000) % (60 * 60));

        StringBuilder formatSB = new StringBuilder();
        if(hour != 0) {
            formatSB.append(String.format("%02d", hour)).append(context.getString(R.string.hour));
        }
        if(hour != 0 || minute != 0) {
            formatSB.append(String.format("%02d", minute)).append(context.getString(R.string.minute));
        }
        //if(second != 0) {
            formatSB.append(String.format("%02d", second)).append(context.getString(R.string.second));
        //}

        return formatSB.toString();
    }
}
