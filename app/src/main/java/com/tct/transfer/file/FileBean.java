package com.tct.transfer.file;

import android.net.Uri;

import java.io.File;
import java.io.Serializable;

public class FileBean implements Serializable {
    //public Uri uri = Uri.EMPTY;
    public int _id;
    public String path;
    public String name;
    public String md5;
    public long size;
    public long transferSize;
    public int action; //0:upload, 1:download
    public long time;
    public long elapsed;
    public int type; //0:image, 1:video, 2:text, 3:audio, 4:other
    public int status; //0:start, 1:ing, 2:end
    public int result; //0:succeed, 1:failed,
    public String uri;

    public FileBean() {

    }

    public FileBean(FileBean bean) {
        this.path = bean.path;
        this.name = bean.name;
        this.md5 = bean.md5;
        this.size = bean.size;
        this.transferSize = bean.transferSize;
        this.action = bean.action; //0:upload, 1:download
        this.time = bean.time;
        this.elapsed = bean.elapsed;
        this.type = bean.type; //0:picture, 1:video, 2:text, 3:other
        this.status = bean.status; //0:start, 1:ing, 2:end
        this.result = bean.result; //0:succeed, 1:failed,
        //this.uri = bean.uri;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder()
                .append("path").append("=").append(path).append(",")
                .append("md5").append("=").append(md5).append(",")
                .append("size").append("=").append(size).append(",")
                .append(action == 0 ? "upload" : "download").append(",")
                .append("type").append("=").append(type);
                //.append("uri").append("=").append(uri);
        return sb.toString();
    }
}
