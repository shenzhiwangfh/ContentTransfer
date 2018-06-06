package com.tct.transfer.queue;

public class WifiP2pMessage {

    public final static int MESSAGE_UNKNOWN = 0;
    public final static int MESSAGE_CANCEL_CONNECT = 100;
    public final static int MESSAGE_REMOVE_GROUP = 101;
    public final static int MESSAGE_DISCOVER_PEERS = 102;
    public final static int MESSAGE_CONNECT = 103;

    //public int action;
    public Action action;
    public OnActionListener listener;
    //public boolean execute = true;

    public enum Action {
        UNKNOWN("unknown", MESSAGE_UNKNOWN),
        CANCEL_CONNECT("cancelConnect", MESSAGE_CANCEL_CONNECT),
        REMOVE_GROUP("removeGroup", MESSAGE_REMOVE_GROUP),
        DISCOVER_PEERS("discoverPeers", MESSAGE_DISCOVER_PEERS),
        CONNECT("connect", MESSAGE_CONNECT);

        private String name;
        private int index;

        Action(String name, int index) {
            this.name = name;
            this.index = index;
        }

        public String getName() {
            return name;
        }

        public int getIndex() {
            return index;
        }
    }


    public interface OnActionListener {
        void onSuccess();
        void onFailure();
    }

    public WifiP2pMessage(int action, OnActionListener listener) {
        switch (action) {
            case MESSAGE_CANCEL_CONNECT:
                this.action = Action.CANCEL_CONNECT;
                break;
            case MESSAGE_REMOVE_GROUP:
                this.action = Action.REMOVE_GROUP;
                break;
            case MESSAGE_DISCOVER_PEERS:
                this.action = Action.DISCOVER_PEERS;
                break;
            case MESSAGE_CONNECT:
                this.action = Action.CONNECT;
                break;
            case MESSAGE_UNKNOWN:
            default:
                this.action = Action.UNKNOWN;
                break;
        }

        this.listener = listener;
    }
}
