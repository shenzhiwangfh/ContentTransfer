package com.tct.transfer.wifi;

import java.io.Serializable;

public class WifiP2pDeviceInfo implements Serializable {
    private String name;
    private String mac;
    private String peerIP;
    private boolean owner;
    private boolean server = false;

    public WifiP2pDeviceInfo() {

    }

    public WifiP2pDeviceInfo(String name, String mac) {
        this.name = name;
        this.mac = mac;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        return sb.append(name).append(",")
                .append(mac).append(",")
                .append(peerIP).append(",")
                .append(owner).append(",")
                .append(server).toString();
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getMac() {
        return mac;
    }

    public void setPeerIp(String peerIP) {
        this.peerIP = peerIP;
    }

    public String getPeerIp() {
        return peerIP;
    }

    public void setOwner(boolean owner) {
        this.owner = owner;
    }

    public boolean isOwner() {
        return owner;
    }

    public void setServer(boolean server) {
        this.server = server;
    }

    public boolean isServer() {
        return server;
    }

    public static WifiP2pDeviceInfo analysis(String data) {
        if (data == null || data.isEmpty())
            return null;

        String tmp[] = data.split(",");
        if (tmp.length != 5)
            return null;

        return new WifiP2pDeviceInfo(tmp[0], tmp[1]);
    }

        /*
        public static Map<String, String> string2map(String data) {
            if (data == null || data.isEmpty())
                return null;

            Map<String, String> map = new HashMap<>();
            String[] infos = data.split(";");
            for (String info : infos) {
                String[] tmp = info.split("=");
                if (tmp.length == 2) {
                    String name = tmp[0];
                    String address = tmp[1];
                    map.put(name, address);
                }
            }

            return map;
        }

        public static String map2string(Map<String, String> map) {
            if (map == null || map.isEmpty())
                return null;

            StringBuilder sb = new StringBuilder();
            Iterator<Map.Entry<String, String>> entries = map.entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry<String, String> entry = entries.next();
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append(";");
            }
            return sb.deleteCharAt(sb.lastIndexOf(";")).toString();
        }
        */
}
