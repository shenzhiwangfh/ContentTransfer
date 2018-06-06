package com.tct.transfer;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tct.libzxing.zxing.activity.CaptureActivity;
import com.tct.libzxing.zxing.encoding.EncodingUtils;
import com.tct.transfer.permission.PermissionHelper;
import com.tct.transfer.permission.PermissionInterface;
import com.tct.transfer.permission.PermissionUtil;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, PermissionInterface {

    //private final static String TAG = "Transfer";

    private EditText mInput;
    private Button mEncode;
    private ImageView mQRCode;
    private Button mScan;
    private TextView mOutput;

    private int mQRCodeLen = DefaultValue.QR_CODE_LEN;
    //private final static int REQUEST_CODE = 1;

    private PermissionHelper mHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mInput = findViewById(R.id.input_content);
        mEncode = findViewById(R.id.encode);
        mQRCode = findViewById(R.id.qr_code);
        mScan = findViewById(R.id.scan);
        mOutput = findViewById(R.id.output_content);

        mEncode.setOnClickListener(this);
        mScan.setOnClickListener(this);

        mQRCodeLen = (int) getResources().getDimension(R.dimen.qr_code_len);

        mHelper = new PermissionHelper(this, this);
        mHelper.requestPermissions();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.encode:
                String content = mInput.getText().toString();
                if(content != null && !content.isEmpty()) {
                    Bitmap bmp = EncodingUtils.createQRCode(content, mQRCodeLen, mQRCodeLen, null);
                    mQRCode.setImageBitmap(bmp);
                }
                break;
            case R.id.scan:
                if (PermissionUtil.hasPermission(this, Manifest.permission.CAMERA)) {
                    Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
                    startActivityForResult(intent, 0);
                } else {
                    Toast.makeText(this, R.string.error_permission_camera, Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            Bundle bundle = data.getExtras();
            if(bundle != null) {
                String scanResult = bundle.getString("result");
                mOutput.setText(scanResult);
            }
        }
    }

    @Override
    public int getPermissionsRequestCode() {
        return 1;
    }

    @Override
    public String[] getPermissions() {
        return new String[]{Manifest.permission.CAMERA};
    }

    @Override
    public void requestPermissionsSuccess() {

    }

    @Override
    public void requestPermissionsFail() {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(mHelper.requestPermissionsResult(requestCode, permissions, grantResults)){
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
