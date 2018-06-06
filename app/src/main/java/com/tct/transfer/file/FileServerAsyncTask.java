package com.tct.transfer.file;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.tct.transfer.DefaultValue;
import com.tct.transfer.wifi.WifiP2pController;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class FileServerAsyncTask extends AsyncTask<Void, Void, String> {

    private Context context;
    //private TextView statusText;

    /**
     * @param context
     * @param statusText
     */
    public FileServerAsyncTask(Context context, View statusText) {
        this.context = context;
        //this.statusText = (TextView) statusText;
    }

    @Override
    protected String doInBackground(Void... params) {
        try {
            Log.e(DefaultValue.TAG, "doInBackground");

            ServerSocket serverSocket = new ServerSocket(DefaultValue.PORT_TRANSFER);
            Socket client = serverSocket.accept();

            //WifiP2pController.log("accept:" + client.getInetAddress());

            Log.e(DefaultValue.TAG, "getInetAddress=" + client.getInetAddress());


/*
            final File f = new File(
                    Environment.getExternalStorageDirectory() + "/" + "RecvPicture" +
                            "/wifip2pshared-" + System.currentTimeMillis() + ".jpg");
            File dirs = new File(f.getParent());

            if (!dirs.exists())
                dirs.mkdirs();
            f.createNewFile();


            InputStream inputstream = client.getInputStream();
            copyFile(inputstream, new FileOutputStream(f));
            serverSocket.close();
            return f.getAbsolutePath();
            */


return "123";
        } catch (IOException e) {
            Log.e(DefaultValue.TAG, e.toString());
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
     */
    @Override
    protected void onPostExecute(String result) {


        //if (result != null) {
        //    Toast.makeText(context, "RecvPic:" + result, Toast.LENGTH_SHORT).show();
        //}


        /*
        if (result != null) {
            //statusText.setText("RecvPic: " + result);
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse("file://" + result), "image/*");
            context.startActivity(intent);
        }
        */

        Log.e(DefaultValue.TAG, "onPostExecute");

    }

    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);

            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }
}