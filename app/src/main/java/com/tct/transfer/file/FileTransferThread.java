package com.tct.transfer.file;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import com.tct.transfer.DefaultValue;

public class FileTransferThread extends Thread {

    private boolean server;
    private String ip;
    private int port;

    private Context context;
    private String path;

    public FileTransferThread(boolean server, String ip, int port, Context context, String path) {
        this.ip = ip;
        this.port = port;
        this.server = server;
        this.context = context;
        this.path = server ? path : DefaultValue.recvPath();
    }

    public interface TransferStatue {
        void sendStatue(boolean server, int status, String file);
    }
    private TransferStatue mTransferStatue;
    public void setTransferStatue(TransferStatue transferStatue) {
        mTransferStatue = transferStatue;
    }

    @Override
    public void run() {
        if (ip == null) {
            try {
                Log.e(DefaultValue.TAG, "FileTransferThread,group owner");

                ServerSocket serverSocket = new ServerSocket(port);
                Socket client = serverSocket.accept();
                Log.e(DefaultValue.TAG, "FileTransferThread,accept");

                if(server) {
                    Log.e(DefaultValue.TAG, "FileTransferThread,group owner/server/start transfer");
                    if(mTransferStatue != null) mTransferStatue.sendStatue(server, DefaultValue.TRANSFER_START, path);

                    OutputStream stream = client.getOutputStream();
                    ContentResolver cr = context.getContentResolver();
                    InputStream is = null;
                    try {
                        is = cr.openInputStream(Uri.parse(path));
                    } catch (FileNotFoundException e) {
                    }

                    FileUtil.copyFile(is, stream);

                    Log.e(DefaultValue.TAG, "FileTransferThread,group owner/server/end transfer");
                    if(mTransferStatue != null) mTransferStatue.sendStatue(server, DefaultValue.TRANSFER_END, path);
                } else {
                    Log.e(DefaultValue.TAG, "FileTransferThread,group owner/client/start transfer");
                    if(mTransferStatue != null) mTransferStatue.sendStatue(server, DefaultValue.TRANSFER_START, path);

                    final File f = new File(path);
                    File dirs = new File(f.getParent());

                    if (!dirs.exists())
                        dirs.mkdirs();
                    f.createNewFile();

                    InputStream inputstream = client.getInputStream();
                    FileUtil.copyFile(inputstream, new FileOutputStream(f));

                    Log.e(DefaultValue.TAG, "FileTransferThread,group owner/client/end transfer");
                    if(mTransferStatue != null) mTransferStatue.sendStatue(server, DefaultValue.TRANSFER_END, path);
                }

                client.close();
                serverSocket.close();

                Log.e(DefaultValue.TAG, "FileTransferThread,group owner/transfer finally");
            } catch (IOException e) {
                Log.e(DefaultValue.TAG, "FileTransferThread,group owner/transfer error=" + e);
                if(mTransferStatue != null) mTransferStatue.sendStatue(server, DefaultValue.TRANSFER_ERROR, e.toString());
            }
        } else {
            Socket socket = new Socket();
            try {
                Log.e(DefaultValue.TAG, "FileTransferThread,group client");
                socket.bind(null);
                socket.connect((new InetSocketAddress(ip, port)), DefaultValue.SOCKET_CONNECT_TIMEOUT);
                Log.e(DefaultValue.TAG, "FileTransferThread,group client/connect");

                if(server) {
                    Log.e(DefaultValue.TAG, "FileTransferThread,group client/server/start transfer");
                    if(mTransferStatue != null) mTransferStatue.sendStatue(server, DefaultValue.TRANSFER_START, path);

                    OutputStream stream = socket.getOutputStream();
                    ContentResolver cr = context.getContentResolver();
                    InputStream is = null;
                    try {
                        is = cr.openInputStream(Uri.parse(path));
                    } catch (FileNotFoundException e) {
                    }
                    FileUtil.copyFile(is, stream);

                    Log.e(DefaultValue.TAG, "FileTransferThread,group client/server/end transfer");
                    if(mTransferStatue != null) mTransferStatue.sendStatue(server, DefaultValue.TRANSFER_END, path);
                } else {
                    Log.e(DefaultValue.TAG, "FileTransferThread,group client/client/start transfer");
                    if(mTransferStatue != null) mTransferStatue.sendStatue(server, DefaultValue.TRANSFER_START, path);

                    final File f = new File(path);
                    final File dirs = new File(f.getParent());
                    if (!dirs.exists())
                        dirs.mkdirs();
                    f.createNewFile();

                    InputStream inputstream = socket.getInputStream();
                    FileUtil.copyFile(inputstream, new FileOutputStream(f));

                    Log.e(DefaultValue.TAG, "FileTransferThread,group client/client/end transfer");
                    if(mTransferStatue != null) mTransferStatue.sendStatue(server, DefaultValue.TRANSFER_END, path);
                }
            } catch (IOException e) {
                Log.e(DefaultValue.TAG, "FileTransferThread,group client/transfer error=" + e);
                if(mTransferStatue != null) mTransferStatue.sendStatue(server, DefaultValue.TRANSFER_ERROR, e.toString());
            } finally {
                Log.e(DefaultValue.TAG, "FileTransferThread,group client/transfer finally");
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            // Give up
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}
