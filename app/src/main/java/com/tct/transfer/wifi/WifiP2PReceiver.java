package com.tct.transfer.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;
import com.tct.transfer.DefaultValue;
import com.tct.transfer.R;
import com.tct.transfer.queue.WifiP2pMessage;
import com.tct.transfer.log.Messenger;

import java.util.Collection;

public class WifiP2PReceiver extends BroadcastReceiver {
    private WifiP2pInterface mWifiP2pInterface;

    public WifiP2PReceiver(WifiP2pInterface wifiP2pInterface) {
        this.mWifiP2pInterface = wifiP2pInterface;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        /*check if the wifi is enable*/
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, WifiP2pManager.WIFI_P2P_STATE_DISABLED);
            Log.e(DefaultValue.TAG, "STATE:" + state);
            mWifiP2pInterface.setWifiState(state);

            Messenger.sendMessage(mWifiP2pInterface.isWifiOpened()
                    ? R.string.status_wifi_opened
                    : R.string.status_wifi_not_opened);
        }
        /*get the list*/
        else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            Messenger.sendMessage(R.string.status_p2p_peer_changed);

            Log.e(DefaultValue.TAG, "PEERS");
            WifiP2pDeviceList peers = intent.getParcelableExtra(WifiP2pManager.EXTRA_P2P_DEVICE_LIST);
            Collection<WifiP2pDevice> aList = peers.getDeviceList();
            Log.e(DefaultValue.TAG, "aList.size()=" + aList.size() + ","
                    + mWifiP2pInterface.isServer() + ","
                    + mWifiP2pInterface.getCustomDevice());

            WifiP2pDeviceInfo customDevice = mWifiP2pInterface.getCustomDevice();
            if(mWifiP2pInterface.isServer() && (customDevice != null)) {
                //WifiP2pDeviceInfo customDevice = mWifiP2pInterface.getCustomDevice();
                String customName = customDevice.getName();
                String customMac = customDevice.getMac();
                WifiP2pDevice matchedDevice = null;
                for (WifiP2pDevice peer : aList) {
                    if (customName.equals(peer.deviceName) && customMac.equals(peer.deviceAddress)) {
                        matchedDevice = peer;
                        Log.e(DefaultValue.TAG, "PEERS matched");
                        break;
                    }
                    Messenger.sendMessage("    " + peer.deviceName);
                }

                if (matchedDevice != null) {
                    Messenger.sendMessage(R.string.status_p2p_connect, matchedDevice.deviceName);
                    mWifiP2pInterface.connect(matchedDevice);
                }
            }
        }
        /*Respond to new connection or disconnections*/
        else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            //mWifiP2pInterface.setLooper(false);
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            Log.e(DefaultValue.TAG, "CONNECTION:" + networkInfo.isConnected());

            Messenger.sendMessage(networkInfo.isConnected()
                    ? R.string.status_p2p_connected
                    : R.string.status_p2p_not_connected);

            if (networkInfo.isConnected()) {
                mWifiP2pInterface.requestConnect(networkInfo);
            }
        }
        /*Respond to this device's wifi state changing*/
        else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            WifiP2pDevice thisDevice = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            Log.e(DefaultValue.TAG, "THIS_DEVICE/deviceName=" + thisDevice.deviceName + ",deviceAddress=" + thisDevice.deviceAddress);
            mWifiP2pInterface.setMyDevice(new WifiP2pDeviceInfo(thisDevice.deviceName, thisDevice.deviceAddress));
            Messenger.sendMessage(R.string.status_host, thisDevice.deviceName);
        }
    }
}
