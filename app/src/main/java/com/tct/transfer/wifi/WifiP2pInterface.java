package com.tct.transfer.wifi;

import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;

public interface WifiP2pInterface {
    void setMyDevice(WifiP2pDeviceInfo myDevice);
    WifiP2pDeviceInfo getMyDevice();

    void setCustomDevice(WifiP2pDeviceInfo customDevice);
    WifiP2pDeviceInfo getCustomDevice();

    void setServer(boolean server);
    boolean isServer();

    void setLooper(boolean looper);
    boolean canLooper();

    void setWifiState(int state);
    boolean isWifiOpened();

    void connect(WifiP2pDevice device);
    void requestConnect(NetworkInfo networkInfo);

    /*
    void sendMessage(int resId, Object... args);
    void sendMessage(String msg, Object... args);
    void clearMessage();
    */
}
