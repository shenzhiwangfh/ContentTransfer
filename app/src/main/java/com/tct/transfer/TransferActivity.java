package com.tct.transfer;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tct.libzxing.zxing.activity.CaptureActivity;
import com.tct.libzxing.zxing.encoding.EncodingUtils;
import com.tct.transfer.adapter.BeanAdapter;
import com.tct.transfer.database.FileBeanHelper;
import com.tct.transfer.file.FileBean;
import com.tct.transfer.file.FileTransferGroupClient;
import com.tct.transfer.file.FileTransferGroupOwner;
import com.tct.transfer.util.DefaultValue;
import com.tct.transfer.util.FileSizeUtil;
import com.tct.transfer.util.FileUtil;
import com.tct.transfer.file.TransferStatus;
import com.tct.transfer.heartbeat.HeartBeatTask;
import com.tct.transfer.log.LogUtils;
import com.tct.transfer.permission.PermissionHelper;
import com.tct.transfer.permission.PermissionInterface;
import com.tct.transfer.permission.PermissionUtil;
import com.tct.transfer.queue.WifiP2pMessage;
import com.tct.transfer.queue.WifiP2pQueueManager;
import com.tct.transfer.util.MediaFileUtil;
import com.tct.transfer.util.Utils;
import com.tct.transfer.view.CircleBarView;
import com.tct.transfer.wifi.WifiP2PReceiver;
import com.tct.transfer.wifi.WifiP2pDeviceInfo;
import com.tct.transfer.wifi.WifiP2pInterface;
import com.tct.transfer.log.Messenger;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class TransferActivity extends AppCompatActivity implements
        View.OnClickListener,
        WifiP2pInterface,
        PermissionInterface, BeanAdapter.OnItemClickListener, BeanAdapter.OnItemLongClickListener {

    private final static String TAG = "TransferActivity";

    private Context mContext;
    private DecimalFormat df;

    private Button mShare;
    //private TextView mFileName;
    private Button mAccept;
    private ImageView mWifiStatus;

    private ViewGroup mDeviceBar;
    private TextView mMyDeviceText;
    private TextView mCustomDeviceText;
    private ImageView mMyDeviceAction;
    private ImageView mCustomDeviceAction;
    private TextView mTransferText;
    private CircleBarView mTransferBar;
    private ImageView mQRCode;
    private RecyclerView mRecyclerView;

    //private ScrollView mScroll;
    //private TextView mLog;
    private int mStatus = DefaultValue.STATUS_INIT;

    private WifiP2PReceiver mReceiver;
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private WifiP2pQueueManager mQueueManager;

    private PermissionHelper mPermissionHelper;

    private int mWifiState = WifiP2pManager.WIFI_P2P_STATE_DISABLED;
    private boolean mLooper = false;

    private WifiP2pDeviceInfo mCustomDevice = null;
    private WifiP2pDeviceInfo mMyDevice = new WifiP2pDeviceInfo();
    //private FileBean mBean;// = new FileBean();
    private String mPath;

    private BeanAdapter mAdapter;
    //private ArrayList<FileBean> mBeans = new ArrayList<>();
    ContentResolver mResolver;

    private TransferStatus mTransferStatus = new TransferStatus() {
        @Override
        public void sendStatus(FileBean bean) {
            if (bean.status == 0) {
                //bean.time = System.currentTimeMillis();
                mThread = new TransferThread(true);
                mThread.setBean(bean);
                mThread.start();
            } else if (bean.status == 1) {
                mThread.setBean(bean);
            } else if (bean.status == 2) {
                mThread.setBean(bean);
                mThread.setLoop(false);
            }
        }
    };

    private TransferThread mThread;
    private class TransferThread extends Thread {

        private ArrayList<FileBean> beans = new ArrayList<>();
        private boolean loop;

        public TransferThread(boolean loop) {
            this.loop = loop;
        }

        @Override
        public void run() {
            sendMessage();

            while (loop) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {

                }

                //if (loop) {
                sendMessage();
                //}
            }

            for (int i = 0; i < beans.size(); i++) {
                sendMessage();
            }
        }

        private synchronized void sendMessage() {
            if (beans.size() > 0) {
                FileBean bean = beans.get(0);
                sendMessage(bean);
                beans.remove(0);
            }
        }

        private void sendMessage(FileBean bean) {
            //if (beans.size() > 0) {
            Message msg = Message.obtain();
            msg.what = DefaultValue.MESSAGE_TRANSFER_STATUS;
            msg.obj = bean;//getString(R.string.transfer_start);
            mHandler.sendMessage(msg);

            //beans.remove(0);
            //}
        }

        public synchronized void setBean(FileBean bean) {
            //this.bean = bean;
            if (beans.size() == 0) {
                //beans.add(bean);
            } else {
                FileBean lastBean = beans.get(beans.size() - 1);
                if (bean.status > lastBean.status) {
                    //beans.add(bean);
                } else {
                    beans.remove(beans.size() - 1);
                    //beans.add(bean);
                }
            }
            beans.add(new FileBean(bean));
        }

        public void setLoop(boolean loop) {
            this.loop = loop;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.transfer);

        mContext = this;

        df = new DecimalFormat("00.0%");
        df.setMaximumFractionDigits(1);

        mPermissionHelper = new PermissionHelper(this, this);
        mPermissionHelper.requestPermissions();

        mShare = findViewById(R.id.share_file);
        mAccept = findViewById(R.id.accept_file);
        mWifiStatus = findViewById(R.id.wifi_status);

        mQRCode = findViewById(R.id.qr_big_code);
        mMyDeviceText = findViewById(R.id.my_device);
        mCustomDeviceText = findViewById(R.id.custom_device);
        mMyDeviceAction = findViewById(R.id.my_device_action);
        mCustomDeviceAction = findViewById(R.id.custom_device_action);
        mTransferText = findViewById(R.id.transfer_status);
        mDeviceBar = findViewById(R.id.device_bar);
        mTransferBar = findViewById(R.id.transfer_bar);
        mRecyclerView = findViewById(R.id.transfer_list);

        //Typeface typeface = null;
        //if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        //    typeface = getResources().getFont(R.font.myfont);
        //}
        //if(typeface != null) mTransferText.setTypeface(typeface);
        mTransferBar.setMaxNum(100);

        //mScroll = findViewById(R.id.log_scrollview);
        //mLog = findViewById(R.id.log);
        Messenger.init(mContext, mHandler, DefaultValue.MESSAGE_LOG);

        mShare.setOnClickListener(this);
        mAccept.setOnClickListener(this);
        mWifiStatus.setOnClickListener(this);

        // TODO 设置布局管理器,不然无法显示
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new BeanAdapter(mContext, null);
        mAdapter.setOnItemClickListener(this);
        mAdapter.setOnItemLongClickListener(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        mResolver = getContentResolver();

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);

        mReceiver = new WifiP2PReceiver(this);

        mQueueManager = WifiP2pQueueManager.init(mManager, mChannel);
        mQueueManager.reset();
        mQueueManager
                .sendMessage(new WifiP2pMessage(WifiP2pMessage.MESSAGE_REMOVE_GROUP, null))
                .sendMessage(new WifiP2pMessage(WifiP2pMessage.MESSAGE_CANCEL_CONNECT, null))
                .setOnFinishListener(new WifiP2pQueueManager.OnFinishListener() {
                    @Override
                    public void onFinish() {
                        mHandler.sendEmptyMessage(DefaultValue.MESSAGE_REGISTER);
                    }
                }).start();
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
                //Messenger.clearMessage();
                mTransferBar.reset();

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

                if(mStatus >= DefaultValue.STATUS_PEER && mStatus <= DefaultValue.STATUS_CONNECTED) {
                    mTransferBar.reset();
                    mQRCode.setVisibility(View.INVISIBLE);
                    mDeviceBar.setVisibility(View.VISIBLE);
                    mTransferBar.setVisibility(View.VISIBLE);
                    mShare.setEnabled(true);
                    mAccept.setText(R.string.accept_file);
                    mStatus = DefaultValue.STATUS_INIT;
                    //Messenger.reset();
                    mLooper = false;
                    keepScreenOn(mContext, false);
                    //Messenger.sendMessage(Messenger.LEVEL1, R.string.status_p2p_not_connected);
                    //Messenger.setStatus(mStatus);

                    //WifiP2pQueueManager queueManager = new WifiP2pQueueManager(mManager, mChannel, null);
                    mQueueManager.reset();
                    mQueueManager
                            .sendMessage(new WifiP2pMessage(WifiP2pMessage.MESSAGE_REMOVE_GROUP, null))
                            .sendMessage(new WifiP2pMessage(WifiP2pMessage.MESSAGE_CANCEL_CONNECT, null))
                            //.sendMessage(new WifiP2pMessage(WifiP2pMessage.MESSAGE_DISCOVER_PEERS, null))
                            /*
                            .setOnFinishListener(new WifiP2pQueueManager.OnFinishListener() {
                                @Override
                                public void onFinish() {
                                    Messenger.reset();
                                    Messenger.sendMessage(Messenger.LEVEL1, R.string.status_p2p_not_connected);
                                }
                            })
                            */
                            .start();
                } else {
                    mMyDevice.setServer(false);
                    //Messenger.clearMessage();

                    mTransferBar.reset();
                    mQRCode.setVisibility(View.VISIBLE);
                    mDeviceBar.setVisibility(View.INVISIBLE);
                    mTransferBar.setVisibility(View.INVISIBLE);
                    mShare.setEnabled(false);
                    mAccept.setText(R.string.cancel_recv);
                    mStatus = DefaultValue.STATUS_PEER;
                    Messenger.setStatus(mStatus);
                    keepScreenOn(mContext, true);

                    int height = mQRCode.getHeight();
                    int width = mQRCode.getWidth();
                    final int len = height > width ? width : height;
                    String shareInfo = mMyDevice.toString();//getMyDevice().toString();
                    Bitmap bmp = EncodingUtils.createQRCode(shareInfo, len, len, null);
                    mQRCode.setImageBitmap(bmp);
                    mLooper = true;

                    mQueueManager.reset();
                    mQueueManager
                            .sendMessage(new WifiP2pMessage(WifiP2pMessage.MESSAGE_REMOVE_GROUP, null))
                            .sendMessage(new WifiP2pMessage(WifiP2pMessage.MESSAGE_CANCEL_CONNECT, null))
                            //.sendMessage(new WifiP2pMessage(WifiP2pMessage.MESSAGE_DISCOVER_PEERS, null))
                            .setOnFinishListener(new WifiP2pQueueManager.OnFinishListener() {
                                @Override
                                public void onFinish() {
                                    LogUtils.e(TAG, "MESSAGE_WIFI_DISCOVER finish");
                                    mHandler.sendEmptyMessage(DefaultValue.MESSAGE_WIFI_DISCOVER);
                                }
                            }).start();
                }
            }
            break;
            case R.id.wifi_status: {
                try {
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                } catch (Exception e) {

                }
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
                    mShare.setEnabled(false);
                    mAccept.setText(R.string.cancel_send);
                    mStatus = DefaultValue.STATUS_PEER;
                    Messenger.setStatus(mStatus);

                    mLooper = true;
                    //mQRCode.setVisibility(View.VISIBLE);

                    mQueueManager.reset();
                    mQueueManager
                            .sendMessage(new WifiP2pMessage(WifiP2pMessage.MESSAGE_REMOVE_GROUP, null))
                            .sendMessage(new WifiP2pMessage(WifiP2pMessage.MESSAGE_CANCEL_CONNECT, null))
                            //.sendMessage(new WifiP2pMessage(WifiP2pMessage.MESSAGE_DISCOVER_PEERS, null))
                            .setOnFinishListener(new WifiP2pQueueManager.OnFinishListener() {
                                @Override
                                public void onFinish() {
                                    LogUtils.e(TAG, "MESSAGE_WIFI_DISCOVER finish");
                                    mHandler.sendEmptyMessage(DefaultValue.MESSAGE_WIFI_DISCOVER);
                                }
                            }).start();
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
                    mAccept.setEnabled(true);
                    mShare.setEnabled(true);
                    break;
                case DefaultValue.MESSAGE_WIFI_STATUS_CHANGED:
                    mWifiStatus.setBackgroundResource(isWifiOpened() ? R.drawable.top_button_disable : R.drawable.top_button_red);
                    mWifiStatus.setImageResource(isWifiOpened() ? R.drawable.ic_wifi_normal : R.drawable.ic_wifi_disable);
                    mShare.setEnabled(isWifiOpened());
                    mAccept.setEnabled(isWifiOpened());
                    mWifiStatus.setEnabled(!isWifiOpened());
                    if(!isWifiOpened()) Messenger.sendMessage(Messenger.LEVEL0, R.string.status_wifi_not_opened);
                    break;
                case DefaultValue.MESSAGE_WIFI_DISCOVER:
                    LogUtils.e(TAG, "handler,MESSAGE_WIFI_DISCOVER");

                    if (mLooper) {
                        //LogUtils.e(TAG, "handler,MESSAGE_WIFI_DISCOVER,loop");
                        Messenger.sendMessage(Messenger.LEVEL1, R.string.status_p2p_peer);

                        mQueueManager.reset();
                        mQueueManager
                                //.sendMessage(new WifiP2pMessage(WifiP2pMessage.MESSAGE_REMOVE_GROUP, null))
                                //.sendMessage(new WifiP2pMessage(WifiP2pMessage.MESSAGE_CANCEL_CONNECT, null))
                                .sendMessage(new WifiP2pMessage(WifiP2pMessage.MESSAGE_DISCOVER_PEERS, null))
                                .setOnFinishListener(new WifiP2pQueueManager.OnFinishListener() {
                                    @Override
                                    public void onFinish() {
                                        LogUtils.e(TAG, "MESSAGE_WIFI_DISCOVER finish");
                                        //registerReceiver();
                                        mHandler.sendEmptyMessageDelayed(DefaultValue.MESSAGE_WIFI_DISCOVER, 30 * 1000);
                                    }
                                })
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
                    FileBean bean = (FileBean) msg.obj;

                    //String text;
                    if (bean.status == 0) {
                        if (bean.action == 0) {
                            mMyDeviceAction.setImageResource(R.drawable.ic_upload);
                            mCustomDeviceAction.setImageResource(R.drawable.ic_download);
                        } else {
                            mMyDeviceAction.setImageResource(R.drawable.ic_download);
                            mCustomDeviceAction.setImageResource(R.drawable.ic_upload);
                        }
                        //mTransferText.setText(getString(R.string.transfer_start));
                        Messenger.sendMessage(Messenger.LEVEL2, R.string.transfer_start);
                    } else if (bean.status == 2) {
                        float percent = (bean.transferSize) / (float) bean.size;
                        String showText = df.format(percent) + " " + Utils.long2elapsed(mContext, bean.elapsed);
                        mTransferBar.setProgressNum(percent, 0, showText, bean);

                        //long elapsed = System.currentTimeMillis() - bean.time;
                        //mTransferText.setText(getString(R.string.transfer_end, Utils.long2time(mContext, elapsed)));
                        //mTransferText.setText(getString(R.string.transfer_end));
                        mShare.setEnabled(true);
                        mAccept.setText(R.string.accept_file);
                        mStatus = DefaultValue.STATUS_COMPLETE;
                        Messenger.setStatus(mStatus);
                        Messenger.sendMessage(Messenger.LEVEL3, bean.result == 0 ? R.string.transfer_end : R.string.transfer_error);

                        //0:picture, 1:video, 2:text, 3:audio, 4:other
                        if(MediaFileUtil.isImageFileType(bean.path)) {
                            bean.type = DefaultValue.TYPE_IMAGE;
                        } else if(MediaFileUtil.isVideoFileType(bean.path)) {
                            bean.type = DefaultValue.TYPE_VIDEO;
                        } else if(MediaFileUtil.isAudioFileType(bean.path)) {
                            bean.type = DefaultValue.TYPE_AUDIO;
                        } else {
                            bean.type = DefaultValue.TYPE_OTHER;
                        }

                        insertBean(bean);
                    } else {
                        float percent = (bean.transferSize) / (float) bean.size;
                        String showText = df.format(percent) + " " + Utils.long2elapsed(mContext, bean.elapsed);
                        mTransferBar.setProgressNum(percent, 0, showText, bean);

                        String transferSize = FileSizeUtil.FormetFileSize(bean.transferSize) + "/" + FileSizeUtil.FormetFileSize(bean.size);
                        //mTransferText.setText(transferSize);
                        Messenger.sendMessage(Messenger.LEVEL3, transferSize);
                    }
                    break;
                case DefaultValue.MESSAGE_SET_CUSTOM_DEVICE:
                    String device = (String) msg.obj;
                    mCustomDeviceText.setText(device);
                    break;
                case DefaultValue.MESSAGE_LOG:
                    //mLog.setText((String) msg.obj);
                    //mScroll.scrollTo(0, mLog.getMeasuredHeight());
                    mTransferText.setText((String) msg.obj);
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
        //mCustomDeviceText.setText(getString(R.string.custom_device_name, customDevice.getName()));

        Message msg = Message.obtain();
        msg.what = DefaultValue.MESSAGE_SET_CUSTOM_DEVICE;
        msg.obj = getString(R.string.custom_device_name, customDevice.getName());
        mHandler.sendMessage(msg);
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
        mStatus = DefaultValue.STATUS_CONNECTED;
        Messenger.setStatus(mStatus);
        Messenger.sendMessage(Messenger.LEVEL3, R.string.status_p2p_connect/*, matchedDevice.deviceName*/);

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

            mQueueManager.reset();
            mQueueManager.setConfig(config);
            mQueueManager
                    //.sendMessage(new WifiP2pMessage(WifiP2pMessage.MESSAGE_CREATE_GROUP, null))
                    .sendMessage(new WifiP2pMessage(WifiP2pMessage.MESSAGE_CONNECT, null))
                    .start();
        }
    }

    @Override
    public void requestConnect(NetworkInfo networkInfo) {
        if (networkInfo.isConnected()) {
            mLooper = false;
            mQRCode.setVisibility(View.INVISIBLE);
            mDeviceBar.setVisibility(View.VISIBLE);
            mTransferBar.setVisibility(View.VISIBLE);
            mShare.setEnabled(false);
            mAccept.setText(mMyDevice.isServer() ? R.string.cancel_send : R.string.cancel_recv);
            //mStatus = DefaultValue.STATUS_CONNECTED;
            //Messenger.setStatus(mStatus);
            keepScreenOn(mContext, false);

            mManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {

                @Override
                public void onConnectionInfoAvailable(WifiP2pInfo info) {
                    LogUtils.e(TAG, "onConnectionInfoAvailable");

                    String ip = null;
                    if (info.groupFormed && info.isGroupOwner) {
                        //确定为组拥有者，创建线程用于接收连接请求
                        //提交图片下载、读取的异步任务
                        //Messenger.sendMessage("Group Owner");
                        mMyDevice.setOwner(true);
                    } else if (info.groupFormed) {
                        //作为客户端，创建一个线程用于连接组拥有者
                        ip = info.groupOwnerAddress.getHostAddress();
                        //Messenger.sendMessage("Group Client");
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

    private PowerManager.WakeLock mWakeLock = null;

    private void keepScreenOn(Context context, boolean on) {
        if (on) {
            if (mWakeLock == null) {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE,
                        "TctTransfer_ScreenLock");
                mWakeLock.acquire();
                LogUtils.i(TAG, "TctTransfer_ScreenLock on");
            }
        } else {
            if ((mWakeLock != null) && mWakeLock.isHeld()) {
                mWakeLock.release();
                mWakeLock = null;
                LogUtils.i(TAG, "TctTransfer_ScreenLock off");
            }
        }
    }

    @Override
    public void onItemClick(FileBean bean) {
        LogUtils.e(TAG, "onItemClick=" + bean.toString());
    }

    @Override
    public void onItemLongClick(FileBean bean) {
        LogUtils.e(TAG, "onItemLongClick=" + bean.toString());
    }

    private void insertBean(FileBean bean) {
        ContentValues value = new ContentValues();
        value.put(FileBeanHelper.PATH, bean.path);
        value.put(FileBeanHelper.NAME, bean.name);
        value.put(FileBeanHelper.MD5, bean.md5);
        value.put(FileBeanHelper.SIZE, bean.size);
        value.put(FileBeanHelper.TRANSFER_SIZE, bean.transferSize);
        value.put(FileBeanHelper.TRANSFER_ACTION, bean.action);
        value.put(FileBeanHelper.TIME, bean.time);
        value.put(FileBeanHelper.ELAPSED, bean.elapsed);
        value.put(FileBeanHelper.TYPE, bean.type);
        value.put(FileBeanHelper.STATUS, bean.status);
        value.put(FileBeanHelper.RESULT, bean.result);
        mResolver.insert(DefaultValue.uri, value);
        mAdapter.changeBeans();
    }

    private void deleteBean() {

    }
}
