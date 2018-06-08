package com.tct.transfer.file;

import java.io.Serializable;

public class FileBean implements Serializable {
    public String name;
    public String md5;
    public long size;
    public int action; //0:upload, 1:download
    public long time;
    public int elapsed;
    public int type; //0:picture, 1:video, 2:text
    public int status; //0:start, 1:ing, 3:end
}
