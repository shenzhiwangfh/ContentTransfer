package com.tct.transfer.util;

public class ThreadUtil {

    public static Object lock = new Object();
    private static boolean mShow = false;
    private static boolean mLoop = true;

    public static void start() {
        mLoop = true;

        new Thread() {
            @Override
            public void run() {
                while (mLoop) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    synchronized (lock) {
                        mShow = true;
                    }
                }
            }
        }.start();
    }

    public static boolean isShow() {
        synchronized (lock) {
            return mShow;
        }
    }

    public static void reset() {
        synchronized (lock) {
            mShow = false;
        }
    }

    public static void end() {
        mLoop = false;
    }
}
