package com.tct.transfer.file;

import android.net.Uri;

import java.io.Serializable;

public class FileBean implements Serializable {
    //public Uri uri = Uri.EMPTY;
    public String path;
    public String name;
    public String md5;
    public long size;
    public int action; //0:upload, 1:download
    public long time;
    public int elapsed;
    public int type; //0:picture, 1:video, 2:text, 3:other
    public int status; //0:start, 1:ing, 3:end

    public String toString() {
        StringBuilder sb = new StringBuilder()
                .append("path").append("=").append(path).append(",")
                .append("md5").append("=").append(md5).append(",")
                .append("size").append("=").append(size).append(",")
                .append(action == 0 ? "upload" : "download").append(",")
                .append("type").append("=").append(type);
        return sb.toString();
    }
}
