package com.tct.transfer.queue;

import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import com.tct.transfer.DefaultValue;

public class WifiP2pQueueManager implements WifiP2pManager.ActionListener {
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private WifiP2pConfig config;

    private WifiP2pMessageQueue queue;
    private OnFinishListener listener;

    @Override
    public void onSuccess() {
        WifiP2pMessage msg = queue.pickMessage();
        Log.e(DefaultValue.TAG, msg.action.getName() + ",onSuccess");

        if (msg.listener != null) msg.listener.onSuccess();
        queue.removeMessage();
        next();
    }

    @Override
    public void onFailure(int reason) {
        WifiP2pMessage msg = queue.pickMessage();
        Log.e(DefaultValue.TAG, msg.action.getName() + ",onFailure");

        if (msg.listener != null) msg.listener.onFailure();
        queue.removeMessage();
        next();
    }

    public interface OnFinishListener {
        void onFinish();
    }

    public WifiP2pQueueManager(WifiP2pManager manager, WifiP2pManager.Channel channel, OnFinishListener listener) {
        this.manager = manager;
        this.channel = channel;
        this.listener = listener;

        if (queue == null) {
            queue = new WifiP2pMessageQueue();
        }
    }

    public void setConfig(WifiP2pConfig config) {
        this.config = config;
    }

    public WifiP2pQueueManager sendMessage(WifiP2pMessage msg) {
        queue.addMessage(msg);
        return this;
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
                manager.cancelConnect(channel, this);
                break;
            case WifiP2pMessage.MESSAGE_REMOVE_GROUP:
                manager.removeGroup(channel, this);
                break;
            case WifiP2pMessage.MESSAGE_DISCOVER_PEERS:
                manager.discoverPeers(channel, this);
                break;
            case WifiP2pMessage.MESSAGE_CONNECT:
                manager.connect(channel, config, this);
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

            Log.e(DefaultValue.TAG,  "queueThread start");

            while (!queue.isIdle()) {
                WifiP2pMessage msg = queue.pickMessage();
                doAction(msg);
                while (msg.execute) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    Log.e(DefaultValue.TAG,  "queueThread while");
                }
                queue.removeMessage();
            }

            if(listener != null) listener.onFinish();
            Log.e(DefaultValue.TAG,  "queueThread end");
        }
    };
    */

}
