package com.tct.transfer.queue;

import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pManager;
import com.tct.transfer.log.LogUtils;

public class WifiP2pQueueManager implements WifiP2pManager.ActionListener {
    private final static String TAG = "WifiP2pQueueManager";

    private static WifiP2pQueueManager instance;

    private static WifiP2pManager p2pManager;
    private static WifiP2pManager.Channel p2pManagerChannel;
    private WifiP2pConfig config;

    private static WifiP2pMessageQueue queue;
    private OnFinishListener listener;

    @Override
    public void onSuccess() {
        WifiP2pMessage msg = queue.pickMessage();
        LogUtils.e(TAG, msg.action.getName() + ",onSuccess");

        if (msg.listener != null) msg.listener.onSuccess();
        queue.removeMessage();
        next();
    }

    @Override
    public void onFailure(int reason) {
        WifiP2pMessage msg = queue.pickMessage();
        LogUtils.e(TAG, msg.action.getName() + ",onFailure");

        if (msg.listener != null) msg.listener.onFailure();
        queue.removeMessage();
        next();
    }

    public interface OnFinishListener {
        void onFinish();
    }

    public WifiP2pQueueManager setOnFinishListener(OnFinishListener listener) {
        this.listener = listener;
        return instance;
    }

    private WifiP2pQueueManager(WifiP2pManager manager, WifiP2pManager.Channel channel) {
        p2pManager = manager;
        p2pManagerChannel = channel;
        //this.listener = listener;

        if (queue == null) {
            queue = new WifiP2pMessageQueue();
        }
    }

    public static WifiP2pQueueManager init(WifiP2pManager manager, WifiP2pManager.Channel channel) {
        if(instance == null) {
            instance = new WifiP2pQueueManager(manager, channel);
        }
        return instance;
    }

    public void reset() {
        this.listener = null;
        queue.cleanMessage();
    }

    public void setConfig(WifiP2pConfig config) {
        this.config = config;
    }

    public WifiP2pQueueManager sendMessage(WifiP2pMessage msg) {
        queue.addMessage(msg);
        return instance;
    }

    public void start() {
        //queueThread.start();
        next();
    }

    private void next() {
        if (queue.isIdle()) {
            if (listener != null) listener.onFinish();
        } else {
            WifiP2pMessage msg = queue.pickMessage();
            doAction(msg);
        }
    }

    private void doAction(WifiP2pMessage msg) {
        switch (msg.action.getIndex()) {
            case WifiP2pMessage.MESSAGE_CANCEL_CONNECT:
                p2pManager.cancelConnect(p2pManagerChannel, this);
                break;
            case WifiP2pMessage.MESSAGE_REMOVE_GROUP:
                p2pManager.removeGroup(p2pManagerChannel, this);
                break;
            case WifiP2pMessage.MESSAGE_DISCOVER_PEERS:
                p2pManager.discoverPeers(p2pManagerChannel, this);
                break;
            case WifiP2pMessage.MESSAGE_CONNECT:
                p2pManager.connect(p2pManagerChannel, config, this);
                break;
            case WifiP2pMessage.MESSAGE_CREATE_GROUP:
                p2pManager.createGroup(p2pManagerChannel, this);
                break;
            case WifiP2pMessage.MESSAGE_UNKNOWN:
            default:
                break;
        }
    }

    /*
    Thread queueThread = new Thread() {
        @Override
        public void run() {

            LogUtils.e(TAG,  "queueThread start");

            while (!queue.isIdle()) {
                WifiP2pMessage msg = queue.pickMessage();
                doAction(msg);
                while (msg.execute) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    LogUtils.e(TAG,  "queueThread while");
                }
                queue.removeMessage();
            }

            if(listener != null) listener.onFinish();
            LogUtils.e(TAG,  "queueThread end");
        }
    };
    */

}
