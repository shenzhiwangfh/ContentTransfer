package com.tct.transfer.wifi;

import android.content.Context;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.util.Log;
import com.tct.transfer.DefaultValue;
import com.tct.transfer.file.FileBean;
import com.tct.transfer.heart.HeartBeatTask;
import com.tct.transfer.queue.WifiP2pMessage;
import com.tct.transfer.queue.WifiP2pQueueManager;
import com.tct.transfer.log.Messenger;

public class WifiP2pController {
    private Context mContext;
    private Handler mHandler;

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;

    private boolean mServer = false;
    private int mWifiState = WifiP2pManager.WIFI_P2P_STATE_DISABLED;
    private WifiP2pDeviceInfo mCustomDevice = null;
    private WifiP2pDeviceInfo mMyDevice = null;
    private boolean mLooper = false;

    public String mPath;

    public WifiP2pController(Context context, Handler handler, WifiP2pManager manager, WifiP2pManager.Channel channel) {
        mContext = context;
        mHandler = handler;
        mManager = manager;
        mChannel = channel;
    }

    public boolean isServer() {
        return mServer;
    }

    public void setServer(boolean server) {
        mServer = server;
    }

    public void setWifiState(int state) {
        mWifiState = state;
        mHandler.sendEmptyMessage(DefaultValue.MESSAGE_WIFI_STATUS_CHANGED);
    }

    public boolean isWifiOpened() {
        return (mWifiState == WifiP2pManager.WIFI_P2P_STATE_ENABLED);
    }

    public void setCustomDevice(WifiP2pDeviceInfo device) {
        mCustomDevice = device;
    }

    public WifiP2pDeviceInfo getCustomDevice() {
        return mCustomDevice;
    }

    public void setMyDevice(WifiP2pDeviceInfo device) {
        mMyDevice = device;
    }

    public WifiP2pDeviceInfo getMyDevice() {
        return mMyDevice;
    }

    public void setLooper(boolean looper) {
        mLooper = looper;
    }

    public boolean canLooper() {
        return mLooper;
    }

    public void setPath(String path) {
        mPath = path;
    }

    public void connect(WifiP2pDevice device) {
        if (device.status == WifiP2pDevice.CONNECTED) {
            //showDialog(DIALOG_DISCONNECT);
            Log.e(DefaultValue.TAG, "p2p connected");
        } else if (device.status == WifiP2pDevice.INVITED) {
            //showDialog(DIALOG_CANCEL_CONNECT);
            Log.e(DefaultValue.TAG, "p2p invited");
        } else {
            Log.e(DefaultValue.TAG, "start p2p connect:" + device.deviceName);
            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = device.deviceAddress;
            config.wps.setup = WpsInfo.PBC;

            WifiP2pQueueManager queueManager = new WifiP2pQueueManager(mManager, mChannel, null);
            queueManager.setConfig(config);
            queueManager
                    //.sendMessage(new WifiP2pMessage(WifiP2pMessage.MESSAGE_CREATE_GROUP, null))
                    .sendMessage(new WifiP2pMessage(WifiP2pMessage.MESSAGE_CONNECT, null))
                    .start();
        }
    }

    public void requestConnect(NetworkInfo networkInfo) {
        if (networkInfo.isConnected()) {
            mManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {

                @Override
                public void onConnectionInfoAvailable(WifiP2pInfo info) {
                    Log.e(DefaultValue.TAG, "onConnectionInfoAvailable");

                    String ip = null;
                    if (info.groupFormed && info.isGroupOwner) {
                        //确定为组拥有者，创建线程用于接收连接请求
                        //提交图片下载、读取的异步任务
                        Messenger.sendMessage("Group Owner");
                    } else if (info.groupFormed) {
                        //作为客户端，创建一个线程用于连接组拥有者
                        ip = info.groupOwnerAddress.getHostAddress();
                        Messenger.sendMessage("Group Client");
                    }

                    boolean owner = (ip == null);
                    FileBean bean = new FileBean();
                    if(mPath != null)
                        bean.path = mPath;

                    HeartBeatTask task = new HeartBeatTask(owner, isServer(), ip, DefaultValue.PORT_HEART_BEAT, mMyDevice, bean);
                    /*
                    task.setOnCustomDevice(new HeartBeatTask.OnSetCustomDevice() {
                        @Override
                        public void onSet(WifiP2pDeviceInfo device) {
                            mCustomDevice = device;
                            Log.e(DefaultValue.TAG, "isServer=" + isServer() + ",mCustomDevice=" + mCustomDevice.toString());
                            //sendMessage("peer device:" + mCustomDevice.toString());

                            //Message msg = Message.obtain();
                            //msg.what = DefaultValue.MESSAGE_TRANSFER_START;
                            //msg.obj = mCustomDevice;
                            //mHandler.sendMessageDelayed(msg, 3000);
                        }
                    });
                    */
                    task.start();
                }
            });
        }
    }
}
