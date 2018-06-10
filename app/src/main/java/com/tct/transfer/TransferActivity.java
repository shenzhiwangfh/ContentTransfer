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
//import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.tct.libzxing.zxing.activity.CaptureActivity;
import com.tct.libzxing.zxing.encoding.EncodingUtils;
import com.tct.transfer.file.FileBean;
import com.tct.transfer.file.FileTransferGroupClient;
import com.tct.transfer.file.FileTransferGroupOwner;
import com.tct.transfer.util.DefaultValue;
import com.tct.transfer.util.FileSizeUtil;
import com.tct.transfer.util.FileUtil;
import com.tct.transfer.file.TransferStatus;
import com.tct.transfer.heart.HeartBeatTask;
import com.tct.transfer.log.LogUtils;
import com.tct.transfer.permission.PermissionHelper;
import com.tct.transfer.permission.PermissionInterface;
import com.tct.transfer.permission.PermissionUtil;
import com.tct.transfer.queue.WifiP2pMessage;
import com.tct.transfer.queue.WifiP2pQueueManager;
import com.tct.transfer.util.Utils;
import com.tct.transfer.wifi.WifiP2PReceiver;
import com.tct.transfer.wifi.WifiP2pDeviceInfo;
import com.tct.transfer.wifi.WifiP2pInterface;
import com.tct.transfer.log.Messenger;

import java.text.DecimalFormat;

public class TransferActivity extends AppCompatActivity implements
        View.OnClickListener,
        WifiP2pInterface,
        PermissionInterface {

    private final static String TAG = "TransferActivity";

    private Context mContext;
    private DecimalFormat df;

    private Button mShare;
    //private TextView mFileName;
    private Button mAccept;
    private ImageView mStatus;
    private ImageView mQRCode;

    private TextView mMyDeviceText;
    private TextView mCustomDeviceText;
    private TextView mTransferText;

    private ScrollView mScroll;
    private TextView mLog;

    private WifiP2PReceiver mReceiver;
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;

    private PermissionHelper mPermissionHelper;

    private int mWifiState = WifiP2pManager.WIFI_P2P_STATE_DISABLED;
    private boolean mLooper = false;

    private WifiP2pDeviceInfo mCustomDevice = null;
    private WifiP2pDeviceInfo mMyDevice = new WifiP2pDeviceInfo();
    //private FileBean mBean;// = new FileBean();
    private String mPath;

    private TransferStatus mTransferStatus = new TransferStatus() {
        @Override
        public void sendStatus(FileBean bean) {

            if (bean.status == 0) {
                //mBean = bean;
                //mShowLoop = true;
                //new Thread(mShowThread).start();
            } else if(bean.status == 1) {
                //mBean = bean;
            } else if (bean.status == 2) {
                //mBean = bean;
                //mShowLoop = false;
                //mTransferText.setText(getString(R.string.transfer_end, Utils.long2time(mBean.elapsed)));
                Message msg = Message.obtain();
                msg.what = DefaultValue.MESSAGE_TRANSFER_STATUS;
                msg.obj = getString(R.string.transfer_end, Utils.long2time(bean.elapsed));
                mHandler.sendMessage(msg);
            }
        }
    };

    /*
    private boolean mShowLoop = false;
    private Runnable mShowThread = new Runnable() {
        @Override
        public void run() {
            while (mShowLoop) {
                String percent = df.format((float) mBean.transferSize / (float) mBean.size);
                String transferSize = FileSizeUtil.FormetFileSize(mBean.transferSize) + " / " + FileSizeUtil.FormetFileSize(mBean.size);
                String showText = percent + "  " + transferSize + "  " + Utils.long2time(mBean.elapsed);
                if (mShowLoop) {
                    //mTransferText.setText(showText);
                    Message msg = Message.obtain();
                    msg.what = DefaultValue.MESSAGE_TRANSFER_STATUS;
                    msg.obj = showText;
                    mHandler.sendMessage(msg);
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.transfer);

        mContext = this;

        df = new DecimalFormat("0%");
        df.setMaximumFractionDigits(2);

        mPermissionHelper = new PermissionHelper(this, this);
        mPermissionHelper.requestPermissions();

        mShare = findViewById(R.id.share_file);
        mAccept = findViewById(R.id.accept_file);
        mStatus = findViewById(R.id.status);
        mQRCode = findViewById(R.id.qr_big_code);

        mMyDeviceText = findViewById(R.id.my_device);
        mCustomDeviceText = findViewById(R.id.custom_device);
        mTransferText = findViewById(R.id.transfer_status);

        mScroll = findViewById(R.id.log_scrollview);
        mLog = findViewById(R.id.log);
        Messenger.init(mContext, mHandler, DefaultValue.MESSAGE_LOG);

        mShare.setOnClickListener(this);
        mAccept.setOnClickListener(this);
        mStatus.setBackgroundColor(Color.GRAY);

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);

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

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.share_file: {
                //setServer(true);
                Messenger.clearMessage();

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                //intent.setType("image/*"); //选择图片
                //intent.setType("audio/*"); //选择音频
                //intent.setType("video/*"); //选择视频 （mp4/3gp 是android支持的视频格式）
                //intent.setType("video/*;image/*"); //同时选择视频和图片
                intent.setType("*/*"); //无类型限制
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, DefaultValue.REQUEST_CODE_QUERY_FILE);
            }
            break;
            case R.id.accept_file: {
                //setServer(false);
                mMyDevice.setServer(false);
                Messenger.clearMessage();

                int height = mQRCode.getHeight();
                int width = mQRCode.getWidth();
                final int len = height > width ? width : height;
                String shareInfo = mMyDevice.toString();//getMyDevice().toString();
                Bitmap bmp = EncodingUtils.createQRCode(shareInfo, len, len, null);
                mQRCode.setImageBitmap(bmp);

                mLooper = true;
                mQRCode.setVisibility(mLooper ? View.VISIBLE : View.INVISIBLE);

                WifiP2pQueueManager queueManager = new WifiP2pQueueManager(mManager, mChannel, new WifiP2pQueueManager.OnFinishListener() {
                    @Override
                    public void onFinish() {
                        LogUtils.e(TAG, "MESSAGE_WIFI_DISCOVER finish");
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
                //mBean.path = FileUtil.getPath(mContext, data.getData());
                mPath = FileUtil.getPath(mContext, data.getData());

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
                    //setServer(true);
                    mMyDevice.setServer(true);

                    mLooper = true;
                    mQRCode.setVisibility(View.VISIBLE);

                    WifiP2pQueueManager queueManager = new WifiP2pQueueManager(mManager, mChannel, new WifiP2pQueueManager.OnFinishListener() {
                        @Override
                        public void onFinish() {
                            LogUtils.e(TAG, "MESSAGE_WIFI_DISCOVER finish");
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
                    registerReceiver();
                    break;
                case DefaultValue.MESSAGE_WIFI_STATUS_CHANGED:
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
                    LogUtils.e(TAG, "handler,MESSAGE_WIFI_DISCOVER");

                    if (mLooper) {
                        //LogUtils.e(TAG, "handler,MESSAGE_WIFI_DISCOVER,loop");
                        Messenger.sendMessage(R.string.status_p2p_peer);

                        WifiP2pQueueManager queueManager = new WifiP2pQueueManager(mManager, mChannel, new WifiP2pQueueManager.OnFinishListener() {
                            @Override
                            public void onFinish() {
                                LogUtils.e(TAG, "MESSAGE_WIFI_DISCOVER finish");
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
                    if (mMyDevice.isOwner()) {
                        FileTransferGroupOwner task =
                                new FileTransferGroupOwner(mContext, DefaultValue.PORT_TRANSFER, mMyDevice, mPath, mTransferStatus);
                        task.start();
                    } else {
                        FileTransferGroupClient task =
                                new FileTransferGroupClient(mContext, DefaultValue.PORT_TRANSFER, mMyDevice, mPath, mTransferStatus);
                        task.start();
                    }
                    break;
                case DefaultValue.MESSAGE_TRANSFER_STATUS:
                    String showText = (String) msg.obj;
                    mTransferText.setText(showText);
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
    public void setMyDevice(String name, String mac) {
        mMyDevice.setName(name);
        mMyDevice.setMac(mac);
        mMyDeviceText.setText(getString(R.string.my_device_name, name));
    }

    //@Override
    public WifiP2pDeviceInfo getMyDevice() {
        return mMyDevice;
    }

    @Override
    public void setCustomDevice(WifiP2pDeviceInfo customDevice) {
        mCustomDevice = customDevice;
        mCustomDeviceText.setText(getString(R.string.custom_device_name, customDevice.getName()));
    }

    @Override
    public WifiP2pDeviceInfo getCustomDevice() {
        return mCustomDevice;
    }

    @Override
    public void setWifiState(int state) {
        mWifiState = state;
        mHandler.sendEmptyMessage(DefaultValue.MESSAGE_WIFI_STATUS_CHANGED);
    }

    @Override
    public boolean isWifiOpened() {
        return (mWifiState == WifiP2pManager.WIFI_P2P_STATE_ENABLED);
    }

    @Override
    public void connect(WifiP2pDevice device) {
        if (device.status == WifiP2pDevice.CONNECTED) {
            //showDialog(DIALOG_DISCONNECT);
            LogUtils.e(TAG, "p2p connected");
        } else if (device.status == WifiP2pDevice.INVITED) {
            //showDialog(DIALOG_CANCEL_CONNECT);
            LogUtils.e(TAG, "p2p invited");
        } else {
            LogUtils.e(TAG, "start p2p connect:" + device.deviceName);
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
        mLooper = false;
        mQRCode.setVisibility(View.INVISIBLE);

        if (networkInfo.isConnected()) {
            mManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {

                @Override
                public void onConnectionInfoAvailable(WifiP2pInfo info) {
                    LogUtils.e(TAG, "onConnectionInfoAvailable");

                    String ip = null;
                    if (info.groupFormed && info.isGroupOwner) {
                        //确定为组拥有者，创建线程用于接收连接请求
                        //提交图片下载、读取的异步任务
                        Messenger.sendMessage("Group Owner");
                        mMyDevice.setOwner(true);
                    } else if (info.groupFormed) {
                        //作为客户端，创建一个线程用于连接组拥有者
                        ip = info.groupOwnerAddress.getHostAddress();
                        Messenger.sendMessage("Group Client");
                        mMyDevice.setOwner(false);
                        mMyDevice.setPeerIp(ip);
                        //mCustomDevice.setIp(ip);
                    }

                    HeartBeatTask task = new HeartBeatTask(DefaultValue.PORT_HEART_BEAT, mMyDevice);
                    task.setOnCustomDevice(new HeartBeatTask.OnSetCustomDevice() {
                        @Override
                        public void onSet(WifiP2pDeviceInfo device) {
                            setCustomDevice(device);
                            //mCustomDevice = device;
                            LogUtils.e(TAG, "mCustomDevice=" + mCustomDevice.toString());
                            //sendMessage("peer device:" + mCustomDevice.toString());

                            Message msg = Message.obtain();
                            msg.what = DefaultValue.MESSAGE_TRANSFER_START;
                            //msg.obj = mCustomDevice;
                            mHandler.sendMessageDelayed(msg, 500);
                        }
                    });
                    task.start();
                }
            });
        }
    }
}
