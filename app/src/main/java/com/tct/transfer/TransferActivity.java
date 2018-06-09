package com.tct.transfer;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.tct.libzxing.zxing.activity.CaptureActivity;
import com.tct.libzxing.zxing.encoding.EncodingUtils;
import com.tct.transfer.file.FileBean;
import com.tct.transfer.file.FileTransferClient;
import com.tct.transfer.file.FileTransferServer;
import com.tct.transfer.file.FileUtil;
import com.tct.transfer.heart.HeartBeatTask;
import com.tct.transfer.permission.PermissionHelper;
import com.tct.transfer.permission.PermissionInterface;
import com.tct.transfer.permission.PermissionUtil;
import com.tct.transfer.queue.WifiP2pMessage;
import com.tct.transfer.queue.WifiP2pQueueManager;
import com.tct.transfer.wifi.WifiP2PReceiver;
import com.tct.transfer.wifi.WifiP2pDeviceInfo;
import com.tct.transfer.wifi.WifiP2pInterface;
import com.tct.transfer.log.Messenger;

public class TransferActivity extends AppCompatActivity implements View.OnClickListener, WifiP2pInterface, PermissionInterface {

    private Context mContext;

    private Button mShare;
    //private TextView mFileName;
    private Button mAccept;
    private ImageView mStatus;
    private ImageView mQRCode;

    private ScrollView mScroll;
    private TextView mLog;

    private WifiP2PReceiver mReceiver;
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;

    //private WifiP2pController mWifiP2pController;
    private PermissionHelper mPermissionHelper;

    private boolean mServer = false;
    private int mWifiState = WifiP2pManager.WIFI_P2P_STATE_DISABLED;
    private boolean mLooper = false;

    private WifiP2pDeviceInfo mCustomDevice = null;
    private WifiP2pDeviceInfo mMyDevice = null;
    private FileBean mBean = new FileBean();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.transfer);

        mContext = this;

        mPermissionHelper = new PermissionHelper(this, this);
        mPermissionHelper.requestPermissions();

        mShare = findViewById(R.id.share_file);
        mAccept = findViewById(R.id.accept_file);
        mStatus = findViewById(R.id.status);
        mQRCode = findViewById(R.id.qr_big_code);

        mScroll = findViewById(R.id.log_scrollview);
        mLog = findViewById(R.id.log);

        mShare.setOnClickListener(this);
        mAccept.setOnClickListener(this);
        mStatus.setBackgroundColor(Color.GRAY);

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);

        //mWifiP2pController = new WifiP2pController(this, mHandler, mManager, mChannel);
        //mWifiP2pController.setLog(mLog);
        Messenger.init(mContext, mHandler, DefaultValue.MESSAGE_LOG);

        mReceiver = new WifiP2PReceiver(this);

        WifiP2pQueueManager queueManager = new WifiP2pQueueManager(mManager, mChannel, new WifiP2pQueueManager.OnFinishListener() {
            @Override
            public void onFinish() {
                mHandler.sendEmptyMessage(DefaultValue.MESSAGE_REGISTER);
            }
        });
        queueManager
                .sendMessage(new WifiP2pMessage(WifiP2pMessage.MESSAGE_REMOVE_GROUP, null))
                .sendMessage(new WifiP2pMessage(WifiP2pMessage.MESSAGE_CANCEL_CONNECT, null))
                .start();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.share_file: {
                //setServer(true);
                Messenger.clearMessage();

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*"); //选择图片
                //intent.setType("audio/*"); //选择音频
                //intent.setType("video/*"); //选择视频 （mp4/3gp 是android支持的视频格式）
                //intent.setType("video/*;image/*"); //同时选择视频和图片
                //intent.setType("*/*"); //无类型限制
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, DefaultValue.REQUEST_CODE_QUERY_FILE);
            }
            break;
            case R.id.accept_file: {
                setServer(false);
                Messenger.clearMessage();

                int height = mQRCode.getHeight();
                int width = mQRCode.getWidth();
                final int len = height > width ? width : height;
                String shareInfo = getMyDevice().toString();
                Bitmap bmp = EncodingUtils.createQRCode(shareInfo, len, len, null);
                mQRCode.setImageBitmap(bmp);

                mLooper = true;
                mQRCode.setVisibility(mLooper ? View.VISIBLE : View.INVISIBLE);


                WifiP2pQueueManager queueManager = new WifiP2pQueueManager(mManager, mChannel, new WifiP2pQueueManager.OnFinishListener() {
                    @Override
                    public void onFinish() {
                        Log.e(DefaultValue.TAG, "MESSAGE_WIFI_DISCOVER finish");
                        mHandler.sendEmptyMessage(DefaultValue.MESSAGE_WIFI_DISCOVER);
                    }
                });
                queueManager
                        .sendMessage(new WifiP2pMessage(WifiP2pMessage.MESSAGE_REMOVE_GROUP, null))
                        .sendMessage(new WifiP2pMessage(WifiP2pMessage.MESSAGE_CANCEL_CONNECT, null))
                        //.sendMessage(new WifiP2pMessage(WifiP2pMessage.MESSAGE_DISCOVER_PEERS, null))
                        .start();
            }
            break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == DefaultValue.REQUEST_CODE_QUERY_FILE) {
                mBean.path = FileUtil.getPath(mContext, data.getData());
                //mWifiP2pController.setPath(mBean.path);

                if (PermissionUtil.hasPermission(this, Manifest.permission.CAMERA)) {
                    Intent intent = new Intent(TransferActivity.this, CaptureActivity.class);
                    startActivityForResult(intent, DefaultValue.REQUEST_CODE_SCAN);
                } else {
                    Toast.makeText(this, R.string.error_permission_camera, Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == DefaultValue.REQUEST_CODE_SCAN) {
                Bundle bundle = data.getExtras();
                if (bundle != null) {
                    String scanResult = bundle.getString("result");
                    setCustomDevice(WifiP2pDeviceInfo.analysis(scanResult));
                    setServer(true);

                    mLooper = true;
                    mQRCode.setVisibility(mLooper ? View.VISIBLE : View.INVISIBLE);

                    WifiP2pQueueManager queueManager = new WifiP2pQueueManager(mManager, mChannel, new WifiP2pQueueManager.OnFinishListener() {
                        @Override
                        public void onFinish() {
                            Log.e(DefaultValue.TAG, "MESSAGE_WIFI_DISCOVER finish");
                            mHandler.sendEmptyMessage(DefaultValue.MESSAGE_WIFI_DISCOVER);
                        }
                    });
                    queueManager
                            .sendMessage(new WifiP2pMessage(WifiP2pMessage.MESSAGE_REMOVE_GROUP, null))
                            .sendMessage(new WifiP2pMessage(WifiP2pMessage.MESSAGE_CANCEL_CONNECT, null))
                            //.sendMessage(new WifiP2pMessage(WifiP2pMessage.MESSAGE_DISCOVER_PEERS, null))
                            .start();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver();
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        registerReceiver(mReceiver, filter);
    }

    private void unregisterReceiver() {
        unregisterReceiver(mReceiver);
    }

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            int what = msg.what;
            switch (what) {
                case DefaultValue.MESSAGE_REGISTER:
                    Log.e(DefaultValue.TAG, "handler,MESSAGE_REGISTER");

                    registerReceiver();
                    break;
                case DefaultValue.MESSAGE_WIFI_STATUS_CHANGED:
                    Log.e(DefaultValue.TAG, "handler,MESSAGE_WIFI_STATUS_CHANGED");

                    if (isWifiOpened()) {
                        mStatus.setBackgroundColor(Color.GREEN);
                        mShare.setEnabled(true);
                        mAccept.setEnabled(true);
                    } else {
                        mStatus.setBackgroundColor(Color.GRAY);
                        mShare.setEnabled(false);
                        mAccept.setEnabled(false);
                    }
                    break;
                case DefaultValue.MESSAGE_WIFI_DISCOVER:
                    Log.e(DefaultValue.TAG, "handler,MESSAGE_WIFI_DISCOVER");

                    if (mLooper) {
                        //Log.e(DefaultValue.TAG, "handler,MESSAGE_WIFI_DISCOVER,loop");
                        Messenger.sendMessage(R.string.status_p2p_peer);

                        WifiP2pQueueManager queueManager = new WifiP2pQueueManager(mManager, mChannel, new WifiP2pQueueManager.OnFinishListener() {
                            @Override
                            public void onFinish() {
                                Log.e(DefaultValue.TAG, "MESSAGE_WIFI_DISCOVER finish");
                                //registerReceiver();
                                mHandler.sendEmptyMessageDelayed(DefaultValue.MESSAGE_WIFI_DISCOVER, 30 * 1000);
                            }
                        });
                        queueManager
                                //.sendMessage(new WifiP2pMessage(WifiP2pMessage.MESSAGE_REMOVE_GROUP, null))
                                //.sendMessage(new WifiP2pMessage(WifiP2pMessage.MESSAGE_CANCEL_CONNECT, null))
                                .sendMessage(new WifiP2pMessage(WifiP2pMessage.MESSAGE_DISCOVER_PEERS, null))
                                .start();

                    }
                    break;
                case DefaultValue.MESSAGE_TRANSFER_START:

                    if (isServer()) {
                        //mBean.path = mWifiP2pController.getPath();
                        FileTransferServer server = new FileTransferServer(mContext, DefaultValue.PORT_TRANSFER, mBean);
                        server.start();
                    } else {
                        FileTransferClient client = new FileTransferClient(mContext, getCustomDevice().getIp(), DefaultValue.PORT_TRANSFER);
                        client.start();
                    }

                    /*
                    FileTransferThread task = new FileTransferThread(
                            isServer(),
                            getCustomDevice().getIp(),
                            DefaultValue.PORT_TRANSFER,
                            TransferActivity.this,
                            mWifiP2pController.getPath());
                    task.setTransferStatue(new FileTransferThread.TransferStatue() {
                        @Override
                        public void sendStatue(boolean server, int status, String file) {
                            //sendMessage("" + server + "," + status);
                            Message msg = Message.obtain();
                            msg.what = DefaultValue.MESSAGE_TRANSFER_STATUS;
                            msg.arg1 = server ? 0 : 1;
                            msg.arg2 = status;
                            msg.obj = file;
                            mHandler.sendMessage(msg);
                        }
                    });
                    task.start();
                    */

                    break;

                case DefaultValue.MESSAGE_TRANSFER_STATUS:
                    boolean server = (msg.arg1 == 0);
                    int status = msg.arg2;
                    String path = (String) msg.obj;

                    Log.e(DefaultValue.TAG, "server=" + server + ",status=" + status + ",path=" + path);

                    String message = getResources().getString(server ? R.string.transfer_server : R.string.transfer_client);
                    if (status == DefaultValue.TRANSFER_START) {
                        if (server)
                            message += getResources().getString(R.string.transfer_server_start, Uri.parse(path).getPath());
                        else
                            message += getResources().getString(R.string.transfer_client_start);
                    } else if (status == DefaultValue.TRANSFER_END) {
                        if (server)
                            message += getResources().getString(R.string.transfer_server_end);
                        else
                            message += getResources().getString(R.string.transfer_client_end, path);
                    } else if (status == DefaultValue.TRANSFER_ERROR) {
                        String error = (String) msg.obj;
                    }
                    Messenger.sendMessage(message);
                    break;

                case DefaultValue.MESSAGE_LOG:
                    mLog.setText((String) msg.obj);
                    mScroll.scrollTo(0, mLog.getMeasuredHeight());
                    break;

                //case DefaultValue.MESSAGE_CANCEL_CONNECT:
                //    mManager.cancelConnect(mChannel, new IgnoreActionListener("cancelConnect"));
                //    break;
                //case DefaultValue.MESSAGE_REMOVE_GROUP:
                //    mManager.removeGroup(mChannel, new IgnoreActionListener("removeGroup"));
                //    break;

                default:
                    break;
            }

            return false;
        }
    });

    @Override
    public void setMyDevice(WifiP2pDeviceInfo myDevice) {
        //mWifiP2pController.setMyDevice(myDevice);
        mMyDevice = myDevice;
    }

    @Override
    public WifiP2pDeviceInfo getMyDevice() {
        return mMyDevice;//mWifiP2pController.getMyDevice();
    }

    @Override
    public void setCustomDevice(WifiP2pDeviceInfo customDevice) {
        //mWifiP2pController.setCustomDevice(customDevice);
        mCustomDevice = customDevice;
    }

    @Override
    public WifiP2pDeviceInfo getCustomDevice() {
        return mCustomDevice;//mWifiP2pController.getCustomDevice();
    }

    @Override
    public void setServer(boolean server) {
        //mWifiP2pController.setServer(server);
        mServer = server;
        mHandler.sendEmptyMessage(DefaultValue.MESSAGE_WIFI_STATUS_CHANGED);
    }

    @Override
    public boolean isServer() {
        //return mWifiP2pController.isServer();
        return mServer;
    }

    //@Override
    //public void setLooper(boolean looper) {
    //    //mWifiP2pController.setLooper(looper);
    //    mLooper = looper;
    //    mQRCode.setVisibility(looper ? View.VISIBLE : View.INVISIBLE);
    //}

    //@Override
    //public boolean canLooper() {
    //    return mLooper;
        //return mWifiP2pController.canLooper();
    //}

    @Override
    public void setWifiState(int state) {
        //mWifiP2pController.setWifiState(state);
        mWifiState = state;
    }

    @Override
    public boolean isWifiOpened() {
        //return mWifiP2pController.isWifiOpened();
        return (mWifiState == WifiP2pManager.WIFI_P2P_STATE_ENABLED);
    }

    @Override
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

    @Override
    public void requestConnect(NetworkInfo networkInfo) {
        //mWifiP2pController.requestConnect(networkInfo);
        mLooper = false;

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
                    //FileBean bean = new FileBean();
                    //if(mPath != null)
                    //    bean.path = mPath;

                    HeartBeatTask task = new HeartBeatTask(owner, isServer(), ip, DefaultValue.PORT_HEART_BEAT, mMyDevice, mBean);
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



    ///////////////////////////////////////
    @Override
    public int getPermissionsRequestCode() {
        return DefaultValue.REQUEST_CODE_PERMISSION;
    }

    @Override
    public String[] getPermissions() {
        return new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
    }

    @Override
    public void requestPermissionsSuccess() {

    }

    @Override
    public void requestPermissionsFail() {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (mPermissionHelper.requestPermissionsResult(requestCode, permissions, grantResults)) {
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
