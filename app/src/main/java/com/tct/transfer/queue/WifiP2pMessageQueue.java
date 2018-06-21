package com.tct.transfer.queue;

import java.util.ArrayList;

public class WifiP2pMessageQueue {

    private ArrayList<WifiP2pMessage> mQueue = new ArrayList<>();

    public synchronized void cleanMessage() {
        mQueue.clear();
    }

    public synchronized void addMessage(WifiP2pMessage msg) {
        mQueue.add(msg);
    }

    public synchronized WifiP2pMessage pickMessage() {
        if(!isIdle()) {
            WifiP2pMessage msg = mQueue.get(0);
            return msg;
        } else {
            return null;
        }
    }

    public synchronized void removeMessage() {
        if(!isIdle())
            mQueue.remove(0);
    }

    public synchronized boolean isIdle() {
        return mQueue.isEmpty();
    }
}
